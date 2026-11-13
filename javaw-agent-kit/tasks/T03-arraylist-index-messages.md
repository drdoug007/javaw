# T03 ArrayList index failure messages

```kit
id: T03
slug: arraylist-index-messages
title: give ArrayList index and subList failures the JDK exception messages
tests: BytecodeArrayListIndexMessagesTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractList.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/ArrayList.java
allow: docs/bytecode-arraylist-index-messages.md
```

## Goal

`java.util.ArrayList` already works in the runtime, but its failures throw exceptions **without a message**,
so `e.getMessage()` is `null` in Wasm. Libraries log and compare these messages (for example
`"Index 0 out of bounds for length 0"`). Make every `ArrayList` index, `subList` and capacity failure carry
the exact JDK 17/21 message. No new classes or members: you only change the text of exceptions that are
already thrown.

## Members to change

All in the runtime copies (not the JDK):

- `AbstractList.index(int)` (`protected final`, runtime-only helper used by `get`/`set`/`remove`).
- `AbstractList.position(int)` (`protected final`, runtime-only helper used by `add(int,E)`,
  `addAll(int,Collection)` and `listIterator(int)`).
- `AbstractList.subList(int,int)`.
- `ArrayList(int)` constructor.

The `subList` view (`AbstractList.View`) and the iterators (`AbstractList.Cursor`) call these helpers, so
they get the right messages automatically. Do not add new public methods.

## Members to leave out

- `ArrayList.removeRange(int,int)` (protected, not needed here).
- `java.util.Objects.checkIndex(int,int)`. Do not add it to the runtime in this task; build the message
  string directly. The test checks both stay unlinked.

## JDK behaviour to match

`i` is the index argument, `n` is the current `size()` of the list or view the method was called on.

| Call | Exception | Message |
|---|---|---|
| `get(i)`, `set(i,x)`, `remove(i)` with `i<0` or `i>=n` | `IndexOutOfBoundsException` | `Index <i> out of bounds for length <n>` |
| `add(i,x)`, `addAll(i,c)`, `listIterator(i)` with `i<0` or `i>n` | `IndexOutOfBoundsException` | `Index: <i>, Size: <n>` |
| `subList(from,to)` with `from<0` | `IndexOutOfBoundsException` | `fromIndex = <from>` |
| `subList(from,to)` with `to>n` | `IndexOutOfBoundsException` | `toIndex = <to>` |
| `subList(from,to)` with `from>to` | `IllegalArgumentException` | `fromIndex(<from>) > toIndex(<to>)` |
| `new ArrayList<>(c)` with `c<0` | `IllegalArgumentException` | `Illegal Capacity: <c>` |

- Note the two different formats: `get/set/remove` say `out of bounds for length`; the insert positions say
  `Index: 5, Size: 2` (with a colon and a comma).
- `subList` checks in this order: `from<0` first, then `to>n`, then `from>to`. So `subList(3,5)` on size 2 is
  `toIndex = 5`, `subList(3,2)` is `IllegalArgumentException fromIndex(3) > toIndex(2)`, and
  `subList(-2,-3)` is `fromIndex = -2`.
- `addAll(3, null)` on size 2 throws the index exception (not a `NullPointerException`): the range check
  comes first. The current code already does that order; keep it.
- A view's messages use the **view's** size: `list(a,b,c,d).subList(1,3).get(2)` is
  `Index 2 out of bounds for length 2`.
- Iterator failures keep **no** message: `NoSuchElementException`, `IllegalStateException` and
  `ConcurrentModificationException` all have `getMessage()==null`. Do not add messages to them.
- Negative numbers print with a minus sign: `Index -2147483648 out of bounds for length 2`.

## Steps

1. Open `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractList.java`.
2. In `index(int index)`, change `throw new IndexOutOfBoundsException()` to throw
   `new IndexOutOfBoundsException("Index "+index+" out of bounds for length "+size())`.
3. In `position(int index)`, use the message `"Index: "+index+", Size: "+size()`.
4. Rewrite the first line of `subList(int start,int end)` as three separate `if` checks in the order above,
   each with its message. Keep the `return` line that builds `RandomView`/`View` unchanged.
5. Open `ArrayList.java`. In `ArrayList(int capacity)`, give the `IllegalArgumentException` the message
   `"Illegal Capacity: "+capacity`.
6. Run `javaw-agent-kit/bin/kit check T03` until it prints `PASS`.
7. Write `docs/bytecode-arraylist-index-messages.md` (see Docs), then the report, then `kit finish T03`.

No registration edit is needed: both files are already registered.

## Hints

- This is a small edit: four `throw` statements get a message string. Calibration passed on the first
  `kit check`. If you find yourself adding methods or classes, stop and re-read the table.
- Change the shared helpers `index`/`position` in `AbstractList`; do **not** add `rangeCheck` copies to
  `ArrayList`. `ArrayList.get/set/remove/add/addAll` and the subList view (`View`) already call them, so one
  edit fixes the list, the view and `listIterator(int)` together.
- Inside `index`/`position` call `size()`, not a field: in a subList view `size()` is the view's length,
  which is what the JDK prints.
- `LinkedList` has its own `element`/`cursor` checks with the right messages already. Do not touch it.
- Build messages with string `+` (`"Index "+index+...`). `String.format` and `Objects.checkIndex` are not
  needed; `Objects.checkIndex` is not in the runtime and the test requires it to stay out.
- Keep the `subList` checks as three separate `if` statements; one combined `if(start<0 || end>size())`
  cannot choose between `fromIndex = ` and `toIndex = `.
- The only other runtime lists using `index` are `Arrays.asList`, `Collections.singletonList`/`emptyList`
  and `Stream.toList`; their messages change from `null` to the new text. No existing test depends on the old
  `null`, and the full suite passed in calibration.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
getEmpty, getPastEnd, getNegative, getMinValue, setPastEnd, setNegative, removePastEnd, removeEmpty, sizeAfterGrowth, inBounds: get/set/remove must print 'IndexOutOfBoundsException:Index 2 out of bounds for length 2' (Wasm shows null or the wrong format). Fix the shared helper AbstractList.index(int), not ArrayList.get/set/remove: keep its condition `index<0 || index>=size()` and throw `new IndexOutOfBoundsException("Index "+index+" out of bounds for length "+size())` (word Index, a space, no colon). Use string +, never Objects.checkIndex or String.format: neither is in the runtime.
addAtPastEnd, addAtNegative, addAllAtPastEnd, addAllAtNegativeEmpty, listIteratorPastEnd, listIteratorNegative, addAtEnd, addAllAtEnd, listIteratorAtEnd: insert positions use the OTHER format 'Index: 5, Size: 2'. Fix AbstractList.position(int): keep its condition `index<0 || index>size()` (index==size() is legal) and throw `new IndexOutOfBoundsException("Index: "+index+", Size: "+size())`. Do not reuse the index() message here.
addAllAtNullCollection: expected 'IndexOutOfBoundsException:Index: 3, Size: 2', not a NullPointerException. In ArrayList.addAll(int,Collection) and AbstractList.addAll(int,Collection) keep position(index) as the FIRST statement, before values.toArray() or the for loop.
subListNegativeFrom, subListToPastEnd, subListFromAfterTo, subListBothBad, subListFromPastSize, subListNegativeAndInverted, subListEmptyAtEnd: in AbstractList.subList(int start,int end) replace the combined check with three separate ifs in this exact order: start<0 throws IndexOutOfBoundsException("fromIndex = "+start); end>size() throws IndexOutOfBoundsException("toIndex = "+end); start>end throws IllegalArgumentException("fromIndex("+start+") > toIndex("+end+")"). So on size 2, subList(3,5) is 'toIndex = 5', subList(3,2) is the IllegalArgumentException, subList(-2,-3) is 'fromIndex = -2', and subList(2,2) is legal. Keep the return line unchanged.
viewGet, viewSet, viewRemove, viewAdd, viewAddAll, viewListIterator, viewSubList, viewSubListInverted, nestedViewGet: a subList view must print its OWN size (view() has size 2, so 'Index 2 out of bounds for length 2' and 'Index: 3, Size: 2'). If the view still prints null, you put the message in ArrayList instead of AbstractList.index/position/subList; inside those helpers call size(), never a field. View already calls these helpers, so do not edit View.
iteratorNextEmpty, iteratorRemoveFirst, listIteratorPreviousAtStart, listIteratorSetFirst, iteratorAfterAdd, viewAfterParentAdd: these must stay 'NoSuchElementException:null', 'IllegalStateException:null' and 'ConcurrentModificationException:null', exactly as on the JVM. Remove any message you added to the throws in AbstractList.Cursor (next, previous, remove, set, check) or View.check; only index, position, subList and ArrayList(int) get messages.
negativeCapacity, zeroCapacity: expected 'IllegalArgumentException:Illegal Capacity: -1'. Fix the ArrayList(int capacity) constructor: keep the check `capacity<0` (0 is legal) and throw `new IllegalArgumentException("Illegal Capacity: "+capacity)` (capital I and C, colon, one space).
leavesOutRemoveRangeAndObjectsCheckIndex: this fails when ArrayList.removeRange(int,int) or Objects.checkIndex(int,int) has become linked. Do not add or call either one (and do not use String.format); build the messages with string + inside AbstractList.index/position.
```

## Docs

Create `docs/bytecode-arraylist-index-messages.md` with this skeleton (fill in the brackets):

    # Bytecode ArrayList index failure messages

    Revision `arraylist-index-messages-vN` gives `ArrayList` index, subList and capacity failures the JDK
    exception messages in the bundled exception runtime.

    Contract:

    - <one line per row of the table in the task>

    Evidence: `BytecodeArrayListIndexMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm
    (both JAR orders, two fresh instances). `ArrayList.removeRange(int,int)` and `Objects.checkIndex(int,int)`
    are rejected before WAT generation.
