`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2017/11/07 13:50:53
// Design Name: 
// Module Name: top
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


module top(
	input wire clk,rst
    );
	wire[31:0] pc,instr,readdata,alu_out,writedata;
   	wire instr_ram_ean,data_ram_ena;
   	wire [3:0] data_ram_wea;
   	
	mips mips(
	.clk(clk),
	.rst(rst),
	.pc(pc),
	.instr(instr),
	.memwrite(data_ram_wea),
	.aluout(alu_out),
	.writedata(writedata),
	.readdata(readdata)
	);
	
	instr_ram instr_ram (
    .clka(!clk),    // input wire clka
    .ena(1'b1),      // input wire ena //instr_ram_ean
    .wea(4'b0000),      // input wire [3 : 0] wea
    .addra(pc[7:0]),  // input wire [7 : 0] addra
    .dina(32'b0),    // input wire [31 : 0] dina
    .douta(instr)  // output wire [31 : 0] douta
    );
    data_ram data_ram (
    .clka(!clk),    // input wire clka
    .ena(1),      // input wire ena //data_ram_ena
    .wea(data_ram_wea),      // input wire [3 : 0] wea
    .addra(alu_out),  // input wire [9 : 0] addra
    .dina(writedata),    // input wire [31 : 0] dina     
    .douta(readdata)  // output wire [31 : 0] douta
     );
endmodule
