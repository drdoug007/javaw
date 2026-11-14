# T15 SecretKeySpec and Mac (HMAC)

```kit
id: T15
slug: hmac
title: link javax.crypto.Mac (HMAC) and SecretKeySpec
tests: BytecodeHmacTest
depends: T12, T13, T14
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/Key.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/KeyException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/InvalidKeyException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/security/spec/KeySpec.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javax/security/auth/Destroyable.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javax/security/auth/DestroyFailedException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javax/crypto/SecretKey.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javax/crypto/spec/SecretKeySpec.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javax/crypto/Mac.java
allow: docs/bytecode-hmac.md
```

## Goal

Add HMAC: `javax.crypto.Mac` over the built-in digests (T13/T14) and the key types it needs. H2 `SHA256.initMac/getHMAC/
getPBKDF2` use `Mac.getInstance`, `init(Key)`, `update(byte[],int,int)`, `doFinal()`, `doFinal(byte[])` and
`new SecretKeySpec(byte[], String)`; Tomcat `PEMFile` builds `SecretKeySpec`s. The test also carries the RFC 4231 case 1
fixture from `javaw-core/src/test/java/one/dastec/javaw/JavaCryptoTest.java`. That source-path test stays `@Disabled`;
the source compiler (javaw-core) still has no crypto. This task covers only the bytecode path.
**Design decision:** a fixed algorithm table, no `Provider`/`MacSpi` framework (same as T13).

## Members to implement

- `java.security.Key extends java.io.Serializable` (interface): `String getAlgorithm()`, `String getFormat()`, `byte[] getEncoded()`.
- `java.security.KeyException extends GeneralSecurityException`, `InvalidKeyException extends KeyException`: constructors
  `()`, `(String)`, `(String, Throwable)`, `(Throwable)`.
- `java.security.spec.KeySpec`: empty marker interface.
- `javax.security.auth.Destroyable` (interface): `default void destroy() throws DestroyFailedException` (throws
  `new DestroyFailedException()`), `default boolean isDestroyed()` (returns false).
- `javax.security.auth.DestroyFailedException extends Exception`: `()`, `(String)`.
- `javax.crypto.SecretKey extends Key, Destroyable` (empty interface).
- `javax.crypto.spec.SecretKeySpec implements KeySpec, SecretKey`: `SecretKeySpec(byte[] key, String algorithm)`,
  `SecretKeySpec(byte[] key, int offset, int len, String algorithm)`, `getAlgorithm()`, `getFormat()`, `getEncoded()`,
  `equals(Object)`, `hashCode()`.
- `public class javax.crypto.Mac implements Cloneable` with a **private** constructor:
  `static final Mac getInstance(String)`, `final String getAlgorithm()`, `final int getMacLength()`,
  `final void init(Key) throws InvalidKeyException`, `final void update(byte)`, `final void update(byte[])`,
  `final void update(byte[], int, int)`, `final byte[] doFinal()`, `final byte[] doFinal(byte[])`, `final void reset()`.

## Members to leave out

`Mac.getInstance(String, String)`/`(String, Provider)`, `getProvider()`, `init(Key, AlgorithmParameterSpec)`,
`update(ByteBuffer)`, `doFinal(byte[], int)` (needs `ShortBufferException`), `clone()`, and `MacSpi`. The test checks three.

## JDK behaviour to match

- `Mac.getInstance`: null -> `NullPointerException("null algorithm name")`. Case-insensitive names `HmacSHA256`,
  `HmacSHA1`, `HmacMD5` (block 64 bytes), `HmacSHA384`, `HmacSHA512` (block 128). Others:
  `NoSuchAlgorithmException("Algorithm " + name + " not available")` (**not** the MessageDigest wording).
  `getAlgorithm()` returns the name as passed. `getMacLength()` = digest length, works before `init`.
- Before `init`: `update(...)`, `doFinal()`, `doFinal(byte[])` throw `IllegalStateException("MAC not initialized")`;
  `reset()` and `getMacLength()` do not throw.
- `init(key)`: `null` -> `InvalidKeyException("No installed provider supports this key: (null)")` on a Mac that has not
  yet chosen a provider (after `getMacLength`/`reset`/`update`/`doFinal`/a successful `init`, the JDK says "Secret key expected"); a `Key` that is not a
  `SecretKey` -> `InvalidKeyException("Secret key expected")`; `getEncoded()` returns null ->
  `InvalidKeyException("Missing key data")`. Any `SecretKey` works, whatever its algorithm name (`"AES"` too).
- HMAC: if the key is longer than the block, replace it by `digest(key)`. Pad with zeros to the block; `ipad = key ^ 0x36`,
  `opad = key ^ 0x5c`. MAC = `H(opad || H(ipad || message))`. `doFinal` returns it and resets (same key, ready again).
  `reset()` forgets data but keeps the key. `init` again replaces the key.
- `update(byte[])` and `update(byte[], off, len)` ignore a null array. `off < 0 || len < 0 || len > input.length - off`
  -> `IllegalArgumentException("Bad arguments")`.
- `SecretKeySpec(key, alg)`: `key == null || alg == null` -> `IllegalArgumentException("Missing argument")`; empty key ->
  `IllegalArgumentException("Empty key")`; stores a **copy**. The 4-argument form checks in this order: Missing argument,
  Empty key (of the whole array), `ArrayIndexOutOfBoundsException("offset is negative")`,
  `ArrayIndexOutOfBoundsException("len is negative")`, `key.length - offset < len` ->
  `IllegalArgumentException("Invalid offset/length combination")`; `len == 0` is allowed.
- `getFormat()` = `"RAW"`; `getEncoded()` returns a new copy every call.
- `hashCode()` (JDK 21 formula; JDK 24+ differs; `keyHashCodes` checks raw values):
  `int v = 0; for (i = 1; i < key.length; i++) v += key[i] * i;` (signed bytes, index 0 skipped) then
  `v ^ "desede".hashCode()` if the algorithm equalsIgnoreCase `TripleDES`, else `v ^ algorithm.toLowerCase(Locale.ENGLISH).hashCode()`.
- `equals(o)`: `o instanceof SecretKey`, algorithms equal ignoring case (or the `DESede`/`TripleDES` pair either way), and
  `MessageDigest.isEqual(key, o.getEncoded())`.
- `SecretKeySpec.destroy()` (inherited default) throws `DestroyFailedException` with a null message; `isDestroyed()` false.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeHmacTest.java`.
2. Write the interfaces and exceptions (copy `java/security/NoSuchAlgorithmException.java` for the exceptions).
3. Write `SecretKeySpec`, then `Mac`. `Mac` gets its digest with `javaw.runtime.Digests.create("SHA-256")` etc.
4. Register, just before `paths.add("javaw/runtime/ExceptionSupport.java");` in `BytecodeExceptionRuntime.java`:
   `for(String name:List.of("Key","KeyException","InvalidKeyException","spec/KeySpec"))paths.add("java/security/"+name+".java");`
   `for(String name:List.of("Destroyable","DestroyFailedException"))paths.add("javax/security/auth/"+name+".java");`
   `for(String name:List.of("SecretKey","spec/SecretKeySpec","Mac"))paths.add("javax/crypto/"+name+".java");`
5. `javaw-agent-kit/bin/kit check T15` until PASS, then docs, report, `kit finish T15`.

## Hints

- Keep in `Mac`: `String algorithm`, `MessageDigest digest`, `int blockLength`, `byte[] innerPad, outerPad`, `boolean initialized`.
  After `init`, `doFinal` and `reset`, call one private `restart()` = `digest.reset(); digest.update(innerPad);`.
  `doFinal`: `inner = digest.digest(); digest.update(outerPad); digest.update(inner); result = digest.digest(); restart();`.
- Hash a long key with the same digest (`digest.reset(); key = digest.digest(key);`) before building the pads; RFC 4231
  case 6 (131-byte key) checks it. HmacSHA512/384 use a 128-byte block, the others 64; a wrong block size breaks only
  the SHA-512 halves of the RFC vectors.
- `(byte)(value ^ 0x36)` where `value` is the signed key byte or 0 past the key end.
- `getInstance` in `Mac` is `static final`, like the JDK. Do not add `MacSpi`.
- Check the key in `init` in the order: null, not `SecretKey`, null encoding. Leave `initialized` false if it throws.
- `Locale.ENGLISH` and `String.toLowerCase(Locale)` exist. `MessageDigest.isEqual` (T13) handles a null `getEncoded()`.
- The fixture's `PlainKey`/`NullSecret`/`CustomSecret` classes implement your interfaces: `Key` must have exactly the three
  abstract methods, and `SecretKey` must add none.
- Calibration: the reference passed `kit check` on the first run. JDK 21 surprises found while writing the test: the
  unknown-name message is `Algorithm X not available`, `init(null)` reports the provider-selection message, and after a
  failed `init` the real JDK Mac is unusable (so the test never reuses one).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
rfc4231Vectors, otherAlgorithmsStreamingAndNames, stateAndKeyChecks, secretKeySpecAndExceptions: if the message says `link a complete acyclic type hierarchy: javax/crypto/SecretKey` (or another new type), the new classes are not linked: add the three registration lines of Step 4 just before `paths.add("javaw/runtime/ExceptionSupport.java");` in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit), and check the chain: SecretKey extends java.security.Key and javax.security.auth.Destroyable, Key extends java.io.Serializable, InvalidKeyException extends KeyException extends GeneralSecurityException; `Cannot build bytecode exception runtime` means javac rejected one of your new files: fix the error it quotes.
hmacSHA256, rfc4231Case1, rfc4231Case2: if the HmacSHA256 value is wrong, fix Mac.init/doFinal: build BOTH pads over all blockLength bytes, `innerPad[i] = (byte) (k ^ 0x36)` and `outerPad[i] = (byte) (k ^ 0x5c)` with k = key[i] or 0 past the key end; doFinal is `inner = digest.digest(); digest.update(outerPad); digest.update(inner); result = digest.digest();` on a digest that already holds innerPad; if only the HmacSHA512 half after the | is wrong, blockLength is wrong: 128 for HmacSHA384 and HmacSHA512, 64 for the others.
rfc4231Case6, otherAlgorithms: a key LONGER than blockLength (131, 100 or 65 bytes) must be hashed before building the pads: `if (secret.length > blockLength) { digest.reset(); secret = digest.digest(secret); }` (a 64-byte key is not hashed with a 64-byte block); HmacSHA1 uses Digests.create("SHA-1"), HmacMD5 "MD5", HmacSHA384 "SHA-384" with block 128.
streamingAndReuse: after init, doFinal and reset call one private restart() = `digest.reset(); digest.update(innerPad);` so the Mac keeps its key and is ready again (reset() before init does nothing); update((byte[]) null) is ignored (no exception, no data); init with another key (any SecretKey, algorithm "AES" too) rebuilds both pads.
names: Mac.getInstance compares with equalsIgnoreCase against HmacSHA256, HmacSHA1, HmacMD5, HmacSHA384 and HmacSHA512, keeps the caller's spelling for getAlgorithm(), throws `new NoSuchAlgorithmException("Algorithm " + algorithm + " not available")` (not the MessageDigest wording) and NullPointerException("null algorithm name") for null; getMacLength() returns digest.getDigestLength() and works before init.
notInitialized, badKeys, customKeyAndBadArguments: update(...) and doFinal(...) throw IllegalStateException("MAC not initialized") before init, reset() and getMacLength() do not; init checks in this order: null -> InvalidKeyException("No installed provider supports this key: (null)"), not `instanceof SecretKey` -> "Secret key expected", getEncoded() == null -> "Missing key data"; update(input, off, len) returns quietly for a null input, else throws IllegalArgumentException("Bad arguments") when `off < 0 || len < 0 || len > input.length - off`.
keySpec, keySpecFailures: SecretKeySpec checks in this order: `key == null || algorithm == null` -> IllegalArgumentException("Missing argument"), `key.length == 0` -> IllegalArgumentException("Empty key") (in the 4-argument form too, on the whole array), `offset < 0` -> ArrayIndexOutOfBoundsException("offset is negative"), `len < 0` -> ArrayIndexOutOfBoundsException("len is negative"), `key.length - offset < len` -> IllegalArgumentException("Invalid offset/length combination") (len 0 is allowed); store `Arrays.copyOfRange(key, offset, offset + len)`, getFormat() returns "RAW", getEncoded() returns `key.clone()` on every call.
keyEquality, keyHashCodes: hashCode is `int v = 0; for (int i = 1; i < key.length; i++) v += key[i] * i;` (start at 1, signed bytes) then `v ^ (algorithm.equalsIgnoreCase("TripleDES") ? "desede" : algorithm.toLowerCase(Locale.ENGLISH)).hashCode()`; equals needs `o instanceof SecretKey`, algorithms equal ignoring case or the DESede/TripleDES pair in either order, and `MessageDigest.isEqual(key, other.getEncoded())`.
```

## Docs

Create `docs/bytecode-hmac.md` with this skeleton (fill in the brackets):

    # Bytecode HMAC

    Revision `hmac-vN` links `javax.crypto.Mac` (HMAC over the built-in digests), `SecretKeySpec`, `SecretKey`, `Key`,
    `KeySpec`, `Destroyable` and the key exceptions in the bundled exception runtime.

    Contract:

    - <algorithms, init/update/doFinal/reset rules, key checks and messages, SecretKeySpec equality/hashCode>
    - javaw-core's `JavaCryptoTest` remains a source-path gap; this is bytecode-path support only.

    Evidence: `BytecodeHmacTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances), including RFC 4231 cases 1, 2 and 6. Provider/parameter overloads and `doFinal(byte[],int)` are rejected
    before WAT generation.
