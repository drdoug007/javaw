# Bytecode Base64

Revision `base64-v278` links `java.util.Base64`, `Base64.Encoder` and `Base64.Decoder` in the bundled exception
runtime. `Base64` is a non-final `public class` with a private constructor, as in the JDK, so reflection sees
the same class flags; `Encoder` and `Decoder` are non-final public static nested classes.

Contract:

- The basic, URL-safe and MIME encoders are shared `static final` instances, so `getEncoder() == getEncoder()`
  (the same for URL/MIME, and `getMimeEncoder(3, "#") == getEncoder()`). Encode tables are byte arrays built from
  the 64-character alphabet strings; `withoutPadding()` returns `this` when padding is already off, otherwise a
  copy with `doPadding = false`. `encode(byte[])` and `encode(dst)` size output with
  `4*((n+2)/3)` with padding, `4*(n/3)+(n%3==0?0:n%3+1)` without; in line mode the estimate grows by
  `(len-1)/linemax*newline.length` and the encoder writes the separator after each group of four once
  `linepos >= linemax` and more data remains, resetting `linepos`. `encodeToString` decodes the result with the
  deprecated `new String(bytes, 0, 0, len)` ISO-8859-1 constructor. `getMimeEncoder(int, byte[])` checks, in
  order: `lineSeparator == null` → `NullPointerException`, then each separator byte against the basic decode
  table (a base64 letter or `=` throws `IllegalArgumentException("Illegal base64 line separator character 0x" +
  hex)`, lowercase hex via `Integer.toString(b, 16)`), then `lineLength &= ~3` and `lineLength <= 0` returns
  `Encoder.BASIC` unchanged.
- The basic, URL-safe and MIME decoders are shared `static final` instances. Decode tables are `int[256]` filled
  with `-1`, indexed by alphabet, with `table['='] = -2`; `decode(String)` maps the string through ISO-8859-1
  first, so a character above 0xff becomes `?` and reports as illegal character `3f`. The shift register walks
  `shiftto` 18 → 12 → 6 → 0 → -6, writing three bytes and resetting at -6; a `=` with `shiftto == 18` or an
  unpaired `=` at `shiftto == 6` throws `IllegalArgumentException("Input byte array has wrong 4-byte ending
  unit")`; one trailing data unit throws `"Last unit does not have enough valid bits"`; any non-base64 byte in
  basic/URL mode throws `"Illegal base64 character " + hex(byte)` (signed-byte hex, e.g. `-3d` for 0xc3).
  `decodedOutLength` counts up to two trailing `=` paddings in basic/URL mode. In MIME mode it cuts `len` at the
  first `=` and then subtracts the number of non-alphabet bytes before that `=`, so wrapped text such as
  `"Zm\r\n8="` sizes to 2 and decodes into an exactly sized `dst`. The result is `3*((len+3)/4) - paddings`.
  A one-byte input sizes to 0 in MIME mode (the decode loop then still throws `"Last unit does not have enough
  valid bits"` for an alphabet byte, so only a lone non-alphabet byte decodes to nothing) but throws
  `"Input byte[] should at least have 2 bytes for base64 bytes"` elsewhere. `decode(src, dst)` throws
  `"Output byte array is too small for decoding all input bytes"` when `dst` is shorter than that estimate,
  before decoding (the encoder analogue says `encoding`). MIME decoding skips every illegal byte anywhere.
  After the padding (or the end of the data), each leftover byte is checked in turn: MIME skips non-alphabet
  bytes, and any leftover alphabet byte (or, in basic/URL mode, any leftover byte at all) throws
  `"Input byte array has incorrect ending byte at sp"`. Following the JDK's short-circuit, MIME reports the index
  after the offending byte (`Zm8=Z` and `Zm8=Zg` report `at 5`, `Zm8= Z` reports `at 6`), while basic/URL report
  the offending byte's own index (`Zm8=Z` reports `at 4`).

Evidence: `BytecodeBase64Test` compares Java 17 javac fixtures, including the MIME tail, exact-size MIME
`decode(src, dst)` and class-flag cases, on the JVM and in Wasm (both JAR orders, two fresh
instances). `Encoder.wrap`, `Decoder.wrap` and the `ByteBuffer` overloads are rejected before WAT generation.
