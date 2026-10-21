/*
 * The Ballarch Core 2 firmware runtime. It enforces the runtime rules for one
 * Ball instance: serial steps (R3: one call into the Ball at a time, from the
 * main loop), atomic commit (R4: one NVM record write), commit before dispatch
 * (R5), one completion per call (R6), outcomes by phase and effect class (R7),
 * no re-send of non-idempotent calls and write-ahead before every send (R8),
 * and bounds (R11).
 *
 * Seeded bugs for the negative controls in test/ (never enabled in a build):
 *   BA_MUTANT_NO_WRITE_AHEAD    transmits before recording the call as Sending
 *   BA_MUTANT_RESEND_AFTER_CRASH re-sends a possibly-sent non-idempotent call at boot
 */
#include "ballarch.h"

#include <string.h>

#define BA_MAGIC 0xBA11A2C4u

uint32_t ba_crc32(const void *data, size_t n)
{
    const uint8_t *p = data;
    uint32_t crc = 0xFFFFFFFFu;
    for (size_t i = 0; i < n; i++) {
        crc ^= p[i];
        for (int b = 0; b < 8; b++) crc = (crc >> 1) ^ (0xEDB88320u & (0u - (crc & 1u)));
    }
    return ~crc;
}

static void trace(const ba_runtime *rt, int event, uint32_t key, const ba_outcome *o)
{
    if (rt->pf->trace) rt->pf->trace(rt->pf->user, event, key, o);
}

static uint32_t now(const ba_runtime *rt) { return rt->pf->now(rt->pf->user); }

static ba_outcome outcome(ba_kind kind, int32_t value)
{
    ba_outcome o = {kind, value};
    return o;
}

static ba_effect effect_of(const ba_runtime *rt, uint8_t port)
{
    return port == BA_TIMER_PORT ? BA_SAFE : rt->ball->effects[port];
}

static uint8_t attempts_allowed(const ba_runtime *rt, uint8_t port)
{
    if (effect_of(rt, port) == BA_NON_IDEMPOTENT || rt->max_attempts == NULL || rt->max_attempts[port] == 0) return 1;
    return rt->max_attempts[port];
}

/* The atomic commit point: the whole record goes to the slot after the current one. */
static void persist(ba_runtime *rt)
{
    rt->rec.magic = BA_MAGIC;
    rt->rec.seq++;
    rt->rec.crc = ba_crc32(&rt->rec, offsetof(ba_record, crc));
    rt->pf->nvm_write(rt->pf->user, (int)(rt->rec.seq & 1u), &rt->rec, sizeof rt->rec);
}

static int valid(const ba_record *r) { return r->magic == BA_MAGIC && r->crc == ba_crc32(r, offsetof(ba_record, crc)); }

static int load(ba_runtime *rt)
{
    ba_record a, b;
    int va = rt->pf->nvm_read(rt->pf->user, 0, &a, sizeof a) && valid(&a);
    int vb = rt->pf->nvm_read(rt->pf->user, 1, &b, sizeof b) && valid(&b);
    if (va && (!vb || a.seq > b.seq)) rt->rec = a;
    else if (vb) rt->rec = b;
    else return 0;
    return 1;
}

static int slot_of(const ba_runtime *rt, uint32_t key)
{
    for (unsigned i = 0; i < BA_MAX_CALLS; i++)
        if (rt->rec.calls[i].used && rt->rec.calls[i].key == key) return (int)i;
    return -1;
}

static int is_queued(const ba_runtime *rt, uint32_t key)
{
    for (unsigned i = 0; i < rt->queued; i++)
        if (rt->queue[i].key == key) return 1;
    return 0;
}

/* Chooses the single outcome of a call; the Ball commits it later in drain() (R6). */
static void resolve(ba_runtime *rt, int i, ba_outcome o)
{
    const ba_call_rec *c = &rt->rec.calls[i];
    if (is_queued(rt, c->key) || rt->queued >= BA_MAX_CALLS) return;
    ba_resolution r = {c->key, c->port, o};
    rt->queue[rt->queued++] = r;
    rt->current_attempt[i] = 0;
}

static void send(ba_runtime *rt, int i)
{
    ba_call_rec *c = &rt->rec.calls[i];
    if (c->port == BA_TIMER_PORT) return; /* timers are kept by the runtime itself */
    c->phase = BA_SENDING;
    c->attempts++;
#ifdef BA_MUTANT_NO_WRITE_AHEAD
    rt->current_attempt[i] = c->attempts;
    trace(rt, BA_T_SENT, c->key, NULL);
    rt->pf->transmit(rt->pf->user, c->port, c->key, c->arg, c->attempts);
    persist(rt);
#else
    persist(rt); /* write-ahead: NVM says "possibly sent" before anything leaves (R8) */
    rt->current_attempt[i] = c->attempts;
    trace(rt, BA_T_SENT, c->key, NULL);
    rt->pf->transmit(rt->pf->user, c->port, c->key, c->arg, c->attempts);
#endif
}

static void fail_attempt(ba_runtime *rt, int i)
{
    const ba_call_rec *c = &rt->rec.calls[i];
    ba_effect e = effect_of(rt, c->port);
    if (e != BA_NON_IDEMPOTENT && c->attempts < attempts_allowed(rt, c->port) && now(rt) < c->deadline) {
        send(rt, i);
        return;
    }
    if (e == BA_SAFE) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NO_ANSWER));
    else resolve(rt, i, outcome(BA_UNKNOWN, c->attempts > 1 ? BA_R_RETRIES_EXHAUSTED : BA_R_LOST_AFTER_SEND));
}

static void quarantine(ba_runtime *rt, uint32_t key)
{
    rt->quarantined = 1;
    trace(rt, BA_T_FAULT, key, NULL);
}

/* Preflight (Core §3.5): bounds, known ports, unique keys, timers made with ba_timer. */
static int preflight(const ba_runtime *rt, const ba_outputs *out, int closing)
{
    if (out->n > BA_MAX_OUTPUTS) return 0;
    unsigned open = 0;
    for (unsigned i = 0; i < BA_MAX_CALLS; i++)
        if (rt->rec.calls[i].used && (int)i != closing) open++;
    if (open + out->n > BA_MAX_CALLS) return 0;
    for (unsigned k = 0; k < out->n; k++) {
        const ba_call *c = &out->call[k];
        if (c->port >= rt->ball->n_ports || c->timeout_ms == 0) return 0;
        if (c->port == BA_TIMER_PORT && c->timeout_ms != (uint32_t)c->arg + BA_TIMER_MARGIN_MS) return 0;
        int existing = slot_of(rt, c->key);
        if (existing >= 0 && existing != closing) return 0;
        for (unsigned m = 0; m < k; m++)
            if (out->call[m].key == c->key) return 0;
    }
    return 1;
}

/*
 * One step: run the Ball, check its decision, commit state and ledger in one
 * record write, then dispatch. Returns 1 (committed), 0 (rejected) or -1 (refused or fault).
 */
static int step(ba_runtime *rt, const ba_input *in, int closing, int32_t *reply)
{
    uint8_t next[BA_MAX_STATE];
    ba_outputs out;
    int32_t rep = 0;
    memset(next, 0, sizeof next);
    memset(&out, 0, sizeof out);
    ba_ctx ctx = {now(rt), rt->rec.revision + 1u, 0};
    ba_verdict v = rt->ball->step(rt->rec.state, in, &ctx, next, &out, &rep);
    if (reply) *reply = rep;
    int completion = in->kind == BA_IN_COMPLETION;
    if (v == BA_REJECT && !completion) return 0;
    if (v == BA_REJECT || !preflight(rt, &out, closing)) {
        if (completion) quarantine(rt, in->key); /* an obligation the instance holds cannot be refused */
        return -1;
    }
    memcpy(rt->rec.state, next, rt->ball->state_size);
    rt->rec.revision++;
    if (closing >= 0) {
        memset(&rt->rec.calls[closing], 0, sizeof rt->rec.calls[closing]);
        rt->current_attempt[closing] = 0;
    }
    int fresh[BA_MAX_OUTPUTS];
    uint32_t t = ctx.now_ms;
    for (unsigned k = 0; k < out.n; k++) {
        unsigned j = 0;
        while (rt->rec.calls[j].used) j++;
        ba_call_rec c = {1, out.call[k].port, BA_PENDING, 0, out.call[k].key, t + out.call[k].timeout_ms, out.call[k].arg};
        rt->rec.calls[j] = c;
        fresh[k] = (int)j;
    }
    persist(rt); /* the commit point (R4) */
    if (completion) trace(rt, BA_T_COMPLETED, in->key, &in->outcome);
    for (unsigned k = 0; k < out.n; k++) trace(rt, BA_T_CALL_COMMITTED, out.call[k].key, NULL);
    for (unsigned k = 0; k < out.n; k++) send(rt, fresh[k]); /* dispatch strictly after the commit (R5) */
    return 1;
}

static void drain(ba_runtime *rt)
{
    while (rt->queued > 0 && !rt->quarantined) {
        ba_resolution r = rt->queue[0];
        rt->queued--;
        memmove(&rt->queue[0], &rt->queue[1], rt->queued * sizeof rt->queue[0]);
        int i = slot_of(rt, r.key);
        if (i < 0) continue;
        ba_input in = {BA_IN_COMPLETION, 0, 0, r.port, r.key, r.outcome};
        step(rt, &in, i, NULL);
    }
}

void ba_boot(ba_runtime *rt, const ba_ball *ball, const ba_platform *pf, const uint8_t *max_attempts)
{
    memset(rt, 0, sizeof *rt);
    rt->ball = ball;
    rt->pf = pf;
    rt->max_attempts = max_attempts;
    if (!load(rt)) {
        memset(&rt->rec, 0, sizeof rt->rec);
        ball->initial(rt->rec.state);
        return;
    }
    uint32_t t = now(rt);
    for (unsigned u = 0; u < BA_MAX_CALLS; u++) {
        int i = (int)u;
        const ba_call_rec *c = &rt->rec.calls[i];
        if (!c->used) continue;
        ba_effect e = effect_of(rt, c->port);
        if (c->port == BA_TIMER_PORT) { /* re-armed to its due time unless its deadline has passed */
            if (t >= c->deadline) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NO_ANSWER));
        } else if (t >= c->deadline) {
            if (c->phase == BA_PENDING) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NEVER_SENT));
            else if (e == BA_SAFE) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NO_ANSWER));
            else resolve(rt, i, outcome(BA_UNKNOWN, BA_R_DEADLINE_AFTER_SEND));
        } else if (c->phase == BA_PENDING) {
            send(rt, i);
        } else if (e == BA_NON_IDEMPOTENT) {
#ifdef BA_MUTANT_RESEND_AFTER_CRASH
            send(rt, i);
#else
            resolve(rt, i, outcome(BA_UNKNOWN, BA_R_RECOVERED_AFTER_CRASH));
#endif
        } else if (c->attempts < attempts_allowed(rt, c->port)) {
            send(rt, i);
        } else {
            resolve(rt, i, e == BA_SAFE ? outcome(BA_NOT_DONE, BA_R_NO_ANSWER) : outcome(BA_UNKNOWN, BA_R_RETRIES_EXHAUSTED));
        }
    }
    drain(rt);
}

int ba_request(ba_runtime *rt, int32_t request, int32_t arg, int32_t *reply)
{
    if (rt->quarantined) return -1;
    ba_input in = {BA_IN_REQUEST, request, arg, 0, 0, {BA_DONE, 0}};
    int r = step(rt, &in, -1, reply);
    drain(rt);
    return r;
}

void ba_report(ba_runtime *rt, uint32_t key, uint8_t attempt, ba_report_kind kind, int32_t value)
{
    int i = slot_of(rt, key);
    if (rt->quarantined || i < 0 || is_queued(rt, key) || rt->rec.calls[i].phase != BA_SENDING) {
        trace(rt, BA_T_LATE_REPORT, key, NULL);
        return;
    }
    const ba_call_rec *c = &rt->rec.calls[i];
    int current = rt->current_attempt[i] == attempt;
    /* Evidence of non-application covers only its attempt: conclusive if no other attempt can have been delivered. */
    int conclusive = c->attempts == 1 || effect_of(rt, c->port) == BA_SAFE;
    switch (kind) {
    case BA_REPORT_OK:
        resolve(rt, i, outcome(BA_DONE, value));
        break;
    case BA_REPORT_REFUSED:
    case BA_REPORT_NOT_APPLIED:
        if (conclusive) resolve(rt, i, outcome(BA_NOT_DONE, kind == BA_REPORT_REFUSED ? BA_R_REFUSED : BA_R_NOT_APPLIED));
        else if (current) fail_attempt(rt, i);
        break;
    case BA_REPORT_FAILED:
        if (current) fail_attempt(rt, i);
        break;
    }
    drain(rt);
}

void ba_tick(ba_runtime *rt)
{
    if (rt->quarantined) return;
    uint32_t t = now(rt);
    for (unsigned u = 0; u < BA_MAX_CALLS; u++) {
        int i = (int)u;
        const ba_call_rec *c = &rt->rec.calls[i];
        if (!c->used || is_queued(rt, c->key)) continue;
        if (c->port == BA_TIMER_PORT) {
            if (t + BA_TIMER_MARGIN_MS >= c->deadline) resolve(rt, i, outcome(BA_DONE, 0));
        } else if (t >= c->deadline) {
            if (c->phase == BA_PENDING) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NEVER_SENT));
            else if (effect_of(rt, c->port) == BA_SAFE) resolve(rt, i, outcome(BA_NOT_DONE, BA_R_NO_ANSWER));
            else resolve(rt, i, outcome(BA_UNKNOWN, BA_R_DEADLINE_AFTER_SEND));
        }
    }
    drain(rt);
}

uint32_t ba_next_wakeup(const ba_runtime *rt)
{
    if (rt->quarantined) return UINT32_MAX;
    if (rt->queued > 0) return now(rt);
    uint32_t next = UINT32_MAX;
    for (unsigned i = 0; i < BA_MAX_CALLS; i++) {
        const ba_call_rec *c = &rt->rec.calls[i];
        if (!c->used) continue;
        uint32_t at = c->port == BA_TIMER_PORT ? c->deadline - BA_TIMER_MARGIN_MS : c->deadline;
        if (at < next) next = at;
    }
    return next;
}

const void *ba_state(const ba_runtime *rt) { return rt->rec.state; }
