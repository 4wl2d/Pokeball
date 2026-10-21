// Exhaustive fault exploration of ballarch_issuer. For each operation (READ,
// CONFIG, FIRE) and each combination of fates of its first two bus attempts,
// a simulated device applies or refuses the request, answers in time, late or
// never, or stalls. The device's registers are the ground truth. The bench
// checks the Ballarch rules on every scenario and prints RESULT: PASS or
// RESULT: FAIL; the mutant builds in the Makefile must fail.
`timescale 1ns / 1ps
module tb;
    localparam int TIMEOUT = 12;
    localparam int MAX_ATTEMPTS = 2;

    localparam logic [1:0] OP_READ = 2'd0, OP_CONFIG = 2'd1, OP_FIRE = 2'd2;
    localparam logic [1:0] DONE = 2'd1, NOT_DONE = 2'd2, UNKNOWN = 2'd3;
    // Fates of one bus attempt.
    localparam int F_OK = 0;      // accepted, applied, answered in time
    localparam int F_LATE = 1;    // accepted, applied, answered after the deadline
    localparam int F_LOST = 2;    // accepted, applied, the answer is lost
    localparam int F_REFUSE = 3;  // accepted, refused without effect (error response)
    localparam int F_STALL = 4;   // never accepted
    localparam int FATES = 5;

    logic clk = 1'b0;
    logic rst_n = 1'b0;
    always #5 clk = ~clk;

    logic req_valid = 1'b0, req_ready;
    logic [1:0] req_op = OP_READ;
    logic [7:0] req_data = 8'h00;
    logic cmp_valid;
    logic [1:0] cmp_kind;
    logic [7:0] cmp_data;
    logic m_valid, m_ready = 1'b0;
    logic [1:0] m_op;
    logic [7:0] m_data;
    logic [3:0] m_id;
    logic s_valid = 1'b0, s_err = 1'b0;
    logic [3:0] s_id = 4'd0;
    logic [7:0] s_data = 8'd0;

    ballarch_issuer #(.TIMEOUT(TIMEOUT), .MAX_ATTEMPTS(MAX_ATTEMPTS)) dut (.*);

    // ---- simulated device and ground truth
    int cyc = 0;
    int fate[0:3];
    int attempt;           // bus attempts seen for the current command
    logic [3:0] last_id;
    int accepted;          // transactions the device accepted
    int fires;             // FIRE effects applied
    logic config_written;  // CONFIG effect applied
    logic [7:0] config_reg;
    int resp_at[0:3];
    logic [3:0] resp_id[0:3];
    logic resp_err[0:3];
    int completions;
    logic [1:0] kind;
    int failures = 0;
    int scenarios = 0;
    logic dispatched_early;

    function automatic int current_fate();
        return attempt >= 1 && attempt <= 4 ? fate[attempt - 1] : F_OK;
    endfunction

    // Drive device outputs between edges.
    always @(negedge clk) begin
        if (m_valid && m_id != last_id) begin
            attempt = attempt + 1;
            last_id = m_id;
        end
        m_ready <= m_valid && current_fate() != F_STALL;
        s_valid <= 1'b0;
        for (int i = 0; i < 4; i++) begin
            if (resp_at[i] == cyc) begin
                s_valid <= 1'b1;
                s_id <= resp_id[i];
                s_err <= resp_err[i];
                s_data <= config_reg;
                resp_at[i] = -1;
            end
        end
    end

    // Observe each edge: acceptance, effects, completions, and dispatch order.
    always @(posedge clk) begin
        cyc <= cyc + 1;
        if (rst_n) begin
            // R5: nothing leaves in the cycle whose edge commits the decision that requests it.
            if (req_valid && req_ready && m_valid) dispatched_early = 1'b1;
            if (m_valid && m_ready) begin
                accepted = accepted + 1;
                case (current_fate())
                    F_OK, F_LATE, F_LOST: begin
                        if (m_op == OP_FIRE) fires = fires + 1;
                        if (m_op == OP_CONFIG) begin
                            config_written = 1'b1;
                            config_reg = m_data;
                        end
                    end
                    default: ;
                endcase
                if (current_fate() != F_LOST) begin
                    // One response slot per attempt; attempts carry distinct tags.
                    resp_at[attempt - 1] = cyc + (current_fate() == F_LATE ? TIMEOUT + 4 : 2);
                    resp_id[attempt - 1] = m_id;
                    resp_err[attempt - 1] = current_fate() == F_REFUSE;
                end
            end
            if (cmp_valid) begin
                completions = completions + 1;
                kind = cmp_kind;
            end
        end
    end

    task automatic fail(input string what, input logic [1:0] op, input int f0, input int f1);
        failures = failures + 1;
        if (failures <= 5) $display("FAIL op=%0d fates=%0d,%0d: %s", op, f0, f1, what);
    endtask

    task automatic scenario(input logic [1:0] op, input int f0, input int f1);
        logic applied;
        fate[0] = f0;
        fate[1] = f1;
        fate[2] = F_OK;
        fate[3] = F_OK;
        attempt = 0;
        last_id = 4'hF;
        accepted = 0;
        fires = 0;
        config_written = 1'b0;
        config_reg = 8'h11;
        completions = 0;
        kind = 2'd0;
        dispatched_early = 1'b0;
        for (int i = 0; i < 4; i++) resp_at[i] = -1;
        rst_n = 1'b0;
        repeat (2) @(posedge clk);
        @(negedge clk) rst_n = 1'b1;
        // The request: held until the issuer accepts it (one edge).
        @(negedge clk) begin
            req_valid = 1'b1;
            req_op = op;
            req_data = 8'h5A;
        end
        @(posedge clk);
        @(negedge clk) req_valid = 1'b0;
        // Run well past every deadline and late answer.
        repeat (6 * TIMEOUT + 20) @(posedge clk);
        scenarios = scenarios + 1;

        applied = op == OP_FIRE ? fires > 0 : op == OP_CONFIG ? config_written : 1'b0;
        if (completions != 1) fail($sformatf("%0d completions for one command (R6)", completions), op, f0, f1);
        if (dispatched_early) fail("bus request in the cycle that accepted the command (R5)", op, f0, f1);
        if (op == OP_FIRE && accepted > 1) fail($sformatf("FIRE accepted %0d times (R8)", accepted), op, f0, f1);
        if (accepted > MAX_ATTEMPTS) fail($sformatf("%0d attempts", accepted), op, f0, f1);
        if (kind == NOT_DONE && applied) fail("NOT_DONE, but the device applied the request (R7)", op, f0, f1);
        if (kind == DONE && op != OP_READ && !applied) fail("DONE without effect (R7)", op, f0, f1);
        if (kind == UNKNOWN && op == OP_READ) fail("UNKNOWN for a Safe read (R7)", op, f0, f1);
    endtask

    initial begin
        for (int op = 0; op <= 2; op++)
            for (int f0 = 0; f0 < FATES; f0++)
                for (int f1 = 0; f1 < FATES; f1++)
                    scenario(op[1:0], f0, f1);
        if (failures == 0) $display("RESULT: PASS (%0d scenarios)", scenarios);
        else $display("RESULT: FAIL (%0d failed checks in %0d scenarios)", failures, scenarios);
        $finish;
    end
endmodule
