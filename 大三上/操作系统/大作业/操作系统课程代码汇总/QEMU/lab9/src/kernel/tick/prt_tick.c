#include "os_attr_armv8_external.h"
#include "prt_typedef.h"
#include "prt_config.h"
#include "os_cpu_armv8_external.h"
#include "prt_task_external.h"
#include "prt_list_external.h"

extern U64 g_timerFrequency;

/* Tick计数 */
OS_SEC_BSS U64 g_uniTicks; //src/core/kernel/sys/prt_sys.c

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

/*
* 描述：Tick中断的处理函数。扫描任务超时链表、扫描超时软件定时器、扫描TSKMON等。
*/
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

/*
* 描述：获取当前的tick计数
*/
OS_SEC_L2_TEXT U64 PRT_TickGetCount(void) //src/core/kernel/sys/prt_sys_time.c
{
    return g_uniTicks;
}

extern U32 PRT_Printf(const char *format, ...);
OS_SEC_TEXT void OsDisplayCurTick(void)
{
    PRT_Printf("\nCurrent Tick: %d", PRT_TickGetCount());
}