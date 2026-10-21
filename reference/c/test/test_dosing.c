/*
 * Exhaustive fault exploration of the therapy controller on the firmware
 * runtime: every link fate for every transmission, and one power loss at any
 * point (between loop iterations, before or in the middle of an NVM write,
 * after the write-ahead record and before the bytes leave). The simulated pump
 * is the ground truth. Exit status 1 means a rule or the therapy's safety
 * property was violated; the mutant builds in the Makefile must fail.
 */
#include "ballarch.h"
#include "dosing.h"

#include <setjmp.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#define PRESCRIBED 2
#define CUT_POINTS 40      /* power loss before one of the first CUT_POINTS points, or never */
#ifndef MAX_CHOICES
#define MAX_CHOICES 9 /* later choices take value 0 (answer in time), which keeps runs finite */
#endif
#define END_MS 30000u
#define LATENCY_MS 40u
#define REBOOT_MS 250u

/* ---------------------------------------------------------------- exhaustive chooser */

typedef struct {
    int prefix[MAX_CHOICES], arity[MAX_CHOICES];
    int len, pos;
} chooser;

static int choose(chooser *c, int n)
{
    if (c->pos >= MAX_CHOICES) return 0;
    if (c->pos < c->len) {
        if (c->arity[c->pos] != n) {
            fprintf(stderr, "nondeterministic replay at choice %d\n", c->pos);
            return 0;
        }
        return c->prefix[c->pos++];
    }
    c->prefix[c->len] = 0;
    c->arity[c->len] = n;
    c->len++;
    c->pos++;
    return 0;
}

static int next_run(chooser *c)
{
    if (c->len > c->pos) c->len = c->pos;
    while (c->len > 0 && c->prefix[c->len - 1] + 1 >= c->arity[c->len - 1]) c->len--;
    if (c->len == 0) return 0;
    c->prefix[c->len - 1]++;
    c->pos = 0;
    return 1;
}

/* ---------------------------------------------------------------- the simulated world */

enum { F_REPLY, F_REPLY_LATE, F_LOSE_REQUEST, F_LOSE_REPLY, F_ERROR_AFTER_APPLY, F_NACK, DOSE_FATES };
enum { C_REPLY, C_LOSE, COUNTER_FATES };

typedef struct {
    uint32_t at, key;
    uint8_t attempt, kind;
    int32_t value;
    int incarnation, live;
} report_event;

typedef struct {
    uint32_t key;
    int committed, completed, transmissions, applied;
    uint8_t port;
    ba_outcome outcome;
} call_log;

typedef struct {
    chooser *ch;
    uint32_t clock;
    ba_record slot[2];
    int written[2];
    int ops, cut_at, incarnation;
    jmp_buf on_cut;
    report_event ev[32];
    int n_ev;
    int doses; /* ground truth: doses the pump really gave */
    call_log calls[64];
    int n_calls;
    int violations;
    char first[256];
} world;

static world W;

static void violation(const char *fmt, ...)
{
    if (W.violations++ == 0) {
        va_list ap;
        va_start(ap, fmt);
        vsnprintf(W.first, sizeof W.first, fmt, ap);
        va_end(ap);
    }
}

static call_log *log_of(uint32_t key)
{
    for (int i = 0; i < W.n_calls; i++)
        if (W.calls[i].key == key) return &W.calls[i];
    call_log fresh = {key, 0, 0, 0, 0, 0, {BA_DONE, 0}};
    if (W.n_calls == (int)(sizeof W.calls / sizeof W.calls[0])) {
        violation("more calls than the test can log");
        return &W.calls[W.n_calls - 1];
    }
    W.calls[W.n_calls] = fresh;
    return &W.calls[W.n_calls++];
}

/* A possible power-loss point. */
static void point(void)
{
    if (W.ops++ == W.cut_at) longjmp(W.on_cut, 1);
}

static void nvm_write(void *u, int slot, const void *data, size_t n)
{
    (void)u;
    point(); /* lost before the write: the slot is unchanged */
    if (W.ops++ == W.cut_at) { /* lost during the write: the slot is torn */
        memcpy(&W.slot[slot], data, n / 2);
        ((uint8_t *)&W.slot[slot])[n / 2] ^= 0xFF;
        W.written[slot] = 1;
        longjmp(W.on_cut, 1);
    }
    memcpy(&W.slot[slot], data, n);
    W.written[slot] = 1;
}

static int nvm_read(void *u, int slot, void *data, size_t n)
{
    (void)u;
    if (!W.written[slot]) return 0;
    memcpy(data, &W.slot[slot], n);
    return 1;
}

static uint32_t now(void *u)
{
    (void)u;
    return W.clock;
}

/* The newest valid record in NVM, as the next boot would see it. */
static const ba_record *durable(void)
{
    const ba_record *best = NULL;
    for (int s = 0; s < 2; s++) {
        const ba_record *r = &W.slot[s];
        if (!W.written[s] || r->crc != ba_crc32(r, offsetof(ba_record, crc))) continue;
        if (best == NULL || r->seq > best->seq) best = r;
    }
    return best;
}

static void schedule(uint32_t delay, uint32_t key, uint8_t attempt, ba_report_kind kind, int32_t value)
{
    report_event e = {W.clock + delay, key, attempt, (uint8_t)kind, value, W.incarnation, 1};
    W.ev[W.n_ev++] = e;
}

static void transmit(void *u, uint8_t port, uint32_t key, int32_t arg, uint8_t attempt)
{
    (void)u;
    (void)arg;
    point(); /* lost after the write-ahead record, before the bytes leave */
    /* R5 and R8: the durable record already says that this call may have been sent. */
    const ba_record *r = durable();
    int recorded = 0;
    for (unsigned i = 0; r && i < BA_MAX_CALLS; i++)
        if (r->calls[i].used && r->calls[i].key == key && r->calls[i].phase == BA_SENDING) recorded = 1;
    if (!recorded) violation("call %u transmitted before NVM recorded it as Sending", key);
    call_log *l = log_of(key);
    l->transmissions++;
    l->port = port;
    if (port == TH_PORT_DOSE) {
        if (l->transmissions > 1) violation("dose call %u transmitted %d times", key, l->transmissions);
        switch (choose(W.ch, DOSE_FATES)) {
        case F_REPLY: W.doses++; l->applied = 1; schedule(LATENCY_MS, key, attempt, BA_REPORT_OK, W.doses); break;
        case F_REPLY_LATE: W.doses++; l->applied = 1; schedule(TH_CALL_TIMEOUT_MS + LATENCY_MS, key, attempt, BA_REPORT_OK, W.doses); break;
        case F_LOSE_REQUEST: break;
        case F_LOSE_REPLY: W.doses++; l->applied = 1; break;
        case F_ERROR_AFTER_APPLY: W.doses++; l->applied = 1; schedule(LATENCY_MS, key, attempt, BA_REPORT_FAILED, 0); break;
        case F_NACK: schedule(LATENCY_MS, key, attempt, BA_REPORT_NOT_APPLIED, 0); break;
        }
    } else if (choose(W.ch, COUNTER_FATES) == C_REPLY) {
        schedule(LATENCY_MS, key, attempt, BA_REPORT_OK, W.doses);
    }
}

static void trace(void *u, int event, uint32_t key, const ba_outcome *o)
{
    (void)u;
    call_log *l = log_of(key);
    if (event == BA_T_CALL_COMMITTED) l->committed++;
    if (event == BA_T_COMPLETED) {
        l->completed++;
        l->outcome = *o;
        if (l->completed > 1) violation("call %u completed twice", key);
        if (!l->committed) violation("completion for call %u, which was never committed", key);
    }
    if (event == BA_T_FAULT) violation("Ball quarantined at call %u", key);
}

static const ba_platform platform = {NULL, nvm_write, nvm_read, transmit, now, trace};

static uint32_t next_event(void)
{
    uint32_t t = UINT32_MAX;
    for (int i = 0; i < W.n_ev; i++)
        if (W.ev[i].live && W.ev[i].at < t) t = W.ev[i].at;
    return t;
}

/* ---------------------------------------------------------------- one run */

typedef struct {
    long runs, done, alarm, idle, cuts;
} totals;

static void run(chooser *ch, totals *t)
{
    memset(&W, 0, sizeof W);
    W.ch = ch;
    W.clock = 1000;
    W.cut_at = choose(ch, CUT_POINTS + 1);
    if (W.cut_at == CUT_POINTS) W.cut_at = -1;
    static ba_runtime rt; /* static: survives longjmp without volatile qualifiers */
    volatile int powered_before = 0;
    if (setjmp(W.on_cut)) { /* power lost: RAM is gone; the controller restarts later */
        W.incarnation++;
        W.cut_at = -1;
        W.clock += REBOOT_MS;
        t->cuts++;
    }
    ba_boot(&rt, &th_ball, &platform, NULL);
    if (!powered_before) {
        powered_before = 1;
        int32_t reply = 0;
        ba_request(&rt, TH_REQ_START, PRESCRIBED, &reply); /* the nurse presses start once */
    }
    for (;;) {
        uint32_t a = ba_next_wakeup(&rt), b = next_event();
        uint32_t at = a < b ? a : b;
        if (at == UINT32_MAX || at > END_MS) break;
        if (at > W.clock) W.clock = at;
        point();
        for (int i = 0; i < W.n_ev; i++) {
            report_event *e = &W.ev[i];
            if (!e->live || e->at > W.clock) continue;
            e->live = 0;
            if (e->incarnation == W.incarnation) ba_report(&rt, e->key, e->attempt, (ba_report_kind)e->kind, e->value);
        }
        ba_tick(&rt);
    }

    /* Oracles at quiescence. */
    const th_state *s = ba_state(&rt);
    if (W.doses > PRESCRIBED) violation("overdose: the pump gave %d doses", W.doses);
    for (int i = 0; i < W.n_calls; i++) {
        const call_log *l = &W.calls[i];
        if (l->committed && !l->completed) violation("call %u never completed", l->key);
        /* R7 against the pump's ground truth: NotDone only without effect, Done only with it. */
        if (l->completed && l->outcome.kind == BA_NOT_DONE && l->applied) violation("call %u: NotDone, but the pump applied it", l->key);
        if (l->completed && l->outcome.kind == BA_DONE && l->port == TH_PORT_DOSE && !l->applied) violation("call %u: Done, but the pump never applied it", l->key);
    }
    switch ((th_stage)s->stage) {
    case TH_DONE:
        t->done++;
        if (W.doses != PRESCRIBED) violation("therapy done, but the pump gave %d doses", W.doses);
        break;
    case TH_ALARM:
        t->alarm++;
        if (s->delivered > W.doses) violation("believes %d doses given; the pump gave %d", s->delivered, W.doses);
        break;
    case TH_IDLE:
        t->idle++;
        if (W.doses != 0) violation("idle, but the pump gave %d doses", W.doses);
        break;
    default:
        violation("unfinished in stage %d", s->stage);
    }
    t->runs++;
}

/* ---------------------------------------------------------------- decider unit test */

static int unit_tests(void)
{
    th_state s, n;
    th_initial(&s);
    ba_outputs out;
    memset(&out, 0, sizeof out);
    ba_ctx ctx = {0, 1, 0};
    int32_t reply = 0;
    ba_input start = {BA_IN_REQUEST, TH_REQ_START, 2, 0, 0, {BA_DONE, 0}};
    int failures = 0;
    if (th_step(&s, &start, &ctx, &n, &out, &reply) != BA_ACCEPT || out.n != 1 || out.call[0].port != TH_PORT_DOSE) failures++;
    /* An unknown dose leads to waiting for the horizon, never to another dose. */
    th_state dosing = n, after;
    memset(&out, 0, sizeof out);
    ba_ctx ctx2 = {500, 2, 0};
    ba_input unknown = {BA_IN_COMPLETION, 0, 0, TH_PORT_DOSE, dosing.call, {BA_UNKNOWN, BA_R_LOST_AFTER_SEND}};
    th_step(&dosing, &unknown, &ctx2, &after, &out, &reply);
    if (after.stage != TH_SETTLING || out.n != 1 || out.call[0].port != BA_TIMER_PORT) failures++;
    if (failures) printf("decider unit tests: %d failed\n", failures);
    return failures;
}

int main(void)
{
    int unit_failures = unit_tests();
    chooser ch;
    memset(&ch, 0, sizeof ch);
    totals t = {0, 0, 0, 0, 0};
    int bad = 0;
    char first[256] = "";
    do { /* stops at the first counterexample, like a model checker */
        run(&ch, &t);
        if (W.violations) {
            bad++;
            snprintf(first, sizeof first, "%s", W.first);
        }
    } while (!bad && next_run(&ch));
    printf("dosing exploration: %ld runs (%ld with a power loss); final stage done=%ld alarm=%ld idle=%ld\n",
           t.runs, t.cuts, t.done, t.alarm, t.idle);
    if (bad) printf("counterexample in run %ld: %s\n", t.runs, first);
    else printf("no violation in any run\n");
    return bad || unit_failures ? 1 : 0;
}
