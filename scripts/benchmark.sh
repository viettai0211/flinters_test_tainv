#!/usr/bin/env bash
# Builds the jar, runs it on the given CSV with a capped heap, and writes a benchmark log.
# Usage: scripts/benchmark.sh <path-to-ad_data.csv> [runs]
set -euo pipefail

input="${1:?usage: scripts/benchmark.sh <path-to-ad_data.csv> [runs]}"
runs="${2:-3}"
heap="-Xmx64m"
root="$(cd "$(dirname "$0")/.." && pwd)"
log="$root/benchmarks/$(date +%Y-%m-%d_%H%M%S).log"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT

cd "$root"
mvn -q clean package -DskipTests

{
    echo "# Ad aggregator benchmark"
    echo "date:   $(date -Iseconds)"
    echo "input:  $input ($(wc -c < "$input") bytes)"
    echo "heap:   $heap"
    echo "cpus:   $(nproc)"
    echo "java:   $(java -version 2>&1 | head -1)"
    echo "os:     $(uname -s -r)"
    echo
    for run in $(seq 1 "$runs"); do
        start=$(date +%s%N)
        summary=$(java "$heap" -jar target/aggregator.jar --input "$input" --output "$out_dir" 2>&1)
        end=$(date +%s%N)
        echo "run $run: wall $(( (end - start) / 1000000 )) ms (includes JVM startup) | $summary"
    done
} | tee "$log"

echo "log written to $log"
