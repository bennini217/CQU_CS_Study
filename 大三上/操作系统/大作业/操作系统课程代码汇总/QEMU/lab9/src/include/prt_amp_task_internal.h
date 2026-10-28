#ifndef PRT_AMP_TASK_INTERNAL_H
#define PRT_AMP_TASK_INTERNAL_H

#include "prt_task_external.h"
#include "prt_list_external.h"

/* 位图操作函数声明 */
extern void OsTskBitMapSet(struct TagOsRunQue *rq, U32 priority);
extern void OsTskBitMapClear(struct TagOsRunQue *rq, U32 priority);

#define OS_TSK_EN_QUE(runQue, tsk, flags) OsEnqueueTaskAmp((runQue), (tsk))
#define OS_TSK_EN_QUE_HEAD(runQue, tsk, flags) OsEnqueueTaskHeadAmp((runQue), (tsk))
#define OS_TSK_DE_QUE(runQue, tsk, flags) OsDequeueTaskAmp((runQue), (tsk))

extern U32 OsTskAMPInit(void);
extern U32 OsIdleTskAMPCreate(void);

OS_SEC_ALW_INLINE INLINE void OsEnqueueTaskAmp(struct TagOsRunQue *runQue, struct TagTskCb *tsk)
{
    U32 priority = tsk->priority;
    
    /* 检查优先级范围 */
    if (priority >= OS_TSK_NUM_OF_PRIORITIES) {
        priority = OS_TSK_NUM_OF_PRIORITIES - 1;
    }
    
    /* 检查该优先级队列是否为空（添加前） */
    bool wasEmpty = ListEmpty(&runQue->readyList[priority]);
    
    /* 添加到对应优先级的队列尾部 */
    ListTailAdd(&tsk->pendList, &runQue->readyList[priority]);
    
    /* 如果队列之前为空，现在需要设置位图 */
    if (wasEmpty) {
        OsTskBitMapSet(runQue, priority);
    }
    return;
}

OS_SEC_ALW_INLINE INLINE void OsEnqueueTaskHeadAmp(struct TagOsRunQue *runQue, struct TagTskCb *tsk)
{
    U32 priority = tsk->priority;
    
    /* 检查优先级范围 */
    if (priority >= OS_TSK_NUM_OF_PRIORITIES) {
        priority = OS_TSK_NUM_OF_PRIORITIES - 1;
    }
    
    /* 检查该优先级队列是否为空（添加前） */
    bool wasEmpty = ListEmpty(&runQue->readyList[priority]);
    
    /* 添加到对应优先级的队列头部 */
    ListAdd(&tsk->pendList, &runQue->readyList[priority]);
    
    /* 如果队列之前为空，现在需要设置位图 */
    if (wasEmpty) {
        OsTskBitMapSet(runQue, priority);
    }
    return;
}

OS_SEC_ALW_INLINE INLINE void OsDequeueTaskAmp(struct TagOsRunQue *runQue, struct TagTskCb *tsk)
{
    U32 priority = tsk->priority;
    
    /* 检查优先级范围 */
    if (priority >= OS_TSK_NUM_OF_PRIORITIES) {
        priority = OS_TSK_NUM_OF_PRIORITIES - 1;
    }
    
    /* 从队列中删除 */
    ListDelete(&tsk->pendList);
    
    /* 如果该优先级的队列为空，清除位图 */
    if (ListEmpty(&runQue->readyList[priority])) {
        OsTskBitMapClear(runQue, priority);
    }
    return;
}

#endif /* PRT_AMP_TASK_INTERNAL_H */