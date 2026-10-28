#!/bin/bash
ROOT="$(cd "$(dirname "$0")" && pwd)"
BIN="$ROOT/bin/compiler"
SYLIB="$ROOT/test/sylib-riscv-linux.a"
for case in 55_sort_test1 70_dijkstra 11_add2 26_while_test1; do
    f=$(find "$ROOT/testcase" -name "${case}.sy" | head -1)
    echo "=== $case ==="
    for opt in "" "-O1"; do
        label="${opt:--noopt}"
        "$BIN" "$f" -S -o /tmp/t.s $opt
        if ! riscv32-unknown-linux-gnu-gcc /tmp/t.s "$SYLIB" -o /tmp/t.exe 2>/dev/null; then
            echo "$label gcc_fail"
            continue
        fi
        ref="$(dirname "$f")/${case}.out"
        out=/tmp/t.out
        in="${f%.sy}.in"
        if [ -f "$in" ]; then qemu-riscv32 /tmp/t.exe < "$in" > "$out" 2>/dev/null; else qemu-riscv32 /tmp/t.exe > "$out" 2>/dev/null; fi
        echo $? >> "$out"
        if diff -wB "$ref" "$out" >/dev/null; then echo "$label OK"; else echo "$label FAIL"; diff -wB "$ref" "$out" | head -5; fi
    done
done
