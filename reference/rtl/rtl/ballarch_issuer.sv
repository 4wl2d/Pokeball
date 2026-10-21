`timescale 1ns / 1ps

// Ballarch Core 2 in register-transfer logic: a command issuer that owns one
// obligation at a time.
//
// The block is a Ball. Its state lives in registers written by exactly one
// always_ff (R1, single owner). Its decider is one always_comb block with a
// default for every next-state signal, so it holds no hidden state (R2: a
// latch would be state no clock edge commits). The clock edge commits the
// whole next state at once (R4), and every output is a function of
// registered state only, so nothing is dispatched before the decision that
// requests it is committed (R5).
//
// Downstream it issues transactions on a bus with a request channel
// (m_valid/m_ready) and id-tagged responses (s_valid/s_id). On this bus the
// master may withdraw a request that was not yet accepted, so the send
// horizon is the m_valid && m_ready handshake. (On AXI, where VALID may not
// be withdrawn, the send horizon is the assertion of VALID.)
//
// Every accepted command gets exactly one completion (R6), whose kind is
// chosen from the phase and the operation's effect class (R7):
//   first attempt withdrawn before acceptance   -> NOT_DONE (provably not applied)
//   retry withdrawn, CONFIG                     -> UNKNOWN (an earlier attempt may have applied)
//   response OK                                 -> DONE
//   response ERR on the only attempt            -> NOT_DONE (the device refused)
//   no response by the deadline, READ (Safe)    -> retry, then NOT_DONE
//   no response, CONFIG (Idempotent)            -> retry with a fresh tag, then UNKNOWN
//   no response, FIRE (NonIdempotent)           -> UNKNOWN, never re-issued (R8)
// Responses carrying an old tag are dropped. Registers do not survive reset:
// this block implements the Transient profile (R13).
//
// Seeded bugs for the negative controls (never defined in a build):
//   MUTANT_RETRY_FIRE        re-issues FIRE after a timeout
//   MUTANT_TIMEOUT_NOT_DONE  reports NOT_DONE when an accepted request times out
//   MUTANT_COMB_DISPATCH     drives m_valid combinationally from the upstream request
module ballarch_issuer #(
    parameter int TIMEOUT = 12,      // cycles a phase may last before its deadline
    parameter int MAX_ATTEMPTS = 2   // for READ and CONFIG
) (
    input  logic       clk,
    input  logic       rst_n,
    // Upstream: the request (inbox) and its completion.
    input  logic       req_valid,
    output logic       req_ready,
    input  logic [1:0] req_op,
    input  logic [7:0] req_data,
    output logic       cmp_valid,
    output logic [1:0] cmp_kind,
    output logic [7:0] cmp_data,
    // Downstream bus.
    output logic       m_valid,
    input  logic       m_ready,
    output logic [1:0] m_op,
    output logic [7:0] m_data,
    output logic [3:0] m_id,
    input  logic       s_valid,
    input  logic [3:0] s_id,
    input  logic       s_err,
    input  logic [7:0] s_data
);
    // Operations and their effect classes. 2'd1 is CONFIG (Idempotent): writing
    // the same configuration twice leaves the device as writing it once.
    localparam logic [1:0] OP_READ = 2'd0;    // Safe
    localparam logic [1:0] OP_FIRE = 2'd2;    // NonIdempotent

    localparam logic [1:0] DONE = 2'd1;
    localparam logic [1:0] NOT_DONE = 2'd2;
    localparam logic [1:0] UNKNOWN = 2'd3;

    typedef enum logic [1:0] {IDLE, PENDING, SENT, REPORT} stage_t;

    // ---- committed state (one owner) and its next value
    stage_t stage_q, stage_d;
    logic [1:0] op_q, op_d, kind_q, kind_d;
    logic [7:0] data_q, data_d, rdata_q, rdata_d;
    logic [3:0] id_q, id_d;
    logic [1:0] attempts_q, attempts_d;
    logic [4:0] timer_q, timer_d;

    // ---- the decider: next state from committed state and inputs, nothing else
    always_comb begin
        stage_d = stage_q;
        op_d = op_q;
        kind_d = kind_q;
        data_d = data_q;
        rdata_d = rdata_q;
        id_d = id_q;
        attempts_d = attempts_q;
        timer_d = timer_q;
        case (stage_q)
            IDLE: if (req_valid) begin
                stage_d = PENDING;
                op_d = req_op;
                data_d = req_data;
                attempts_d = 2'd1;
                id_d = id_q + 4'd1;
                timer_d = 5'(TIMEOUT);
            end
            PENDING: if (m_ready) begin             // handshake: the send horizon is crossed
                stage_d = SENT;
                timer_d = 5'(TIMEOUT);
            end else if (timer_q == 5'd0) begin     // withdrawn before acceptance
                stage_d = REPORT;
                // Provably not applied only if no earlier attempt was accepted, or if READ has no effect.
                kind_d = (attempts_q == 2'd1 || op_q == OP_READ) ? NOT_DONE : UNKNOWN;
            end else begin
                timer_d = timer_q - 5'd1;
            end
            SENT: if (s_valid && s_id == id_q) begin
                stage_d = REPORT;
                rdata_d = s_data;
                // A refusal is evidence only for its own attempt.
                kind_d = !s_err ? DONE : (attempts_q == 2'd1 || op_q == OP_READ) ? NOT_DONE : UNKNOWN;
            end else if (timer_q == 5'd0) begin
`ifdef MUTANT_RETRY_FIRE
                if (attempts_q < 2'(MAX_ATTEMPTS)) begin
`else
                if (op_q != OP_FIRE && attempts_q < 2'(MAX_ATTEMPTS)) begin
`endif
                    stage_d = PENDING;              // runtime-owned retry with a fresh tag
                    attempts_d = attempts_q + 2'd1;
                    id_d = id_q + 4'd1;
                    timer_d = 5'(TIMEOUT);
                end else begin
                    stage_d = REPORT;
`ifdef MUTANT_TIMEOUT_NOT_DONE
                    kind_d = NOT_DONE;
`else
                    kind_d = op_q == OP_READ ? NOT_DONE : UNKNOWN;
`endif
                end
            end else begin
                timer_d = timer_q - 5'd1;
            end
            REPORT: stage_d = IDLE;
        endcase
    end

    // ---- the commit: every register at the same edge
    always_ff @(posedge clk or negedge rst_n) begin
        if (!rst_n) begin
            stage_q <= IDLE;
            op_q <= OP_READ;
            kind_q <= DONE;
            data_q <= 8'd0;
            rdata_q <= 8'd0;
            id_q <= 4'd0;
            attempts_q <= 2'd0;
            timer_q <= 5'd0;
        end else begin
            stage_q <= stage_d;
            op_q <= op_d;
            kind_q <= kind_d;
            data_q <= data_d;
            rdata_q <= rdata_d;
            id_q <= id_d;
            attempts_q <= attempts_d;
            timer_q <= timer_d;
        end
    end

    // ---- dispatch: outputs depend on committed state only
    assign req_ready = stage_q == IDLE;
`ifdef MUTANT_COMB_DISPATCH
    assign m_valid = stage_q == PENDING || (stage_q == IDLE && req_valid);
`else
    assign m_valid = stage_q == PENDING;
`endif
    assign m_op = op_q;
    assign m_data = data_q;
    assign m_id = id_q;
    assign cmp_valid = stage_q == REPORT;
    assign cmp_kind = kind_q;
    assign cmp_data = rdata_q;
endmodule
