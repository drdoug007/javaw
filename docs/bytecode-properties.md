# Bytecode Properties

Revision `properties-v283` links `java.util.Properties` in the bundled exception runtime, as a subclass of the
monitor-free runtime `Hashtable` that keeps its entries in the inherited table.

Contract:

- Constructors: `Properties()`, `Properties(int)` and `Properties(Properties)` all call the `Hashtable()`
  constructor (the capacity is not observable); `Properties(int n)` throws a message-less
  `IllegalArgumentException` when `n < 0` (not `Hashtable`'s `"Illegal Capacity"` message). Only
  `Properties(Properties)` assigns the `protected Properties defaults` field; `null` is allowed and means no
  defaults.
- Defaults chain and the String-only rule: `getProperty(key)` reads the own table with `super.get(key)` (the
  overridable `get` is never called) and counts a non-`String` value as absent; when the own value is missing or
  not a `String` it asks `defaults.getProperty(key)` (virtual) and recurses to the root. `getProperty(key, d)`
  calls the one-argument `getProperty` (virtual) and returns `d` when it yields `null`. The defaults object is
  live: later changes to it are visible. `get`, `containsKey`, `size`, `isEmpty`, `toString` and `equals` never
  look at the defaults, and the defaults entries are not entries of this table.
- `setProperty(key, value)` is `return put(key, value)` — a virtual call, so a subclass that overrides `put`
  sees it; it returns the previous value (which may be a non-`String`) and throws `NullPointerException` from
  `put` for a null key or value.
- `propertyNames()` copies, into a fresh `Hashtable<String,Object>`, the defaults' names (recursively, first)
  and then the own entries, and returns `h.keys()` — a real `Hashtable` enumeration (past the end it throws
  `NoSuchElementException: Hashtable Enumerator`). The copy walks the overridable `entrySet()`, so a subclass
  that hides entries changes the names; a non-`String` key anywhere in the chain throws `ClassCastException`,
  non-`String` values are kept, and an already-returned enumeration is a snapshot.
  `stringPropertyNames()` fills a fresh `HashMap<String,String>` the same way but keeps an entry only when both
  key and value are `String`s (others are skipped silently, and a non-`String` own value does not remove a
  defaults entry), and returns `Collections.unmodifiableSet(h.keySet())` — `add`, `remove`, `clear` and
  `iterator().remove()` throw `UnsupportedOperationException` with a null message.
- ConcurrentHashMap rules copied from the JDK: `remove(key, value)` throws `NullPointerException` for a null
  key and returns `false` (not an exception) when the value is null; `merge(key, value, f)` throws
  `NullPointerException` when the key, the value or the function is null (before looking at the table) and
  otherwise delegates to `Hashtable.merge`; `forEach(action)` is not fail-fast — after
  `Objects.requireNonNull(action)` (even when empty) it walks a snapshot of `super.keySet()` and re-reads each
  value with `super.get`, so removing visited or other keys never throws `ConcurrentModificationException` and
  a removed key is skipped; `equals(o)` is the `ConcurrentHashMap` algorithm over the `Hashtable`'s own entries
  with no `o == this` shortcut and no size check; `toString()` walks `super.entrySet()` and never calls
  `size()` or the overridable `entrySet()`.
- Virtual versus non-virtual calls: `setProperty` → `put` (virtual), `getProperty(k, d)` → `getProperty(k)`
  (virtual), `defaults.getProperty(k)` (virtual), `enumerate`/`enumerateStringProperties` → `entrySet()`
  (virtual); `getProperty` reads `super.get`, `putAll` writes with `super.put` after a `t.size()` presize call
  (so an overridden `put` is never called and the source map sees exactly `size` then `entrySet`), `forEach`
  uses `super.keySet`/`super.get`, `toString`/`equals` use `super.entrySet`, and `remove(k, v)`/`merge` delegate
  to `super`. `toString`, `equals`, `hashCode`, `isEmpty`, `forEach` and `clone` therefore log nothing against a
  recording subclass; only `equals(o)` calls `get`/`entrySet` on its argument.
- Inherited unchanged from `Hashtable`: `put`, `get`, `remove(k)`, `containsKey`, `contains`, `size`, `isEmpty`,
  `keys`, `elements`, `keySet`, `values`, `entrySet`, `clear`, `clone` (which shares the `defaults` object),
  `hashCode`, `putIfAbsent`, `replace`, `computeIfAbsent`, `computeIfPresent`, `compute`, `getOrDefault`,
  `replaceAll`. No `rehash()` is declared (the JDK's is a no-op).

Known differences from the JDK (not covered by the test): the JDK 9+ keeps its entries in a private
`ConcurrentHashMap`, so multi-entry iteration order of `keySet()`, `values()`, `entrySet()`, `keys()`,
`elements()`, `toString()` and `forEach` follows the runtime `Hashtable` instead of the `ConcurrentHashMap`, and so does `propertyNames()` when names
share a `Hashtable` bucket, because it fills its fresh `Hashtable` in this object's `entrySet()` order;
view iterators and `keys()`/`elements()` are fail-fast `Hashtable` enumerators (with
`"Hashtable Enumerator"` messages) where the JDK's are weakly consistent; `compute*`/`replaceAll` functions
that modify the table throw `ConcurrentModificationException` as in `Hashtable`; `keySet()` returns the same
cached view each time; and `forEach` visits a snapshot of the keys, so keys added by the action are never
visited (the JDK may visit them). The inherited `getOrDefault`, `containsValue` and the `keySet()`/`values()` views
also call a subclass's overridden `get`, `containsKey`, `contains` or `remove`, where the JDK forwards them to
its internal map.

Left out (for later tasks): `load(Reader)`/`load(InputStream)` (T23), `store`/`save` (they write the current
date, which the runtime forbids), `list` (no print streams in the runtime) and the XML methods (no XML).

Evidence: `BytecodePropertiesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Properties.rehash()` and `Properties.loadFromXML(InputStream)` are rejected before WAT
generation.
