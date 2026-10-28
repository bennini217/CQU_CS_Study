#include "prt_typedef.h"
#include "prt_shell.h"
#include "prt_tick.h"
#include "prt_task.h"
#include "prt_sem.h"
#include "cpu_config.h"
#include "prt_gic_external.h"

extern U32 PRT_Printf(const char *format, ...);
extern void PRT_UartInit(void);
extern void UartPutChar(unsigned char ch);
extern void CoreTimerInit(void);
extern U32 OsHwiInit(void);
extern U32 OsActivate(void);
extern U32 OsTskInit(void);
extern U32 OsSemInit(void);
extern U32 ShellTaskInit(ShellCB *shellCB);
extern U32 PRT_UartInterruptInit(void);

static SemHandle sem_sync;
ShellCB g_shellCB;


S32 main(void)
{

    // 初始化GIC
    OsGicConfigRegister((uintptr_t)OS_GIC_BASE_ADDR, (uintptr_t)OS_GICR_OFFSET, (uintptr_t)OS_GICR_STRIDE);
    OsHwiInit();

    // OsGicClearInt(TEST_CLK_INT); // 清除中断
    OsGicEnableInt(TEST_CLK_INT);

    // 启用Timer
    CoreTimerInit();
    // 任务系统初始化
    OsTskInit();
    OsSemInit(); // 参见demos/ascend310b/config/prt_config.c 系统初始化注册表

    PRT_UartInit();
    PRT_UartInterruptInit();

    PRT_Printf("            _       _ _____      _             _             _   _ _   _ _   _           \n");
    PRT_Printf("  _ __ ___ (_)_ __ (_) ____|   _| | ___ _ __  | |__  _   _  | | | | \\ | | | | | ___ _ __ \n");
    PRT_Printf(" | '_ ` _ \\| | '_ \\| |  _|| | | | |/ _ \\ '__| | '_ \\| | | | | |_| |  \\| | | | |/ _ \\ '__|\n");
    PRT_Printf(" | | | | | | | | | | | |__| |_| | |  __/ |    | |_) | |_| | |  _  | |\\  | |_| |  __/ |   \n");
    PRT_Printf(" |_| |_| |_|_|_| |_|_|_____\\__,_|_|\\___|_|    |_.__/ \\__, | |_| |_|_| \\_|\\___/ \\___|_|   \n");
    PRT_Printf("                                                     |___/                               \n");

    // PRT_Printf("ctr-a h: print help of qemu emulator. ctr-a x: quit emulator.\n\n");

    ShellTaskInit(&g_shellCB);

    // 启动调度
    OsActivate();
    // while(1);
    return 0;

}