#include <stdarg.h>
#include "pl011.h"
#include "cpu_config.h"
#include "prt_gic_external.h"
#include "prt_task.h"
#include "prt_sem.h"
#include "prt_shell.h"

#define OS_MAX_SHOW_LEN 0x200

SemHandle sem_uart_rx;

#define WRITE_UINT32(val, addr) (*((volatile U32 *)(addr)) = (val))

static inline void UartSetBaudrate()
{
    U32 baudRate;
    U32 divider;
    U32 remainder;
    U32 fraction;

    baudRate  = PL011_CLK_DIV * CONSOLE_UART_BAUDRATE;
    divider   = UART_CLK_INPUT / baudRate;
    remainder = UART_CLK_INPUT % baudRate;
    baudRate  = (PL011_NUM_8 * remainder) / CONSOLE_UART_BAUDRATE;
    fraction  = (baudRate >> 1) + (baudRate & 1);

    UART_REG(UART_IBRD) = divider;
    UART_REG(UART_FBRD) = fraction;
}

void PRT_UartInit(void)
{
    WRITE_UINT32(0, GPIO_UTXD2_ADDR);
    WRITE_UINT32(0, GPIO_URXD2_ADDR);
    /* First, disable everything */
    UART_REG(UART_CR) = 0;

    /* set Scale factor of baud rate */
    UartSetBaudrate();

    /* Set the UART to be 8 bits, 1 stop bit, no parity, fifo enabled. */
    UART_REG(UART_LCR_H) = UART_LCR_H_8_BIT | UART_LCR_H_FIFO_EN | (1<<1) | (0<<3);

    /* enable the UART */
    UART_REG(UART_CR) = UART_CR_EN | UART_CR_TX_EN | UART_CR_RX_EN;
}

U32 PRT_UartInterruptInit(void)
{
    UART_REG(UART_ILPR) = 16;
    UART_REG(UART_IFLS) = UART_INT_HALF;
    UART_REG(UART_IMSC) |= UART_RXRIS | UART_RTRIS;

    OsGicSetPriority(UART_INT_NUM, 0);
    OsGicEnableInt(UART_INT_NUM);

    U32 ret;
    ret = PRT_SemCreate(0, &sem_uart_rx);
    if (ret != OS_OK) {
        UartPrintf("failed to create uart_rx sem\n");
        return 1;
    }

    return OS_OK;
}

void UartPutChar(unsigned char ch)
{
    while (UART_REG(UART_FR) & UART_TXFF) {
        asm volatile("yield" ::: "memory");
    }
    UART_REG(UART_DR) = ch;
}

void TestPutc(unsigned char ch)
{
    UartPutChar(ch);
    if (ch == '\n') {
        UartPutChar('\r');
    }
}

extern int vsnprintf_s(char *buff, int buff_size, int count, char const *fmt, va_list arg);
int TestPrintf(const char *format, va_list vaList)
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
        TestPutc(*str);
        str++;
    }

    return OS_OK;
}

U32 UartPrintf(const char *format, ...)
{
    va_list vaList;
    S32 count;
    
    va_start(vaList, format);
    count = TestPrintf(format, vaList);
    va_end(vaList);

    return count;
}

extern ShellCB g_shellCB;
void OsUartRxHandle(void)
{
    U32 flag = 0;
    U32 result = 0;
    U32 reg_base = UART_BASE_ADDR;

    flag = UART_REG(UART_FR);
    while((flag & (1<<4)) == 0)
    {
        result = UART_REG(UART_DR);

        // 将收到的字符存到g_shellCB的缓冲区
        g_shellCB.shellBuf[g_shellCB.shellBufOffset] = (char) result;
        g_shellCB.shellBufOffset++;
        if (g_shellCB.shellBufOffset == SHELL_SHOW_MAX_LEN)
            g_shellCB.shellBufOffset = 0;

        PRT_SemPost(sem_uart_rx);
        flag = UART_REG(UART_FR);
    }
    return;
}