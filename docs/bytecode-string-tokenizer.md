# Bytecode StringTokenizer

Revision `string-tokenizer-v263` links `java.util.StringTokenizer` (implementing `Enumeration<Object>`) in the bundled exception runtime.

Contract:

- Delimiters are matched per `char`. JDK supplementary-character (surrogate pair) delimiter handling is not required.
- A token is a maximal run of non-delimiter characters. Runs of delimiters, leading and trailing delimiters never produce empty tokens.
- `returnDelims == true`: each delimiter character is returned as its own one-character token, and `countTokens` counts them too.
- `hasMoreTokens()` and `countTokens()` never advance the position.
- `nextToken()` with no token left throws `NoSuchElementException` with no message.
- `nextToken(String delim)`: first replace the delimiter set permanently, then behave like `nextToken()` from the current position.
- `str == null`: the constructor throws `NullPointerException`. `delim == null` is accepted by the constructor; later `hasMoreTokens()`/`nextToken()` throw `NullPointerException`, while `countTokens()` on an empty string returns 0. `nextToken(null)` throws `NullPointerException` and leaves the delimiter set null.

Evidence: `BytecodeStringTokenizerTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh instances). The test is in `javaw-agent-kit`.

History: kit task T02. Three local-agent sessions (Qwen3-Coder-30B) got 4 of 6 tests passing but kept a `skipDelimiters` that skipped delimiters even with `returnDelims`. The reviewer (Claude) finished the class from the agent's structure; the lessons became the kit's per-fixture failure hints.

Limits: the JDK's `delimsChanged`/`newPosition` caching is not reproduced (it has no observable effect), and surrogate-pair delimiters are not supported.