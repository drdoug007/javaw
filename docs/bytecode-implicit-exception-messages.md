# Bytecode implicit array failure messages

Revision `implicit-array-messages-v254` gives implicit array failures the JVM detail messages when the exception runtime is linked:

- Element loads and stores (`baload`…`aaload`, `bastore`…`aastore`, including covariant reference stores) raise `ArrayIndexOutOfBoundsException("Index <i> out of bounds for length <n>")`, built by `javaw.runtime.ExceptionSupport.arrayIndexFailure`.
- `newarray`, `anewarray` and `multianewarray` raise `NegativeArraySizeException("<length>")`. For `multianewarray` this is the first negative dimension in operand order, matching HotSpot.
- The runtime also links the JDK `ArrayIndexOutOfBoundsException(int)` (`"Array index out of range: <i>"`) and `IndexOutOfBoundsException(int|long)` (`"Index out of range: <i>"`) constructors.

Builds without the exception runtime keep the previous message-free helper or trap. `System.arraycopy` failures still use the message-free helper; their JDK messages (`arraycopy: last source index …`) are not claimed. The runtime's `StringIndexOutOfBoundsException(int)` and its `StringBuilder` callers still do not match JDK messages.

Evidence: `BytecodeArrayBoundsMessageTest` compares Java 17 javac fixtures against the JVM in both JAR orders and two fresh dirty-memory instances. It covers loads and stores for every primitive and reference array kind, including covariant arrays and compound assignment. Indexes cover -1, in-range values, length, length+1, `Integer.MIN_VALUE` and `Integer.MAX_VALUE`. It also covers empty and nested arrays, catching as `IndexOutOfBoundsException`, single- and multi-dimensional negative allocation sizes, and the explicit index constructors.
