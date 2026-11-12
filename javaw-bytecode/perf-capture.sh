#!/usr/bin/env bash
# Runs the bytecode test selection and bundles its performance statistics for later analysis.
#
#   javaw-bytecode/perf-capture.sh [--label NAME] [--java-home DIR] [--forks F] [--test PATTERN] [--repeat N]
#                                  [--cold] [--core] [--sample-seconds S] [--no-sample] [--allow-new-jdk]
#                                  [-- EXTRA_MAVEN_ARGS...]
#
# Each run writes to javaw-bytecode/target/perf-runs/<timestamp>-<label>/run-<n>/:
#   environment.txt     machine, toolchain versions, commit and working-tree state, options
#   mvn.log             full Maven output
#   time.txt            /usr/bin/time resource usage for the whole Maven process tree (CPU, max RSS)
#   wall.txt            wall seconds and Maven exit status
#   samples.tsv         periodic CPU% / RSS per process kind (java, node, wat2wasm, wasm-opt, other) and load average
#   test-timings/       raw per-class TSVs from BytecodeTestTimings
#   test-timings.md     test-timings.py report;  test-timings.json  every row as JSON
#   surefire-reports/   surefire XML and text reports
#   core/               the same Maven log, time and surefire reports for javaw-core when --core is given
# The whole capture is also packed as target/perf-runs/<timestamp>-<label>.tar.gz.
#
# --cold removes javaw-bytecode/target/wasm-cache first, so wat2wasm output is not reused.
# Runs offline (mvn -o), like the prescribed validation command. A failing test does not stop the capture.
# --java-home sets JAVA_HOME for Maven and the test forks. The tests compile fixtures with the running JDK's javac,
# and the pinned ASM (9.7) and Byte Buddy cannot read class files newer than Java 23 (major 67), so the script refuses
# a newer JDK unless --allow-new-jdk is given. Use a JDK 21, as CI does.
set -uo pipefail

label=run java_home='' allow_new_jdk=0 forks='' test='one.dastec.javaw.bytecode.*Test' repeat=1 cold=0 core=0 sample=2 extra=()
while [ $# -gt 0 ]; do
    case "$1" in
        --label) label=$2; shift 2 ;;
        --java-home) java_home=$2; shift 2 ;;
        --allow-new-jdk) allow_new_jdk=1; shift ;;
        --forks) forks=$2; shift 2 ;;
        --test) test=$2; shift 2 ;;
        --repeat) repeat=$2; shift 2 ;;
        --cold) cold=1; shift ;;
        --core) core=1; shift ;;
        --sample-seconds) sample=$2; shift 2 ;;
        --no-sample) sample=0; shift ;;
        --) shift; extra=("$@"); break ;;
        -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
        *) echo "unknown option $1 (see --help)" >&2; exit 2 ;;
    esac
done

root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
if [ -n "$java_home" ]; then export JAVA_HOME=$java_home PATH="$java_home/bin:$PATH"; fi
feature=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.specification.version = //p')
if [ "${feature:-0}" -gt 23 ] && [ "$allow_new_jdk" = 0 ]; then
    echo "java is JDK $feature: the pinned ASM and Byte Buddy cannot read its class files, so about 66 tests fail early" >&2
    echo "and the timings are not comparable. Pass --java-home <JDK 21>, as CI uses (or --allow-new-jdk to run anyway)." >&2
    exit 2
fi
module="$root/javaw-bytecode"
stamp=$(date +%Y%m%d-%H%M%S)
capture="$module/target/perf-runs/$stamp-$label"
mkdir -p "$capture"
cd "$root"

if [ "$(uname)" = Darwin ]; then timeflags=(-l); else timeflags=(-v); fi
maven=(mvn -o -B -ntp)
[ -n "$forks" ] && maven+=("-Djavaw.test.forks=$forks")

environment() {
    echo "date: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "options: label=$label java_home=${JAVA_HOME:-unset} forks=${forks:-pom default} test=$test repeat=$repeat cold=$cold core=$core extra=${extra[*]:-}"
    echo "commit: $(git rev-parse HEAD) ($(git log -1 --format=%s))"
    echo "uncommitted diff sha256: $(git diff HEAD | shasum -a 256 | cut -d' ' -f1)"
    echo "--- git status --short"; git status --short
    echo "--- machine"; uname -a
    if [ "$(uname)" = Darwin ]; then
        sysctl -n machdep.cpu.brand_string hw.ncpu hw.perflevel0.physicalcpu hw.perflevel1.physicalcpu hw.memsize 2>/dev/null
        pmset -g batt 2>/dev/null | head -2
    else
        nproc; grep -m1 'model name' /proc/cpuinfo; grep MemTotal /proc/meminfo
    fi
    echo "--- toolchain"
    java -version 2>&1; mvn -v 2>&1 | head -3; echo "node $(node --version)"
    echo "wat2wasm $(wat2wasm --version)"; command -v wasm-opt >/dev/null && wasm-opt --version
    echo "--- pom test settings"; grep -n 'javaw.test\.' "$module/pom.xml"
    echo "--- wasm-cache entries: $(find "$module/target/wasm-cache" -type f 2>/dev/null | wc -l | tr -d ' ')"
}

# Samples CPU% and RSS (KB) per process kind until the given PID exits.
sampler() {
    local watched=$1 out=$2
    printf 'epoch\tload1\tkind\tprocesses\tcpu_pct\trss_kb\n' > "$out"
    while kill -0 "$watched" 2>/dev/null; do
        local now load
        now=$(date +%s)
        load=$(uptime | sed -E 's/.*load averages?: *([0-9.]+).*/\1/')
        ps -Ao pcpu=,rss=,comm= | awk -v now="$now" -v load="$load" '
            { n=split($3, part, "/"); name=part[n]
              kind="other"
              if (name ~ /^java/) kind="java"; else if (name ~ /^node/) kind="node"
              else if (name ~ /^wat2wasm/) kind="wat2wasm"; else if (name ~ /^wasm-opt/) kind="wasm-opt"
              count[kind]++; cpu[kind]+=$1; rss[kind]+=$2 }
            END { for (k in count) printf "%s\t%s\t%s\t%d\t%.1f\t%d\n", now, load, k, count[k], cpu[k], rss[k] }' >> "$out"
        sleep "$sample"
    done
}

# Runs one Maven invocation under /usr/bin/time with the sampler; writes mvn.log, time.txt, wall.txt, samples.tsv.
measured() {
    local out=$1; shift
    local start status
    start=$(date +%s)
    /usr/bin/time "${timeflags[@]}" -o "$out/time.txt" "$@" > "$out/mvn.log" 2>&1 &
    local pid=$!
    [ "$sample" != 0 ] && { sampler "$pid" "$out/samples.tsv" & }
    wait "$pid"; status=$?
    wait
    printf 'wall seconds: %s\nmaven exit status: %s\n' "$(( $(date +%s) - start ))" "$status" > "$out/wall.txt"
    grep -E 'Tests run:.*Fail|BUILD|Total time' "$out/mvn.log" | tail -4 >> "$out/wall.txt"
    cat "$out/wall.txt"
}

for n in $(seq 1 "$repeat"); do
    out="$capture/run-$n"
    mkdir -p "$out"
    echo "== run $n/$repeat -> $out"
    [ "$cold" = 1 ] && rm -rf "$module/target/wasm-cache"
    # Timing files and surefire reports are replaced per class; clear them so the report covers this run only.
    rm -rf "$module/target/test-timings" "$module/target/surefire-reports"
    environment > "$out/environment.txt"

    measured "$out" "${maven[@]}" -pl javaw-bytecode -am "-Dtest=$test" -Dsurefire.failIfNoSpecifiedTests=false \
        ${extra[@]+"${extra[@]}"} test
    python3 "$module/test-timings.py" --markdown "$out/test-timings.md" --json "$out/test-timings.json" > /dev/null
    cp -R "$module/target/test-timings" "$module/target/surefire-reports" "$out/" 2>/dev/null

    if [ "$core" = 1 ]; then
        mkdir -p "$out/core"
        rm -rf "$root/javaw-core/target/surefire-reports"
        echo "-- javaw-core tests"
        measured "$out/core" "${maven[@]}" -pl javaw-core ${extra[@]+"${extra[@]}"} test
        cp -R "$root/javaw-core/target/surefire-reports" "$out/core/" 2>/dev/null
    fi
done

tar -czf "$capture.tar.gz" -C "$(dirname "$capture")" "$(basename "$capture")"
echo "Captured $capture"
echo "Archive  $capture.tar.gz"
