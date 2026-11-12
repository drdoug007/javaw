// Loads the Go and Rust modules, exposes their exports as globals, then runs the Java module against them.
// Usage: node run.cjs <directory containing golib.wasm, rustlib.wasm and ForeignWasm.wasm>
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const { WASI } = require('node:wasi');
const dir = process.argv[2], file = name => fs.readFileSync(path.join(dir, name));
(async () => {
  // Go: a WASI reactor (-buildmode=c-shared); initialize() runs its _initialize export once.
  const wasi = new WASI({ version: 'preview1', args: [], env: {} });
  const go = await WebAssembly.instantiate(file('golib.wasm'), wasi.getImportObject());
  wasi.initialize(go.instance);
  globalThis.golib = go.instance.exports;

  // Rust: wasm32-unknown-unknown with no imports.
  globalThis.rustlib = (await WebAssembly.instantiate(file('rustlib.wasm'), {})).instance.exports;

  // Java: compiled by javaw-core; its @JS.Import glue reads the globals above.
  const { instantiateJavaw } = require(path.resolve(__dirname, '../../javaw-core/src/main/resources/javaw-runtime.js'));
  const java = (await instantiateJavaw(file('ForeignWasm.wasm'))).instance.exports;

  const results = {
    'goSum(40, 2)': [java.goSum(40, 2), 84],
    'goFibonacci(90)': [java.goFibonacci(90), 2880067194370816121n],
    'rustProduct(6, 7)': [java.rustProduct(6, 7), 55],
    'rustHypot(3, 4)': [java.rustHypot(3, 4), 5],
    'rustVowels()': [java.rustVowels(), 11],
  };
  for (const [call, [actual, expected]] of Object.entries(results)) {
    console.log(`${call.padEnd(18)} = ${actual}`);
    assert.equal(actual, expected, call);
  }
  console.log('all foreign-module calls match');
})().catch(error => { console.error(error); process.exit(1); });
