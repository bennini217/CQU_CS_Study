# 位图调度 Bug 修复说明

## 问题描述
系统启动后无法输入命令，可能因为：
1. 位图查找算法错误
2. `g_highestTask` 为 NULL 时导致崩溃

## Bug 修复

### 修复 1: `src/kernel/task/prt_task.c` - 修复位图查找算法

#### 位置：`OsTskBitMapGetHighest()` 函数（约第45-88行）

#### 修改前：
```c
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
```

#### 修改后：
```c
OS_SEC_L0_TEXT U32 OsTskBitMapGetHighest(struct TagOsRunQue *rq)
{
    U32 mainBitMap = rq->taskReadyListBitMap;
    
    if (mainBitMap == 0) {
        return OS_TSK_NUM_OF_PRIORITIES;  /* 没有就绪任务 */
    }
    
    /* 找到第一个非零组（最高优先级组 = 最低的组索引） */
    U32 groupIdx;
    #if defined(__GNUC__) || defined(__clang__)
        /* 使用 __builtin_ctz 查找最低位（尾随零计数） */
        groupIdx = __builtin_ctz(mainBitMap);
    #else
        /* 如果没有内置函数，使用循环查找最低位 */
        groupIdx = 0;
        while (groupIdx < OS_GET_WORD_NUM_BY_PRIONUM(OS_TSK_NUM_OF_PRIORITIES) && 
               (mainBitMap & (1U << groupIdx)) == 0) {
            groupIdx++;
        }
        if (groupIdx >= OS_GET_WORD_NUM_BY_PRIONUM(OS_TSK_NUM_OF_PRIORITIES)) {
            return OS_TSK_NUM_OF_PRIORITIES;
        }
    #endif
    
    /* 在子位图中查找最高优先级位（最高位 = 最低优先级值） */
    U32 childBitMap = rq->tskChildBitMap[groupIdx];
    U32 bitPos;
    #if defined(__GNUC__) || defined(__clang__)
        /* 使用 __builtin_clz 查找最高位（前导零计数） */
        bitPos = __builtin_clz(childBitMap);
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
```

**关键修改**：
- 主位图查找：从 `__builtin_clz` 改为 `__builtin_ctz`（查找最低位，对应最高优先级组）
- 子位图查找：保持 `__builtin_clz`（查找最高位，对应最高优先级）

---

### 修复 2: `src/kernel/sched/prt_sched_single.c` - 添加 NULL 检查

#### 位置：`OsFirstTimeSwitch()` 函数（约第69-77行）

#### 修改前：
```c
OS_SEC_L4_TEXT void OsFirstTimeSwitch(void)
{
    OsTskHighestSet();
    RUNNING_TASK = g_highestTask;
    TSK_STATUS_SET(RUNNING_TASK, OS_TSK_RUNNING);
    OsTskContextLoad((uintptr_t)RUNNING_TASK);
    // never get here
    return;
}
```

#### 修改后：
```c
OS_SEC_L4_TEXT void OsFirstTimeSwitch(void)
{
    OsTskHighestSet();
    
    /* 检查是否有就绪任务 */
    if (g_highestTask == NULL) {
        /* 没有就绪任务，系统错误，进入死循环 */
        while (1) {
            /* 系统错误：没有可运行的任务 */
        }
    }
    
    RUNNING_TASK = g_highestTask;
    TSK_STATUS_SET(RUNNING_TASK, OS_TSK_RUNNING);
    OsTskContextLoad((uintptr_t)RUNNING_TASK);
    // never get here
    return;
}
```

---

### 修复 3: `src/kernel/sched/prt_sched_single.c` - 添加 NULL 检查

#### 位置：`OsMainSchedule()` 函数（约第33-49行）

#### 修改前：
```c
OS_SEC_L0_TEXT void OsMainSchedule(void)
{
    struct TagTskCb *prevTsk;
    if ((UNI_FLAG & OS_FLG_TSK_REQ) != 0) {
        prevTsk = RUNNING_TASK;

        /* 清除OS_FLG_TSK_REQ标记位 */
        UNI_FLAG &= ~OS_FLG_TSK_REQ;

        RUNNING_TASK->taskStatus &= ~OS_TSK_RUNNING;
        g_highestTask->taskStatus |= OS_TSK_RUNNING;

        RUNNING_TASK = g_highestTask;
    }
    // 如果中断没有驱动一个任务ready，直接回到被打断的任务
    OsTskContextLoad((uintptr_t)RUNNING_TASK);
}
```

#### 修改后：
```c
OS_SEC_L0_TEXT void OsMainSchedule(void)
{
    struct TagTskCb *prevTsk;
    if ((UNI_FLAG & OS_FLG_TSK_REQ) != 0) {
        prevTsk = RUNNING_TASK;

        /* 清除OS_FLG_TSK_REQ标记位 */
        UNI_FLAG &= ~OS_FLG_TSK_REQ;

        /* 检查是否有最高优先级任务 */
        if (g_highestTask == NULL) {
            /* 没有就绪任务，继续运行当前任务 */
            OsTskContextLoad((uintptr_t)RUNNING_TASK);
            return;
        }

        RUNNING_TASK->taskStatus &= ~OS_TSK_RUNNING;
        g_highestTask->taskStatus |= OS_TSK_RUNNING;

        RUNNING_TASK = g_highestTask;
    }
    // 如果中断没有驱动一个任务ready，直接回到被打断的任务
    OsTskContextLoad((uintptr_t)RUNNING_TASK);
}
```

---

## 修复总结

### 主要问题：
1. **位图查找算法错误**：使用 `__builtin_clz` 查找主位图是错误的，应该使用 `__builtin_ctz` 查找最低位
2. **缺少 NULL 检查**：当没有就绪任务时，`g_highestTask` 可能为 NULL，导致系统崩溃

### 修复内容：
1. ✅ 修复主位图查找：使用 `__builtin_ctz` 查找最低位（最高优先级组）
2. ✅ 添加 NULL 检查：在 `OsFirstTimeSwitch()` 和 `OsMainSchedule()` 中添加检查

### 修复后：
- 系统应该能正常启动
- Shell 任务应该能正常运行
- 可以正常输入命令

---

## 测试步骤

修复后：
1. 重新编译：`sh makeMiniEuler.sh`
2. 运行测试：`sh runMiniEuler.sh`
3. 应该能看到 Shell 提示符：`miniEuler #`
4. 输入 `top` 命令测试


