// Browser host for Nebula Run. Java (nebula.wasm, compiled by javaw-bytecode) decides the rules, drone behavior,
// collisions, scoring and every draw and sound command. This page maps those commands to canvas and WebAudio calls,
// and owns input mapping, loop timing, tone scheduling, the state banners and the stored best score.
'use strict';
(async () => {
  const STATES = ['IGNITION', '', 'CAUGHT', 'SECTOR CLEAR', 'GAME OVER', 'PAUSED'];
  const coarse = matchMedia('(pointer: coarse)').matches, resumeKey = coarse ? 'tap ⏯' : 'press Enter';
  // Storage can be unavailable (privacy modes, file: pages); the best score then lasts for the visit only.
  const storage = {
    get() { try { return Number(localStorage.getItem('nebula-run-best') || 0); } catch { return 0; } },
    set(value) { try { localStorage.setItem('nebula-run-best', value); } catch { } },
  };
  const layers = [document.getElementById('sprites').getContext('2d'), document.getElementById('maze').getContext('2d')];
  let ctx = layers[0];

  // ARGB ints to CSS colors, cached: the game uses a few dozen.
  const colors = new Map();
  const css = argb => {
    let color = colors.get(argb);
    if (color === undefined) {
      const a = (argb >>> 24) / 255, r = (argb >>> 16) & 255, g = (argb >>> 8) & 255, b = argb & 255;
      colors.set(argb, color = `rgba(${r},${g},${b},${a.toFixed(3)})`);
    }
    return color;
  };

  let audio = null, muted = false;
  const waves = ['sine', 'square', 'sawtooth', 'triangle'];
  let audioTime = 0;

  const score = document.getElementById('score'), best = document.getElementById('best'), level = document.getElementById('level');
  const lives = document.getElementById('lives'), message = document.getElementById('message');
  let bestScore = storage.get(), shown = '';
  best.textContent = bestScore;

  const imports = {
    canvas: {
      layer: index => { ctx = layers[index]; },
      clear: argb => {
        ctx.clearRect(0, 0, 504, 504);
        if (argb >>> 24) { ctx.fillStyle = css(argb); ctx.fillRect(0, 0, 504, 504); }
      },
      rect: (x, y, w, h, argb) => { ctx.fillStyle = css(argb); ctx.fillRect(x, y, w, h); },
      circle: (x, y, r, argb) => { ctx.fillStyle = css(argb); ctx.beginPath(); ctx.arc(x, y, r, 0, 2 * Math.PI); ctx.fill(); },
      diamond: (x, y, r, argb) => {
        ctx.fillStyle = css(argb); ctx.beginPath();
        ctx.moveTo(x, y - r); ctx.lineTo(x + r, y); ctx.lineTo(x, y + r); ctx.lineTo(x - r, y); ctx.closePath(); ctx.fill();
      },
    },
    hud: {
      update: (points, comets, sector, state) => {
        const key = `${points}/${comets}/${sector}/${state}`;
        if (key === shown) return;
        shown = key;
        score.textContent = points; level.textContent = sector;
        lives.innerHTML = 'COMETS' + '<span></span>'.repeat(Math.max(0, comets));
        if (points > bestScore) { bestScore = points; best.textContent = points; storage.set(points); }
        message.innerHTML = STATES[state] + (state === 4 ? `<small>${resumeKey} to relaunch</small>` : state === 5 ? `<small>${resumeKey} to resume</small>` : '');
      },
    },
    audio: {
      tone: (hertz, millis, wave) => {
        if (!audio || muted) return;
        // Tones from one frame play one after another, not on top of each other.
        const start = Math.max(audio.currentTime, audioTime), seconds = millis / 1000;
        const oscillator = audio.createOscillator(), gain = audio.createGain();
        oscillator.type = waves[wave] || 'sine';
        oscillator.frequency.value = hertz;
        gain.gain.setValueAtTime(0.06, start);
        gain.gain.exponentialRampToValueAtTime(0.0001, start + seconds);
        oscillator.connect(gain).connect(audio.destination);
        oscillator.start(start); oscillator.stop(start + seconds);
        audioTime = millis > 60 ? start + seconds * 0.9 : start;
      },
    },
  };

  const memory = new WebAssembly.Memory({ initial: 2 });
  const response = await fetch('nebula.wasm');
  const { instance } = WebAssembly.instantiateStreaming
    ? await WebAssembly.instantiateStreaming(response, { env: { memory }, ...imports })
    : await WebAssembly.instantiate(await response.arrayBuffer(), { env: { memory }, ...imports });
  const game = instance.exports;
  const call = name => game['nebula.Nebula.' + name];
  const update = call('update'), render = call('render'), steer = call('steer'), press = call('press');
  window.nebula = game;  // for the console: nebula['nebula.Nebula.score']()

  let started = false;
  const title = document.getElementById('title');
  function launch() {
    if (started) return;
    started = true;
    title.remove();
    audio = audio || new (window.AudioContext || window.webkitAudioContext)();
    resumeAudio();
    call('start')(Date.now() & 0x7fffffff);
    requestAnimationFrame(loop);
  }

  // Some browsers start the context suspended until a gesture they accept; retry on later gestures.
  function resumeAudio() { if (audio && audio.state !== 'running') audio.resume().catch(() => { }); }
  addEventListener('pointerup', resumeAudio);

  const keys = { ArrowUp: 0, KeyW: 0, ArrowLeft: 1, KeyA: 1, ArrowDown: 2, KeyS: 2, ArrowRight: 3, KeyD: 3 };
  addEventListener('keydown', event => {
    if (event.ctrlKey || event.metaKey || event.altKey) return;  // leave browser shortcuts alone
    resumeAudio();
    if (event.code in keys) { event.preventDefault(); if (started) steer(keys[event.code]); }
    else if (event.code === 'Enter' || event.code === 'Space') {
      event.preventDefault();
      if (!event.repeat) started ? press() : launch();  // a held key toggles pause once
    }
    else if (event.code === 'KeyM') muted = !muted;
  });
  title.addEventListener('click', launch);
  document.querySelectorAll('#pad [data-dir]').forEach(button =>
    button.addEventListener('pointerdown', () => { launch(); steer(Number(button.dataset.dir)); }));
  document.querySelector('#pad [data-press]').addEventListener('pointerdown', () => started ? press() : launch());
  // Swipes on the maze steer too.
  let touch = null;
  const stage = document.getElementById('stage');
  stage.addEventListener('pointerdown', event => { touch = [event.clientX, event.clientY]; });
  stage.addEventListener('pointerup', event => {
    if (!touch || !started) return;
    const dx = event.clientX - touch[0], dy = event.clientY - touch[1];
    if (Math.max(Math.abs(dx), Math.abs(dy)) > 24) steer(Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? 1 : 3) : (dy < 0 ? 0 : 2));
    touch = null;
  });

  // Fixed 60 Hz game steps, rendered once per animation frame; the managed heap is collected between frames.
  const STEP = 1000 / 60;
  let last = performance.now(), pending = 0;
  function loop(now) {
    pending = Math.min(pending + now - last, 5 * STEP);
    last = now;
    while (pending >= STEP) { update(); pending -= STEP; }
    render();
    game.__gc_collect();
    requestAnimationFrame(loop);
  }

  // Draw the first frame behind the title.
  call('start')(1);
  render();
})().catch(error => {
  document.getElementById('title')?.remove();
  document.getElementById('message').textContent = 'Failed to load: ' + error.message;
  console.error(error);
});
