// 直接相联cache  direct mapped cache
// cache大小为32块，主存大小为1024块，1块=4字，1字=32bit
// 主存地址为12bit，其中[1:0]是块内偏移，[6:2]是索引，[11:7]是tag
// V+D+Tag+Data = 1+1+5+128 = 135 即是有效位（表示Cache行中的数据是否有效）+脏位（如果数据被修改（即写入新值），那么在将数据写回主存之前，这个位会被设置为1。它也占用1位）
//+标签（存储主存中块的地址）+数据

//计算行数：行数 = Cache大小 / 块大小 = 32块 / 1块 = 32行。
//计算offset（偏移量）：offset是用来在一个块中定位一个特定的字，因为每个块有4个字，所以我们需要2位来表示这些字（因为2^2 = 4），所以offset为2位。
//计算index（索引）：index是用来在Cache中定位一个特定的行，因为我们有32行，所以我们需要5位来表示这些行（因为2^5 = 32），所以index为5位。
//计算tag（标签）：tag是用来在一个特定的行中定位一个特定的块，剩下的位数就是tag，所以tag为12 - 5 - 2 = 5位。

//	【 5bit | 5bit | 2bit】
//      tag   index  offset

module cache (
	input	wire				clk,
	input	wire				rst,
	
	// cpu <-> cache
	input	wire	[ 11:  0]	cpu_req_addr	,//CPU 请求地址
	input	wire				cpu_req_rw		,//读写标志
	input	wire				cpu_req_valid	,//请求有效信号
	input	wire	[ 31:  0]	cpu_data_write	,//写入数据 
	output	reg 	[ 31:  0]	cpu_data_read	,//从缓存读出的数据 
	output	reg 				cpu_ready		,//缓存是否就绪的信号
	
	// cache <-> mem
	output	reg		[ 11:  0]	mem_req_addr	,//缓存发起的内存请求地址
	output	reg					mem_req_rw		,//读写标志 
	output	reg					mem_req_valid	,//请求有效信号
	output	reg		[127:  0]	mem_data_write	,
	input	wire	[127:  0]	mem_data_read	,//从内存读出的数据 
	input	wire				mem_ready//内存是否就绪的信号
	
);

	//
	parameter	V		=	134	;//表示有效位的位置
	parameter	D		=	133	;//表示脏位的位置
	parameter	TagMSB	=	132	;
	parameter	TagLSB	=	128	;//分别表示标签字段（Tag）的最高位和最低位的索引，用于从地址中提取标签。
	parameter	BlockMSB	=	127	;
	parameter	BlockLSB	=	0	;//分别表示缓存块（Block）的最高位和最低位的索引，用于从缓存数据中提取块数据。

	// state
	parameter	IDLE		=	2'd0;//表示空闲状态，即没有进行任何操作。
	parameter	CompareTag	=	2'd1;//表示比较标签的状态，用于判断缓存是否命中。
	parameter	Allocate	=	2'd2;//表示分配状态，用于从主存中读取数据到缓存中。
	parameter	WriteBack	=	2'd3;//表示写回状态，用于将缓存中的数据写回主存。
	
	reg	[134:0]	cache_data	[0:31];//定义了一个二维数组，用于存储缓存中的数据，每个缓存行对应一个数组元素。
	
	reg	[1:0]	curr_state,	next_state;//定义了当前状态和下一状态的寄存器。
	
	reg	hit;//用于存储缓存是否命中的信息。


	wire	[4:0]	cpu_req_tag;
	wire	[4:0]	cpu_req_index;
	wire	[1:0]	cpu_req_offset;
	
	assign	cpu_req_tag		=	cpu_req_addr[11: 7];
	assign	cpu_req_index	=	cpu_req_addr[ 6: 2];
	assign	cpu_req_offset	=	cpu_req_addr[ 1: 0];
	//分别用于从 CPU 请求地址中提取标签、索引和偏移。
	integer	i;
	// 初始化cache，遍历缓存中的所有行，每行的数据初始化为全0。
	initial	begin
		for (i = 0; i < 32; i = i + 1)
			cache_data[i] = 135'b0;//cache_data 是缓存数据的存储数组，cache_data[i] 表示第 i 行的缓存数据。
	end
	
	// 每一个时钟沿产生一次可能的状态变化，会在时钟的上升沿或复位信号变化时执行。如果复位信号为低电平，则将当前状态 curr_state 置为 IDLE；否则，将当前状态置为下一个状态 next_state。
	always @ (posedge clk or negedge rst)
		if (rst == 1'b0)
			curr_state	<=	IDLE;
		else
			curr_state	<=	next_state;
	
// 产生下一状态的组合逻辑，根据当前状态 curr_state 生成下一个状态 next_state。根据当前状态的不同，会有不同的状态转移逻辑：
//在 IDLE 状态下，如果有 CPU 的读写请求，则转移到 CompareTag 状态，否则保持在 IDLE 状态。
//在 CompareTag 状态下，如果缓存命中，则转移到 IDLE 状态；否则，根据缓存块的有效位和脏位决定是转移到 WriteBack 状态还是 Allocate 状态。
//在 Allocate 状态下，等待主存返回读有效信号，若得到则转移到 CompareTag 状态，否则继续保持在 Allocate 状态。
//在 WriteBack 状态下，等待主存返回写有效信号，若得到则转移到 Allocate 状态，否则继续保持在 WriteBack 状态。
//默认情况下，保持在 IDLE 状态。
	always @ (*)
		case (curr_state)
			IDLE:
				if (cpu_req_valid)		// CPU发起读写请求
					next_state	<=	CompareTag;
				else
					next_state	<=	IDLE;
			
			CompareTag:
				if (hit)
					next_state	<=	IDLE;		// hit
				else	if (cache_data[cpu_req_index][V:D] == 2'b11)
					next_state	<=	WriteBack;	// cache块有效并且脏位为1，需要将块写回mem
				else
					next_state	<=	Allocate;	// 需要从mem读数据更新cache
			
			Allocate:
				if (mem_ready)
					next_state	<=	CompareTag;	// 等待mem返回读有效信号
				else
					next_state	<=	Allocate;
			
			WriteBack:
				if (mem_ready)
					next_state	<=	Allocate;	// 等待mem返回写有效信号
				else
					next_state	<=	WriteBack;
			
			default:next_state	<=	IDLE;
		endcase

	// 根据tag判断cache是否命中
	always @ (*)
		if (curr_state == CompareTag)
			if (cache_data[cpu_req_index][134] == 1'b1 && cache_data[cpu_req_index][TagMSB:TagLSB] == cpu_req_tag)
			//表示从缓存中选择特定的缓存行，其中 cpu_req_index 是根据请求地址计算出的缓存索引，用于确定是哪一行缓存。
			//表示从缓存数据中取出标记位（Tag）的范围，这个范围是通过参数 TagMSB 和 TagLSB 定义的。标记位是用来与请求地址中的标记部分进行比较，以确定缓存是否命中。
				hit	<=	1'b1;
			else
				hit	<=	1'b0;
				//是一个寄存器，用于表示缓存是否命中。如果提取出来的标记位与请求地址中的标记部分相匹配，并且对应的缓存行有效（最高位为1），则认为命中，将 hit 置为 1；否则，将 hit 置为 0。
				
	// 各个状态下对mem的读写操作     
	always @ (posedge clk) 
		if (curr_state ==Allocate)			// 读mem
			//表示从内存中读取数据到缓存中。如果内存准备就绪（mem_ready == 1'b0），则发出读取内存的请求，
			//地址为 cpu_req_addr 的高 10 位（cpu_req_addr[11:2]）与 00 拼接，以确保按照缓存块大小（Block）对齐访问。
			//如果内存读取完毕，将 mem_req_valid 置为 0，同时将读取到的数据写入缓存中对应的位置，并将该缓存块标记为有效（V = 10），
			//同时将标记位更新为请求地址的标记部分。
			if (mem_ready == 1'b0)		// 还没从mem读出一个块大小的数据，保持读mem请求
				begin
					mem_req_valid	<=	1'b1;
					mem_req_addr	<=	{cpu_req_addr[11:2], 2'b00};
					mem_req_rw		<=	1'b0;
				end
			else						// mem返回读有效，拉低请求，更新cache对应块
				begin
					mem_req_valid	<=	1'b0;
					cache_data[cpu_req_index][BlockMSB:BlockLSB]	<=	mem_data_read;
					cache_data[cpu_req_index][V:D]					<=	2'b10;
					cache_data[cpu_req_index][TagMSB:TagLSB]		<=	cpu_req_tag;
				end
		else	if (curr_state == WriteBack)	// 写mem
		//表示将缓存中的数据写回内存。如果内存准备就绪（mem_ready == 1'b0），
		//则发出写回内存的请求，地址由缓存中的标记位和索引位组成，同时将写回的数据填入 mem_data_write 中。
		//当内存写入完成后，将 mem_req_valid 置为 0。
			if (mem_ready == 1'b0)		// 还没写好，保持写mem请求
				begin
					mem_req_valid	<=	1'b1;
					mem_req_addr	<=	{cache_data[cpu_req_index][TagMSB:TagLSB], cpu_req_index, 2'b00};
					mem_req_rw		<=	1'b1;
					mem_data_write	<=	cache_data[cpu_req_index][BlockMSB:BlockLSB];
				end
			else						// mem返回写有效，拉低请求
				begin
					mem_req_valid	<=	1'b0;
				end
		else
			mem_req_valid	<=	1'b0;
	
	// cache命中后的读写操作
	//如果在 CompareTag 状态下检测到缓存命中，并且命中的是读操作（cpu_req_rw == 1'b0），则将 cpu_ready 置为 1，
	//同时将缓存中对应位置的数据输出到 cpu_data_read 中；
	//如果是写操作（cpu_req_rw == 1'b1），则将 cpu_ready 置为 1，并将数据写入到缓存中对应位置，同时将该缓存块的脏位（D）置为 1。
	//如果缓存未命中或者不是读操作，则将 cpu_ready 置为 0。
	always @ (posedge clk)
		if (curr_state == CompareTag && hit)
			if (cpu_req_rw == 1'b0)		// read hit
				begin
					cpu_ready	<=	1'b1;
					cpu_data_read	<=	cache_data[cpu_req_index][cpu_req_offset*32 +:32];
				end
			else						// write hit
				begin
					cpu_ready	<=	1'b1;
					cache_data[cpu_req_index][cpu_req_offset*32 +:32]	<=	cpu_data_write;
					cache_data[cpu_req_index][D]	<=	1'b1;
				end
		else
			cpu_ready	<=	1'b0;

endmodule
