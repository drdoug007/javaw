# Bytecode LineNumberReader

Revision `line-number-reader-v307` links `java.io.LineNumberReader` in the bundled exception runtime, on top of
`BufferedReader` (which gains the package-private `readLine(boolean, boolean[])` the JDK subclass calls).

Contract:

- Line counting: `lineNumber` starts at 0 and counts each line terminator `\n`, `\r` or `\r\n` (one line) seen
  through `read()`, `read(char[],int,int)` and `readLine()`. `read()` returns `'\n'` (10) for both `\r` and
  `\n` (a `\r` sets `skipLF`, so a following `\n` is read past with another `super.read()`); a `\r\n` split
  across two `read(char[],int,int)` calls still counts once because `skipLF` survives between calls, and the
  returned chars are never translated. At end of input, one more line is added when the last thing seen was an
  ordinary char (`prevChar == CHAR`); after a terminator, EOF, or a `skip` that skipped something
  (`prevChar = NONE`), it does not. `setLineNumber` stores the value verbatim and `getLineNumber` returns it.
- `skip(n)`: `IllegalArgumentException("skip() value is negative")` before any stream check; a `skipBuffer` of
  `min(n, 8192)` chars is allocated once and reused when big enough; the loop calls the overridable
  `this.read(char[],int,int)` (never `super`) until `-1` and returns the count actually skipped.
- `mark(readAheadLimit)`: the limit is incremented by 1 before `super.mark` when `skipLF` is set (the pending
  `\n`), which overflows `Integer.MAX_VALUE` to a negative limit and throws `IllegalArgumentException`; the
  line number and `skipLF` are saved. `reset()` is `super.reset()` first, then restores the line number and
  `skipLF` (`prevChar` is not saved); a failed reset changes nothing.
- After `close()`, `getLineNumber`/`setLineNumber` still work; `read`, `readLine`, `skip(1)`, `mark`, `reset`
  and `ready` throw `IOException: Stream closed` (inherited from `BufferedReader`); `close`, `ready`,
  `markSupported`, `read(char[])` and `transferTo(Writer)` are inherited, and `lines()` stays unlinked.

Evidence: `BytecodeLineNumberReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
