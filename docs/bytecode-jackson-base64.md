# Pinned Jackson Base64

Fixtures link unmodified class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle. Each exported case is compared against the JVM in both JAR entry orders and in two fresh dirty-memory instances.

`Base64Variant`, `Base64Variant$PaddingReadBehaviour` and `Base64Variants` are linked whole, together with the whole `ByteArrayBuilder` and `BufferRecycler` used by decoding.

`BytecodeJacksonBase64Test` covers:
- `encoding`: `encode(byte[])`, `encode(byte[],boolean)` and `encode(byte[],boolean,String)` for MIME, MIME without linefeeds, PEM and URL-safe variants. Inputs cover every length from 0 to 80, a 700-byte input and all 256 byte values. Quoting, custom, empty and null linefeeds are included, as are null-input failures.
- `encodingPieces`: `encodeBase64BitsAsChar`/`Byte` for all 64 values and out-of-range indexes, plus the `encodeBase64Chunk`/`encodeBase64Partial` overloads for `char[]`, `byte[]` and `StringBuilder`, including insufficient-buffer and null failures.

Java serialization (`readResolve` through object streams), Jackson parser/generator binary paths and whole-JAR admission remain open.
