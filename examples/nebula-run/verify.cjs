// Headless checks for Nebula Run, after build.sh:
//  1. The autopilot simulation (nebula.Simulation.run) in Wasm must match the JVM run exactly: draw-command hash,
//     command count, score, level, lives and games.
//  2. The browser entry points (nebula.Nebula.*) run 3,000 frames against counting stub imports without trapping,
//     with the managed heap collected between frames and staying bounded.
// Usage: node verify.cjs <target directory> "<JVM result line>" [seed frames]
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const [dir, expected, seed = '2026', frames = '20000'] = process.argv.slice(2);
const compiled = new WebAssembly.Module(fs.readFileSync(path.join(dir, 'site', 'nebula.wasm')));

const calls = {};
const count = name => (...args) => { calls[name] = (calls[name] || 0) + 1; };
const imports = {
  canvas: { layer: count('layer'), clear: count('clear'), rect: count('rect'), circle: count('circle'), diamond: count('diamond') },
  hud: { update: count('hud') },
  audio: { tone: count('tone') },
};
console.log('imports:', WebAssembly.Module.imports(compiled).map(i => `${i.module}.${i.name}`).join(', '));

function instance() {
  const memory = new WebAssembly.Memory({ initial: 2 });
  const e = new WebAssembly.Instance(compiled, { env: { memory }, ...imports }).exports;
  const text = pointer => {
    const view = new DataView(memory.buffer), chars = view.getInt32(pointer + 4, true), length = view.getInt32(chars + 4, true);
    let result = '';
    for (let i = 0; i < length; i++) result += String.fromCharCode(view.getUint16(chars + 8 + 2 * i, true));
    return result;
  };
  return { e, memory, text };
}

// 1. JVM versus Wasm.
{
  const { e, text } = instance();
  const started = process.hrtime.bigint();
  const actual = text(e['nebula.Simulation.run'](Number(seed), Number(frames)));
  const millis = Number(process.hrtime.bigint() - started) / 1e6;
  console.log(`wasm: ${actual}  (${millis.toFixed(0)} ms for ${frames} frames)`);
  console.log(`jvm:  ${expected}`);
  assert.equal(actual, expected, 'Wasm and JVM runs differ');
}

// 2. The browser entry points, driven like the page drives them.
{
  const { e, memory } = instance();
  e['nebula.Nebula.start'](7);
  let peak = 0;
  for (let frame = 0; frame < 3000; frame++) {
    if (frame % 45 === 0) e['nebula.Nebula.steer'](frame / 45 % 4);
    if (frame === 1500 || frame === 1560) e['nebula.Nebula.press']();  // pause, then resume
    e['nebula.Nebula.update']();
    e['nebula.Nebula.render']();
    e.__gc_collect();
    peak = Math.max(peak, e.__gc_live_bytes());
  }
  console.log('stub calls over 3000 frames:', JSON.stringify(calls));
  console.log(`state=${e['nebula.Nebula.state']()} score=${e['nebula.Nebula.score']()} live heap peak=${peak} bytes, memory=${memory.buffer.byteLength} bytes`);
  for (const name of ['layer', 'clear', 'rect', 'circle', 'diamond', 'hud', 'tone']) assert.ok(calls[name] > 0, `${name} was never called`);
  assert.ok(peak < 64 * 1024, `live heap peaked at ${peak} bytes`);
  assert.ok(memory.buffer.byteLength <= 4 * 65536, 'memory grew beyond 4 pages');
}
console.log('nebula run verified');
