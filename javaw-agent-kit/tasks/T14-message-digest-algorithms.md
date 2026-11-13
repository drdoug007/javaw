# T14 MessageDigest SHA-1, MD5 and SHA-512

```kit
id: T14
slug: message-digest-algorithms
title: add MD5, SHA-1, SHA-512 and SHA-384 to the built-in MessageDigest table
tests: BytecodeMessageDigestAlgorithmsTest
depends: T13
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Md5.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Sha1.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Sha512.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/Digests.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/BlockDigest.java
allow: docs/bytecode-message-digest-algorithms.md
```

## Goal

T13 added `MessageDigest` with SHA-256 and a fixed algorithm table in `javaw/runtime/Digests.java`. Add MD5, SHA-1,
SHA-512 and SHA-384 engines. Spring `DigestUtils` uses MD5, Tomcat's `DigestAuthenticator` and AspectJ `ClassGen.getSUID`
use MD5/SHA-1, and H2 `HashFunction` accepts SHA-1/SHA-512 by name. T15 (HMAC) builds on these engines.

## Members to implement

No new public JDK members. New internal engines, each `extends BlockDigest` (from T13) and implementing
`compress(byte[] block, int offset)`, `output(byte[] out, int offset)`, `resetState()` and `clone()`:

- `javaw/runtime/Md5.java`: `public final class Md5`, `public Md5(String algorithm)`:
  `super(algorithm, "MD5", 16, 64, 8, true)` (**little-endian**).
- `javaw/runtime/Sha1.java`: `public final class Sha1`, `public Sha1(String algorithm)`:
  `super(algorithm, "SHA-1", 20, 64, 8, false)`.
- `javaw/runtime/Sha512.java`: `public final class Sha512` with a private constructor and two factories
  `public static Sha512 sha512(String algorithm)` (`"SHA-512"`, 64 bytes) and `public static Sha512 sha384(String algorithm)`
  (`"SHA-384"`, 48 bytes). Both use block 128, length field 16, big-endian.
- `Digests.create`: add the names below.

## Members to leave out

SHA-224, SHA-512/224, SHA-512/256, SHA3-* and MD2 (the JDK has them; nothing pinned needs them). No new JDK API.

## JDK behaviour to match

- Names (case-insensitive; `getAlgorithm()` returns the spelling passed in):
  - SHA-1: `SHA-1`, `SHA1`, `SHA`, `1.3.14.3.2.26`, `OID.1.3.14.3.2.26`
  - MD5: `MD5` (JDK 21 also accepts `1.2.840.113549.2.5` and `OID.1.2.840.113549.2.5`, JDK 17 does not; untested, optional)
  - SHA-512: `SHA-512`, `SHA512`, `2.16.840.1.101.3.4.2.3`, `OID.2.16.840.1.101.3.4.2.3`
  - SHA-384: `SHA-384`, `SHA384`, `2.16.840.1.101.3.4.2.2`, `OID.2.16.840.1.101.3.4.2.2`
  - anything else still throws `NoSuchAlgorithmException(name + " MessageDigest not available")` (e.g. `SHA_1`, `MD6`).
- `getDigestLength()`: MD5 16, SHA-1 20, SHA-384 48, SHA-512 64. The short-buffer message uses the canonical name:
  `Length must be at least 16 for MD5digests`, `... 20 for SHA-1digests`, `... 48 for SHA-384digests`, `... 64 for SHA-512digests`.
- Known answers: MD5("") = `d41d8cd98f00b204e9800998ecf8427e`, MD5("abc") = `900150983cd24fb0d6963f7d28e17f72`,
  SHA-1("abc") = `a9993e364706816aba3e25717850c26c9cd0d89d`, SHA-512("abc") starts `ddaf35a193617aba`,
  SHA-384("abc") starts `cb00753f45a35e8b`.

## Steps

0. T13's tests only exercised BlockDigest's big-endian, 8-byte-length, 64-byte-block path. Open `BlockDigest.java`
   and check the padding code against the contract: the last `lengthBytes` bytes of the final block hold the bit
   count, least significant byte first when `littleEndian` (MD5), most significant first otherwise; with
   `lengthBytes = 16` (SHA-512) the 0x80 byte must still fit before the 16-byte field (padding needs a second block
   when more than `blockSize - lengthBytes - 1` bytes are buffered). Fix it here if needed (`BlockDigest.java` is allowed).
1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeMessageDigestAlgorithmsTest.java`
   and `javaw/runtime/Sha256.java` + `BlockDigest.java` from T13. Copy Sha256's structure.
2. Write `Sha1.java`, then `kit check T14` (the `sha1` test should pass first).
3. Write `Md5.java`, then `Sha512.java`.
4. Add the names to `Digests.create` (one `if` per algorithm, like the SHA-256 one).
5. Register: in `BytecodeExceptionRuntime.java`, find the `javaw/runtime` registration that T13 added for
   `"Digests"`, `"BlockDigest"`, `"Sha256"` and add `"Sha1"`, `"Md5"`, `"Sha512"` next to `"Sha256"` (same form T13
   used: a `List.of(...)` entry or one `paths.add("javaw/runtime/Sha1.java");` line each).
6. `javaw-agent-kit/bin/kit check T14` until PASS, then docs, report, `kit finish T14`.

## Hints

- **MD5 is little-endian everywhere**: message words are `(b[j]&0xff) | (b[j+1]&0xff)<<8 | (b[j+2]&0xff)<<16 | (b[j+3]&0xff)<<24`,
  the output writes each state word low byte first, and the bit length goes into the block low byte first (that is
  what `littleEndian = true` in the BlockDigest constructor does). Writing MD5 big-endian like SHA is the classic bug:
  every MD5 vector is then wrong while SHA-1 passes.
- MD5 rounds: `F=(b&c)|(~b&d)`, `g=i` for i<16; `F=(d&b)|(~d&c)`, `g=(5i+1)&15` for i<32; `F=b^c^d`, `g=(3i+5)&15`
  for i<48; `F=c^(b|~d)`, `g=(7i)&15`. Then `tmp=d; d=c; c=b; b=b+rotateLeft(a+F+K[i]+x[g], S[i]); a=tmp`.
  Write the 64 `K` constants out (`0xd76aa478, 0xe8c7b756, ...`); do not compute them with `Math.sin`.
  Shifts `S`: `7,12,17,22` x4, `5,9,14,20` x4, `4,11,16,23` x4, `6,10,15,21` x4. Initial state `0x67452301,
  0xefcdab89, 0x98badcfe, 0x10325476`.
- SHA-1: `w[t] = rotateLeft(w[t-3]^w[t-8]^w[t-14]^w[t-16], 1)` (the rotate is SHA-1, not SHA-0); round constants
  `0x5a827999, 0x6ed9eba1, 0x8f1bbcdc, 0xca62c1d6`; `c = rotateLeft(b, 30)`.
- SHA-512 works on `long`: `Long.rotateRight` exists. Use `>>>` for the shifts in the message schedule (`>>> 7`, `>>> 6`).
  Constants need the `L` suffix (`0x428a2f98d728ae22L`). Read words as 8 bytes big-endian:
  `value = (value << 8) | (b[i] & 0xff)`.
- SHA-384 = SHA-512 with the SHA-384 initial values, output truncated to 48 bytes. `output` can write
  `engineGetDigestLength()` bytes: `out[offset+i] = (byte)(h[i>>3] >>> (56 - 8*(i&7)))`.
- The 16-byte length field: only its low 8 bytes can be non-zero (a `long` bit count); the high 8 stay zero.
  If MD5 fails on every vector while SHA-1 passes, suspect the little-endian length (step 0) or word order; if only
  SHA-512 fails at lengths 112..127 (`sha512Long` boundaries), suspect the 16-byte length/second-block rule.
- **Copy the constant tables, do not type them from memory** (one wrong digit only shows up as a wrong hash). From
  `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/sun/security/provider/<File>.java`:
  `SHA5.java` has `ROUND_CONSTS` (80 longs) and `INITIAL_HASHES` inside `class SHA512` and `class SHA384` (not
  SHA512_224/256); `MD5.java` has the 64 T constants as the last argument of each `FF/GG/HH/II` call (in round
  order) and the initial state in `implReset`; `SHA.java` has `round1_kt`..`round4_kt` and the initial state in
  `implReset`. Copying constant tables is allowed; write the round logic yourself.
- `clone()`: copy the state array (`copy.h = h.clone()`), like Sha256.
- Before you start, every test fails with `Wasm execution differs` and `...:NoSuchAlgorithmException` in the actual
  value. That is expected: there are no new JDK members, only new names in `Digests.create`.
- Calibration: the reference passed `kit check` on the first run (about 230 new lines). No existing test changed.

## Docs

Create `docs/bytecode-message-digest-algorithms.md` with this skeleton (fill in the brackets):

    # Bytecode MessageDigest algorithms

    Revision `message-digest-algorithms-vN` adds MD5, SHA-1, SHA-384 and SHA-512 engines to the built-in
    `MessageDigest` table.

    Contract:

    - <names and aliases, digest lengths, failure messages, clone>

    Evidence: `BytecodeMessageDigestAlgorithmsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances), with the RFC 1321 / FIPS 180 vectors, one million `a`, block boundaries and
    multi-part updates.
