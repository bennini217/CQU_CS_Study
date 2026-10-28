#include "os_attr_armv8_external.h"
#include "prt_typedef.h"
#include "prt_config.h"
#include "os_cpu_armv8_external.h"
#include "os_cpu_armv8.h"

extern U64 g_timerFrequency;

/* Tick计数 */
OS_SEC_BSS U64 g_uniTicks; //src/core/kernel/sys/prt_sys.c

/*
* 描述：Tick中断的处理函数。扫描任务超时链表、扫描超时软件定时器、扫描TSKMON等。
*/
OS_SEC_TEXT void OsTickDispatcher(void)
{
    uintptr_t intSave;

    intSave = OsIntLock();
    g_uniTicks++;
    OsIntRestore(intSave);

    U32 cfgMask = 0x0;
    U64 cycle = g_timerFrequency / OS_TICK_PER_SECOND;
    OS_EMBED_ASM("MSR CNTP_TVAL_EL0, %0" : : "r"(cycle) : "memory", "cc"); //设置中断周期

    OS_EMBED_ASM("MSR CNTP_CTL_EL0, %0" : : "r"(cfgMask) : "memory");
    PRT_ISB();
    OS_EMBED_ASM("MSR CNTP_TVAL_EL0, %0" : : "r"(cycle) : "memory", "cc");

    cfgMask = 0x1;
    OS_EMBED_ASM("MSR CNTP_CTL_EL0, %0" : : "r"(cfgMask) : "memory");
    PRT_ISB();

}

/*
* 描述：获取当前的tick计数
*/
OS_SEC_L2_TEXT U64 PRT_TickGetCount(void) //src/core/kernel/sys/prt_sys_time.c
{
    return g_uniTicks;
}


extern U32 UartPrintf(const char *format, ...);
OS_SEC_TEXT void OsDisplayCurTick(void)
{
    UartPrintf("\nCurrent Tick: %d", PRT_TickGetCount());
}