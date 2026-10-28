`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2017/11/07 10:58:03
// Design Name: 
// Module Name: mips
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


module mips(
	input wire clk,rst,
	input wire[31:0] instr,
	input wire[31:0] readdata,
	output wire memwrite,
	output wire[31:0] pc,aluout,writedata
    );
    
	wire memtoreg,alusrc,regdst,regwrite,jump,pcsrc,branch;
	wire[2:0] alucontrol;

	controller controller(
	.inst(instr),
    .jump(jump),
    .branch(branch),
    .alusrc(alusrc),
    .memwrite(memwrite),
    .memtoreg(memtoreg),
    .regwrite(regwrite),
    .regdst(regdst),
    .alucontrol(alucontrol)
	);
	datapath datapath(
	.clka(clk),
	.rst(rst),
	.jump(jump),
	.branch(branch),
	.alusrc(alusrc),
	.memtoreg(memtoreg),
	.regwrite(regwrite),
	.regdst(regdst),
    .alucontrol(alucontrol),
    .aluout(aluout),
    .pc(pc),
    .writedata(writedata),
    .instr(instr),
    .readdata(readdata)
	);
	
endmodule
