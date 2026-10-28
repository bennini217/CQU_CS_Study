`timescale 1ns / 1ps

module mem (
	input	wire				clk,
	input	wire				rst,
	
	input	wire	[ 11:  0]	mem_req_addr	,//表示内存请求的地址
	input	wire				mem_req_rw		,//表示内存请求的读/写操作,1写0读
	input	wire				mem_req_valid	,
	input	wire	[127:  0]	mem_data_write	,//写入内存的数据
	output	reg 	[127:  0]	mem_data_read	,//从内存中读取的数据
	output	reg 				mem_ready
);

	reg	[31: 0]	mem	[0:4095];
	
	integer	i;
	//  初始化mem
	initial	begin
		for (i = 0; i < 4096; i = i +1)
			mem[i]	=	32'd0;
	end

	always @ (posedge clk or negedge rst)
		if (rst == 1'b0)
			mem_ready	<=	1'b0;
		else	if (mem_req_valid && mem_req_rw == 1'b1 && mem_ready == 1'b0)	// 有写mem请求，但是还没写好
			begin
				mem[mem_req_addr + 3]	<=	mem_data_write[127: 96];
				mem[mem_req_addr + 2]	<=	mem_data_write[ 95: 64];
				mem[mem_req_addr + 1]	<=	mem_data_write[ 63: 32];
				mem[mem_req_addr    ]	<=	mem_data_write[ 31:  0];
				mem_ready	<=1'b1;
			end
		else	if (mem_req_valid && mem_req_rw == 1'b0 && mem_ready == 1'b0)	// 有读mem请求，但是还没读好
			begin
				mem_data_read	<=	{mem[mem_req_addr + 3], 
									 mem[mem_req_addr + 2],
									 mem[mem_req_addr + 1],
									 mem[mem_req_addr    ] };
				mem_ready	<=	1'b1;
			end
		else	if (mem_req_valid && mem_ready == 1'b1)
			begin
				mem_ready	<=	1'b0;
			end

endmodule
