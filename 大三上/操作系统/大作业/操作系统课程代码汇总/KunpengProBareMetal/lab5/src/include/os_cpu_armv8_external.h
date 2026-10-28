/*
 * Copyright (c) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * UniProton is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 * Create: 2022-11-22
 * Description: 属性宏相关内部头文件
 */

#ifndef OS_CPU_ARMV8_EXTERNAL_H
#define OS_CPU_ARMV8_EXTERNAL_H

#include "prt_gic_external.h"
#include "prt_typedef.h"

extern uintptr_t PRT_HwiUnLock(void);
extern uintptr_t PRT_HwiLock(void);
extern void PRT_HwiRestore(uintptr_t intSave);

#define OsIntUnLock() PRT_HwiUnLock()
#define OsIntLock()   PRT_HwiLock()
#define OsIntRestore(intSave) PRT_HwiRestore(intSave)

/*
 * 描述: 获取当前PENDING的中断号, 中断状态PENDING->ACTIVE
 */
OS_SEC_ALW_INLINE INLINE U32 OsHwiNumGet(void)
{
    U32 iar;

    OS_EMBED_ASM("MRS    %0," REG_ALIAS(ICC_IAR1_EL1)" \n"
                 : "=&r"(iar) : : "memory");

    return iar;
}

/*
 * 描述: 清除中断ACTIVE状态
 */
OS_SEC_ALW_INLINE INLINE void OsHwiClear(U32 intId)
{
    OS_EMBED_ASM("MSR " REG_ALIAS(ICC_EOIR1_EL1)", %0 \n"
                 : : "r"(intId) : "memory");
    return;
}



#endif /* OS_CPU_ARMV8_EXTERNAL_H */
