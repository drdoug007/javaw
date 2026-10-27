# Pinned Jackson character escapes

Fixtures link unmodified class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle. Each exported case is compared against the JVM in both JAR entry orders and in two fresh dirty-memory instances.

`CharacterEscapes`, `JsonpCharacterEscapes`, `CharTypes`, `CharTypes$AltEscapes`, `JsonStringEncoder`, `ByteArrayBuilder` and `BufferRecycler` are linked whole and unmodified.

`java.nio.ByteBuffer` and object-stream serialization are not linked. So the `SerializableString` and `SerializedString` carriers drop exactly the members whose descriptors use them: `putQuotedUTF8`, `putUnquotedUTF8`, `readObject` and `writeObject`. Every other member keeps its original instructions. `TextBuffer` and `UTF8Writer` keep only the method closure reachable from retained members, as in [the text contract](bytecode-jackson-text.md). ASM re-serializes the selected classes; no instruction is rewritten.

`BytecodeJacksonCharacterEscapesTest` covers:

- `customEscapes`: the `ESCAPE_*` constants, fresh `standardAsciiEscapesForJSON` copies that are independent of `CharTypes` tables, and a fixture subclass with custom/standard/none codes and custom escape sequences.
- `jsonpEscapes`: `instance()` identity, ASCII escape codes, and cached `U+2028`/`U+2029` escape sequences, with `null` for other code points, including negative and supplementary values.
- `serializedStrings`: values covering quotes, controls, line separators, supplementary characters and 150-character text. Checks include `charLength`, the cached `asQuotedChars`/`asQuotedUTF8`/`asUnquotedUTF8` arrays, the `appendQuoted`/`appendUnquoted`/`appendQuotedUTF8`/`appendUnquotedUTF8` results (including insufficient-buffer `-1`), `writeQuotedUTF8`/`writeUnquotedUTF8` into a fixture `OutputStream`, `equals`/`hashCode`/`toString`, and the explicit null-value failure.

`ByteBuffer` output, Java serialization, generator escaping integration and whole-JAR admission remain open.
