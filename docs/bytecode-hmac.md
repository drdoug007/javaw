# Bytecode HMAC

Revision `hmac-v276` links `javax.crypto.Mac` (HMAC over the built-in digests), `SecretKeySpec`, `SecretKey`, `Key`,
`KeySpec`, `Destroyable` and the key exceptions in the bundled exception runtime.

Contract:

- `Mac.getInstance` matches `HmacSHA256`, `HmacSHA1`, `HmacMD5`, `HmacSHA384` and `HmacSHA512`
  case-insensitively and `getAlgorithm()` returns the spelling the caller passed. Any other name, including
  `HmacFOO`, `SHA-256` and the empty string, throws `NoSuchAlgorithmException("Algorithm " + name + " not
  available")` (the Mac wording, not the `MessageDigest` one), and a `null` name throws
  `NullPointerException("null algorithm name")`. `getMacLength()` is the digest length (16, 20, 32, 48 or 64)
  and works before `init`.
- HMAC follows RFC 2104 over the built-in digests, with a 64-byte block for MD5, SHA-1 and SHA-256 and a
  128-byte block for SHA-384 and SHA-512. `init(key)` requires a `SecretKey`. A `null` key throws
  `InvalidKeyException("No installed provider supports this key: (null)")` while the JDK would still be choosing a
  provider: before any `getMacLength()`, `reset()`, `update`, `doFinal` or successful `init`. After any of those it
  throws `"Secret key expected"`, the same as a `Key` that is not a `SecretKey`. `getEncoded() == null` throws
  `"Missing key data"`. A failed `init` leaves the Mac in its previous state. A key longer than the block is
  replaced by its digest first (a key of exactly the block size is not), then zero-padded, and `ipad`/`opad` are
  built over the whole block. Like the JDK's `HmacCore`, that digest starts from any data still pending since the
  last `init`, `doFinal` or `reset`. So re-keying mid-stream with a long key gives the JVM's MAC, not a fresh one.
  As in the JDK, `init` zeroes the array that `getEncoded()` returned.
- Before `init`, `update(...)` and `doFinal(...)` throw `IllegalStateException("MAC not initialized")`;
  `reset()` and `getMacLength()` do not. `update` takes a byte, an array, or `(array, offset, len)`; a `null`
  array is ignored quietly, and a bad offset or length throws `IllegalArgumentException("Bad arguments")`.
  `doFinal()` computes `H(opad || H(ipad || message))` and reloads `ipad` so the Mac is ready again with the
  same key; `reset()` does the same without producing output; a further `init` replaces the key.
- `SecretKeySpec` stores a copy of the key and checks in this order: `Missing argument` (null key or
  algorithm), `Empty key` (the whole array, also in the 4-argument form), `ArrayIndexOutOfBoundsException`
  (`"offset is negative"` then `"len is negative"`), `IllegalArgumentException("Invalid offset/length
  combination")`; `len == 0` is allowed. `getFormat()` is `"RAW"` and `getEncoded()` returns a fresh copy on
  every call. `equals` is true for the same object. Otherwise it needs a `SecretKey` with an equal algorithm
  ignoring case (the `DESede`/`TripleDES` pair is accepted in either order, as in the JDK) and the same encoded
  bytes. It calls the other key's `getAlgorithm()` first, so a null algorithm throws `NullPointerException`, and it
  zeroes the array the other key's `getEncoded()` returned; `hashCode` uses the JDK 21
  formula: `sum of key[i] * i for i >= 1`, xor the algorithm lower-cased in English, with `TripleDES` hashing
  as `"desede"` for 1.1 compatibility. `destroy()` and `isDestroyed()` are the `Destroyable` defaults:
  `DestroyFailedException` with a null message, and `false`.
- Left out: `Mac.getInstance(String, String)` and `(String, Provider)`, `getProvider()`,
  `init(Key, AlgorithmParameterSpec)`, `update(ByteBuffer)`, `doFinal(byte[], int)`, `clone()` and `MacSpi`.
- javaw-core's `JavaCryptoTest` remains a source-path gap; this is bytecode-path support only.

Evidence: `BytecodeHmacTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances), including RFC 4231 cases 1, 2 and 6. Provider/parameter overloads and `doFinal(byte[],int)` are rejected
before WAT generation.
