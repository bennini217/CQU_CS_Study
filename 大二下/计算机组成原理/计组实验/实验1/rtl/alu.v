`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2022/04/15 16:51:53
// Design Name: 
// Module Name: alu
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


module alu(
    input wire [7:0] num1,
    input wire [7:0] num2,
    input wire [2:0] op,
    output wire zero,
    output wire [7:0] result 
    );    
    assign result = (op == 3'b010)? num1 + num2:
                    (op == 3'b110)? num1 - num2:
                    (op == 3'b000)? num1 & num2:
                    (op == 3'b001)? num1 | num2:
                    (op == 3'b100)? ~num1:
                    32'h00000000;
    assign zero = ((num1==num2)&&(op==3'b001)) ? 1 : 0;   
endmodule
