/* 选做：强度削弱 — 观察 i*a[i] 一类模式中乘法是否随优化减弱或变为递推 */
#include <stdio.h>

#ifndef N
#define N 1000000
#endif

static int a[N];

int main(void) {
    long long sum = 0;
    for (int i = 0; i < N; i++) {
        a[i] = i & 511;
    }
    for (int i = 0; i < N; i++) {
        sum += (long long)a[i] * (long long)i;
    }
    printf("%lld\n", sum);
    return 0;
}
