`timescale 1ns / 1ps
module sim_direct_cache ();
	
	reg		clk	;
	reg		rst	;

	parameter	WriteMiss		=	0;	// 写不命中，不用回写，从mem读一个块，填入cache，修改cache
	parameter	WriteHit		=	1;
	parameter	ReadMiss		=	2;
	parameter	ReadHit			=	3;	// 读不命中
	parameter	WriteDirtyMiss	=	4;	// 脏位 写不命中
	parameter	ReadDirtyMiss	=	5;	// 脏位 读不命中

	reg		[  3:  0]	curr_state, next_state;
	
	reg		[ 11:  0]	cpu_req_addr	;
	reg					cpu_req_rw		;
	reg					cpu_req_valid	;
	reg		[ 31:  0]	cpu_data_write	;
	wire	[ 31:  0]	cpu_data_read	;
	wire				cpu_ready		;
	
	wire	[ 11:  0]	mem_req_addr	;
	wire				mem_req_rw		;
	wire				mem_req_valid	;
	wire	[127:  0]	mem_data_write	;
	wire	[127:  0]	mem_data_read	;
	wire				mem_ready		;

	initial	begin
		clk = 0;
		rst = 0;
		#10
		rst = 1;
	end
	
	always #5 clk = ~clk;
	
	always @ (posedge clk or negedge rst)
		if (rst == 1'b0)
			curr_state	<=	WriteMiss;
		else
			curr_state	<=	next_state;
			
	always @ (*)
		//不同状态的处理逻辑
		case (curr_state)
			WriteMiss:
				if (cpu_ready == 1'b1)
					begin
						next_state		<=	WriteHit;
						cpu_req_valid	<=	1'b0;
					end
				else
					next_state	<=	WriteMiss;
			
			WriteHit:
				if (cpu_ready == 1'b1)
					begin
						next_state		<=	ReadMiss;
						cpu_req_valid	<=	1'b0;
					end
				else
					next_state	<=	WriteHit;
			
			ReadMiss:
				if (cpu_ready == 1'b1)
					begin
						next_state		<=	ReadHit;
						cpu_req_valid	<=	1'b0;
					end
				else
					next_state	<=	ReadMiss;
			
			ReadHit:
				if (cpu_ready == 1'b1)
					begin
						next_state		<=	WriteDirtyMiss;
						cpu_req_valid	<=	1'b0;
					end
				else
					next_state	<=	ReadHit;
			
			WriteDirtyMiss:
				if (cpu_ready == 1'b1)
					begin
						next_state		<=	ReadDirtyMiss;
						cpu_req_valid	<=	1'b0;
					end
				else
					next_state	<=	WriteDirtyMiss;
			
			ReadDirtyMiss:   begin		
				if (cpu_ready == 1'b1)
					cpu_req_valid	<=	1'b0;
				next_state	<=	ReadDirtyMiss;
			end
			
			default:
				next_state	<=	ReadDirtyMiss;
			
		endcase
	
	always @ (posedge clk or negedge rst)
		if (rst == 1'b0)
			begin
			// 初始化CPU请求和数据
				cpu_req_addr	<=	12'd0;
				cpu_req_rw		<=	1'b0;
				cpu_req_valid	<=	1'b0;
				cpu_data_write	<=	32'd0;
			end
		else
			case (curr_state)// 不同状态下的CPU请求操作
				WriteMiss:	// 写一个cache中没有的块，cache先从mem读对应的块到cache，然后在cache中修改这个快，置D为1
					begin
						cpu_req_addr	<=	12'd0;
						cpu_req_rw		<=	1'b1;
						cpu_data_write	<=	32'd8;
						cpu_req_valid	<=	1'b1;
					end
				
				WriteHit:	// 修改cacahe中有的块，在cache中修改这个块，置D为1
					begin
						cpu_req_addr	<=	12'd0;
						cpu_req_rw		<=	1'b1;
						cpu_data_write	<=	32'd9;
						cpu_req_valid	<=	1'b1;
					end
				
				ReadMiss:	// 读cache中没有的数据，cache会先从mem读一个块到cache，然后返回32bit数据到cpu
					begin
						cpu_req_addr	<=	12'd4;
						cpu_req_rw		<=	1'b0;
						cpu_data_write	<=	cpu_data_write;
						cpu_req_valid	<=	1'b1;
					end
				
				ReadHit:	// 读cache中有的块，直接返回32bit数据到cpu
					begin
						cpu_req_addr	<=	12'd0;
						cpu_req_rw		<=	1'b0;
						cpu_data_write	<=	cpu_data_write;
						cpu_req_valid	<=	1'b1;						
					end				
				
				WriteDirtyMiss:	// 给脏位cache中写数据，先保存cache块到mem，然后读一个块到mem，最后在cache中修改这个块，置D为1
					begin
						cpu_req_addr	<=	12'd128;	// 00001|00000|00 index=0000，当前cache中V=1 D=1 data=32'd9
						cpu_req_rw		<=	1'b1;
						cpu_data_write	<=	32'd6;
						cpu_req_valid	<=	1'b1;
					end
				
				ReadDirtyMiss:
					begin
						cpu_req_addr	<=	32'd0;
						cpu_req_rw		<=	1'b0;
						cpu_data_write	<=	cpu_data_write;
						cpu_req_valid	<=	1'b1;
					end
				
				default:
					begin
						cpu_req_addr	<=	12'd0;
						cpu_req_rw		<=	1'b0;
						cpu_data_write	<=	32'd8;
						cpu_req_valid	<=	1'b0;
					end
			endcase
	
	cache	cache_inst (
		.clk	(clk	),
		.rst	(rst	),
		
		// cpu <-> cache
		.cpu_req_addr	(cpu_req_addr	),
		.cpu_req_rw		(cpu_req_rw		),
		.cpu_req_valid	(cpu_req_valid	),
		.cpu_data_write	(cpu_data_write	),
		.cpu_data_read	(cpu_data_read	),
		.cpu_ready		(cpu_ready		),
		
		// cache <-> mem
		.mem_req_addr	(mem_req_addr	),
		.mem_req_rw		(mem_req_rw		),
		.mem_req_valid	(mem_req_valid	),
		.mem_data_write	(mem_data_write	),
		.mem_data_read	(mem_data_read	),
		.mem_ready		(mem_ready		)
	);
	
	mem		mem_inst (
		.clk	(clk	),
		.rst	(rst	),
	
		.mem_req_addr	(mem_req_addr	),
		.mem_req_rw		(mem_req_rw		),
		.mem_req_valid	(mem_req_valid	),
		.mem_data_write	(mem_data_write	),
		.mem_data_read	(mem_data_read	),
		.mem_ready		(mem_ready		)
	);

endmodule

