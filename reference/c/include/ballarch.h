/*
 * Ballarch Core 2 for firmware: the kernel types and a small runtime API in C11.
 *
 * One runtime executes one Ball instance (a firmware task that owns a piece of
 * device state). Decisions are pure functions; the runtime commits the new
 * state and the call ledger to non-volatile memory in one atomic record
 * (two slots, sequence number, CRC), transmits only after that commit, and
 * gives every call exactly one outcome chosen from its phase and its port's
 * effect class. The rules are the same as for the Kotlin reference runtime;
 * see spec/ballarch-core-2.0.md.
 *
 * Copyright (c) 2026 Vladislav Tomilov (4wl2d). See NOTICE.md at the
 * repository root: this code has no software license yet.
 */
#ifndef BALLARCH_H
#define BALLARCH_H

#include <stddef.h>
#include <stdint.h>

/* Bounds (rule R11). */
#define BA_MAX_STATE 32u      /* bytes of Ball state in the committed record */
#define BA_MAX_CALLS 4u       /* open calls per instance */
#define BA_MAX_OUTPUTS 4u     /* calls per decision */
#define BA_MAX_PORTS 8u

/* Port 0 is the built-in timer (Safe). A timer call's argument is its delay. */
#define BA_TIMER_PORT 0u
#define BA_TIMER_MARGIN_MS 1000u

typedef enum { BA_SAFE = 0, BA_IDEMPOTENT = 1, BA_NON_IDEMPOTENT = 2 } ba_effect;

typedef enum { BA_DONE = 1, BA_NOT_DONE = 2, BA_UNKNOWN = 3 } ba_kind;

/* Reasons carried by NotDone and Unknown outcomes. */
enum {
    BA_R_NEVER_SENT = 1,        /* NotDone: the deadline passed before the call was sent */
    BA_R_REFUSED = 2,           /* NotDone: the destination refused without applying */
    BA_R_NOT_APPLIED = 3,       /* NotDone: the adapter has evidence nothing was applied */
    BA_R_NO_ANSWER = 4,         /* NotDone: a Safe call got no answer */
    BA_R_DEADLINE_AFTER_SEND = 5,
    BA_R_LOST_AFTER_SEND = 6,
    BA_R_RECOVERED_AFTER_CRASH = 7,
    BA_R_RETRIES_EXHAUSTED = 8
};

/* The single terminal outcome of a call. value: the reply (Done) or a reason (otherwise). */
typedef struct {
    ba_kind kind;
    int32_t value;
} ba_outcome;

typedef struct {
    uint8_t port;
    uint32_t key;
    int32_t arg;
    uint32_t timeout_ms;
} ba_call;

typedef struct {
    uint8_t n;
    ba_call call[BA_MAX_OUTPUTS];
} ba_outputs;

typedef enum { BA_IN_REQUEST = 1, BA_IN_COMPLETION = 2 } ba_input_kind;

typedef struct {
    ba_input_kind kind;
    int32_t request; /* BA_IN_REQUEST */
    int32_t arg;
    uint8_t port; /* BA_IN_COMPLETION */
    uint32_t key;
    ba_outcome outcome;
} ba_input;

/* Trusted values for one decision. Keys come from ba_new_key, never from a clock or a random source. */
typedef struct {
    uint32_t now_ms;
    uint32_t revision; /* revision the decision commits as */
    uint8_t keys_issued;
} ba_ctx;

static inline uint32_t ba_new_key(ba_ctx *ctx) { return ctx->revision * 16u + ctx->keys_issued++; }

static inline ba_call ba_timer(uint32_t key, uint32_t delay_ms)
{
    ba_call c = {BA_TIMER_PORT, key, (int32_t)delay_ms, delay_ms + BA_TIMER_MARGIN_MS};
    return c;
}

static inline void ba_emit(ba_outputs *out, ba_call c)
{
    if (out->n < BA_MAX_OUTPUTS) out->call[out->n] = c;
    out->n++; /* more than BA_MAX_OUTPUTS is refused by the runtime's preflight */
}

typedef enum { BA_REJECT = 0, BA_ACCEPT = 1 } ba_verdict;

/*
 * A Ball type. step() must be pure (rule R2): it reads only its arguments and
 * writes only *next, *out and *reply. A completion cannot be refused, so for
 * completions step() must return BA_ACCEPT.
 */
typedef struct {
    const char *type;
    size_t state_size;
    uint8_t n_ports;          /* including the timer port */
    const ba_effect *effects; /* effects[port] */
    void (*initial)(void *state);
    ba_verdict (*step)(const void *state, const ba_input *in, ba_ctx *ctx, void *next, ba_outputs *out, int32_t *reply);
} ba_ball;

/* What an adapter reports for one attempt (exactly once, never after retrying by itself). */
typedef enum {
    BA_REPORT_OK = 1,          /* the destination processed the request; value is its answer */
    BA_REPORT_REFUSED = 2,     /* the destination refused without applying anything */
    BA_REPORT_NOT_APPLIED = 3, /* evidence that nothing was applied (e.g. NACK before the command was latched) */
    BA_REPORT_FAILED = 4       /* no usable answer after the request may have left */
} ba_report_kind;

/* Trace events for tests and diagnostics. */
enum { BA_T_CALL_COMMITTED = 1, BA_T_SENT = 2, BA_T_COMPLETED = 3, BA_T_LATE_REPORT = 4, BA_T_FAULT = 5 };

/* Services of the board (or of a simulator). */
typedef struct {
    void *user;
    /* Non-volatile memory with two record slots. A write may be cut short by power loss. */
    void (*nvm_write)(void *user, int slot, const void *data, size_t n);
    int (*nvm_read)(void *user, int slot, void *data, size_t n); /* 0 if the slot was never written */
    /* One attempt of a call on an adapter port (port >= 1). The adapter reports later with ba_report. */
    void (*transmit)(void *user, uint8_t port, uint32_t key, int32_t arg, uint8_t attempt);
    uint32_t (*now)(void *user);
    void (*trace)(void *user, int event, uint32_t key, const ba_outcome *outcome); /* may be NULL */
} ba_platform;

typedef enum { BA_PENDING = 1, BA_SENDING = 2 } ba_phase;

typedef struct {
    uint8_t used, port, phase, attempts;
    uint32_t key, deadline;
    int32_t arg;
} ba_call_rec;

/* Everything that survives a power loss. Written as a whole, to one slot, with a CRC. */
typedef struct {
    uint32_t magic, seq, revision;
    ba_call_rec calls[BA_MAX_CALLS];
    uint8_t state[BA_MAX_STATE];
    uint32_t crc;
} ba_record;

typedef struct {
    uint32_t key;
    uint8_t port;
    ba_outcome outcome;
} ba_resolution;

/* The runtime. Everything here except `rec` is volatile and lost at a power loss. */
typedef struct {
    const ba_ball *ball;
    const ba_platform *pf;
    const uint8_t *max_attempts; /* per port; only Safe and Idempotent ports retry */
    ba_record rec;
    ba_resolution queue[BA_MAX_CALLS]; /* resolved, not yet committed by the Ball */
    uint8_t queued;
    uint8_t current_attempt[BA_MAX_CALLS];
    int quarantined;
} ba_runtime;

/* Recover from NVM (or start fresh) and apply the restart rules of Core §3.7. */
void ba_boot(ba_runtime *rt, const ba_ball *ball, const ba_platform *pf, const uint8_t *max_attempts);

/* Decide an external request. Returns 1 if accepted and committed, 0 if rejected, -1 if refused by a bound or fault. */
int ba_request(ba_runtime *rt, int32_t request, int32_t arg, int32_t *reply);

/* An adapter's report for an attempt. Reports for closed calls or old attempts are dropped. */
void ba_report(ba_runtime *rt, uint32_t key, uint8_t attempt, ba_report_kind kind, int32_t value);

/* Fire due timers and deadlines, then let the Ball commit pending completions. */
void ba_tick(ba_runtime *rt);

/* Earliest time at which ba_tick has work, or UINT32_MAX. */
uint32_t ba_next_wakeup(const ba_runtime *rt);

/* Read-only view of the committed state (rule R12). */
const void *ba_state(const ba_runtime *rt);

uint32_t ba_crc32(const void *data, size_t n);

#endif
