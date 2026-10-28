/* 数组加法：用于对比「关闭向量化」与「默认 O2 向量化」 */
#include <stdio.h>

#ifndef N
#define N 4000000
#endif

static int a[N], b[N], c[N];

int main(void) {
    for (int i = 0; i < N; i++) {
        a[i] = i & 0xffff;
        b[i] = (i * 3) & 0xffff;
    }
    for (int i = 0; i < N; i++) {
        c[i] = a[i] + b[i];
    }
    long long s = 0;
    for (int i = 0; i < N; i++) {
        s += c[i];
    }
    printf("%lld\n", s);
    return 0;
}
