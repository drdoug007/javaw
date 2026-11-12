// Runs the javaw-bytecode module against the Go and Rust modules. Their export objects are passed as the Java
// module's import namespaces, so each Java native call is a direct Wasm-to-Wasm call.
// Usage: node run-bytecode.cjs <directory containing golib.wasm, rustlib.wasm and ForeignBytecode.wasm>
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const { WASI } = require('node:wasi');
const dir = process.argv[2], file = name => fs.readFileSync(path.join(dir, name));
(async () => {
  const wasi = new WASI({ version: 'preview1', args: [], env: {} });
  const go = await WebAssembly.instantiate(file('golib.wasm'), wasi.getImportObject());
  wasi.initialize(go.instance);
  const rust = await WebAssembly.instantiate(file('rustlib.wasm'), {});

  const module = new WebAssembly.Module(file('ForeignBytecode.wasm'));
  console.log('imports:', WebAssembly.Module.imports(module).map(i => `${i.module}.${i.name}`).join(', '));
  const memory = new WebAssembly.Memory({ initial: 2 });
  const java = new WebAssembly.Instance(module, { env: { memory }, golib: go.instance.exports, rustlib: rust.instance.exports }).exports;

  const results = {
    'goSum(40, 2)': [java['foreign.ForeignBytecode.goSum'](40, 2), 84],
    'goFibonacci(90)': [java['foreign.ForeignBytecode.goFibonacci'](90), 2880067194370816121n],
    'rustProduct(6, 7)': [java['foreign.ForeignBytecode.rustProduct'](6, 7), 55],
    'rustHypot(3, 4)': [java['foreign.ForeignBytecode.rustHypot'](3, 4), 5],
    // "[55, 6765, 832040, 102334155, 12586269025] 13.0".length
    'summary()': [java['foreign.ForeignBytecode.summary'](), 47],
  };
  for (const [call, [actual, expected]] of Object.entries(results)) {
    console.log(`${call.padEnd(18)} = ${actual}`);
    assert.equal(actual, expected, call);
  }
  console.log('all bytecode foreign-module calls match');
})().catch(error => { console.error(error); process.exit(1); });
