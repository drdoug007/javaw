# Bytecode StringJoiner

Revision `string-joiner-v262` links `java.util.StringJoiner` in the bundled exception runtime.

Contract:

- Constructors: StringJoiner(delimiter) behaves like StringJoiner(delimiter, "", ""), and the 3-argument constructor checks parameters in order (prefix, delimiter, suffix) with exact JDK messages
- add(): appends elements with delimiter, treating null as "null"
- setEmptyValue(): sets the value returned when no elements were added (keeps old value on null input)
- toString(): returns prefix + joined elements + suffix, or empty value when no elements added
- merge(): adds other's joined elements as one element using other's delimiter but without prefix/suffix, leaving other unchanged. merge(null) throws `NullPointerException` with no message, as the JDK does
- length(): always matches toString().length() in both states

Evidence: `BytecodeStringJoinerTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh instances). The test is in `javaw-agent-kit`; the class was implemented by the local agent (kit task T01) and corrected in review (merge(null) message).

Limits: the JDK's `OutOfMemoryError` check for joined lengths above `Integer.MAX_VALUE` is not implemented.