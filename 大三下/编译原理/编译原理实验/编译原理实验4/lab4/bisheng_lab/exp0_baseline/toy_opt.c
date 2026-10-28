#include <stdio.h>

__attribute__((noinline)) int bar() {
    int a = 10 * 10;
    int b = 20;
    if (a > 0) return a + 1;
    else return a - 1;
}

int main() {
    volatile int x = bar();
    printf("%d\n", x);
    return 0;
}
