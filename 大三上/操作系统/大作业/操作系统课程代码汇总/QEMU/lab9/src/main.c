#include "prt_typedef.h"
#include "prt_tick.h"
#include "prt_task.h"
#include "prt_sem.h"
#include "prt_shell.h"
#include "os_attr_armv8_external.h"
#include "os_attr_armv8_external.h"

extern U32 PRT_Printf(const char *format, ...);
extern void PRT_UartInit(void);
extern U32 OsActivate(void);
extern U32 OsTskInit(void);
extern U32 OsSemInit(void);
extern void CoreTimerInit(void);
extern U32 OsHwiInit(void);
extern U32 ShellTaskInit(ShellCB *shellCB);
extern U32 PRT_TaskDelay(U32 tick);
extern U32 PRT_TaskCreate(TskHandle *taskPid, struct TskInitParam *initParam);
extern U32 PRT_TaskResume(TskHandle taskPid);

static SemHandle sem_sync;
ShellCB g_shellCB;

// 测试任务函数
OS_SEC_TEXT void TestTask1(uintptr_t param1, uintptr_t param2, uintptr_t param3, uintptr_t param4)
{
    U32 count = 0;
    while (1) {
        PRT_Printf("[Task1-Prio5] Count: %d\n", count++);
        PRT_TaskDelay(1000);  // 延时1000个tick（约1秒），让其他任务有机会运行
    }
}

OS_SEC_TEXT void TestTask2(uintptr_t param1, uintptr_t param2, uintptr_t param3, uintptr_t param4)
{
    U32 count = 0;
    while (1) {
        PRT_Printf("[Task2-Prio10] Count: %d\n", count++);
        PRT_TaskDelay(1000);
    }
}

OS_SEC_TEXT void TestTask3(uintptr_t param1, uintptr_t param2, uintptr_t param3, uintptr_t param4)
{
    U32 count = 0;
    while (1) {
        PRT_Printf("[Task3-Prio15] Count: %d\n", count++);
        PRT_TaskDelay(1000);
    }
}

void CreateTestTasks(void)
{
    U32 ret;
    TskHandle taskHandle;
    struct TskInitParam param = {0};

    // 创建优先级5的任务（高优先级）
    param.taskEntry = (TskEntryFunc)TestTask1;
    param.taskPrio = 5;
    param.stackSize = 0x1000;
    param.args[0] = 0;
    ret = PRT_TaskCreate(&taskHandle, &param);
    if (ret == OS_OK) {
        PRT_TaskResume(taskHandle);
        PRT_Printf("Created TestTask1 with priority 5\n");
    }

    // 创建优先级10的任务（中优先级）
    param.taskEntry = (TskEntryFunc)TestTask2;
    param.taskPrio = 10;
    ret = PRT_TaskCreate(&taskHandle, &param);
    if (ret == OS_OK) {
        PRT_TaskResume(taskHandle);
        PRT_Printf("Created TestTask2 with priority 10\n");
    }

    // 创建优先级15的任务（低优先级）
    param.taskEntry = (TskEntryFunc)TestTask3;
    param.taskPrio = 15;
    ret = PRT_TaskCreate(&taskHandle, &param);
    if (ret == OS_OK) {
        PRT_TaskResume(taskHandle);
        PRT_Printf("Created TestTask3 with priority 15\n");
    }
}

S32 main(void)
{
    

    // 初始化GIC
    OsHwiInit();
    // 启用Timer
    CoreTimerInit();
    // 初始化任务管理模块
    OsTskInit();
    // 初始化信号量模块
    OsSemInit(); // 参见demos/ascend310b/config/prt_config.c 系统初始化注册表

    PRT_UartInit();

    PRT_Printf("            _       _ _____      _             _             _   _ _   _ _   _           \n");
    PRT_Printf("  _ __ ___ (_)_ __ (_) ____|   _| | ___ _ __  | |__  _   _  | | | | \\ | | | | | ___ _ __ \n");
    PRT_Printf(" | '_ ` _ \\| | '_ \\| |  _|| | | | |/ _ \\ '__| | '_ \\| | | | | |_| |  \\| | | | |/ _ \\ '__|\n");
    PRT_Printf(" | | | | | | | | | | | |__| |_| | |  __/ |    | |_) | |_| | |  _  | |\\  | |_| |  __/ |   \n");
    PRT_Printf(" |_| |_| |_|_|_| |_|_|_____\\__,_|_|\\___|_|    |_.__/ \\__, | |_| |_|_| \\_|\\___/ \\___|_|   \n");
    PRT_Printf("                                                     |___/                               \n");

    PRT_Printf("ctr-a h: print help of qemu emulator. ctr-a x: quit emulator.\n\n");

    CreateTestTasks();
    ShellTaskInit(&g_shellCB);


    // 启动调度
    OsActivate();
    // while(1);
    return 0;

}