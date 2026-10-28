#include "prt_task_external.h"
#include "prt_typedef.h"
#include "os_attr_armv8_external.h"
#include "prt_asm_cpu_external.h"
#include "os_cpu_armv8_external.h"
#include "prt_amp_task_internal.h"

OS_SEC_BSS struct TagOsRunQue g_runQueue;  // 核的局部运行队列

/*
* 描述：位图操作 - 设置优先级位
*/
OS_SEC_L0_TEXT void OsTskBitMapSet(struct TagOsRunQue *rq, U32 priority)
{
    U32 groupIdx = priority >> OS_TSK_PRIO_BIT_MAP_POW;  /* 优先级组索引：0或1 */
    U32 bitPos = priority & (OS_WORD_BIT_NUM - 1);  /* 组内位位置：0-31 */
    
    /* 设置主位图 */
    rq->taskReadyListBitMap |= (1U << groupIdx);
    /* 设置子位图 */
    rq->tskChildBitMap[groupIdx] |= (1U << (OS_WORD_BIT_NUM - 1 - bitPos));
}

/*
* 描述：位图操作 - 清除优先级位
*/
OS_SEC_L0_TEXT void OsTskBitMapClear(struct TagOsRunQue *rq, U32 priority)
{
    U32 groupIdx = priority >> OS_TSK_PRIO_BIT_MAP_POW;
    U32 bitPos = priority & (OS_WORD_BIT_NUM - 1);
    
    /* 清除子位图 */
    rq->tskChildBitMap[groupIdx] &= ~(1U << (OS_WORD_BIT_NUM - 1 - bitPos));
    
    /* 如果该组没有就绪任务，清除主位图 */
    if (rq->tskChildBitMap[groupIdx] == 0) {
        rq->taskReadyListBitMap &= ~(1U << groupIdx);
    }
}

/*
* 描述：位图操作 - 查找最高优先级
* 返回：优先级值，如果没有就绪任务返回 OS_TSK_NUM_OF_PRIORITIES
*/
OS_SEC_L0_TEXT U32 OsTskBitMapGetHighest(struct TagOsRunQue *rq)
{
    U32 mainBitMap = rq->taskReadyListBitMap;
    
    if (mainBitMap == 0) {
        return OS_TSK_NUM_OF_PRIORITIES;  /* 没有就绪任务 */
    }
    
    /* 找到第一个非零组（最高优先级组） */
    U32 groupIdx;
    #if defined(__GNUC__) || defined(__clang__)
        groupIdx = __builtin_clz(mainBitMap);  /* 计算前导零个数 */
        groupIdx = 31 - groupIdx;  /* 转换为组索引 */
    #else
        /* 如果没有内置函数，使用循环查找 */
        groupIdx = 0;
        while (groupIdx < OS_GET_WORD_NUM_BY_PRIONUM(OS_TSK_NUM_OF_PRIORITIES) && 
               (mainBitMap & (1U << groupIdx)) == 0) {
            groupIdx++;
        }
        if (groupIdx >= OS_GET_WORD_NUM_BY_PRIONUM(OS_TSK_NUM_OF_PRIORITIES)) {
            return OS_TSK_NUM_OF_PRIORITIES;
        }
    #endif
    
    /* 在子位图中查找最高优先级位 */
    U32 childBitMap = rq->tskChildBitMap[groupIdx];
    U32 bitPos;
    #if defined(__GNUC__) || defined(__clang__)
        bitPos = __builtin_clz(childBitMap);  /* 计算前导零个数 */
    #else
        bitPos = 0;
        while (bitPos < OS_WORD_BIT_NUM && 
               (childBitMap & (1U << (OS_WORD_BIT_NUM - 1 - bitPos))) == 0) {
            bitPos++;
        }
        if (bitPos >= OS_WORD_BIT_NUM) {
            return OS_TSK_NUM_OF_PRIORITIES;
        }
    #endif
    
    /* 计算实际优先级：组索引 * 32 + 位位置 */
    return (groupIdx << OS_TSK_PRIO_BIT_MAP_POW) + bitPos;
}

/*
* 描述：将任务添加到就绪队列, 调用者确保不会换核，并锁上rq
*/
OS_SEC_L0_TEXT void OsTskReadyAdd(struct TagTskCb *task)
{
    struct TagOsRunQue *rq = &g_runQueue;
    TSK_STATUS_SET(task, OS_TSK_READY);

    OS_TSK_EN_QUE(rq, task, 0);
    OsTskHighestSet();

    return;
}

/*
* 描述：将任务从就绪队列中移除，关中断外部保证
*/
OS_SEC_L0_TEXT void OsTskReadyDel(struct TagTskCb *taskCb)
{
    struct TagOsRunQue *runQue = &g_runQueue;
    TSK_STATUS_CLEAR(taskCb, OS_TSK_READY);

    OS_TSK_DE_QUE(runQue, taskCb, 0);
    OsTskHighestSet();

    return;
}

// src/core/kernel/task/prt_task_del.c
/*
* 描述：任务结束退出
*/
OS_SEC_L4_TEXT void OsTaskExit(struct TagTskCb *tsk)
{

    uintptr_t intSave = OsIntLock();

    OsTskReadyDel(tsk);
    OsTskSchedule();

    OsIntRestore(intSave);

}

// src/core/kernel/task/prt_amp_task.c
/*
* 描述：如果快速切换后只有中断恢复，使用此接口
*/
OS_SEC_TEXT void OsTskScheduleFastPs(uintptr_t intSave)
{
    /* Find the highest task */
    OsTskHighestSet();

    /* In case that running is not highest then reschedule */
    if ((g_highestTask != RUNNING_TASK) && (g_uniTaskLock == 0)) {
        UNI_FLAG |= OS_FLG_TSK_REQ;

        /* only if there is not HWI or TICK the trap */
        if (OS_INT_INACTIVE) {
            OsTaskTrapFastPs(intSave);
        }
    }
}

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

extern U32 PRT_Printf(const char *format, ...);
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