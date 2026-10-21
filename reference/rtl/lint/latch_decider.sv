// Negative control for the R2 lint: the next state is not assigned on every
// path, so synthesis infers a latch, which is state that no clock edge commits.
module latch_decider (
    input  logic clk,
    input  logic go,
    output logic busy
);
    logic busy_q, busy_d;
    always_comb begin
        if (go) busy_d = 1'b1;  // no default: busy_d keeps its old value when !go
    end
    always_ff @(posedge clk) busy_q <= busy_d;
    assign busy = busy_q;
endmodule
