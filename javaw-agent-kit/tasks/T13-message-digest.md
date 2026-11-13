# T13 MessageDigest with SHA-256

```kit
id: T13
slug: message-digest
title: link java.security.MessageDigest with a built-in SHA-256
tests: BytecodeMessageDigestTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/GeneralSecurityException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/NoSuchAlgorithmException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/DigestException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/MessageDigestSpi.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/MessageDigest.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Digests.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/BlockDigest.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Sha256.java
allow: docs/bytecode-message-digest.md
```

## Goal

Add `java.security.MessageDigest` with SHA-256. Hibernate `NamingHelper.hashedName` (getInstance/update/digest/reset),
H2 `HashFunction` and `SHA3` (H2's SHA3 **extends** MessageDigest through `protected MessageDigest(String)`),
Spring `DigestUtils`/`ApplicationTemp` and Tomcat (`isEqual`, `update(byte[],int,int)`) use it.
**Design decision:** a fixed built-in algorithm table. There is no `java.security.Provider`/`Security` framework.

## Members to implement

- `GeneralSecurityException extends Exception`, `NoSuchAlgorithmException extends GeneralSecurityException`,
  `DigestException extends GeneralSecurityException`: each with `()`, `(String)`, `(String, Throwable)`, `(Throwable)`.
- `public abstract class MessageDigestSpi`: `public MessageDigestSpi()`, `protected int engineGetDigestLength()`
  (returns 0), `protected abstract void engineUpdate(byte)`, `protected abstract void engineUpdate(byte[], int, int)`,
  `protected abstract byte[] engineDigest()`, `protected abstract void engineReset()`,
  `protected int engineDigest(byte[] buf, int offset, int len) throws DigestException`, `public Object clone() throws CloneNotSupportedException`.
- `public abstract class MessageDigest extends MessageDigestSpi`: `protected MessageDigest(String algorithm)`,
  `static MessageDigest getInstance(String) throws NoSuchAlgorithmException`, `void update(byte)`, `void update(byte[])`,
  `void update(byte[], int, int)`, `byte[] digest()`, `byte[] digest(byte[])`,
  `int digest(byte[], int, int) throws DigestException`, `void reset()`, `final String getAlgorithm()`,
  `final int getDigestLength()`, `static boolean isEqual(byte[], byte[])`, `String toString()`,
  `Object clone() throws CloneNotSupportedException`.
- Internal (`package javaw.runtime;`): `Digests` with `public static MessageDigest create(String algorithm)` (returns a new
  engine or `null`), and `public final class Sha256 extends BlockDigest` (`Sha256(String algorithm)`).
- `public abstract class javaw.runtime.BlockDigest extends MessageDigest implements Cloneable`. **T14 builds MD5,
  SHA-1 and SHA-512 on it, so implement every parameter even though SHA-256 uses only some:**
  `protected BlockDigest(String algorithm, String canonicalName, int digestLength, int blockSize, int lengthBytes, boolean littleEndian)`;
  abstract `protected void compress(byte[] block, int offset)` (one `blockSize` block), `protected void output(byte[] out,
  int offset)` (write `digestLength` bytes of final state), `protected void resetState()` (initial chaining values);
  `final` `engine*` methods; `clone()`. Padding contract: after `0x80` and zeros, the last `lengthBytes` bytes of the
  final block hold the message length **in bits**, most significant byte first if `littleEndian` is false, least
  significant first if true (MD5); `lengthBytes` is 8 or 16 (SHA-512 uses 16; its high 8 bytes are 0).
  SHA-256 passes `(algorithm, "SHA-256", 32, 64, 8, false)`.

## Members to leave out

`getInstance(String, String)`, `getInstance(String, Provider)`, `getProvider()` (no Provider class),
`update(ByteBuffer)` and `engineUpdate(ByteBuffer)` (`java.nio.ByteBuffer` does not exist). The test checks three of them.
SHA-1/MD5/SHA-512 are task T14.

## JDK behaviour to match

- `getInstance(null)`: `NullPointerException("null algorithm name")`. Names are case-insensitive. SHA-256 answers to
  `SHA-256`, `SHA256`, `2.16.840.1.101.3.4.2.1`, `OID.2.16.840.1.101.3.4.2.1`. Anything else:
  `NoSuchAlgorithmException(name + " MessageDigest not available")` (`""` gives `" MessageDigest not available"`).
- `getAlgorithm()` returns the string **as passed** to `getInstance` (`"sha256"` stays `"sha256"`). `getDigestLength()` = 32.
- `update(byte[] b, int off, int len)`: `b == null` -> `IllegalArgumentException("No input buffer given")`;
  `b.length - off < len` -> `IllegalArgumentException("Input buffer too short")`; then the engine throws
  `ArrayIndexOutOfBoundsException` if `off < 0 || len < 0`. `len == 0` does nothing. `update((byte[]) null)` -> NPE.
- `digest()` returns the hash and resets the digest; a second `digest()` is the hash of nothing.
  `digest(byte[] input)` = `update(input)` then `digest()`.
- `digest(byte[] buf, int off, int len)`: `buf == null` -> `IllegalArgumentException("No output buffer given")`;
  `buf.length - off < len` -> `IllegalArgumentException("Output buffer too small for specified offset and length")`;
  `len < 32` -> `DigestException("Length must be at least 32 for SHA-256digests")` (no space before `digests`, the
  name is always the canonical `SHA-256`). Writes 32 bytes at `off`, returns 32, resets.
- `isEqual(a, b)`: true if same reference (so `null, null` is true); false if exactly one is null; else equal length and
  equal bytes.
- `toString()`: `algorithm + " Message Digest from " + provider + ", " + state + "\n"` where provider is `SUN` for
  objects from `getInstance` and `(no provider)` for user subclasses, and state is `<initialized>` (new, after
  `digest`/`reset`) or `<in progress>` (after any `update`).
- `clone()`: if `this instanceof Cloneable` return `super.clone()`, else throw `CloneNotSupportedException`. Built-in
  digests are Cloneable and a clone continues independently (deep-copy the arrays).
- `getDigestLength()`: `engineGetDigestLength()`, or if that is 0, the length of `clone().digest()` (0 if not cloneable).
- `MessageDigestSpi.engineDigest(buf, off, len)` default (used by user subclasses): `engineDigest()`, then
  `DigestException("partial digests not returned")` if `len` is too small, copy, return the length
  (`userDigestIntoBuffer`). `getDigestLength()` fallback is checked by `digestLengthFallback` (0 when not Cloneable).

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeMessageDigestTest.java`.
2. Write the three exceptions (copy `java/lang/IllegalStateException.java`, change package and names).
3. Write `MessageDigestSpi` and `MessageDigest`. `getInstance` calls `javaw.runtime.Digests.create(algorithm)`, throws if
   it returns null, and marks the result's provider as `SUN` (a private `String providerName` field).
4. Write `BlockDigest` (see Hints) and `Sha256`, then `Digests.create`.
5. Register in `BytecodeExceptionRuntime.java`: add these lines just before `paths.add("javaw/runtime/ExceptionSupport.java");`:
   `for(String name:List.of("GeneralSecurityException","NoSuchAlgorithmException","DigestException","MessageDigestSpi","MessageDigest"))paths.add("java/security/"+name+".java");`
   and `for(String name:List.of("Digests","BlockDigest","Sha256"))paths.add("javaw/runtime/"+name+".java");`
6. `javaw-agent-kit/bin/kit check T13` until PASS, then docs, report, `kit finish T13`.

## Hints

- `BlockDigest` keeps `byte[] buffer` (one block), `int used`, `long count` (bytes so far). The tests here only reach
  the big-endian, 8-byte-length, 64-byte-block path; write the `littleEndian`/`lengthBytes` branches carefully anyway
  (T14 checks them).
- Constants: copy the 64 `ROUND_CONSTS` and the 8 `INITIAL_HASHES` of `class SHA256` (not SHA224) from
  `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/sun/security/provider/SHA2.java`.
  Copying constant tables is allowed (one wrong digit only shows up as a wrong hash); write the round logic yourself.
- Padding (`finish`): append `0x80`; if fewer than `lengthBytes` bytes remain in the block, zero-fill, compress, start a
  new block; zero-fill; write `bits = count * 8`: for `i` in 0..7, `(byte)(bits >>> (8*i))` goes to
  `buffer[blockSize - lengthBytes + i]` if little-endian, else `buffer[blockSize - 1 - i]`; compress; `output`; reset.
  The 55/56/63/64 byte cases in `paddingBoundaries` catch an off-by-one here.
- `engineUpdate(byte[],off,len)`: first top up a partly filled buffer, then compress whole blocks straight from the
  input, then copy the tail. `engineUpdate(byte)` = put one byte in the buffer, compress when full.
- SHA-256 needs `>>>` (not `>>`) for `w[t-15] >>> 3` and `w[t-2] >>> 10`; read bytes as `(b & 0xff)`.
  `Integer.rotateRight` exists.
- Words are big-endian: `w[t] = (b[i]&0xff)<<24 | (b[i+1]&0xff)<<16 | (b[i+2]&0xff)<<8 | (b[i+3]&0xff)`.
- `clone()` in `Sha256`: `Sha256 copy = (Sha256) super.clone(); copy.h = h.clone(); ...`; BlockDigest's clone copies `buffer`.
  Without this the `cloneInProgress` fixture shows the copy and the original sharing state.
- `MessageDigest` must keep `protected MessageDigest(String)`: the `userSubclass` fixture extends it like H2's SHA3.
- Speed: 1,000,000 bytes in 1000-byte chunks plus 100,000 single-byte updates take about 2 s per test in Wasm.
- Calibration: the reference (about 330 lines over 8 files) passed the first `kit check`; no existing test changed.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
sha256KnownAnswers, blockBoundariesMultiPartAndReset, outputBuffersFailuresAndExceptions, namesCloneToStringAndSubclasses, userSubclassDefaults: if the message says `link a complete acyclic type hierarchy: java/security/MessageDigest`, the new classes are not linked: add the two `for(String name:List.of(...))` registration lines from Step 5 just before `paths.add("javaw/runtime/ExceptionSupport.java");` in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit) and check the chain MessageDigest extends MessageDigestSpi, NoSuchAlgorithmException and DigestException extend GeneralSecurityException extends Exception; `Cannot build bytecode exception runtime` means javac rejected one of your runtime files: fix the error it quotes.
emptyAndAbc, nistLong: if even SHA-256 of "" is wrong, look first at Sha256 (compress, output, resetState): call resetState() at the end of the Sha256 constructor, not from BlockDigest's constructor (Sha256's h array is still null there); read words big-endian with `& 0xff` on every byte, use `>>>` (never `>>`) in `w[t-15] >>> 3` and `w[t-2] >>> 10`, rotate with Integer.rotateRight (7,18 and 17,19 in the schedule; 6,11,25 for e; 2,13,22 for a), copy ROUND_CONSTS and class SHA256's INITIAL_HASHES from the JDK SHA2.java, and write each h[i] high byte first in output; for "" BlockDigest.finish only has to put 0x80 at buffer[0] and zeros after it.
nistTwoBlock, paddingBoundaries: if emptyAndAbc is right but the 56-byte nistTwoBlock or lengths 55/56/119/120 differ, fix BlockDigest.finish: after `buffer[used++] = (byte) 0x80;` start a second block exactly when `used > blockSize - lengthBytes` (zero-fill, compress, used = 0), then zero-fill to blockSize and write the BIT count `bits = count * 8` into the last bytes, big-endian `buffer[blockSize - 1 - i] = (byte) (bits >>> (8 * i))` for i 0..7.
multiPartUpdates, millionA, singleByteUpdates, resetAndReuse: the bug is in BlockDigest.engineUpdate/engineReset, not Sha256: do `count += len` (and `count++` in engineUpdate(byte)), return early for len == 0, top up a partly filled buffer first, compress whole blocks with `compress(input, offset)` (the input and its offset, not buffer and 0, and Sha256.compress must read block[offset + 4 * t]), copy the tail into buffer; digest() must end with engineReset(), which calls resetState() and sets used = 0 and count = 0.
digestIntoBuffer, userDigestIntoBuffer: MessageDigest.digest(buf, off, len) itself throws IllegalArgumentException("No output buffer given") for null and IllegalArgumentException("Output buffer too small for specified offset and length") when `buf.length - off < len`, then calls engineDigest(buf, off, len); BlockDigest's override throws `new DigestException("Length must be at least " + digestLength + " for " + canonicalName + "digests")` (no space before digests) when len < digestLength; MessageDigestSpi's default calls engineDigest() FIRST (the data is consumed even on failure), then throws DigestException("partial digests not returned") if len < result.length, else copies and returns the length.
updateFailures: MessageDigest.update(b, off, len) throws IllegalArgumentException("No input buffer given") for null, then IllegalArgumentException("Input buffer too short") when `b.length - off < len`, before calling the engine; off = -1 passes that check, so BlockDigest.engineUpdate(byte[], int, int) must itself `throw new ArrayIndexOutOfBoundsException(...)` when `off < 0 || len < 0` (do not rely on System.arraycopy); update((byte[]) null) simply uses input.length, which throws NullPointerException.
names, unknownNames: getInstance starts with `Objects.requireNonNull(algorithm, "null algorithm name")`, then Digests.create compares with equalsIgnoreCase against each alias (SHA-256, SHA256, 2.16.840.1.101.3.4.2.1, OID.2.16.840.1.101.3.4.2.1; no startsWith/contains, so SHA-257 and "SHA 256" fail) and passes the caller's spelling to `new Sha256(algorithm)` so getAlgorithm() returns it unchanged; unknown names throw `new NoSuchAlgorithmException(algorithm + " MessageDigest not available")`.
cloneInProgress, describe, userSubclass, digestLengthFallback: clone() must deep-copy (BlockDigest copies buffer, Sha256 does `copy.h = h.clone()` and gives the copy its own w); toString is `algorithm + " Message Digest from " + (provider == null ? "(no provider)" : provider) + ", " + (inProgress ? "<in progress>" : "<initialized>") + "\n"` with provider set to "SUN" only in getInstance, inProgress set by every update and cleared by digest and reset; MessageDigest.clone() throws CloneNotSupportedException unless `this instanceof Cloneable`, and getDigestLength() returns `((MessageDigest) clone()).digest().length` (0 on CloneNotSupportedException) when engineGetDigestLength() is 0.
```

## Docs

Create `docs/bytecode-message-digest.md` with this skeleton (fill in the brackets):

    # Bytecode MessageDigest

    Revision `message-digest-vN` links `java.security.MessageDigest` (SHA-256), `MessageDigestSpi` and the
    `GeneralSecurityException`/`NoSuchAlgorithmException`/`DigestException` family in the bundled exception runtime.

    Contract:

    - <fixed built-in algorithm table, names, update/digest/reset rules, failure messages, clone, toString>

    Evidence: `BytecodeMessageDigestTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). Provider overloads, `getProvider()` and `update(ByteBuffer)` are rejected before WAT generation.
