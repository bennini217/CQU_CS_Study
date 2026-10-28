set(SRCS prt_gic_init.c prt_gicd_init.c prt_gicr_init.c)
add_library(hwi OBJECT ${SRCS})  # OBJECT类型只编译生成.o目标文件，但不实际链接成库