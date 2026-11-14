# T17 Base64

```kit
id: T17
slug: base64
title: link java.util.Base64 with its Encoder and Decoder
tests: BytecodeBase64Test
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Base64.java
allow: docs/bytecode-base64.md
```

## Goal

Add `java.util.Base64` with its nested classes `Base64.Encoder` and `Base64.Decoder` (RFC 4648 basic and URL-safe
alphabets, RFC 2045 MIME lines). The pinned libraries call it directly (`runtime-target/target/audit/external-types.tsv`):
H2 `JdbcXid` uses `getUrlEncoder().withoutPadding()`, `encodeToString` and `getUrlDecoder().decode(String)`; Hibernate
`XmlHelper` uses `getDecoder().decode(String)` (its `XMLAppender` uses `getEncoder().wrap(OutputStream)`, which stays
out of scope); Spring Core `Base64Utils` uses `encode(byte[])`, `decode(byte[])`, `decode(String)` and
`getUrlDecoder()`, and `UrlResource` uses `getUrlEncoder().encodeToString`; Spring Web `HttpHeaders.encodeBasicAuth` and `ContentDisposition.parse` use
`encode`/`decode(String)`; Spring Boot `PemCertificateParser` uses `decode(byte[])` and the OAuth2 auto-configuration
uses `getMimeDecoder()`; Tomcat's `OpenSSLContext` uses `getMimeEncoder(int, byte[])`; Log4j's `Base64Util` uses
`getEncoder().encodeToString`.

## Members to implement

One file, `java/util/Base64.java` (`package java.util;`). It holds three classes; only the outer one is registered.

`public class Base64` (private constructor):

- `public static Base64.Encoder getEncoder()`, `getUrlEncoder()`, `getMimeEncoder()`
- `public static Base64.Encoder getMimeEncoder(int lineLength, byte[] lineSeparator)`
- `public static Base64.Decoder getDecoder()`, `getUrlDecoder()`, `getMimeDecoder()`

`public static class Encoder` (nested in `Base64`; private fields `boolean isURL; byte[] newline; int linemax;
boolean doPadding`, one private constructor taking all four):

- `public byte[] encode(byte[] src)`
- `public int encode(byte[] src, byte[] dst)`
- `public String encodeToString(byte[] src)`
- `public Base64.Encoder withoutPadding()`

`public static class Decoder` (nested in `Base64`; private fields `boolean isURL; boolean isMIME`, one private
constructor taking both):

- `public byte[] decode(byte[] src)`
- `public byte[] decode(String src)`
- `public int decode(byte[] src, byte[] dst)`

## Members to leave out

- `Encoder.wrap(java.io.OutputStream)` and `Decoder.wrap(java.io.InputStream)`.
- `Encoder.encode(java.nio.ByteBuffer)` and `Decoder.decode(java.nio.ByteBuffer)`.

Do not declare `Encoder.wrap(OutputStream)`, `Decoder.wrap(InputStream)`, `Encoder.encode(ByteBuffer)` or
`Decoder.decode(ByteBuffer)`, not even as stubs: `leavesStreamsAndByteBuffersUnlinked` requires all four to stay
unlinked. (`java.nio.ByteBuffer` and `java.io.FilterOutputStream` are not in the runtime at all, and mentioning a
missing type breaks every bytecode test. `OutputStream`, and `InputStream` once T18 has landed, do exist, but the wrap
methods must still be left out.)

## JDK behaviour to match

Alphabets (index 0..63): basic `A-Z a-z 0-9 + /`; URL-safe is the same with `-` and `_` in place of `+` and `/`.
Padding character `=`.

Instances (the test compares some with `==`):

- `getEncoder()`, `getUrlEncoder()`, `getMimeEncoder()`, `getDecoder()`, `getUrlDecoder()`, `getMimeDecoder()` each
  return one shared static instance. Encoders: basic = `(isURL false, newline null, linemax -1, padding true)`, URL =
  `(true, null, -1, true)`, MIME = `(false, {'\r','\n'}, 76, true)`. Decoders: basic `(false,false)`, URL
  `(true,false)`, MIME `(false,true)`.
- `withoutPadding()` returns `this` when padding is already off, else a new encoder with the same alphabet, line
  separator and line length and padding off.
- `getMimeEncoder(lineLength, lineSeparator)`, in this order:
  1. `Objects.requireNonNull(lineSeparator)` (a null separator throws `NullPointerException` even when the length
     is 0 or negative);
  2. every separator byte `b` must NOT be in the basic decode table (no `A-Za-z0-9+/` and no `=`), else
     `IllegalArgumentException("Illegal base64 line separator character 0x" + Integer.toString(b, 16))`
     (e.g. `0x41` for `A`, `0x3d` for `=`, `0x2f` for `/`). `-` and `_` are allowed (they are URL-only);
  3. `lineLength &= ~3` (round down to a multiple of 4); if the result is `<= 0` return the shared basic encoder
     `getEncoder()` itself;
  4. otherwise `new Encoder(false, lineSeparator, lineLength, true)`.

Encoding (`encode0`):

- Each group of 3 input bytes gives 4 characters. A final group of 1 byte gives 2 characters plus `==`, of 2 bytes
  3 characters plus `=`; without padding the `=` are just left off (`"f"` -> `Zg`, `"fo"` -> `Zm8`). Treat bytes as
  unsigned (`b & 0xff`): `{-1,-2,-3}` -> `//79`.
- MIME/line mode (`linemax > 0`): after every `linemax` output characters write the separator bytes, but only if
  more input follows: never a trailing separator. Make this check only right after a whole 3-byte group has been
  written, and "more input follows" means `sp < src.length` after that group was consumed (never check after the
  final partial group). Example: `getMimeEncoder(4, "-")` on `"foob"` gives `Zm9v-Yg==`. 57 bytes with `getMimeEncoder()` give exactly 76 chars and no
  CRLF; 58 bytes give 76 chars + CRLF + 4 chars = 82. The padding of the last group stays on the last line.
- Output length (`encodedOutLength`): with padding `4 * ((n + 2) / 3)`; without padding `4 * (n / 3) + (n % 3 == 0 ?
  0 : n % 3 + 1)`. Let `len` be the character count from that formula (before separators, NOT the input byte count
  `n`); in line mode the total is `len + (len - 1) / linemax * newline.length` (Java integer division, so len 0 stays
  0). Example: 58 bytes with `getMimeEncoder()`: len = 80, total = 80 + 79/76*2 = 82.
- `encode(byte[] src)` returns a new array of exactly that length.
- `encode(byte[] src, byte[] dst)`: if `dst.length` is less than the output length throw
  `IllegalArgumentException("Output byte array is too small for encoding all input bytes")`; otherwise write from
  `dst[0]`, leave the rest of `dst` unchanged and return the number of bytes written.
- `encodeToString(src)`: the encoded bytes as a String, one char per byte (`(char)(b & 0xff)`). In JDK source this
  is `new String(encoded, 0, 0, encoded.length)`, and that deprecated constructor exists in the runtime.
- A null `src` (or null `dst`) throws `NullPointerException` (just touching `src.length` does it).

Decoding (`decode0`), for all three decoders. `base64[b & 0xff]` is the alphabet index, `-2` for `=`, `-1` for
anything else. The URL decoder uses the URL table (so `+` and `/` are illegal there) and the others the basic table
(so `-` and `_` are illegal for the basic decoder and skipped by MIME).

- Walk the input, collecting 6 bits per alphabet character (`bits |= v << shiftto; shiftto -= 6`, starting at
  `shiftto = 18`); after 4 characters write 3 bytes and reset.
- On `=`: throw `IllegalArgumentException("Input byte array has wrong 4-byte ending unit")` when `shiftto == 18`
  (padding at the start of a group, `====`, `QUJD=`), or when `shiftto == 6` and the next byte is missing or is not a
  second `=` (`QQ=`, `QQ=x`). Otherwise stop the main loop (`Zm8=` stops at `shiftto == 0`; `Q===` stops at
  `shiftto == 12`). When `shiftto == 6`, the second `=` is consumed.
- Other non-alphabet byte: MIME skips it; the others throw `IllegalArgumentException("Illegal base64 character " +
  Integer.toString(src[sp - 1], 16))` where `src[sp - 1]` is the SIGNED byte: space -> `20`, newline -> `a`, byte
  `0xc3` -> `-3d`, byte `0x80` -> `-80`.
- After the loop: `shiftto == 6` writes 1 more byte, `shiftto == 0` writes 2; `shiftto == 12` (one dangling
  character: `Q===`, `QUJDR`, MIME `Z`) throws `IllegalArgumentException("Last unit does not have enough valid
  bits")`. Missing padding is accepted (`Zg` -> `f`, `Zm8` -> `fo`), and unused low bits are ignored (`QR==` -> `A`).
- Then anything left over after the padding: non-MIME throws `IllegalArgumentException("Input byte array has
  incorrect ending byte at " + sp)` with `sp` = index of the leftover byte (`QQ==QQ` -> `at 4`, `Zm8==` -> `at 4`).
  MIME skips leftover non-alphabet bytes (including more `=`), but a leftover alphabet byte throws the same message
  with `sp` = its index **plus one** (`Zm8=Zg` -> `at 5`), because the JDK writes
  `if (isMIME && base64[src[sp++] & 0xff] < 0) continue; throw ...` and the `sp++` only runs for MIME.

Output length (`decodedOutLength(src)`), used for the array size and the "too small" check:

- `len = src.length`; `len == 0` gives 0.
- `len == 1`: MIME gives 0 for EVERY single byte (and then decoding `Z` still throws "Last unit ...", `=` throws
  "Input byte array has wrong 4-byte ending unit", while `!` gives an empty array);
  others throw `IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes")` (also for
  `=`).
- MIME: scan from the start; at the first `=` subtract it and everything after it from `len` and stop; subtract one
  for every byte whose table value is `-1`. Non-MIME: `paddings` = number of `=` among the last two bytes (count the
  second-to-last only if the last is `=`).
- If `paddings == 0` and `len % 4 != 0`, `paddings = 4 - len % 4`. Result `3 * ((len + 3) / 4) - paddings`.
- `decode(byte[] src)` decodes into an array of that size and returns `Arrays.copyOf(dst, written)` when fewer bytes
  were written.
- `decode(byte[] src, byte[] dst)`: compute `decodedOutLength(src)` first (it may throw, e.g. the 2-byte message for a
  1-byte non-MIME input wins over the "too small" message); then, if `dst.length` is less than that size throw `IllegalArgumentException("Output
  byte array is too small for decoding all input bytes")`; otherwise decode from `dst[0]`, leave the rest unchanged
  and return the count.
- `decode(String s)`: convert with ISO-8859-1 (`s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)`, which
  exists in the runtime) and call `decode(byte[])`. Chars above `0xff` become `?` (`0x3f`), so `"€QQQ"` throws
  `Illegal base64 character 3f`, and `é` (`0xe9`) becomes the byte `-23`, reported as `-17`.
- A null argument throws `NullPointerException`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBase64Test.java`. The fixture prints
   exception class names and messages; for `NullPointerException` only the class name is compared.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/Base64.java` with the outer class and the
   two `public static` nested classes.
3. Register it: `javaw-agent-kit/bin/kit register java/util/Base64.java`. The nested classes need no registration.
4. `javaw-agent-kit/bin/kit check T17` until PASS.
5. Write `docs/bytecode-base64.md` (below), the report, then `kit finish T17`.

## Hints

- Copy the compact style of `java/util/HexFormat.java`. Build the two encode tables as
  `private static final byte[]` (or `char[]`) from a String such as
  `"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"`, and the two decode tables as
  `int[256]` in a `static {}` block with `Arrays.fill(table, -1)`, the alphabet indexes, and `table['='] = -2`.
  Declare the alphabet fields (a `static final String` literal is safest, because it is a compile-time constant)
  BEFORE the `static {}` block that fills the decode tables, or build the tables in a helper method called from the
  field initialisers, such as `private static int[] table(String alphabet)`; otherwise the block reads null.
  `Arrays.fill(int[], int)`, `Arrays.copyOf(byte[], int)`, `Integer.toString(int, int)` and `Objects.requireNonNull`
  exist in the runtime.
- Write one private `int encode0(byte[] src, byte[] dst)` returning the count, one private
  `int encodedOutLength(int srcLength)`, one private `int decode0(byte[] src, byte[] dst)` and one private
  `int decodedOutLength(byte[] src)`. The public methods are then two or three lines each. A simple per-character
  loop is enough; the JDK's fast `encodeBlock`/`decodeBlock` paths are optimisations and change nothing.
- In `encode0` keep a counter of characters on the current line; after each whole 3-byte group, when it reaches
  `linemax` and `sp < src.length`, write the separator and reset it. Check `linemax > 0` first: the basic encoders have `linemax == -1` and a null
  separator.
- Do not use `String.getBytes()` (that is UTF-8) or `new String(bytes)` (UTF-8): use the ISO-8859-1 charset for
  `decode(String)` and the `(byte[], 0, 0, length)` constructor for `encodeToString`.
- `Integer.toString(b, 16)` on the signed `byte` (not `b & 0xff`, not `Integer.toHexString`) gives `-3d`.
- The overflow checks in the JDK's `encodedOutLength` (`OutOfMemoryError("Encoded size is too large")`) cannot be
  reached by the test; plain `int` arithmetic is fine.
- Do not declare `Encoder.wrap(OutputStream)`, `Decoder.wrap(InputStream)`, `Encoder.encode(ByteBuffer)` or
  `Decoder.decode(ByteBuffer)`, not even as stubs: `leavesStreamsAndByteBuffersUnlinked` requires all four to stay
  unlinked. (`java.nio.ByteBuffer` and `java.io.FilterOutputStream` are not in the runtime at all, and mentioning a
  missing type breaks every bytecode test.)
- Keep the decode tables and alphabets as static fields of the outer `Base64` class (the nested classes can read
  them) and create the shared instances as `static final` fields of `Encoder` and `Decoder`.
- JDK 21 surprises found while writing the test: `Zm8=xyz` (MIME) is NOT a padding error, because `Zm8` already
  completed `shiftto == 0` before the `=`; it fails later with `incorrect ending byte at 5`. `QUJD=` and `=Zm9v` are
  padding errors (`=` at `shiftto == 18`). The MIME decoder accepts `Zm8= ==` (extra `=` and spaces are skipped).
- A mutation check during calibration (trailing separator always written, `Integer.toHexString(b & 0xff)` in the
  message) failed `mimeEncode`, `roundTrip` and `highBitFailures`, so those fixtures point at exactly these rules.
- Calibration: the reference passed kit check after 1 run (about 190 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
encodesBasicAndUrl, encodesMime, decodesBasicAndUrl, reportsDecodeFailures, decodesMime: if the message says `unsupported static call java/util/Base64.getEncoder()` (or another `java/util/Base64...` member), Base64 is not registered or lacks that member: run `javaw-agent-kit/bin/kit register java/util/Base64.java` (do not edit BytecodeExceptionRuntime.java by hand) and give Base64.java / its nested `public static class Encoder` / `public static class Decoder` a public method with exactly that name, parameter types and return type; if it says `Cannot build bytecode exception runtime`, javac rejected Base64.java: fix the line it quotes.
basicEncode, urlEncode, withoutPadding: each 3 input bytes give 4 chars from `(b0 & 0xff) << 16 | (b1 & 0xff) << 8 | (b2 & 0xff)` shifted by 18, 12, 6, 0 and masked with 0x3f; one leftover byte gives 2 chars + `==`, two leftover bytes 3 chars + `=`, and withoutPadding just drops the `=`; withoutPadding() returns `this` when padding is already off; the URL encoder uses `-` and `_` instead of `+` and `/`.
encodeIntoArray, mimeEncodeIntoArray: encode(src, dst) first computes the full output length (with padding `4 * ((n + 2) / 3)`, without `4 * (n / 3) + (n % 3 == 0 ? 0 : n % 3 + 1)`; call that character count `len` (not the input byte count n); in line mode the total is `len + (len - 1) / linemax * newline.length`, e.g. 58 bytes with getMimeEncoder(): 80 + 79/76*2 = 82) and throws `IllegalArgumentException("Output byte array is too small for encoding all input bytes")` if `dst.length` is smaller; it writes from dst[0], never touches the bytes after the output, and returns the count.
mimeEncode, customMime: the separator goes after every `linemax` output chars ONLY when more input follows (never at the end: 57 bytes give 76 chars without CRLF), checked only after a whole 3-byte group (getMimeEncoder(4, "-") on "foobar" gives `Zm9v-YmFy`); getMimeEncoder(len, sep) rounds `len &= ~3` and returns the shared getEncoder() instance itself when the result is <= 0; the separator bytes are copied as they are (bytes like 0xc3 or 0 are allowed).
mimeEncoderFailures: getMimeEncoder checks, in this order, `Objects.requireNonNull(lineSeparator)`, then each separator byte against the BASIC decode table (alphabet bytes and `=` are illegal, `-` and `_` are fine) with `new IllegalArgumentException("Illegal base64 line separator character 0x" + Integer.toString(b, 16))`, and only then rounds the length (so getMimeEncoder(0, "/") still throws).
basicDecode, urlDecode, roundTrip: decode collects 6 bits per character with `bits |= v << shiftto; shiftto -= 6` from shiftto = 18 and writes 3 bytes when shiftto < 0; at the end shiftto == 6 writes `(byte)(bits >> 16)` and shiftto == 0 writes two bytes; missing padding is fine (`Zm8` -> fo) and leftover low bits are ignored (`QR==` -> [65]); the URL decoder uses the URL table, the basic decoder the basic table; getDecoder()/getUrlDecoder()/getMimeDecoder() return shared static instances.
decodeIntoArray, mimeDecode: decode(src, dst) compares dst.length with decodedOutLength(src): `3 * ((len + 3) / 4) - paddings`, where non-MIME paddings counts `=` in the last two bytes and MIME reduces len by every byte whose table value is -1 and cuts len at the first `=`; if paddings is 0 and len % 4 != 0, paddings = 4 - len % 4; too small throws `IllegalArgumentException("Output byte array is too small for decoding all input bytes")`; decode(String) converts with StandardCharsets.ISO_8859_1, never with getBytes() (UTF-8).
decodeFailures: messages, exactly: one-byte input (non-MIME) `Input byte[] should at least have 2 bytes for base64 bytes` (checked in decodedOutLength before decoding); `=` with shiftto == 18, or with shiftto == 6 not followed by a second `=`: `Input byte array has wrong 4-byte ending unit`; a dangling single character at the end (shiftto == 12): `Last unit does not have enough valid bits`; bytes after the padding: `Input byte array has incorrect ending byte at ` + index of that byte (`QQ==QQ` -> 4); other bad characters: `Illegal base64 character ` + Integer.toString(signedByte, 16) (space -> 20, newline -> a).
highBitFailures: the illegal-character message uses the SIGNED byte: `Integer.toString(src[i], 16)` gives `-3d` for 0xc3 and `-80` for 0x80 (do not mask with 0xff, do not use Integer.toHexString); decode(String) must use ISO-8859-1, which turns chars above 0xff into `?` (`3f`) and `é` into byte -23 (`-17`).
mimeDecodeFailures, mimeDecode: the MIME decoder skips every byte whose table value is -1 (CR, LF, `!`, `-`, bytes >= 0x80) but `=` still ends the data with the same padding rules (`Zm=9` and `=Zm9v` throw `Input byte array has wrong 4-byte ending unit`); after the padding it skips non-alphabet bytes (also more `=`), and a leftover alphabet byte throws `Input byte array has incorrect ending byte at ` + (its index + 1) (`Zm8=Zg` -> at 5, not 4); a one-byte input gives an empty array unless it is an alphabet char, which throws `Last unit does not have enough valid bits`.
leavesStreamsAndByteBuffersUnlinked: this test fails when Base64.java declares Encoder.wrap(OutputStream), Decoder.wrap(InputStream), Encoder.encode(ByteBuffer) or Decoder.decode(ByteBuffer): delete those methods; nothing else needs to change.
```

## Docs

Create `docs/bytecode-base64.md` with this skeleton (fill in the brackets):

    # Bytecode Base64

    Revision `base64-vN` links `java.util.Base64`, `Base64.Encoder` and `Base64.Decoder` in the bundled exception
    runtime.

    Contract:

    - <basic, URL-safe and MIME encoders; padding; line separators; getMimeEncoder checks>
    - <decoders; padding rules; MIME skipping; exact IllegalArgumentException messages; output-size checks>

    Evidence: `BytecodeBase64Test` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Encoder.wrap`, `Decoder.wrap` and the `ByteBuffer` overloads are rejected before WAT generation.
