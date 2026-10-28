#!/bin/bash
# Autotuner on loop_fusion (FUSED=0): stable feedback = ns from program / 1e9
set -euo pipefail
cd "$(dirname "$0")"
# 挂载目录下保存，换容器不丢；勿用纯 /tmp
export AUTOTUNE_DATADIR="$(pwd)/autotune_data"
rm -rf "$AUTOTUNE_DATADIR"
mkdir -p "$AUTOTUNE_DATADIR"

# 加大工作量，使单次约 0.5～数秒，反馈稳定（可按机器改小）
BIG="-DN=20000000 -DREPEAT=8"

echo "=== [1] generate ==="
clang -O2 $BIG -DFUSED=0 loop_fusion_test.c -o fusion_tune -fautotune-generate

echo "=== [2] minimize ==="
llvm-autotune minimize

TUNE_ITERS="${TUNE_ITERS:-10}"
last_sec="1.0"
echo "=== [3] iterate: $TUNE_ITERS rounds (feedback = ns/1e9) ==="
for i in $(seq 1 "$TUNE_ITERS"); do
  clang -O2 $BIG -DFUSED=0 loop_fusion_test.c -o fusion_tune -fautotune
  out="$(./fusion_tune 2>&1)" || true
  echo "$out"
  sec="$(echo "$out" | awk '/ns/{for (k=1;k<=NF;k++) if ($k=="ns") {print $(k+1)/1e9; exit}}')"
  ok="$(awk -v s="$sec" 'BEGIN{if (s+0 > 1e-6 && s+0 < 3600) print 1; else print 0}')"
  if [ "$ok" != "1" ]; then
    echo "iter $i bad sec='$sec', reuse last_sec=$last_sec"
    sec="$last_sec"
  else
    last_sec="$sec"
  fi
  echo "iter $i -> feedback_sec=$sec"
  llvm-autotune feedback "$sec"
done

echo "=== [4] finalize ==="
llvm-autotune finalize

echo "=== [5] build autotuned & baseline ==="
clang -O2 $BIG -DFUSED=0 loop_fusion_test.c -o fusion_autotuned -fautotune
clang -O2 $BIG -DFUSED=0 loop_fusion_test.c -o fusion_baseline

echo "=== [6] compare (各跑 1 次，可多跑取中位数) ==="
echo "--- baseline ---"
./fusion_baseline
echo "--- autotuned ---"
./fusion_autotuned
echo "config: $AUTOTUNE_DATADIR/config.yaml"
