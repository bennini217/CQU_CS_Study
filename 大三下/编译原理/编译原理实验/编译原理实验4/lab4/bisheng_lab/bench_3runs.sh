#!/bin/bash
# 各实验跑 3 次，输出 real / ns / CoreMark Total time 的中位数（用于报告表格）
set -euo pipefail

ROOT="/home/student/workspace/bisheng_lab"
RUNS="${RUNS:-3}"

median() {
  python3 - "$@" <<'PY'
import sys
vals = [float(x) for x in sys.argv[1:] if x.strip()]
vals.sort()
n = len(vals)
if n == 0:
    print("NA")
elif n % 2:
    print(f"{vals[n//2]:.6f}".rstrip("0").rstrip("."))
else:
    m = (vals[n//2 - 1] + vals[n//2]) / 2
    print(f"{m:.6f}".rstrip("0").rstrip("."))
PY
}

echo "========== 实验 1：自动并行化（time -p real，${RUNS} 次中位数）=========="
cd "$ROOT/exp1_parallel"
clang -O2 -fno-vectorize vec_add.c -o vec_no_vec 2>/dev/null || true
clang -O2 vec_add.c -o vec_vec 2>/dev/null || true
clang -O2 -fopenmp vec_add_omp.c -o vec_omp 2>/dev/null || true

bench_time() {
  local bin="$1"
  local reals=()
  local i out
  for ((i=1; i<=RUNS; i++)); do
    out="$( { time -p "./$bin" >/dev/null; } 2>&1 )"
    reals+=("$(echo "$out" | awk '/^real/{print $2}')")
    echo "  $bin run$i real=$(echo "$out" | awk '/^real/{print $2}')"
  done
  echo "  => $bin median_real=$(median "${reals[@]}")"
  echo
}

bench_time vec_no_vec
bench_time vec_vec
bench_time vec_omp

echo "========== 实验 3：PGO（time -p real，${RUNS} 次中位数）=========="
cd "$ROOT/exp3_pgo"
if [[ ! -f pgo.exe ]]; then
  echo "  未找到 pgo.exe，请先完成 PGO 全流程（base/sample/profdata/pgo）"
else
  bench_time base.exe
  bench_time pgo.exe
fi

echo "========== 实验 4：CoreMark O2 vs Autotuned（Total time，${RUNS} 次中位数）=========="
cd "$ROOT/exp4_autotuner_coremark/coremark"
if [[ ! -f coremark_O2 ]] || [[ ! -f coremark_autotuned ]]; then
  echo "  未找到 coremark_O2 / coremark_autotuned，请先在该目录执行 ./run.sh"
else
  ITERS="${ITERS:-60000}"
  bench_coremark() {
    local bin="$1"
    local times=()
    local i line t
    for ((i=1; i<=RUNS; i++)); do
      line="$(./"$bin" 0x0 0x0 0x66 "$ITERS" 2>/dev/null | grep 'Total time' || true)"
      t="$(echo "$line" | awk '{print $4}')"
      times+=("$t")
      echo "  $bin run$i Total_time=$t"
    done
    echo "  => $bin median_Total_time=$(median "${times[@]}")"
    echo
  }
  bench_coremark coremark_O2
  bench_coremark coremark_autotuned
fi

echo "========== 实验 5：Loop Fusion（程序 ns，${RUNS} 次中位数；real 仅参考）=========="
cd "$ROOT/exp5_loopopt_project"
clang -O2 -DFUSED=0 loop_fusion_test.c -o fusion_u 2>/dev/null || true
clang -O2 -DFUSED=1 loop_fusion_test.c -o fusion_f 2>/dev/null || true

bench_fusion() {
  local bin="$1"
  local nss=() reals=()
  local i out ns real
  for ((i=1; i<=RUNS; i++)); do
    out="$( { time -p "./$bin"; } 2>&1 )"
    ns="$(echo "$out" | awk '/ns/{for(k=1;k<=NF;k++) if($k=="ns"){print $(k+1); exit}}')"
    real="$(echo "$out" | awk '/^real/{print $2}')"
    nss+=("$ns")
    reals+=("$real")
    echo "  $bin run$i ns=$ns real=$real"
  done
  echo "  => $bin median_ns=$(median "${nss[@]}") median_real=$(median "${reals[@]}")"
  echo
}

bench_fusion fusion_u
bench_fusion fusion_f

echo "========== 实验 5 可选：fusion Autotuner（程序 ns，${RUNS} 次中位数）=========="
if [[ -f fusion_baseline ]] && [[ -f fusion_autotuned ]]; then
  bench_fusion fusion_baseline
  bench_fusion fusion_autotuned
else
  echo "  未找到 fusion_baseline / fusion_autotuned，跳过（需先 ./tune_fusion.sh）"
fi

echo "========== 完成：请将 median 行填入报告表格 =========="
