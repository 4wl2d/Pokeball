// Negative control for the R1 lint: two processes write the same state.
module two_owners (
    input  logic clk,
    input  logic a,
    input  logic b,
    output logic flag
);
    logic flag_q;
    always_ff @(posedge clk) if (a) flag_q <= 1'b1;
    always_ff @(posedge clk) if (b) flag_q <= 1'b0;
    assign flag = flag_q;
endmodule
