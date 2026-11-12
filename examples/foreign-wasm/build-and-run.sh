#!/usr/bin/env bash
# Builds the Go and Rust libraries and the Java module into examples/foreign-wasm/target/, then runs them in Node.
# Needs: Go 1.24+, Rust with the wasm32-unknown-unknown target, wabt (wat2wasm), Node, a JDK, the javaw-core jar
# (mvn -pl javaw-core package -DskipTests) and compiled javaw-bytecode classes (mvn -pl javaw-bytecode -am compile).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
out="$here/target"
mkdir -p "$out"

(cd "$here/go" && GOOS=wasip1 GOARCH=wasm go build -buildmode=c-shared -o "$out/golib.wasm" .)
cargo build --quiet --release --target wasm32-unknown-unknown --manifest-path "$here/rust/Cargo.toml" --target-dir "$out/cargo"
cp "$out/cargo/wasm32-unknown-unknown/release/rustlib.wasm" "$out/rustlib.wasm"

# javaw-core writes its .wat/.wasm next to the source, so compile a copy inside target/.
cp "$here/java/ForeignWasm.java" "$out/ForeignWasm.java"
java -jar "$here/../../javaw-core/target/javaw-core-1.0-SNAPSHOT.jar" "$out/ForeignWasm.java"

node --no-warnings "$here/run.cjs" "$out"

# javaw-bytecode: javac output, with each native method bound to a Go or Rust export (no JavaScript glue).
root="$here/../.."
asm="$HOME/.m2/repository/org/ow2/asm"
classpath="$root/javaw-bytecode/target/classes:$root/javaw-core/target/classes:$root/javaw-unicode-data/target/classes"
for artifact in asm asm-tree asm-analysis asm-commons; do classpath="$classpath:$asm/$artifact/9.7/$artifact-9.7.jar"; done
rm -rf "$out/bytecode-classes" && mkdir -p "$out/bytecode-classes"
javac --release 17 -d "$out/bytecode-classes" "$here/bytecode/ForeignBytecode.java"
(cd "$out/bytecode-classes" && jar --create --file "$out/ForeignBytecode.jar" .)
java -cp "$classpath" one.dastec.javaw.bytecode.BytecodeCompiler --exception-runtime \
    --native-import='foreign.GoLib.add(II)I=golib:add' --native-import='foreign.GoLib.fib(I)J=golib:fib' \
    --native-import='foreign.RustLib.mul(II)I=rustlib:mul' --native-import='foreign.RustLib.hypot(DD)D=rustlib:hypot' \
    "$out/ForeignBytecode.jar" "$out/ForeignBytecode.wat"
# wabt before 1.0.36 needs the exception-handling flag; later versions enable it by default and reject the flag.
flags=(); wat2wasm --help | grep -q -- --enable-exceptions && flags=(--enable-exceptions)
wat2wasm ${flags[@]+"${flags[@]}"} "$out/ForeignBytecode.wat" -o "$out/ForeignBytecode.wasm"
node --no-warnings "$here/run-bytecode.cjs" "$out"
