/* 数组加法 OpenMP 并行版：对比线程级并行（可与 SIMD 叠加观察） */
#include <omp.h>
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
#pragma omp parallel for schedule(static)
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
