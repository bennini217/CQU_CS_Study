/* AoS vs SoA 等价求和，校验一致并对比汇编访存模式 */
#include <stdio.h>
#include <stdlib.h>

#ifndef N
#define N 2000000
#endif

typedef struct {
    int a;
    int b;
} Pair;

static Pair aos[N];
static int soa_a[N], soa_b[N];

/* noinline + 非 static：避免 -O2 内联进 main，便于 objdump 对比 sum_aos / sum_soa */
__attribute__((noinline)) long long sum_aos(void) {
    long long s = 0;
    for (int i = 0; i < N; i++) {
        s += aos[i].a + aos[i].b;
    }
    return s;
}

__attribute__((noinline)) long long sum_soa(void) {
    long long s = 0;
    for (int i = 0; i < N; i++) {
        s += soa_a[i] + soa_b[i];
    }
    return s;
}

int main(void) {
    for (int i = 0; i < N; i++) {
        aos[i].a = i & 0xfff;
        aos[i].b = (i * 7) & 0xfff;
        soa_a[i] = aos[i].a;
        soa_b[i] = aos[i].b;
    }
    long long sa = sum_aos();
    long long ss = sum_soa();
    printf("aos %lld soa %lld same %d\n", sa, ss, sa == ss);
    return sa == ss ? 0 : 1;
}
