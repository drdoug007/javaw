# T16 Checksum, CRC32 and Adler32

```kit
id: T16
slug: checksums
title: link java.util.zip.Checksum, CRC32 and Adler32
tests: BytecodeChecksumTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/zip/Checksum.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/zip/CRC32.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/zip/Adler32.java
allow: docs/bytecode-checksums.md
```

## Goal

Add the `java.util.zip` checksums: the `Checksum` interface and its two classic implementations `CRC32` and
`Adler32`. The pinned AspectJ weaver uses `CRC32` (`external-types.tsv` rows for
`org/aspectj/weaver/tools/cache/AbstractCacheBacking`, `DefaultCacheKeyResolver` and `SimpleCache`;
`member-references.tsv`: `AbstractCacheBacking.crc([B)J` calls `CRC32.<init>()V`, `CRC32.update([B)V` and
`CRC32.getValue()J`). `CRC32.update([B)V` has no body in `CRC32`: it resolves to the Java 9 default method
`Checksum.update(byte[])`, so the interface default must exist. `Adler32` is the zlib stream checksum; a future
pure-Java `Deflater`/`Inflater` would need it (no pinned artifact references it directly yet). This is pure arithmetic: no
`ByteBuffer`, no native code, no intrinsics.

## Members to implement

Three new files in a new package directory `javaw-bytecode/src/main/resources/exception-runtime/java/util/zip/`
(`package java.util.zip;`):

- `public interface Checksum`:
  - `void update(int b)` (abstract)
  - `default void update(byte[] b)` (body: `update(b, 0, b.length);`)
  - `void update(byte[] b, int off, int len)` (abstract)
  - `long getValue()` (abstract)
  - `void reset()` (abstract)
- `public class CRC32 implements Checksum` with one private `int crc` field (starts at 0):
  `public CRC32()`, `public void update(int b)`, `public void update(byte[] b, int off, int len)`,
  `public void reset()`, `public long getValue()`.
- `public class Adler32 implements Checksum` with one private `int adler` field (starts at **1**):
  `public Adler32()`, `public void update(int b)`, `public void update(byte[] b, int off, int len)`,
  `public void reset()`, `public long getValue()`.

`CRC32` and `Adler32` do **not** declare `update(byte[])`; they inherit the `Checksum` default, as in the JDK.

## Members to leave out

- `Checksum.update(ByteBuffer)`, `CRC32.update(ByteBuffer)`, `Adler32.update(ByteBuffer)`: the runtime has no
  `java.nio.ByteBuffer`, so mentioning it breaks admission. Do not import `java.nio` at all.
- `java.util.zip.CRC32C` (a different polynomial; not needed yet).

The test checks that these four stay unlinked (`leavesByteBufferOverloadsAndCrc32cUnlinked`).

## JDK behaviour to match

- **CRC32** is the standard reflected CRC-32 (zip, PNG, Ethernet): polynomial `0xEDB88320`, register preset to
  all ones, output inverted. Keep the field `crc` as the *finished* value (0 for no data), so one byte is
  `c = ~crc; c = TABLE[(c ^ b) & 0xff] ^ (c >>> 8); crc = ~c;`. Known values:
  `""` -> `0`, `"a"` -> `e8b7be43`, `"abc"` -> `352441c2`, `"123456789"` -> `cbf43926`.
- **Adler32** keeps `a` (low 16 bits) and `b` (high 16 bits) in one `int adler`; a fresh or reset `Adler32` has
  `adler = 1` (so `getValue()` is 1 before any data). Per byte `x` (unsigned, `x & 0xff`):
  `a = (a + x) % 65521; b = (b + a) % 65521;` then `adler = (b << 16) | a`. Known values:
  `""` -> `1`, `"a"` -> `620062`, `"abc"` -> `24d0127`, `"Wikipedia"` -> `11e60398`.
- `getValue()` returns the 32 bits **unsigned**: `(long) crc & 0xffffffffL` (same for `adler`). So
  `"123456789"` gives `3421780262`, never a negative number.
- `update(int b)` uses only the low 8 bits of `b`: `update(0x161)`, `update(-159)` and `update('a')` are all the
  byte `0x61`; `update(-1)` is the same as `update(255)`.
- `reset()`: CRC32 back to 0, Adler32 back to 1.
- `update(byte[] b, int off, int len)` checks, **in this order**, before changing any state:
  1. `b == null` -> `throw new NullPointerException();` (no message, so `getMessage()` is `null`). This comes
     first even when the range is also bad (`update(null, 5, -1)` throws `NullPointerException`).
  2. the JDK `checkFromIndexSize` range check: if `(b.length | off | len) < 0 || len > b.length - off` throw
     `new ArrayIndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length)`.
     Note the text is literally `off + len` written out, not the sum:
     `update(new byte[3], 1, 5)` -> `Range [1, 1 + 5) out of bounds for length 3`;
     `(-1, 1)` -> `Range [-1, -1 + 1) out of bounds for length 3`; `(1, -1)` -> `Range [1, 1 + -1) ...`;
     `(1, Integer.MAX_VALUE)` -> `Range [1, 1 + 2147483647) ...` (use `len > b.length - off`, never
     `off + len > b.length`, which overflows); `(4, 0)` -> `Range [4, 4 + 0) ...`. `(3, 0)` on a 3-byte array
     is fine and does nothing.
  3. then process bytes `off .. off + len - 1`. A failed call leaves the checksum unchanged.
- The `Checksum.update(byte[])` default with a null array throws `NullPointerException` from `b.length` (the test
  checks only the type, not the message).
- Implementations can be mixed freely: `update(data, 0, 1)` then `update(data, 1, n - 1)` equals
  `update(data)`; a fixture class `Recorder implements Checksum` relies on the default `update(byte[])` calling
  its own `update(b, 0, b.length)`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeChecksumTest.java`.
2. Create the directory `javaw-bytecode/src/main/resources/exception-runtime/java/util/zip/` and the three files
   `Checksum.java`, `CRC32.java`, `Adler32.java`.
3. Register each file (the command adds a `paths.add(...)` line before `paths.add("javaw/runtime/ExceptionSupport.java");`):
   - `javaw-agent-kit/bin/kit register java/util/zip/Checksum.java`
   - `javaw-agent-kit/bin/kit register java/util/zip/CRC32.java`
   - `javaw-agent-kit/bin/kit register java/util/zip/Adler32.java`
4. `javaw-agent-kit/bin/kit check T16` until PASS.
5. Write `docs/bytecode-checksums.md` (below), the report, then `javaw-agent-kit/bin/kit finish T16`.

## Hints

- Copy the interface style from `java/util/Collection.java` (a `default` method in an interface). `Checksum` is
  about 10 lines.
- CRC32 table: `private static final int[] TABLE = new int[256];` filled in a `static { ... }` block:
  for each `n` in 0..255, `int c = n; for (int k = 0; k < 8; k++) c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; TABLE[n] = c;`.
  Use `>>>` (unsigned shift) everywhere; `>>` sign-extends and gives wrong values for every byte >= 0x80.
  `0xEDB88320` is a negative `int` literal in Java; that is fine.
- Write one private helper per class that does the real work, for example `private void add(int x)` taking an
  already masked byte (`x & 0xff`), and call it from both `update` methods.
- Put the null and range checks in one private static method (e.g. `checkRange(byte[] b, int off, int len)`)
  and call it from both classes, or copy it into each; `java.util.Objects` in this runtime has **no**
  `checkFromIndexSize`/`checkIndex`, so do not call them.
- `ArrayIndexOutOfBoundsException` (not `IndexOutOfBoundsException`) with the `Range [...)` message. Its
  `(int)` constructor gives a different message (`Array index out of range: n`); use the `(String)` one.
- Adler32: do `% 65521` after **every** byte, so `int` arithmetic never overflows (the test feeds 120000 bytes
  of `0xff`). Do not try zlib's NMAX=5552 batching; it is an optimisation and easy to get wrong.
- Keep `getValue()` as `return (long) crc & 0xffffffffL;` (and the same with `adler`). `(long) crc` alone is
  negative for half of all inputs. Do not call `Integer.toUnsignedLong`; the mask is enough.
- Do not add `update(byte[])` to `CRC32`/`Adler32`, `java.nio` imports, `synchronized`, or `native`.
- An inherited interface default (`CRC32.update(byte[])` resolving to `Checksum.update(byte[])`) links fine in
  this compiler; you do not need a forwarding override.
- Calibration: the reference passed kit check after 1 run (about 77 new lines: Checksum 10, CRC32 40, Adler32 27,
  with Adler32 reusing a package-private static `CRC32.checkRange`). Before the reference, every matchesJvm test
  failed with `link a complete acyclic type hierarchy: java/util/zip/Checksum`; the unlinked-members test passes
  from the start by design.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
computesCrc32, computesAdler32, checksArgumentsLikeTheJdk, dispatchesThroughChecksumInterface: if the message says `unsupported ... java/util/zip/CRC32` (or Adler32/Checksum, or `link a complete acyclic type hierarchy`), the classes are not registered or lack that member: run `javaw-agent-kit/bin/kit register java/util/zip/Checksum.java` (and the same for CRC32.java and Adler32.java; do not edit BytecodeExceptionRuntime.java by hand), and give the class a public method with exactly that name and descriptor (`update(I)V`, `update([BII)V`, `getValue()J`, `reset()V`; `update([B)V` is the Checksum default); `Cannot build bytecode exception runtime` means javac rejected one of your files: fix the line it quotes.
crcVectors, crcAllBytes, crcIncremental: CRC32 values wrong means the table or the per-byte step is wrong: build TABLE with `c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1` eight times per entry, and per byte do `int c = ~crc; c = TABLE[(c ^ x) & 0xff] ^ (c >>> 8); crc = ~c;` (`>>>`, not `>>`; invert before AND after; the field holds the finished value, 0 at start). `"123456789"` must give `cbf43926`.
crcUnsignedValue: getValue() must be `(long) crc & 0xffffffffL`; a negative number or `ffffffffcbf43926` means the mask is missing.
crcSingleBytes, adlerSingleBytesAndReset: update(int b) must use only `b & 0xff` (0x161, -159 and 'a' are the same byte; -1 is 255) before the table lookup or the Adler sums.
crcReset, independentInstances: reset() sets CRC32 `crc = 0` and Adler32 `adler = 1`; the state must be an instance field (not static) so two instances do not share it.
adlerVectors, adlerLarge, adlerIncremental: Adler32 starts at `adler = 1`; per byte `a = (a + (x & 0xff)) % 65521; b = (b + a) % 65521;` with `a = adler & 0xffff` and `b = adler >>> 16`, then `adler = (b << 16) | a`; take `% 65521` after EVERY byte (not 65536, not only at the end) or long inputs of 0xff overflow.
rangeFailures, failedUpdateKeepsState: update(b, off, len) throws `new ArrayIndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length)` when `(b.length | off | len) < 0 || len > b.length - off` (write `off + " + " + len` as text, do not add them; do not use `off + len > b.length`, it overflows for Integer.MAX_VALUE); check BEFORE touching the state; `(3, 0)` on a 3-byte array is ok; java.util.Objects has no checkFromIndexSize here.
nullArrays: update(null, off, len) must `throw new NullPointerException()` with NO message, before the range check (`update(null, 5, -1)` is a NullPointerException, not an ArrayIndexOutOfBoundsException).
defaultUpdateDelegates, throughInterface: Checksum needs `default void update(byte[] b) { update(b, 0, b.length); }`; the fixture's own Recorder class implements Checksum and only defines update(int), update(byte[], int, int), getValue and reset, so all other Checksum methods must be abstract and the default must call update(b, 0, b.length) exactly (log `r0+3`, also for an empty array `r0+0`).
leavesByteBufferOverloadsAndCrc32cUnlinked: this fails when Checksum, CRC32 or Adler32 declares update(java.nio.ByteBuffer) or a CRC32C class exists: delete that method/file; there is no ByteBuffer in the runtime.
```

## Docs

Create `docs/bytecode-checksums.md` with this skeleton (fill in the brackets):

    # Bytecode Checksums

    Revision `checksums-vN` links `java.util.zip.Checksum`, `CRC32` and `Adler32` in the bundled exception runtime.

    Contract:

    - <CRC32 polynomial and table, Adler32 modulus, unsigned getValue, update(int) low byte, reset values>
    - <argument checks: NullPointerException first, then ArrayIndexOutOfBoundsException "Range [off, off + len) out of bounds for length n">
    - `Checksum.update(byte[])` is the interface default; `update(ByteBuffer)` and `CRC32C` are not linked.

    Evidence: `BytecodeChecksumTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). The `ByteBuffer` overloads and `CRC32C` are rejected before WAT generation.
