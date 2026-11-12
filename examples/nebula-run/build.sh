#!/usr/bin/env bash
# Builds Nebula Run into examples/nebula-run/target/site: javac, then javaw-bytecode with the bundled runtime and
# one --native-import per canvas/HUD/audio function, then wat2wasm. Serve target/site over HTTP to play.
# Needs: a JDK 17+, wabt (wat2wasm) and compiled javaw-bytecode classes (mvn -o -pl javaw-bytecode -am compile).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
root="$here/../.."
out="$here/target"
asm="$HOME/.m2/repository/org/ow2/asm"
classpath="$root/javaw-bytecode/target/classes:$root/javaw-core/target/classes:$root/javaw-unicode-data/target/classes"
for artifact in asm asm-tree asm-analysis asm-commons; do classpath="$classpath:$asm/$artifact/9.7/$artifact-9.7.jar"; done

rm -rf "$out/classes" "$out/site" && mkdir -p "$out/classes" "$out/site"
javac --release 17 -g -d "$out/classes" "$here"/src/nebula/*.java
(cd "$out/classes" && jar --create --file "$out/nebula.jar" .)

screen=nebula.CanvasScreen
java -cp "$classpath" one.dastec.javaw.bytecode.BytecodeCompiler --exception-runtime \
    --native-import="$screen.selectLayer(I)V=canvas:layer" \
    --native-import="$screen.fill(I)V=canvas:clear" \
    --native-import="$screen.fillRect(IIIII)V=canvas:rect" \
    --native-import="$screen.fillCircle(IIII)V=canvas:circle" \
    --native-import="$screen.fillDiamond(IIII)V=canvas:diamond" \
    --native-import="$screen.showHud(IIII)V=hud:update" \
    --native-import="$screen.playTone(III)V=audio:tone" \
    "$out/nebula.jar" "$out/nebula.wat"

# wabt before 1.0.36 needs the exception-handling flag; later versions enable it by default and reject the flag.
flags=(); wat2wasm --help | grep -q -- --enable-exceptions && flags=(--enable-exceptions)
wat2wasm ${flags[@]+"${flags[@]}"} "$out/nebula.wat" -o "$out/site/nebula.wasm"
cp "$here"/web/* "$out/site/"
echo "Built $out/site ($(wc -c < "$out/site/nebula.wasm") bytes of Wasm). Serve it, e.g.: python3 -m http.server -d $out/site 8080"

# Headless verification: the same autopilot game on the JVM and in Wasm must match command for command.
if [ "${1:-}" = "--verify" ]; then
    mkdir -p "$out/jvm-classes"
    javac --release 17 -d "$out/jvm-classes" "$here"/src/nebula/*.java "$here"/jvm/nebula/*.java
    jvm=$(java -cp "$out/jvm-classes" nebula.SimulationMain 2026 20000)
    node "$here/verify.cjs" "$out" "$jvm" 2026 20000
fi
