#!/usr/bin/env bash
# Times a Gradle command a few times (3 unless RUNS says otherwise) and prints each run and the
# median, for docs/evaluations/faster-tests.md. Warm the daemon first: run the command once before
# timing it. The output of the last run is in build/time-gradle.log.
#
#   tools/time-gradle.sh build
#   RUNS=5 tools/time-gradle.sh runFightGameTests -Ptests='*beating*' -Prepeat=3
set -euo pipefail
cd "$(dirname "$0")/.."
runs=${RUNS:-3}
mkdir -p build
times=()
for i in $(seq "$runs"); do
	start=$(date +%s.%N)
	if ! ./gradlew "$@" > build/time-gradle.log 2>&1; then
		echo "run $i failed; see build/time-gradle.log" >&2
		exit 1
	fi
	end=$(date +%s.%N)
	seconds=$(awk -v s="$start" -v e="$end" 'BEGIN { printf "%.1f", e - s }')
	echo "run $i: $seconds s"
	times+=("$seconds")
done
printf '%s\n' "${times[@]}" | sort -n | awk '{ t[NR] = $1 } END { m = (NR % 2) ? t[(NR + 1) / 2] : (t[NR / 2] + t[NR / 2 + 1]) / 2; printf "median of %d: %.1f s\n", NR, m }'
