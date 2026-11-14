# Bytecode MessageDigest algorithms

Revision `message-digest-algorithms-v275` adds MD5, SHA-1, SHA-384 and SHA-512 engines to the built-in
`MessageDigest` table.

Contract:

- `MessageDigest.getInstance` matches these names case-insensitively, and `getAlgorithm()` returns the spelling
  that was passed in:
  - SHA-1 (`javaw.runtime.Sha1`, 20 bytes): `SHA-1`, `SHA1`, `SHA`, `1.3.14.3.2.26`, `OID.1.3.14.3.2.26`
  - MD5 (`javaw.runtime.Md5`, 16 bytes): `MD5` only. JDK 21 also accepts the `1.2.840.113549.2.5` OID and JDK 17
    does not, so the OID is left out.
  - SHA-512 (`Sha512.sha512`, 64 bytes): `SHA-512`, `SHA512`, `2.16.840.1.101.3.4.2.3`, `OID.2.16.840.1.101.3.4.2.3`
  - SHA-384 (`Sha512.sha384`, 48 bytes): `SHA-384`, `SHA384`, `2.16.840.1.101.3.4.2.2`, `OID.2.16.840.1.101.3.4.2.2`
  - Any other name, including `SHA_1`, `SHA-0`, `SHA-1024` and `MD6`, still throws
    `NoSuchAlgorithmException(name + " MessageDigest not available")`.
- Each engine extends `javaw.runtime.BlockDigest`. MD5 and SHA-1 use 64-byte blocks with an 8-byte length field.
  MD5 is little-endian: message words, output words and the bit length. SHA-384 and SHA-512 share one engine
  with `long` words, 128-byte blocks and a 16-byte big-endian length field. Only the low 8 bytes of that field
  hold the bit count; the high 8 bytes stay zero. SHA-384 uses its own initial hashes and truncates the output
  to 48 bytes.
- `digest(buf, offset, len)` with `len` below the digest length throws
  `DigestException("Length must be at least " + length + " for " + canonical + "digests")`. It names the
  canonical algorithm (`MD5`, `SHA-1`, `SHA-384`, `SHA-512`), not the alias that was passed in.
- `clone()` copies the hash state and the message schedule, so the copy and the original move on separately.
- The round constants and initial hashes were copied from the JDK's `sun.security.provider` `MD5`, `SHA` and `SHA5`.
  The round logic was written separately.
- Left out: SHA-224, SHA-512/224, SHA-512/256, SHA3-* and MD2.

Fix to T13's `BlockDigest`: the length field was written with `bits >>> (8 * i)` for every byte of the field.
Java masks a `long` shift count to 6 bits, so for SHA-512's 16-byte field, bytes 8..15 repeated the count
instead of staying zero. SHA-256 (8-byte field) was not affected. The loop now writes only the low 8 bytes.

Evidence: `BytecodeMessageDigestAlgorithmsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances), with the RFC 1321 / FIPS 180 vectors, one million `a`, block boundaries and
multi-part updates.
