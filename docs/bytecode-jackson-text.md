# Pinned Jackson text escaping

Fixtures execute original Jackson Core 2.15.4 `CharTypes`, `CharTypes$AltEscapes` and `JsonStringEncoder` class files, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. These three classes are linked whole and unmodified. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle.

The encoder also references `TextBuffer`, `ByteArrayBuilder`, `BufferRecycler` and `UTF8Writer`. Linking those classes whole would pull in `IOContext`, `AtomicReferenceArray` and the full numeric parser. Their carriers therefore keep only the method closure reachable from the linked classes, `<clinit>` and `ByteArrayBuilder`'s `OutputStream` overrides. Unreachable methods and fields that no retained method uses are removed. Retained method bodies, superclasses and constant pools are original bytes as parsed by ASM; no method is rewritten. In practice, `UTF8Writer` keeps only `illegalSurrogateDesc`, and `BufferRecycler` keeps only its declaration. The selection uses the original `java.util.LinkedList` (v252) and `java.io.OutputStream`/`Writer` (v251) runtime classes.

`BytecodeJacksonTextTest` compares observable hashes against the JVM in both JAR entry orders and two fresh dirty-memory instances per exported invocation. Coverage includes hex conversion over the Latin-1 range and out-of-range values, cloned hex tables, all six input-code tables, standard and alternate-quote 7-bit escape tables with identity caching, and `appendQuoted` over the first 300 characters. Encoder cases cover control characters, quotes/backslashes, `</script>`, line separators, BMP and supplementary text, and inputs long enough to force `TextBuffer`/`ByteArrayBuilder` segment growth. They also cover String and `StringBuilder` sources, appending to a shared builder and the singleton/fresh encoder instances. Failure cases cover null inputs/outputs, out-of-range quote characters and unmatched-surrogate `IllegalArgumentException` messages from the UTF-8 paths.

Completed focused cases (each passes):

- `charToHex`
- `hexDigits`
- `inputCodes`
- `outputEscapes`

Complete serializer/generator execution, `IOContext`/buffer-recycling paths, whole-JAR admission, sustained allocation stability and the broader text/JDK/framework milestones remain open.
