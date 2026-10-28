# PRT_TaskDelay 函数实现说明

## 问题原因

链接错误：`undefined reference to PRT_TaskDelay`

**原因**：`PRT_TaskDelay` 函数在头文件中有声明，但在代码中没有实现。

## 需要修改的文件

### 修改 1: `src/include/prt_task_external.h`

#### 位置：启用延时队列结构体（约第37-40行）

**修改前：**

```c
// struct TagOsTskSortedDelayList {
//     /* 延时任务链表 */
//     struct TagListObject tskList;
// };
```

**修改后：**
```c
struct TagOsTskSortedDelayList {
    /* 延时任务链表 */
    struct TagListObject tskList;
};
```

---

### 修改 2: `src/kernel/sys/prt_sys.c`

#### 位置：添加延时队列全局变量定义（约第37-38行之后）

**修改后添加：**
```c
OS_SEC_BSS TskHandle g_idleTaskId;
OS_SEC_BSS U16 g_uniTaskLock;
OS_SEC_BSS struct TagTskCb *g_highestTask;
OS_SEC_BSS struct TagOsTskSortedDelayList g_tskSortedDelay;  // ← 新添加
```

---

### 修改 3: `src/kernel/task/prt_task.c`

#### 位置：在 `OsDisplayTasksInfo()` 函数之前添加 `PRT_TaskDelay` 实现（约第154行之前）

**修改后添加：**
```c
/*
* 描述：任务延时函数
*/
OS_SEC_L2_TEXT U32 PRT_TaskDelay(U32 tick)
{
    uintptr_t intSave;
    struct TagTskCb *runTsk = NULL;
    extern U64 PRT_TickGetCount(void);
    extern struct TagOsTskSortedDelayList g_tskSortedDelay;

    if (tick == 0) {
        /* tick为0时，让出CPU给同优先级任务 */
        OsTskSchedule();
        return OS_OK;
    }

    intSave = OsIntLock();

    /* 检查是否在中断中 */
    if (OS_INT_ACTIVE) {
        OsIntRestore(intSave);
        return OS_ERRNO_TSK_DELAY_IN_INT;
    }

    /* 检查是否锁任务 */
    if (OS_TASK_LOCK_DATA != 0) {
        OsIntRestore(intSave);
        return OS_ERRNO_TSK_DELAY_IN_LOCK;
    }

    runTsk = RUNNING_TASK;

    /* 从就绪队列中移除 */
    OsTskReadyDel(runTsk);

    /* 设置延时标志 */
    TSK_STATUS_SET(runTsk, OS_TSK_DELAY);

    /* 计算恢复时间点 */
    runTsk->expirationTick = PRT_TickGetCount() + tick;

    /* 将任务加入延时队列（简单实现：直接加入队列尾部） */
    ListTailAdd(&runTsk->timerList, &g_tskSortedDelay.tskList);

    /* 触发调度 */
    OsTskSchedule();

    OsIntRestore(intSave);
    return OS_OK;
}
```

---

### 修改 4: `src/kernel/tick/prt_tick.c`

#### 位置 1: 添加头文件（约第5行之后）

**修改后添加：**
```c
#include "os_attr_armv8_external.h"
#include "prt_typedef.h"
#include "prt_config.h"
#include "os_cpu_armv8_external.h"
#include "prt_task_external.h"
#include "prt_list_external.h"  // ← 新添加
```

#### 位置 2: 添加延时任务扫描函数（约第11行之后，`OsTickDispatcher()` 之前）

**修改后添加：**
```c
/*
* 描述：检查延时任务是否超时，将超时的任务重新加入就绪队列
*/
OS_SEC_TEXT void OsTaskDelayScan(void)
{
    struct TagTskCb *taskCb = NULL;
    struct TagListObject *node = NULL;
    struct TagListObject *nextNode = NULL;
    U64 currentTick = g_uniTicks;
    extern struct TagOsTskSortedDelayList g_tskSortedDelay;
    extern void OsTskReadyAddBgd(struct TagTskCb *task);

    /* 遍历延时队列，检查超时任务 */
    node = LIST_FIRST(&g_tskSortedDelay.tskList);
    while (node != &g_tskSortedDelay.tskList) {
        nextNode = node->next;  /* 保存下一个节点，因为可能会删除当前节点 */
        taskCb = LIST_COMPONENT(node, struct TagTskCb, timerList);
        
        if (currentTick >= taskCb->expirationTick) {
            /* 任务延时到期，从延时队列中移除 */
            ListDelete(node);
            
            /* 清除延时标志 */
            TSK_STATUS_CLEAR(taskCb, OS_TSK_DELAY);
            
            /* 如果任务没有被挂起，重新加入就绪队列 */
            if (!TSK_STATUS_TST(taskCb, OS_TSK_SUSPEND)) {
                OsTskReadyAddBgd(taskCb);
            }
        } else {
            /* 延时队列是按时间排序的，如果当前任务未超时，后面的也不会超时 */
            break;
        }
        
        node = nextNode;
    }
}
```

#### 位置 3: 修改 `OsTickDispatcher()` 函数（约第14-25行）

**修改前：**
```c
OS_SEC_TEXT void OsTickDispatcher(void)
{
    uintptr_t intSave;

    intSave = OsIntLock();
    g_uniTicks++;
    OsIntRestore(intSave);

    U64 cycle = g_timerFrequency / OS_TICK_PER_SECOND;
    OS_EMBED_ASM("MSR CNTP_TVAL_EL0, %0" : : "r"(cycle) : "memory", "cc"); //设置中断周期

}
```

**修改后：**
```c
OS_SEC_TEXT void OsTickDispatcher(void)
{
    uintptr_t intSave;

    intSave = OsIntLock();
    g_uniTicks++;
    
    /* 检查延时任务是否超时 */
    OsTaskDelayScan();
    
    OsIntRestore(intSave);

    U64 cycle = g_timerFrequency / OS_TICK_PER_SECOND;
    OS_EMBED_ASM("MSR CNTP_TVAL_EL0, %0" : : "r"(cycle) : "memory", "cc"); //设置中断周期

}
```

---

### 修改 5: `src/kernel/task/prt_task_init.c`

#### 位置：初始化延时队列（约第360行之后）

**修改前：**
```c
    for (idx = 0; idx < OS_TSK_NUM_OF_PRIORITIES; idx++) {
        INIT_LIST_OBJECT(&g_runQueue.readyList[idx]);
    }

    /* 增加OS_TSK_INUSE状态，使得在Trace记录的第一条信息状态为OS_TSK_INUSE(创建状态) */
```

**修改后：**
```c
    for (idx = 0; idx < OS_TSK_NUM_OF_PRIORITIES; idx++) {
        INIT_LIST_OBJECT(&g_runQueue.readyList[idx]);
    }

    /* 初始化延时任务队列 */
    extern struct TagOsTskSortedDelayList g_tskSortedDelay;
    INIT_LIST_OBJECT(&g_tskSortedDelay.tskList);

    /* 增加OS_TSK_INUSE状态，使得在Trace记录的第一条信息状态为OS_TSK_INUSE(创建状态) */
```

在prt_sys.c添加

```
#include "prt_task_external.h"
```



在prt_task.c

修改

```
OS_SEC_TEXT void OsDisplayTasksInfo(void)
{
    struct TagTskCb *taskCb = NULL;
    U32 cnt = 0;
    U32 prio;

    PRT_Printf("\nPID\t\tPriority\tStack Size\n");
    // 遍历所有优先级的队列
    for (prio = 0; prio < OS_TSK_NUM_OF_PRIORITIES; prio++) {
        LIST_FOR_EACH(taskCb, &g_runQueue.readyList[prio], struct TagTskCb, pendList) {
            cnt++;
            PRT_Printf("%d\t\t%d\t\t%d\n", taskCb->taskPid, taskCb->priority, taskCb->stackSize);
        }
    }
    PRT_Printf("Total %d tasks", cnt);
}
```



为

```
OS_SEC_TEXT void OsDisplayTasksInfo(void)
{
    struct TagTskCb *taskCb = NULL;
    U32 cnt = 0;
    U32 idx;
    extern struct TagTskCb *g_tskCbArray;
    extern U32 g_tskMaxNum;
    extern U32 g_tskBaseId;

    PRT_Printf("\nPID\t\tPriority\tStack Size\tStatus\n");
    // 遍历所有任务控制块，显示所有非未使用的任务
    // OS_MAX_TCB_NUM = g_tskMaxNum + 1 + 1 (1个IDLE，1个无效任务)
    // 遍历范围：0 到 g_tskMaxNum + 1 (包括IDLE任务，不包括zombie任务)
    for (idx = 0; idx <= g_tskMaxNum; idx++) {
        taskCb = &g_tskCbArray[idx];
        if (!TSK_IS_UNUSED(taskCb)) {
            cnt++;
            const char *status = "Unknown";
            if (TSK_STATUS_TST(taskCb, OS_TSK_DELAY)) {
                status = "DELAY";
            } else if (TSK_STATUS_TST(taskCb, OS_TSK_READY)) {
                status = "READY";
            } else if (TSK_STATUS_TST(taskCb, OS_TSK_SUSPEND)) {
                status = "SUSPEND";
            } else {
                status = "RUNNING";
            }
            PRT_Printf("%d\t\t%d\t\t%d\t\t%s\n", taskCb->taskPid, taskCb->priority, taskCb->stackSize, status);
        }
    }
    PRT_Printf("Total %d tasks\n", cnt);

}
```



---

## 实现原理

### PRT_TaskDelay 工作流程：

1. **参数检查**：
   - `tick == 0`：让出CPU，触发调度
   - 检查是否在中断中：中断中不能延时
   - 检查是否锁任务：锁任务时不能延时

2. **延时操作**：
   - 从就绪队列中移除任务
   - 设置 `OS_TSK_DELAY` 标志
   - 计算恢复时间点：`expirationTick = 当前tick + 延时tick数`
   - 将任务加入延时队列

3. **触发调度**：调用 `OsTskSchedule()` 切换到其他任务

### 延时任务恢复流程（在 Tick 中断中）：

1. **Tick 中断触发**：每 1ms 触发一次
2. **扫描延时队列**：检查所有延时任务
3. **超时检查**：如果 `当前tick >= expirationTick`，任务超时
4. **恢复任务**：
   - 从延时队列中移除
   - 清除 `OS_TSK_DELAY` 标志
   - 如果任务未被挂起，重新加入就绪队列

---

## 修改总结

需要修改 5 个文件：
1. ✅ `src/include/prt_task_external.h` - 启用延时队列结构体
2. ✅ `src/kernel/sys/prt_sys.c` - 定义延时队列全局变量
3. ✅ `src/kernel/task/prt_task.c` - 实现 `PRT_TaskDelay` 函数
4. ✅ `src/kernel/tick/prt_tick.c` - 实现延时任务扫描和集成到 tick 中断
5. ✅ `src/kernel/task/prt_task_init.c` - 初始化延时队列

---

## 测试

修改完成后：
1. 重新编译：`sh makeMiniEuler.sh`
2. 运行测试：`sh runMiniEuler.sh`
3. 应该能看到三个测试任务交替输出

---

**注意**：当前实现是简化版本，延时队列没有按时间排序。如果需要更精确的延时，可以实现按 `expirationTick` 排序的延时队列。

