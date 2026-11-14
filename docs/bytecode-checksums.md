# Bytecode Checksums

Revision `checksums-v277` links `java.util.zip.Checksum`, `CRC32` and `Adler32` in the bundled exception runtime.

Contract:

- CRC32 is the standard reflected CRC-32 (zip, PNG, Ethernet): polynomial `0xEDB88320` over a 256-entry table built
  in a static initializer (`c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1`, eight times per entry), register
  preset to all ones and output inverted per byte: `c = ~crc; c = TABLE[(c ^ b) & 0xff] ^ (c >>> 8); crc = ~c;`.
  The `crc` field holds the finished value, so a fresh or reset CRC32 has `crc = 0`. Adler32 keeps `a` (low 16 bits)
  and `b` (high 16 bits) in one int starting at 1, so a fresh or reset Adler32 has `getValue() == 1`; per byte
  `a = (a + (x & 0xff)) % 65521; b = (b + a) % 65521; adler = (b << 16) | a`, taking `% 65521` after every byte so
  int arithmetic never overflows. `getValue()` returns the 32 bits unsigned, `(long) value & 0xffffffffL`, so it is
  never negative (`"123456789"` gives `3421780262`). `update(int b)` uses only `b & 0xff`. State is an instance
  field; two instances do not share it.
- `update(byte[] b, int off, int len)` checks, in this order and before touching any state: `b == null` throws
  `NullPointerException` with no message (`update(null, 5, -1)` is a `NullPointerException`, not a range
  failure), then the JDK `checkFromIndexSize` range check: `(b.length | off | len) < 0 || len > b.length - off`
  throws `ArrayIndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length "
  + b.length)` — the text literally writes `off + " + " + len`, and `len > b.length - off` (not `off + len >
  b.length`) avoids overflow at `Integer.MAX_VALUE`. `(3, 0)` on a 3-byte array is legal and does nothing; a failed
  call leaves the checksum unchanged.
- `Checksum.update(byte[])` is the interface default; `update(ByteBuffer)` and `CRC32C` are not linked.

Evidence: `BytecodeChecksumTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). The `ByteBuffer` overloads and `CRC32C` are rejected before WAT generation.
