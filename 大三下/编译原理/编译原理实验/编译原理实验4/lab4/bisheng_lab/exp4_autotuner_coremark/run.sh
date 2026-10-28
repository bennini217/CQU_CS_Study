#!/bin/bash
set -euo pipefail
export AUTOTUNE_DATADIR=/tmp/autotuner_data
rm -rf "$AUTOTUNE_DATADIR"
mkdir -p "$AUTOTUNE_DATADIR"
ITERS=${ITERS:-60000}
TUNE_ITERS=${TUNE_ITERS:-10}
CC=(
  clang
  -Iposix -I.
  -g
  -DFLAGS_STR=\"\"
  -DITERATIONS="$ITERS"
  core_list_join.c core_main.c core_matrix.c core_state.c core_util.c
  posix/core_portme.c
  -O2
  -o coremark
)
echo "=== [1] generate ==="
"${CC[@]}" -fautotune-generate
echo "=== [2] init ==="
llvm-autotune minimize
echo "=== [3] iterate: ${TUNE_ITERS} rounds ==="
for i in $(seq 1 "$TUNE_ITERS"); do
  "${CC[@]}" -fautotune
  time_cost=$(./coremark 0x0 0x0 0x66 "$ITERS" | grep "Total time" | awk '{print $4}')
  echo "Iteration $i -> time: $time_cost"
  llvm-autotune feedback "$time_cost"
done
echo "=== [4] finalize ==="
llvm-autotune finalize
echo "=== [5] build autotuned & baseline ==="
"${CC[@]}" -fautotune
mv coremark coremark_autotuned
"${CC[@]}"
mv coremark coremark_O2
echo "=== [6] run compare ==="
echo "--- O2 baseline ---"
./coremark_O2 0x0 0x0 0x66 "$ITERS" | grep -E "Total time|Iterations/Sec|CoreMark"
echo "--- autotuned ---"
./coremark_autotuned 0x0 0x0 0x66 "$ITERS" | grep -E "Total time|Iterations/Sec|CoreMark"
echo "config: $AUTOTUNE_DATADIR/config.yaml"
