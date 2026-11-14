# Nebula Run

An original maze-chase game whose rules run in Java, compiled from `javac` bytecode to WebAssembly by `javaw-bytecode`. A comet gathers stardust in a 21 × 21 maze. Four drones hunt it, each with its own behavior. Nova cores dim the drones for a while, and a crystal appears twice per sector. The tunnel at the top and bottom of the maze wraps vertically.

It is the first browser demo built with the bytecode compiler rather than the source compiler. All other showcase demos use `javaw-core`.

```
mvn -o -pl javaw-bytecode -am compile      # once
examples/nebula-run/build.sh --verify      # builds target/site, then compares the JVM and Wasm runs
python3 -m http.server -d examples/nebula-run/target/site 8080
```

Open <http://localhost:8080>.

The published copy is at <https://drdoug007.github.io/javaw/>. `.github/workflows/pages.yml` builds it on pushes to `main` that touch the demo or the compiler, or on a manual run. The workflow runs `build.sh --verify` and publishes only if the JVM and Wasm runs match. It puts `pages/index.html` (the project page) at the site root and the game under `play/`, and replaces the `<!-- site-nav -->` line in the game page with links back to the project page. Repository settings must have Pages > Source set to "GitHub Actions". Controls: arrows or WASD steer, Enter or Space starts and pauses, M toggles sound. On touch screens, use the on-screen pad or swipe on the maze.

## How it is built

| Part | Where | Runs as |
| --- | --- | --- |
| Rules, movement, drone behavior, collisions, scoring, what to draw | `src/nebula/*.java` (12 source files) | javac → `javaw-bytecode --exception-runtime` → `nebula.wasm` |
| Canvas drawing, HUD text, WebAudio tones, input, the 60 Hz loop | `web/nebula-host.js` | JavaScript |
| JVM side of the comparison | `jvm/nebula/SimulationMain.java` | JVM only |

The Java code uses:
- interfaces and virtual dispatch: `Screen`, and the drone `Behavior`s;
- a lambda (the hunter behavior);
- nested classes;
- `ArrayList` and enhanced `for` loops;
- `switch` expressions on `char` and `int`;
- `long` arithmetic (the random generator);
- string concatenation;
- `Arrays.fill` and `Math`.

It runs on the bundled runtime and its managed heap, and the page calls `__gc_collect` after each frame.

Drawing goes through `CanvasScreen`, whose `static native` methods are bound to Wasm imports by `--native-import` (see [the bytecode native-import contract](../../docs/foreign-wasm-modules.md#bytecode-modules)):

```
nebula.CanvasScreen.fillRect(IIIII)V=canvas:rect      (and layer, clear, circle, diamond, hud:update, audio:tone)
```

Only numbers cross: coordinates, ARGB colors, score, lives and tone frequencies. The page supplies the imports as small JavaScript functions. Here the imports are host functions rather than another module's exports, which the same contract allows.

Each frame, the page runs fixed 60 Hz `nebula.Nebula.update` steps (at most five to catch up), one `render`, and `__gc_collect`. The maze is drawn on its own canvas layer, and only when it changes.

## Evidence

`build.sh --verify` (`verify.cjs`) checks:

1. **JVM versus Wasm, command for command.** `Simulation.run` plays 20,000 frames from a fixed seed with a deterministic autopilot, rendering every other frame into `ChecksumScreen`, which hashes every draw and sound command. The run covers deaths, level clears, game overs and restarts. On 2026-11-12, the JVM and Wasm results were identical: `hash=7fec52f19711c237 commands=1206983 score=5955 level=3 lives=1 games=4 levelsCleared=4 best=4570`. Wasm in Node 24 took 824 ms for the 20,000 frames.
2. **The browser entry points against counting stubs.** Over 3,000 frames with steering, pause and resume, every import is called. The live heap peaks at about 7.7 KB, and memory stays at its initial two pages.

`BytecodeNebulaRunTest`, in the prescribed bytecode selection, compiles these sources directly and makes both checks, with 6,000 frames and the heap's scribble mode on. It needs only the JDK, `wat2wasm` and Node, which the suite already requires.

The page was also played in Chromium (Playwright). It loads with no errors, steering and pause work, and the HUD, drone drawing, the caught animation and game over all appear.

## Limits found while building it

- **No `System.out` in Wasm.** The bundled runtime has no `java.lang.System`. Admission checks every method, used or not, so a `main` that prints is rejected. The JVM entry point therefore lives in `jvm/`, outside the Wasm JAR.
- **Only numbers cross.** HUD and state text is rendered by the page; strings would need JS glue that bytecode modules cannot express yet.
- **Module size.** `nebula.wasm` is about 700 KB unoptimized (702,783 bytes at compiler revision v272; it was 1.1 MB when the demo was added). Most of it is bundled-runtime code reachable from string concatenation and collections; see the Phase 7 notes in `Todo.md`.
