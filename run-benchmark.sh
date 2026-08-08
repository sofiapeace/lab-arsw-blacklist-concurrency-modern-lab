#!/bin/zsh
# Runs the full Laboratory 1 benchmark matrix (10 configurations,
# 2 warm-ups + 5 measured runs each) and collects results/results.csv.
set -e

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd "$(dirname "$0")"

IP="202.24.34.55"
WARMUPS=2
RUNS=5
OUT="results/results.csv"

mkdir -p results
echo "scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers" > "$OUT"

run_config() {
  echo ">>> $*"
  mvn -q exec:java -Dexec.args="$*" | tee /dev/tty | grep '^csv,' | cut -d, -f2- >> "$OUT"
}

mvn -q clean compile

for SIM in false true; do
  run_config SEQUENTIAL "$IP" "$SIM" "$WARMUPS" "$RUNS"
  run_config FIXED      "$IP" "$SIM" "$WARMUPS" "$RUNS" 2
  run_config FIXED      "$IP" "$SIM" "$WARMUPS" "$RUNS" 4
  run_config FIXED      "$IP" "$SIM" "$WARMUPS" "$RUNS" 8
  run_config VIRTUAL    "$IP" "$SIM" "$WARMUPS" "$RUNS"
done

echo "Benchmark finished. Results in $OUT"
