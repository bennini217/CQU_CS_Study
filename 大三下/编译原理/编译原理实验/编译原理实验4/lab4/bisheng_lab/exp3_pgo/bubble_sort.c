/* PGO 实验用排序程序：升序数据影响分支预测与 profile */
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>

#ifndef ARRAY_LEN
#define ARRAY_LEN 20000
#endif

static int data[ARRAY_LEN];

static void start_timer(struct timeval *tm) {
    gettimeofday(tm, NULL);
}

static unsigned long long elapsed_ms(struct timeval *tm1, struct timeval *tm2) {
    return (unsigned long long)(tm2->tv_sec - tm1->tv_sec) * 1000ULL +
           (unsigned long long)(tm2->tv_usec - tm1->tv_usec) / 1000ULL;
}

static void swap_int(int *a, int *b) {
    int t = *a;
    *a = *b;
    *b = t;
}

/* 故意写成易分支预测/ profile 敏感的形式 */
void bubble_sort(int *a, int n) {
    for (int i = 0; i < n - 1; i++) {
        for (int j = i + 1; j < n; j++) {
            if (a[i] < a[j]) {
                swap_int(&a[i], &a[j]);
            }
        }
    }
}

static void get_data(void) {
    for (int i = 0; i < ARRAY_LEN; i++) {
        data[i] = i;
    }
}

int main(void) {
    struct timeval t1, t2;
    get_data();
    start_timer(&t1);
    bubble_sort(data, ARRAY_LEN);
    gettimeofday(&t2, NULL);
    printf("time_ms %llu first %d last %d\n",
           elapsed_ms(&t1, &t2), data[0], data[ARRAY_LEN - 1]);
    return 0;
}
