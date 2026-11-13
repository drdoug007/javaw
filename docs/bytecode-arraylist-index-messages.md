# Bytecode ArrayList index failure messages

Revision `arraylist-index-messages-v264` gives `ArrayList` index, subList and capacity failures the JDK
exception messages in the bundled exception runtime.

Contract:

- `get(i)`, `set(i,x)`, `remove(i)` with `i<0` or `i>=n` throws `IndexOutOfBoundsException` with message `Index <i> out of bounds for length <n>`
- `add(i,x)`, `addAll(i,c)`, `listIterator(i)` with `i<0` or `i>n` throws `IndexOutOfBoundsException` with message `Index: <i>, Size: <n>`
- `subList(from,to)` with `from<0` throws `IndexOutOfBoundsException` with message `fromIndex = <from>`
- `subList(from,to)` with `to>n` throws `IndexOutOfBoundsException` with message `toIndex = <to>`
- `subList(from,to)` with `from>to` throws `IllegalArgumentException` with message `fromIndex(<from>) > toIndex(<to>)`
- `new ArrayList<>(c)` with capacity `c<0` throws `IllegalArgumentException` with message `Illegal Capacity: <c>`

Evidence: `BytecodeArrayListIndexMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm
(both JAR orders, two fresh instances). `ArrayList.removeRange(int,int)` and `Objects.checkIndex(int,int)`
are rejected before WAT generation. The test is in `javaw-agent-kit`.

Scope: the index, insert-position and subList checks live in the shared `AbstractList` helpers, so every runtime list built on them reports the same messages, including `subList` views (which report their own size). Iterator failures keep their JDK `null` messages. Kit task T03, implemented by the local agent in two checks.
