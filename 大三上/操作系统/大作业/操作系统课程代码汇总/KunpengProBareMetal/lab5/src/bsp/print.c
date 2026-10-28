#include <stdarg.h>
#include "prt_typedef.h"


#define UART_0_REG_BASE 0xC4010000 // pl011 设备寄存器地址
// 寄存器及其位定义参见：https://developer.arm.com/documentation/ddi0183/g/programmers-model/summary-of-registers
#define DW_UART_THR 0x00 // UARTDR(Data Register) 寄存器
#define DW_UART_FR 0x18  // UARTFR(Flag Register) 寄存器
#define DW_UART_LCR_HR 0x2c  // UARTLCR_H(Line Control Register) 寄存器
#define DW_XFIFO_NOT_FULL 0x020  // 发送缓冲区满置位
#define DW_FIFO_ENABLE 0x10 // 启用发送和接收FIFO

#define UART_BUSY_TIMEOUT   1000000
#define OS_MAX_SHOW_LEN 0x200


#define UART_REG_READ(addr)          (*(volatile U32 *)(((uintptr_t)addr)))  // 读设备寄存器
#define UART_REG_WRITE(value, addr)  (*(volatile U32 *)((uintptr_t)addr) = (U32)value) // 写设备寄存器

U32 PRT_UartInit(void)
{
    return OS_OK;
}

void uart_poll_send(unsigned char ch)
{
    volatile int time = 1000;
    *(unsigned long long int *)(0xC4010000) = ch;
    while(time--);
}

// 轮询的方式发送字符到串口，且转义换行符
void TryPutc(unsigned char ch)
{
    uart_poll_send(ch);
    if (ch == '\n') {
        uart_poll_send('\r');
    }
}

extern int vsnprintf_s(char *buff, int buff_size, int count, char const *fmt, va_list arg);
int TryPrintf(const char *format, va_list vaList)
{
    int len;
    char buff[OS_MAX_SHOW_LEN];
    for(int i = 0; i < OS_MAX_SHOW_LEN; i++) {
        buff[i] = 0;
    }
    char *str = buff;

    len = vsnprintf_s(buff, OS_MAX_SHOW_LEN, OS_MAX_SHOW_LEN, format, vaList);
    if (len == -1) {
        return len;
    }
    
    while (*str != '\0') {
        TryPutc(*str);
        str++;
    }

    return OS_OK;
}

U32 PRT_Printf(const char *format, ...)
{
    va_list vaList;
    S32 count;
    
    va_start(vaList, format);
    count = TryPrintf(format, vaList);
    va_end(vaList);

    return count;
}

