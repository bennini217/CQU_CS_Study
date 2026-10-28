`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2022/04/15 20:28:14
// Design Name: 
// Module Name: alu_sim
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////


module alu_sim(
    reg num1,
    reg op,
    wire s
    );
    alu test(
    .num1(num1),
    .op(op),
    .s(s)
    );
    initial begin
        #0   num1 = 8'b00000001;
             op = 000;
        #100 num2 = 8'b00000010;
             op = 001;
    end
endmodule
