# T22 Properties

```kit
id: T22
slug: properties
title: link java.util.Properties on top of the runtime Hashtable
tests: BytecodePropertiesTest
depends: T21
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Properties.java
allow: docs/bytecode-properties.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-properties-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePropertiesTest.java`.

## Goal

Add `java.util.Properties` to the bundled runtime as a subclass of the runtime `java.util.Hashtable` (task T21). 282
classes in the pinned libraries name the type (`runtime-target/target/audit/external-types.tsv`: logback, Jackson,
HikariCP, H2, Hibernate, Spring, Tomcat...). `member-references.tsv` counts `getProperty(String)` 132 times,
`<init>()` 120, `put` 116, `setProperty` 77, `getProperty(String,String)` 49, `putAll` 41, `get` 33, `containsKey` 27,
`remove` 19, `entrySet` 16, `propertyNames` 15, `stringPropertyNames` 10, `keySet` 9, `size` 7, `forEach` 6,
`keys` 5, `isEmpty` 5, `<init>(Properties)` 5, `clone` 4, `equals` 3, `toString` 2 and `<init>(int)` 1. `load` (31
calls) is task T23; `store`, `list` and the XML methods stay out.

**Design decided at calibration: entries live in the inherited Hashtable, and the tests are order-independent.**
JDK 9+ `Properties` keeps its entries in a private `ConcurrentHashMap` and forwards every `Map` method to it, so on
the JVM `toString()`, `keySet()`, `entrySet()`, `keys()` and `forEach` follow `ConcurrentHashMap` order. Copying that
layout (lazy table, tail insertion, `lastRun` resize splitting, `putAll` presizing) would double the size of this
task, so here **the entries are stored in the inherited `Hashtable` itself** (call `super.put`, `super.get`, ...)
and multi-entry iteration order differs from the JDK. Every fixture is written so that order does not matter
(single entries, sorted name lists, sizes and `contains`). The one exception is `propertyNames()`, which in the JDK
too returns the keys of a **fresh `Hashtable`**, so its order is exactly the runtime Hashtable's (T21) and the test
checks it (with keys that never share a bucket).

You only override what `Properties` adds, plus the few `Map` methods whose `ConcurrentHashMap` behaviour a caller
can see (null rules, which overridable methods get called). Everything else is inherited from `Hashtable` unchanged.

## Members to implement

One file, `java/util/Properties.java` (`package java.util;`):
`public class Properties extends Hashtable<Object,Object>`. It needs `import java.util.function.*;` (or the full
names `java.util.function.BiConsumer` and `java.util.function.BiFunction`).

- `protected Properties defaults;` (no `volatile`; subclasses read and assign it directly, the test does)
- `public Properties()`
- `public Properties(int initialCapacity)`
- `public Properties(Properties defaults)`
- `public Object setProperty(String key, String value)`
- `public String getProperty(String key)`
- `public String getProperty(String key, String defaultValue)`
- `public Enumeration<?> propertyNames()`
- `public Set<String> stringPropertyNames()`
- `public void putAll(Map<?,?> t)`
- `public boolean remove(Object key, Object value)`
- `public Object merge(Object key, Object value, BiFunction<? super Object,? super Object,?> remappingFunction)`
- `public void forEach(BiConsumer<? super Object,? super Object> action)`
- `public String toString()`
- `public boolean equals(Object o)`
- two private helpers `private void enumerate(Map<String,Object> h)` and
  `private void enumerateStringProperties(Map<String,String> h)`

Do **not** declare `put`, `get`, `remove(Object)`, `containsKey`, `contains`, `size`, `isEmpty`, `keys`,
`elements`, `keySet`, `values`, `entrySet`, `clear`, `clone`, `hashCode`, `putIfAbsent`, `replace`, `compute*`,
`getOrDefault` or `replaceAll`: the inherited `Hashtable` versions are what the test expects.

## Members to leave out

- `load(Reader)`, `load(InputStream)`: task T23.
- `store(Writer,String)`, `store(OutputStream,String)`, `save`: they write the current date, which the runtime
  forbids. `list(PrintStream)`, `list(PrintWriter)`: no print streams in the runtime.
- `loadFromXML`, `storeToXML` (all overloads): no XML. The test checks that `Properties.loadFromXML(InputStream)`
  stays unlinked.
- `protected void rehash()`: the JDK's is a no-op. Do not declare any method named `rehash` (the test checks that
  `Properties.rehash()` stays unlinked, like `Hashtable.rehash()` in T21).
- The JDK's private `ConcurrentHashMap map` field, serialization hooks, and wrapping `keySet()`/`values()`/
  `entrySet()` in `Collections.synchronizedSet`.

## JDK behaviour to match

- Constructors. All three call `super()` (the `Hashtable` capacity is not observable). `Properties(int n)` throws
  `new IllegalArgumentException()` (message `null`, not Hashtable's `"Illegal Capacity: -1"`) when `n < 0`;
  `new Properties(0)` and `new Properties(100)` are fine. `Properties(Properties defaults)` stores `defaults`
  (`null` is allowed and means no defaults). `Properties()` and `Properties(int)` leave `defaults` null.
- `setProperty(key, value)` is `return put(key, value);`: a **virtual** call, so a subclass that overrides `put` sees
  it (`setPropertyCallsPut` expects `put(a=1) put(a=2)`). It returns the old value, which may be a non-`String`
  (`p.put("n", 5); p.setProperty("n", "five")` returns the `Integer` 5). A null key or value throws
  `NullPointerException` from `put` (`setProperty("a", null)` prints `NullPointerException: null`).
- `getProperty(key)`: `Object value = super.get(key);` (**`super.get`**, so a subclass's `get` override is NOT called);
  only a `String` counts: `String text = value instanceof String ? (String) value : null;` then
  `return text == null && defaults != null ? defaults.getProperty(key) : text;` (the call on `defaults` is virtual:
  a `Recorder` used as defaults logs `getProperty(k)`). A non-String value hides nothing: it falls through to the
  defaults, and with no default the result is `null`. `getProperty(null)` throws `NullPointerException` (from
  `Hashtable.get`). The defaults object is live: later changes to it are visible.
- `getProperty(key, defaultValue)`: `String value = getProperty(key); return value == null ? defaultValue : value;`
  (virtual call to the one-argument `getProperty`; the test records it). `get`, `containsKey`, `size`,
  `isEmpty`, `toString`, `equals`, `keySet()` never look at the defaults.
- `propertyNames()`: `Hashtable<String,Object> h = new Hashtable<>(); enumerate(h); return h.keys();`.
  `enumerate(h)`: first `if (defaults != null) defaults.enumerate(h);` (recursively, on the defaults object), then
  `for (Map.Entry<Object,Object> e : entrySet()) h.put((String) e.getKey(), e.getValue());`. Note `entrySet()` is the
  **virtual** call (a subclass that overrides it changes the names; the test has one), and the cast throws
  `ClassCastException` for a non-String key anywhere in the chain. Non-String **values** are kept. The result is a
  snapshot: later changes to the properties do not show in an enumeration already returned. For defaults
  `a,c,e` and own `b,d,a` the names come out as `b a e d c`, and past the end `nextElement()` throws
  `NoSuchElementException: Hashtable Enumerator` (because it is a real `Hashtable` enumeration); an empty
  `Properties` gives an enumeration whose `nextElement()` throws `NoSuchElementException: null`.
- `stringPropertyNames()`: `Map<String,String> h = new HashMap<>(); enumerateStringProperties(h);
  return Collections.unmodifiableSet(h.keySet());`. `enumerateStringProperties(h)`: first the defaults (recursively,
  as above), then for each entry of `entrySet()` put it only `if (k instanceof String && v instanceof String)`.
  Non-String keys or values are skipped silently (no exception), and a String key whose own value is not a String
  does not remove the same key inherited from the defaults (`child.put("k", 42)` over defaults `k=grand` keeps `k`
  in the set, and `getProperty("k")` returns `grand`). The test uses three levels: `grand` has `g=1, k=grand`,
  `parent = new Properties(grand)` does `parent.put("k", 42)`, `child = new Properties(parent)`; then
  `child.stringPropertyNames()` still contains `k` and `child.getProperty("k")` is `grand`. The set is a copy; `add`, `remove`, `clear` and
  `iterator().remove()` throw `UnsupportedOperationException` with message `null`.
- `putAll(t)`: `t.size();` first (the JDK presizes its map with it; a recording map logs `size entrySet`, and
  `putAll(null)` throws `NullPointerException` here), then
  `for (Map.Entry<?,?> e : t.entrySet()) super.put(e.getKey(), e.getValue());`. **`super.put`**: the JDK's `putAll`
  never calls an overridden `put` (the inherited `Hashtable.putAll` would). A null key or value throws
  `NullPointerException` at that entry; earlier entries stay.
- `remove(key, value)` (ConcurrentHashMap rule): `if (key == null) throw new NullPointerException();` then
  `return value != null && super.remove(key, value);`. So `remove("a", null)` returns `false` instead of throwing.
- `merge(key, value, f)` (ConcurrentHashMap rule): `if (key == null || value == null || remappingFunction == null)
  throw new NullPointerException();` then `return super.merge(key, value, remappingFunction);`. So
  `merge("a", null, f)` throws `NullPointerException: null` even when `a` is absent.
- `forEach(action)`: not fail-fast, like `ConcurrentHashMap.forEach`. `Objects.requireNonNull(action);` (even when
  empty), then take a snapshot of the keys `new ArrayList<Object>(super.keySet())` and for each key
  `Object value = super.get(key); if (value != null) action.accept(key, value);`. So an action may remove the visited
  key (3 entries: 3 calls, no `ConcurrentModificationException`, `{}` afterwards) or another key (that key is then
  skipped). Use `super.keySet()`/`super.get`, never the overridable `keySet()`/`get` (a `Recorder` logs nothing).
- `toString()`: walk `super.entrySet()` (the Hashtable's own view, not the overridable `entrySet()`), giving `"{}"`
  when empty, else `"{" + key + "=" + value + ", " ... + "}"` using `StringBuilder.append(Object)`. Do **not** add a
  `(this Map)` case: the JDK's check compares against its internal map, so on the JVM a `Properties` that contains
  itself overflows the stack; the test does not try it. Do not call `size()`.
- `equals(o)`: the `ConcurrentHashMap.equals` algorithm over the Hashtable's own entries, with **no** `o == this`
  shortcut (the JDK's shortcut compares `o` with its internal map, so `p.equals(p)` goes the long way and a `Recorder`
  logs `get(a) entrySet` for `r.equals(r)`):
  `if (!(o instanceof Map)) return false; Map<?,?> m = (Map<?,?>) o;` then
  for each entry of `super.entrySet()`: `Object v = m.get(key); if (v == null || (v != value && !v.equals(value)))
  return false;` then for each `e` of `m.entrySet()`: if `e.getKey() == null`, or `e.getValue() == null`, or
  `super.get(e.getKey()) == null`, or the value differs (`mv != mine && !mv.equals(mine)`), return `false`;
  finally `return true;`. No `size()` call and no `try/catch`. For `p = {a=1}` a recording map logs `get(a) entrySet`
  when equal, only `get(a)` when its value for `a` differs, and `get(a) entrySet` when it has an extra entry
  (`b=2` or `b=null`), each returning the result shown in `equalsCallPattern`.
- Inherited without change (the test relies on it): `put`/`get`/`remove(k)`/`containsKey`/`contains`/`size`/
  `isEmpty`/`clear`/`hashCode`, `putIfAbsent`, `replace`, `computeIfAbsent`, `computeIfPresent`, `compute`,
  `getOrDefault`, the views and `clone()`. `Hashtable.clone()` already calls `super.clone()`, so a subclass clones to
  its own class, the copy has its own entries, and the copy **shares** the `defaults` object (`c.defaults == d`).

Known differences from the JDK, to write in the docs (not tested): iteration order of the views, `keys()`,
`elements()`, `toString()` and `forEach` follows the runtime Hashtable instead of `ConcurrentHashMap`; view iterators
and `keys()`/`elements()` are Hashtable's (fail-fast iterators, `"Hashtable Enumerator"` messages) where the JDK's are
weakly consistent; `compute*`/`replaceAll` functions that change the table throw
`ConcurrentModificationException` as in Hashtable; `keySet()` returns the same cached view each time; `forEach`
visits a snapshot of the keys, so keys added by the action are never visited (the JDK may visit them).

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePropertiesTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/Hashtable.java` (the superclass; see which methods
   exist and their exact signatures).
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/Properties.java` with exactly the members
   above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/Properties.java`.
4. `javaw-agent-kit/bin/kit check T22` until PASS.
5. Write `docs/bytecode-properties.md` (skeleton below), the report, then `javaw-agent-kit/bin/kit finish T22`.

## Hints

- The whole class is about 80 lines. Inside `Properties`, `super.get(k)`, `super.put(k, v)`, `super.remove(k, v)`,
  `super.merge(...)`, `super.keySet()` and `super.entrySet()` call the `Hashtable` methods directly and skip any
  subclass override. That is the whole trick of this task: use `super.` exactly where the spec says so, and a plain
  call (`put`, `getProperty`, `entrySet()`, `defaults.getProperty`) where the spec says virtual.
- `HashMap`, `Hashtable`, `ArrayList(Collection)`, `Collections.unmodifiableSet`, `Objects.requireNonNull`,
  `IllegalArgumentException`, `ClassCastException` and `java.util.function.BiConsumer`/`BiFunction` exist in the
  runtime.
- `private` helpers are fine here because `defaults.enumerate(h)` is called from inside `Properties` itself.
- Signatures: `putAll(Map<?,?> t)` overrides `Hashtable.putAll(Map<? extends Object,? extends Object>)`; javac
  accepts both, but keep `Map<?,?>`. `merge` must be
  `public Object merge(Object key, Object value, BiFunction<? super Object,? super Object,?> remappingFunction)`.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` anywhere in the file, not even in comments
  (`kit guard` rejects them). The JDK methods are synchronized; here they are plain methods.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture
  of the same group may fail next. That is progress.
- Calibration pitfalls: (1) a `(this Map)` check copied from Hashtable is harmless for the test but not JDK
  behaviour, leave it out; (2) inheriting `Hashtable.equals` fails `equalsCallPattern` (it calls `m.size()` and stops
  on a size mismatch) and `objectMethodsIgnoreOverrides` (it calls the overridable `entrySet()`); (3) inheriting
  `Hashtable.putAll` fails `putAllBypassesPut` (it calls the overridden `put`, and never calls `t.size()`);
  (4) inheriting `Hashtable.forEach` fails `forEachMayRemove` with `ConcurrentModificationException`.
- Mutations of the reference were run against the test: dropping the `equals` override fails `equalsCallPattern`
  and `objectMethodsIgnoreOverrides`; dropping `putAll` gives `put(a=1) put(b=2)|entrySet|...` instead of
  `|size entrySet|...` in `putAllBypassesPut`; dropping `forEach` gives `ConcurrentModificationException: null|1|...`
  instead of `no exception|3|{}|...` in `forEachMayRemove`.
- Calibration: the reference passed kit check after 1 run (about 80 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndLookup, defaultsChain, propertyNamesAndStringPropertyNames, concurrentMapRules, subclassCallPatterns: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/Properties` or `link the descriptor class/interface java/util/Properties`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/Properties.java` once (never edit BytecodeExceptionRuntime.java by hand). If it says `link a matching instance method in the declaring hierarchy`, the member the named fixture calls is missing, not public, or has another signature: add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
hierarchy: the class must be `public class Properties extends Hashtable<Object,Object>` (it is a Hashtable, a Dictionary and a Map, but not an AbstractMap).
negativeCapacity: Properties(int n) calls `super()` and then `if (n < 0) throw new IllegalArgumentException();` with NO message; do not pass n to the Hashtable constructor (its message "Illegal Capacity: -1" is wrong here).
setAndGet, getPropertyWithDefault, nonStringEntries, nullFailures: setProperty is `return put(key, value);`; getProperty(key) is `Object value = super.get(key); String text = value instanceof String ? (String) value : null; return text == null && defaults != null ? defaults.getProperty(key) : text;`; getProperty(key, d) is `String value = getProperty(key); return value == null ? d : value;`.
defaultsLookup, defaultsAreLive, threeLevels, nonStringValuesFallThrough, defaultsField, defaultsAreNotEntries: `defaults` must be a `protected Properties defaults;` field set only by the Properties(Properties) constructor and read on every getProperty call (never copied into the table); a non-String own value falls through to `defaults.getProperty(key)`; get, containsKey, size, toString and equals ignore the defaults.
propertyNamesOrder, emptyNames, propertyNamesKeepsNonStringValues, propertyNamesRejectsNonStringKeys, propertyNamesIsACopy: propertyNames() is `Hashtable<String,Object> h = new Hashtable<>(); enumerate(h); return h.keys();` and enumerate(h) is `if (defaults != null) defaults.enumerate(h); for (Map.Entry<Object,Object> e : entrySet()) h.put((String) e.getKey(), e.getValue());` (the cast must throw ClassCastException for a non-String key; non-String values stay). Do not return Collections.enumeration or a Vector: the expected order `b a e d c` and the message `Hashtable Enumerator` come from a real Hashtable.
stringPropertyNames, stringPropertyNamesIsAnUnmodifiableCopy: stringPropertyNames() fills a new HashMap<String,String> with the defaults first (recursively) and then every own entry whose key AND value are both Strings, and returns `Collections.unmodifiableSet(h.keySet())`; skip other entries without throwing and never remove a defaults key because the own value is not a String.
conditionalRemove: remove(key, value) is `if (key == null) throw new NullPointerException(); return value != null && super.remove(key, value);` so remove("a", null) is false, not an exception.
mergeRejectsNulls: merge starts with `if (key == null || value == null || remappingFunction == null) throw new NullPointerException();` and then returns `super.merge(key, value, remappingFunction)`.
forEachMayRemove, forEachUpdatesAndNulls: forEach is `Objects.requireNonNull(action); for (Object key : new ArrayList<Object>(super.keySet())) { Object value = super.get(key); if (value != null) action.accept(key, value); }` so removing keys inside the action never throws ConcurrentModificationException and a removed key is skipped.
putAllStopsAtNull, putAllBypassesPut: putAll(t) is `t.size(); for (Map.Entry<?,?> e : t.entrySet()) super.put(e.getKey(), e.getValue());` (call t.size() first, use super.put so an overridden put is not called; the recording map must log exactly `size entrySet`).
toStringForms: toString walks `super.entrySet()` and builds `{k=v, k=v}` (`{}` when empty) with StringBuilder.append(Object); do not call size() or the overridable entrySet().
equalityAndHash, equalsCallPattern: equals has no `o == this` shortcut and no size check: `if (!(o instanceof Map)) return false;` then for each own entry (from `super.entrySet()`) `Object v = m.get(key); if (v == null || (v != value && !v.equals(value))) return false;` then for each entry of `m.entrySet()` return false if its key or value is null, `super.get(key)` is null, or the values differ; then true. The recording map must log `get(a) entrySet`, or only `get(a)` when the first loop already failed.
inheritedMapMembers: do not override putIfAbsent, computeIfAbsent, computeIfPresent, compute, replace, getOrDefault, put or contains: the Hashtable versions give the expected values.
setPropertyCallsPut, getPropertyCalls, defaultsCallGetProperty: setProperty calls the virtual `put`, getProperty(k, d) calls the virtual `getProperty(k)`, getProperty(k) reads with `super.get(k)` (never the overridable `get`) and asks `defaults.getProperty(k)` (virtual) when the own value is missing or not a String.
namesUseEntrySet, overriddenEntrySet: enumerate and enumerateStringProperties call the overridable `entrySet()` on this object and recurse on the defaults object (`defaults.enumerate(h)`), so each Recorder logs exactly `entrySet` once; do not use keySet(), keys() or super.entrySet() there. toString and equals use `super.entrySet()`, so a subclass that hides entries from entrySet() still prints and compares them.
objectMethodsIgnoreOverrides: toString, equals(other), hashCode, isEmpty, forEach and clone must not call any overridable method on this object (`[]` in the expected log); only equals(o) calls `o.get(key)` and `o.entrySet()` on its ARGUMENT, which logs `get(a) entrySet` for r.equals(r).
cloneIsIndependentAndSharesDefaults: do not override clone(); the inherited Hashtable.clone() copies the table and keeps the same `defaults` object.
leavesOutRehashAndXml: this fails when Properties.java declares a method named `rehash` or `loadFromXML`; delete it.
```

## Docs

Create `docs/bytecode-properties.md` with this skeleton (fill in the brackets):

    # Bytecode Properties

    Revision `properties-vN` links `java.util.Properties` in the bundled exception runtime, as a subclass of the
    monitor-free runtime `Hashtable` that keeps its entries in the inherited table.

    Contract:

    - <constructors, defaults chain and the String-only rule, setProperty, propertyNames and stringPropertyNames,
      the ConcurrentHashMap rules it copies (remove(k, null), merge nulls, non-fail-fast forEach, equals and toString
      that ignore overrides), which calls are virtual, and the known differences from the JDK (iteration order,
      Hashtable iterators)>

    Evidence: `BytecodePropertiesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Properties.rehash()` and `Properties.loadFromXML(InputStream)` are rejected before WAT
    generation.
