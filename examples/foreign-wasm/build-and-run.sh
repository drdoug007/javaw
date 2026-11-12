#!/usr/bin/env bash
# Builds the Go and Rust libraries and the Java module into examples/foreign-wasm/target/, then runs them in Node.
# Needs: Go 1.24+, Rust with the wasm32-unknown-unknown target, wabt (wat2wasm), Node, and the javaw-core jar
# (mvn -pl javaw-core package -DskipTests).
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
