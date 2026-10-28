/* 选做：Loop Fusion — FUSED=0 两个循环，FUSED=1 合并为一个 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#ifndef N
#define N 6000000
#endif
#ifndef REPEAT
#define REPEAT 3
#endif

static int *a, *b, *c;

int main(void) {
    a = malloc(sizeof(int) * N);
    b = malloc(sizeof(int) * N);
    c = malloc(sizeof(int) * N);
    if (!a || !b || !c) {
        return 1;
    }
    for (int i = 0; i < N; i++) {
        a[i] = i & 1023;
        b[i] = (i * 3) & 1023;
        c[i] = 0;
    }

    struct timespec t0, t1;
    clock_gettime(CLOCK_MONOTONIC, &t0);

#if FUSED
    for (int r = 0; r < REPEAT; r++) {
        for (int i = 0; i < N; i++) {
            c[i] = a[i] + b[i];
            c[i] += a[i] * 2;
        }
    }
#else
    for (int r = 0; r < REPEAT; r++) {
        for (int i = 0; i < N; i++) {
            c[i] = a[i] + b[i];
        }
        for (int i = 0; i < N; i++) {
            c[i] += a[i] * 2;
        }
    }
#endif

    clock_gettime(CLOCK_MONOTONIC, &t1);
    long long ns = (t1.tv_sec - t0.tv_sec) * 1000000000LL + (t1.tv_nsec - t0.tv_nsec);

    long long s = 0;
    for (int i = 0; i < N; i++) {
        s += c[i];
    }
    printf("FUSED=%d ns %lld checksum %lld\n", FUSED, ns, s);
    free(a);
    free(b);
    free(c);
    return 0;
}
