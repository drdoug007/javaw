# Bytecode MessageDigest

Revision `message-digest-v274` links `java.security.MessageDigest` (SHA-256), `MessageDigestSpi` and the
`GeneralSecurityException`/`NoSuchAlgorithmException`/`DigestException` family in the bundled exception runtime.

Contract:

- Fixed built-in algorithm table: there is no `java.security.Provider`/`Security` framework.
  `Digests.create` answers case-insensitively to `SHA-256`, `SHA256`, `2.16.840.1.101.3.4.2.1` and
  `OID.2.16.840.1.101.3.4.2.1` (no prefix or substring matching, so `SHA-257` and `SHA 256` fail) and
  passes the caller's spelling to the engine, so `getAlgorithm()` returns it unchanged. `getInstance(null)`
  throws `NullPointerException("null algorithm name")`; anything else throws
  `NoSuchAlgorithmException(name + " MessageDigest not available")` (so `""` gives
  `" MessageDigest not available"`). `getProvider()`, the `getInstance(String, String)` and
  `getInstance(String, Provider)` overloads, and `update(ByteBuffer)` are deliberately left out (no
  `Provider` class, no `java.nio.ByteBuffer`).
- SHA-256 engine: `javaw.runtime.Sha256` on the generic `javaw.runtime.BlockDigest` — 64-byte blocks,
  32-byte digest, big-endian 64-bit length padding (`0x80`, zeros, then the bit count in the last 8 bytes;
  T14 reuses `BlockDigest` for MD5/SHA-1/SHA-512, including little-endian and 16-byte length forms).
  `update(byte)`, `update(byte[])` and `update(byte[], int, int)` accumulate into a one-block buffer,
  compressing whole blocks straight from the input. `update(b, off, len)` throws
  `IllegalArgumentException("No input buffer given")` for `null` and
  `IllegalArgumentException("Input buffer too short")` when `b.length - off < len`, then,
  as in the JDK's `DigestBase`, `len == 0` returns at once (even with a negative `off`) and otherwise
  `off < 0 || len < 0` throws `ArrayIndexOutOfBoundsException("Range [off, off + len) out of bounds for length n")`.
  `update((byte[]) null)` throws `NullPointerException`. `digest()` returns the hash. The built-in engines reset
  themselves, so a second `digest()` is the hash of nothing. `MessageDigest.digest()` does not call `engineReset()`;
  as in the JDK, resetting is the engine's job, so a user engine that does not reset keeps its state; `digest(byte[])` is `update` + `digest`; `reset()` drops everything.
- `digest(buf, off, len)` is validated in `MessageDigest` (`IllegalArgumentException("No output buffer
  given")` for `null`, `IllegalArgumentException("Output buffer too small for specified offset and
  length")` when `buf.length - off < len`), then the engine throws
  `DigestException("Length must be at least 32 for SHA-256digests")` (no space before `digests`, canonical
  name) when `len < 32`, then `DigestException("Buffer too short to store digest")` when `off < 0`. Both checks run before
  hashing, so a failed call keeps the pending input. It writes 32 bytes at `off`, returns 32 and resets. User
  subclasses inherit `MessageDigestSpi.engineDigest(buf, off, len)`, which consumes `engineDigest()` first and then
  throws `DigestException("partial digests not returned")` when `len` is too small, or
  `DigestException("insufficient space in the output buffer to store the digest")` when the digest does not fit
  after `off`.
- `getDigestLength()` returns `engineGetDigestLength()`, or `clone().digest().length` when that is 0
  (0 when the object is not `Cloneable`). `clone()` returns `super.clone()` for `Cloneable` digests — a
  deep copy: `BlockDigest` clones its block buffer, `Sha256` clones its state and schedule words, so a
  clone continues independently — and throws `CloneNotSupportedException` otherwise.
- `isEqual(a, b)`: true for the same reference (so `null, null` is true), false if exactly one is null or
  the lengths differ, else byte-equal. `toString()` is
  `algorithm + " Message Digest from " + provider + ", " + state + "\n"` with provider `SUN` for
  built-in digests (set by `getInstance`) and `(no provider)` for user subclasses, and state
  `<initialized>` (new, after `digest`/`reset`) or `<in progress>` (after any successful `update`).

Evidence: `BytecodeMessageDigestTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). Provider overloads, `getProvider()` and `update(ByteBuffer)` are rejected before WAT generation.
