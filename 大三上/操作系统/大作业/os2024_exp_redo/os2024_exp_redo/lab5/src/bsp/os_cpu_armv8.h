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
 * Description: cpu架构相关的外部头文件
 */
#ifndef OS_CPU_ARMV8_H
#define OS_CPU_ARMV8_H

#include "prt_typedef.h"

// CurrentEl等级
#define CURRENT_EL_2       0x8
#define CURRENT_EL_1       0x4
#define CURRENT_EL_0       0x0

#define DAIF_DBG_BIT      (1U << 3)
#define DAIF_ABT_BIT      (1U << 2)
#define DAIF_IRQ_BIT      (1U << 1)
#define DAIF_FIQ_BIT      (1U << 0)

#define INT_MASK          (1U << 7)


#define PRT_DSB() OS_EMBED_ASM("DSB sy" : : : "memory")
#define PRT_DMB() OS_EMBED_ASM("DMB sy" : : : "memory")
#define PRT_ISB() OS_EMBED_ASM("ISB" : : : "memory")



#endif /* OS_CPU_ARMV8_H */
