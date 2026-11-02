#!/usr/bin/env bash
# Runs the bytecode workflow's steps in a local container instead of on a GitHub-hosted runner.
#
#   javaw-bytecode/docker/docker-tests.sh [--platform linux/amd64|linux/arm64] [--cpus N] [--memory SIZE]
#                                         [--forks F] [--heap SIZE] [--ref REF] [--test PATTERN]
#
# Tests a clean export of REF (default HEAD) so local target/ directories and uncommitted files are not used.
# Maven's repository persists in the javaw-bytecode-m2 volume; the pinned JARs are downloaded and hash-checked
# as in CI. --test narrows the surefire selection (default: the prescribed one.dastec.javaw.bytecode.*Test).
# Reports are copied to javaw-bytecode/target/docker-tests/<platform>-<ref>/.
set -euo pipefail

platform=linux/amd64 cpus=4 memory=7g forks=1C heap=1536m ref=HEAD test='one.dastec.javaw.bytecode.*Test'
while [ $# -gt 0 ]; do
    case "$1" in
        --platform) platform=$2; shift 2 ;;
        --cpus) cpus=$2; shift 2 ;;
        --memory) memory=$2; shift 2 ;;
        --forks) forks=$2; shift 2 ;;
        --heap) heap=$2; shift 2 ;;
        --ref) ref=$2; shift 2 ;;
        --test) test=$2; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 2 ;;
    esac
done

root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
commit=$(git -C "$root" rev-parse --short "$ref")
image=javaw-bytecode-tests:${platform##*/}
out="$root/javaw-bytecode/target/docker-tests/${platform##*/}-$commit"
mkdir -p "$out"

docker build --platform "$platform" -t "$image" "$root/javaw-bytecode/docker"

echo "Testing $commit on $platform with $cpus CPUs, $memory memory, forks=$forks, -Xmx$heap; reports in $out"
git -C "$root" archive --format=tar "$ref" | docker run --rm -i --platform "$platform" --cpus "$cpus" --memory "$memory" \
    -e TEST="$test" -v javaw-bytecode-m2:/root/.m2 -v "$out":/out "$image" bash -c '
        set -uo pipefail
        tar -x -C /work
        start=$(date +%s)
        { uname -m; java -version 2>&1; mvn -v | head -1; node --version; wat2wasm --version; } | tee /out/environment.txt
        python3 runtime-target/verify-lock.py --online || exit 1
        mvn -B -ntp -pl javaw-bytecode -am -Djavaw.test.forks='"$forks"' -Djavaw.test.argLine=-Xmx'"$heap"' \
            -Djavaw.jdk17.home="$JAVA_HOME_17" \
            -Dtest="$TEST" -Dsurefire.failIfNoSpecifiedTests=false test 2>&1 | tee /out/mvn.log
        status=${PIPESTATUS[0]}
        echo "wall seconds: $(( $(date +%s) - start ))" | tee -a /out/environment.txt
        python3 javaw-bytecode/test-timings.py --markdown /out/test-timings.md \
            --json /out/test-timings.json --warn-compile-seconds 90
        cp -r javaw-bytecode/target/test-timings javaw-bytecode/target/surefire-reports /out/ 2>/dev/null
        exit $status'
