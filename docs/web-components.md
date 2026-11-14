# Web components from Java classes (design contract)

Status: **design only. Nothing in this document is implemented.** It records the agreed authoring model and the runtime contracts an implementation must satisfy. Milestones and their evidence are tracked in [Todo.md section 15](../Todo.md#15-web-components-from-java-classes-browser-interop-track). The track sits outside the AGENTS.md goal order and does not take priority over compiler correctness or library execution.

The aim is a Lit-like authoring experience in Java. A class annotated `@CustomElement` becomes a native browser custom element with reactive fields, declarative templates, scoped styles and typed events. Lit relies on TypeScript; this design relies on the Java type system and the javaw whole-program compiler instead.

Agreed decisions:

1. Templates are written as `html("""...${expr}...""")` text blocks. A source-to-source pre-pass rewrites them into plain, typed Java before compilation.
2. javaw has its own renderer. lit-html serves as the behavioural reference and as a test oracle; it is not a runtime dependency.
3. Reactivity comes from annotated fields (`@Property`, `@State`). The pre-pass rewrites writes to those fields into change notifications.

Semantics follow Lit wherever Lit has an equivalent. Each deliberate difference is listed in [Differences from Lit](#differences-from-lit).

## Contents

- [Authoring model](#authoring-model)
- [Template language](#template-language)
- [Reactive fields](#reactive-fields)
- [Update lifecycle](#update-lifecycle)
- [Renderer](#renderer)
- [Styles](#styles)
- [Events](#events)
- [Host contract and lifetime](#host-contract-and-lifetime)
- [Generated artifacts and TypeScript interop](#generated-artifacts-and-typescript-interop)
- [Implementation placement](#implementation-placement)
- [Validation strategy](#validation-strategy)
- [Differences from Lit](#differences-from-lit)
- [Non-goals and limitations](#non-goals-and-limitations)
- [Open questions](#open-questions)

## Authoring model

```java
package demo;

import one.dastec.javaw.components.*;
import one.dastec.javaw.dom.Element;
import java.util.ArrayList;
import java.util.List;

@CustomElement("todo-list")
public class TodoList extends WebComponent {

    static final Css STYLES = css("""
        :host { display: block; font: 14px system-ui; }
        li.done { text-decoration: line-through; opacity: .6; }
        """);

    public record Todo(int id, String text, boolean done) {}
    public record RemainingChanged(int remaining) implements ComponentEvent {}

    @Property(reflect = true) private String heading = "Todos";   // attribute "heading"
    @Property private List<Todo> items = List.of();               // JS property only (no attribute converter)
    @State private String draft = "";                             // internal; never an attribute
    @Query("input") private Element input;                        // read = renderRoot().querySelector("input")
    private int nextId = 1;

    @Override protected Template render() {
        return html("""
            <h2>${heading}</h2>
            <input .value=${draft} @input=${e -> draft = e.target().getValue()}>
            <button ?disabled=${draft.isBlank()} @click=${this::add}>Add</button>
            <ul>${repeat(items, Todo::id, this::row)}</ul>
            <p>${remaining()} left</p>
            """);
    }

    private Template row(Todo t) {
        return html("""
            <li class=${t.done() ? "done" : ""} @click=${() -> toggle(t)}>${t.text()}</li>
            """);
    }

    private void add() {
        List<Todo> next = new ArrayList<>(items);
        next.add(new Todo(nextId++, draft, false));
        items = next;            // a new list: reference change, so an update is scheduled
        draft = "";
    }

    private void toggle(Todo t) {
        List<Todo> next = new ArrayList<>();
        for (Todo x : items) next.add(x == t ? new Todo(x.id(), x.text(), !x.done()) : x);
        items = next;
    }

    private int remaining() {
        int n = 0;
        for (Todo t : items) if (!t.done()) n++;
        return n;
    }

    @Override protected void updated(Changes changes) {
        if (changes.has("items")) dispatch(new RemainingChanged(remaining()));
    }
}
```

### API surface

Package `one.dastec.javaw.components` (module `javaw-components`):

| Element | Role |
| --- | --- |
| `@CustomElement("tag-name")` | Registers the class with `customElements.define`. It is not called `@Component` because the Spring stand-in already uses that name. |
| `WebComponent` | Base class. Overridable hooks: `connected`, `disconnected`, `adopted`, `attributeChanged`, `shouldUpdate`, `willUpdate`, `render`, `firstUpdated`, `updated`. Instance methods: `requestUpdate()`, `updateComplete(Runnable)`, `dispatch(ComponentEvent)`, `host()`, `renderRoot()`, `listen(target, type, listener)`. |
| `html`, `css`, `repeat`, `when`, `classMap`, `styleMap`, `ref`, `nothing` | `protected static` methods of `WebComponent`. Subclasses can call them unqualified, including from static initializers. Other classes use `import static one.dastec.javaw.components.Templates.*`. |
| `@Property`, `@State` | Reactive fields; see [Reactive fields](#reactive-fields). |
| `@Query(selector)` | A `private` field of type `Element`. Every read is rewritten to `renderRoot().querySelector(selector)`; writes are a diagnostic. |
| `Template`, `Css`, `Changes`, `Ref<T>` | Values produced by the rewrite and by the lifecycle. |
| `ComponentEvent`, `@EventName`, `@EventOptions`, `@Fires` | Event records; see [Events](#events). |

New DOM surface in `javaw-core` package `one.dastec.javaw.dom` (M1 unless noted otherwise):

- `Event`, with `target()`, `currentTarget()`, `type()`, `preventDefault()`, `defaultPrevented()` and `stopPropagation()`;
- `MouseEvent`, `PointerEvent`, `KeyboardEvent`, `FocusEvent`, `InputEvent`, `SubmitEvent` and `CustomEvent`;
- `EventListener<E extends Event>`;
- `Element.querySelector` and `Element.querySelectorAll`, so lookups can be scoped to an element;
- `ShadowRoot`;
- the instance-bound overload `Element.addEventListener(String, EventListener<?>)` and its `removeEventListener` counterpart, covered by the [first prerequisite](#prerequisites).

`target()` and `currentTarget()` return `Element`, which is `null` for targets that are not elements.

### Class rules

Each rule below is enforced by a compile-time diagnostic.

- The class is a top-level or static nested class. It is not abstract, it extends `WebComponent`, and it has a no-argument constructor.
- The tag name is a valid custom element name. It starts with an ASCII lowercase letter, contains a hyphen, contains no ASCII uppercase letters, and otherwise contains only PCENChar characters. It is not one of the reserved names: `annotation-xml`, `color-profile`, `font-face`, `font-face-src`, `font-face-uri`, `font-face-format`, `font-face-name`, `missing-glyph`.
- No two classes in the module claim the same tag.
- `new X()` for a component class `X` anywhere in the module is rewritten to `WebComponent.create(X.class)`. That call runs `document.createElement(tag)`, and the resulting JS constructor allocates the peer and returns it. Only the runtime's construction path invokes the Java constructor itself.
- Constructors and field initializers must not touch the DOM. The constructor runs inside the browser's element constructor, and the spec requires the element to have no attributes or children when that constructor returns. A DOM call made while a component is being constructed throws `IllegalStateException`. The runtime attaches the shadow root (`open` mode) on the first connection, before the first update.

## Template language

### Syntax

`html(...)` takes exactly one argument, and it must be a string literal or a text block. Concatenations and variables are diagnostics.

- **Scanning.** Bindings are found by scanning the *raw source characters* between the literal's delimiters. JLS 3.10.6 processing (incidental-whitespace stripping, then escape translation) applies only to the static parts. The incidental indentation is computed over the whole raw block, the same way javac computes it.
- **Binding syntax.** `${` *expression* `}` is a binding. The expression is the verbatim raw text up to the matching `}`. The scanner counts braces and skips over string literals (honouring backslash escapes), character literals and `//` and `/* */` comments.
- **No escape syntax.** Every `${` starts a binding, and Java rejects `\$` as an invalid escape. To render a literal `${`, bind a string such as `${"${"}`. A lone `$` is ordinary text, so `$${price}` renders a dollar sign followed by the price.
- **Nesting.** A text block cannot contain another text block, because a `"""` inside the block ends it. Nested templates are therefore methods or locals that return `Template`, as `row` does above. A binding expression may contain an inner one-line `html("...")` call. The pre-pass rewrites it recursively, and its raw text is the inner literal's source text.

### Binding kinds

The position of a binding in the HTML determines its kind. The pre-pass has syntax only: it uses JavaParser without a symbol solver, as `javaw-core` does. So it does not check expression types itself. Instead, it wraps every binding in a typed `Parts` call for its kind, and the downstream compiler (javac or javaw-core) enforces the table below. The resulting errors are mapped back to the binding's line and column through the [offset map](#rewrite).

| Position | Kind | Emitted call | Accepted static types | Commit |
| --- | --- | --- | --- | --- |
| Child content: `<p>${x}</p>` | child | `Parts.child(x)` | Overloads for `String`, every primitive, `Object` boxes of primitives, `Enum<?>`, `Template`, `Iterable<?>`, directive results | `String.valueOf` text in a text node (never parsed as HTML); a `Template` is rendered in place; `null` renders nothing. Elements of an `Iterable` are checked at runtime, and an unsupported element type throws `IllegalArgumentException`. |
| Attribute value: `class=${x}` or `class="a ${x} b"` | attribute | `Parts.attr(x)` per binding | `String`, primitives, boxes, `Enum<?>`, `nothing()` | `setAttribute` with the parts concatenated; `null` or `nothing()` in any part removes the attribute |
| `?name=${x}` | boolean attribute | `Parts.bool(x)` | `boolean` (a `Boolean` is unboxed; `null` throws `NullPointerException`) | adds or removes the attribute |
| `.name=${x}` on a known Java component | property | `Parts.prop(Target.$NAME, x)` | `T` of the generated `PropertyKey<Target, T>` | sets the Java field directly through the generated setter, with no marshalling; an unknown name is a compile error |
| `.name=${x}` on any other element | property | `Parts.prop("name", x)` | the types accepted by [property marshalling](#property-marshalling) | sets the JS property |
| `@name=${x}` | event | `Parts.onRun(x)` for a zero-parameter lambda; `Parts.on(E.class, x)` for a one-parameter lambda; overloaded `Parts.on(x)` otherwise | `Runnable` or `EventListener<? super E>` | calls the part's current Java listener; see [Renderer](#renderer) |
| Element position: `<input ${ref(r)}>` | element | `Parts.element(x)` | `ref(...)` results | assigns the element to the `Ref` |

A literal `null` written as a binding is emitted syntactically as `Parts.nothing()`, so overload resolution never sees an ambiguous `null`.

The event type `E` is determined by the event name, and the pre-pass picks it syntactically:

| Event names | `E` |
| --- | --- |
| `click`, `dblclick`, `auxclick`, `contextmenu`, `mousedown`, `mouseup`, `mousemove`, `mouseover`, `mouseout`, `mouseenter`, `mouseleave` | `MouseEvent` |
| `pointer*` | `PointerEvent` |
| `keydown`, `keyup` | `KeyboardEvent` |
| `focus`, `blur`, `focusin`, `focusout` | `FocusEvent` |
| `beforeinput` | `InputEvent` |
| `submit` | `SubmitEvent` |
| an event declared by a known Java component (see [Events](#events)) | the event record type |
| anything else, including `input` and `change`, which do not fire `InputEvent` on every control | `Event` |

`click` is listed as `MouseEvent`, the type that every engine's `click` event is an instance of. `PointerEvent` extends `MouseEvent`, so code that needs pointer fields can test for and cast to it.

A method reference or any expression that is not a lambda (such as `this::add`) uses the overloaded `Parts.on(Runnable)` / `Parts.on(EventListener<? super E>)`, and overload resolution selects by arity. This relies on arity-based overload selection for method references in javaw-core, which is part of the first prerequisite.

These positions are diagnosed by the build-time tokenizer:

- bindings in tag names, attribute names or closing tags;
- bindings inside comments, `<script>`, `<style>`, `<textarea>`, `<title>` or `<template>` content;
- unquoted attribute values that mix static text with a binding;
- any position the tokenizer cannot classify unambiguously.

### Evaluation semantics

Expressions are evaluated eagerly, left to right in source order, each time `render()` runs, using Java semantics. Exceptions propagate out of `render()`. Lambdas capture variables under the ordinary Java rules: captured locals must be effectively final, and `this` is lexical. An expression may refer to anything in scope at the `html(...)` call.

### Rewrite

The pre-pass replaces each call with plain Java. For `row`, the output is:

```java
private Template row(Todo t) {
    return Template.of(TodoList$$Templates.T1,
        Parts.attr(t.done() ? "done" : ""),
        Parts.onRun(() -> toggle(t)),
        Parts.child(t.text()));
}
```

- **Arguments.** `Template.of(TemplateDescriptor, Part...)` receives only `Part` values, never arrays. That avoids Java's varargs pitfalls: a lone `null` argument, or an array argument being used as the varargs array itself.
- **Descriptor.** `TodoList$$Templates.T1` is a static descriptor generated per call site. It holds a stable template id, the template HTML with markers, and the kind, name and static strings of each part.
- **Offset map.** Rewritten expressions stay on their original source lines. Columns shift because of the inserted `Parts.*(...)` wrappers, so the pre-pass also writes an offset map from each rewritten expression node to its original line and column. Diagnostics and source-map entries for the files it generates are translated through that map.
- **Output.** The generated sources go under `target/generated-sources/javaw-components/`. They are standard Java, so javac also compiles them. That allows JVM-side rewrite tests, and later the bytecode path.

## Reactive fields

`@Property(attribute = "...", reflect = false)` declares a public reactive property, visible to JS and TypeScript. `@State` declares internal reactive state, with no attribute and no JS property.

### Field rules

Each rule is a compile-time diagnostic.

- The field is an instance field. It is **`private`**, not `static`, and not `final`. Because the field is private, every write is inside the top-level class's nest (JLS 6.6.1), and the pre-pass can find all of them syntactically. JS reads and writes go through generated accessors. Other Java code uses ordinary methods the author writes, or the generated `PropertyKey` setters that `.prop` bindings use.
- A `@Property` field gets an attribute only if its type has an [attribute converter](#attribute-conversion). Any other type is a JS property only, and writing `attribute = "..."` on it is an error.

### Write rewriting

Inside the nest, these writes to a reactive field `f` of class `C` are rewritten:

- **Simple names.** Unqualified `f` that is not shadowed by a local, parameter or lambda parameter. Shadowing is resolved lexically.
- **This forms.** `this.f` and `C.this.f`.
- **Typed receivers.** `r.f`, where `r` is a simple name whose declared type is `C` or a subclass: a local, a parameter, a field, `var r = new C()`, or a cast expression `((C) e).f`.

Any other qualified write to a field named `f` inside the nest is a diagnostic. This includes complex receivers such as `next().f` or `a[i()].f`. Fix it with a local variable or a cast. Because every rewritten receiver is a simple name, `this`, or a cast of a side-effect-free expression, a receiver is never evaluated twice.

The pre-pass generates static helpers in `C`. Each returns the value that Java defines for the original expression:

| Source | Rewritten | Result value |
| --- | --- | --- |
| `r.f = E` | `C.$set$f(r, E)` | the new value |
| `r.f op= E` | `C.$set$f(r, (T) (r.f op E))` | the new value |
| `++r.f`, `--r.f` | `C.$set$f(r, (T) (r.f + 1))` and `- 1` | the new value |
| `r.f++`, `r.f--` | `C.$post$f(r, (T) (r.f + 1))` and `- 1` | the old value |

`T` is the field's type. The cast restores the implicit narrowing of compound assignment for `byte`, `short`, `char` and `int` combined with `double`. `String +=` is a concatenation. Java evaluates `r.f` before `E`, which preserves JLS 15.26.2's rule that the old value is saved before the right-hand side is evaluated. The rewrite tests compare each form between javac on the JVM and javaw.

The pre-pass does **not** rewrite writes in field initializers, instance initializer blocks or constructors. After construction, the runtime adds every reactive field to the first update's `Changes`, using the type's default value as the old value. It does this whatever the initial value is, so `@State int count = 0` still appears in the first update.

Writes made through reflection (`Field.set`) are not rewritten. They change the field without scheduling an update.

### Change detection

A setter records a change, and calls `requestUpdate(name, old)`, when the new value differs from the current value under these rules:

| Type | Changed when |
| --- | --- |
| `float`, `double` and their boxes | `Float.compare` / `Double.compare` != 0. This matches Lit's `!Object.is`: NaN to NaN is not a change, but 0.0 to -0.0 is. |
| other primitives | `!=` |
| boxed primitives (other than `Float`/`Double`) and `String` | `!Objects.equals` |
| enums and all other references | reference identity |

Mutating a `List` or other object in place is not detected. Assign a new value or call `requestUpdate()`.

### Attribute conversion

The default attribute name is the field name in kebab-case (`maxItems` → `max-items`); `attribute = "..."` overrides it. This is a [difference from Lit](#differences-from-lit).

| Java type | attribute → field | field → attribute (`reflect = true`) |
| --- | --- | --- |
| `String` | value; `null` when removed | value; `null` removes |
| `boolean` | `true` when present; `false` when removed | `true` sets `""`; `false` removes |
| `int`, `short`, `byte` | `Integer.parseInt` and equivalents; `0` when removed | `String.valueOf` |
| `double`, `float` | `Double.parseDouble` / `Float.parseFloat`; `0.0` when removed | `String.valueOf` |
| boxes of the above | the same parse; `null` when removed | `String.valueOf`; `null` removes |
| `enum E` | `E.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'))` | `name().toLowerCase(Locale.ROOT).replace('_', '-')` |

`long` has no attribute converter, because its values do not fit a JS number exactly.

If conversion fails (`NumberFormatException`, `IllegalArgumentException`), the field is left unchanged and the exception is reported through the [error path](#errors). Setters never touch the DOM. Reflection to attributes happens during the update (see [Update lifecycle](#update-lifecycle)). When the runtime reflects a property to its attribute, the resulting `attributeChangedCallback` does not convert the value back into the field.

### Property marshalling

These rules cover values that cross into JS:

- `.prop` bindings on elements that are not Java components;
- JS reads of the generated `@Property` accessors;
- event `detail` objects.

| Java | JS | JS → Java |
| --- | --- | --- |
| `boolean`, `int`, `short`, `byte`, `float`, `double`, and their boxes | `boolean` / `number` | the JS value must have the matching `typeof`; numbers are narrowed with Java casting rules |
| `long`, `Long` | `bigint` | requires a `bigint` |
| `char`, `Character` | a one-character string | requires a string of length 1 |
| `String` | `string` | requires a string |
| `enum E` | the kebab-case name | requires a known name |
| record `R` | a plain object built from its components, recursively | an object with those keys |
| `List<T>`, `T[]` | an array | an array |
| `Map<String, V>` | a plain object | a plain object |
| `JSValue`, `Element` | the JS object | the JS object |
| `null` (any reference) | `null` | `null` or `undefined` |

A JS write whose value does not match throws a JS `TypeError`. Its message is the Java `ClassCastException` message.

A JS property that was set on an element before it was upgraded becomes an own data property that would shadow the generated accessor. During construction, the runtime checks `Object.hasOwn(this, name)` for each `@Property`. If the property is present, the runtime saves its value, deletes the own property, and assigns the saved value through the accessor after the Java constructor runs and before the first update, as Lit does.

## Update lifecycle

1. **Requesting.** `requestUpdate` records a change and adds the component to the pending queue if it is not already queued. For each field, `Changes` keeps the first old value in the cycle. A field that ends the batch at its original value still counts as changed.
2. **Scheduling.** The runtime schedules one `queueMicrotask` call to `__wc_flush` (an exported Wasm entry) while the queue is non-empty. Microtasks run only when the JS execution stack is empty, so `__wc_flush` never runs nested inside Wasm. `__wc_flush` processes components in request order. It loops until the queue is empty, so components queued during the flush (for example, children whose properties a parent's render set) are updated in the same call. If one component updates more than 100 times in a single flush, the runtime reports an error and drops that component's further work for the flush.
3. **Per component:**
   1. `shouldUpdate(changes)`; if it returns `false`, the changes are discarded;
   2. `willUpdate(changes)`; writes made here are folded into the current cycle;
   3. `render()`;
   4. reflection of the changed reflected properties to their attributes;
   5. DOM commit;
   6. `firstUpdated(changes)`, once per instance;
   7. `updated(changes)`; writes made here start a new cycle;
   8. `updateComplete` callbacks.

   As in Lit, writes made during `render()` change the field but neither schedule nor extend an update. Debug builds warn about them.
4. **Connection.** Updates start only after the element is connected. An element that is disconnected and then reconnected keeps its peer, its state and its rendered DOM.

### Collector safepoint

When `__wc_flush` returns, no Wasm frame is active. That is half of the [safepoint condition](memory-management.md#root-set). The other half is that host code retains every Wasm pointer it keeps across a task or microtask boundary. Exported-class wrappers now do this: they retain in the constructor and release through finalization ([DOM handle scopes](dom-handle-scopes.md#exported-class-wrappers)). The runtime option to call `__gc_collect` after `__wc_flush` still stays **off by default** until M2 audits the remaining host code that keeps pointers.

### Errors

Any exception that escapes a lifecycle hook, `render`, an attribute conversion or an event listener is reported once through the runtime's error sink. That sink is `globalThis.reportError` when it exists; otherwise it is `queueMicrotask(() => { throw err; })`. Tests can install a capturing sink. The reported `Error` carries, in its message and `stack`, the Java exception class, message and stack trace. Other components and later updates keep working, because the component's pending flag is cleared. A failed render leaves the previously committed DOM in place.

## Renderer

### Preparing a template

The template compiler emits, for each descriptor:

- the template HTML, with comment markers for child parts and marker attributes for attribute, event, property and element parts;
- the kind, name and static strings of each part.

These are stored in a custom section, `javaw_wc_templates`, keyed by template id.

At runtime, the first use of a template id assigns the HTML to one `<template>` element, walks its `content` once to find the markers, and caches the child-index path of each part. Finding parts at runtime, rather than at build time, keeps paths correct when the HTML parser changes the tree: implied `<tbody>`, automatically closed `<p>` and `<li>`, foster parenting. Each template id is parsed once and walked once per page, not once per instance.

### Rendering

- **First render of an instance.** The runtime clones the cached content and resolves each part's node from its cached path.
- **Instance state lives in Wasm.** A `TemplateInstance` is a Java object that holds each part's previous value. On every render, values are compared in Wasm, and only changed parts cross to JS. The host imports for committing are `commitText`, `commitAttribute`, `commitBoolean`, `commitProperty`, `insertChild`, `moveChild` and `removeChildRange`. An update in which nothing changed makes no host calls.
- **Event parts.**
  - On first render, the runtime adds one stable JS listener per event part. It captures the component's scope object (see [Peers and lifetime](#peers-and-lifetime)) and the part index.
  - When it fires, it checks `scope.released` and does nothing if the scope has been released; otherwise it calls the part's current Java listener slot.
  - A render that produces a new lambda only updates the slot in Wasm, with no `addEventListener` call. A `null` listener removes the JS listener.
  - `removeChildRange` removes the JS listeners of every event part in the removed DOM. A listener left on a node that JS still holds after the renderer removed it does nothing.
- **Child parts.** If the template id is unchanged, the content is updated in place; if it differs, the content is replaced. A plain `Iterable` reuses entries by index.
- **Keyed lists.** `repeat(items, keyFn, templateFn)` uses lit-html's `repeat` reconciliation: a two-ended scan matching head to head, tail to tail, old head to new tail and old tail to new head, then lazily built key maps for the remainder. It moves existing DOM, so node identity is preserved. Keys are compared with `equals`/`hashCode`. A duplicate key is reported through the error path in debug builds, and the later item gets a fresh instance.
- **No HTML injection.** Text goes into text nodes and attributes go through `setAttribute`. The first version has no `unsafeHTML`.

## Styles

A style is a `static final Css` field, of any access, declared in the component class or one of its superclasses. Its initializer must be either a direct `css(literal)` call or a simple reference to another such field. Any other initializer is a diagnostic. `css(...)` accepts one string literal or text block with no bindings.

Each `css(...)` call site becomes one shared `CSSStyleSheet`, created once. A shadow root's `adoptedStyleSheets` lists the sheets superclass first, then in declaration order, with duplicates removed by call site. Where constructable stylesheets are unavailable, each sheet falls back to a `<style>` element in each shadow root.

## Events

A record that implements `ComponentEvent` is dispatched as a `CustomEvent`:

- **Name.** The record's simple name in kebab-case, with any `Event` suffix removed: `RemainingChanged` becomes `remaining-changed`. `@EventName("...")` overrides it.
- **Options.** `bubbles` and `composed` default to `true`, and `cancelable` defaults to `false`. `@EventOptions(bubbles, composed, cancelable)` overrides them. `dispatch` returns `!event.defaultPrevented`, so it can only return `false` for a cancelable event.
- **Detail.** `detail` is the record, marshalled as described in [Property marshalling](#property-marshalling). Java listeners bound in a template with `@remaining-changed=${...}` receive the original record instance. The `CustomEvent` carries a hidden reference to it.
- **Declared events.** A component's events are the record types in its `dispatch(new X(...))` calls, plus any listed in `@Fires(X.class)`. They are recorded in the manifest and give event bindings in other Java components a static type.
- **Listeners on long-lived targets.** For listeners on `window`, `document` or other objects that outlive the component, use `listen(target, type, listener)`. It adds the listener on `connected` and removes it on `disconnected`. See [callback ownership](#callback-ownership).

## Host contract and lifetime

### Registration

One module instance defines every component in the module. Definition happens after instantiation and class initialization, in declaration order, with one `customElements.define` per tag. At that point the browser upgrades matching elements already in the document. Splitting components across Wasm modules is not supported: Java objects cannot cross modules, and each module would carry its own runtime.

The generated JS class for each tag:

- extends `HTMLElement`;
- declares `static observedAttributes`, read once at define time, from the `@Property` fields that have attributes, plus any extra names in `@CustomElement(observedAttributes = ...)`;
- forwards the four lifecycle reactions to Wasm exports, passing the peer.

On upgrade and parse, `attributeChangedCallback` runs for each initial observed attribute before `connectedCallback`. These reactions may run while Wasm frames are active, for example when a Java `setAttribute` call reaches a component synchronously. The collector is never invoked re-entrantly, as the memory-management contract already requires.

### Prerequisites

1. **Instance-bound JS callbacks.** **Done:** see [DOM event listeners](dom-event-listeners.md). Lambdas and bound method references can be passed to `Element.addEventListener` and removed again, and they keep their receivers. Overloads that differ only in callback arity, which `Parts.on` relies on, are selected by callback shape.
2. **Scoped handles and finalization-based release.** **Done:** see [DOM handle scopes](dom-handle-scopes.md). It covers the handle scheme below, peer release, liveness checks in listeners, and retaining in exported-class wrappers. M1 creates the scope from each custom element's constructor.

### Peers and lifetime

Each element has a Java peer. The JS element holds the peer pointer under a counted `runtime.retain`. A `FinalizationRegistry` on the element *may* release the peer after the element is garbage collected. Release is best-effort, because finalization callbacks are not guaranteed to run. A peer that is never released is a leak, not a correctness problem. Disconnecting an element does not release its peer.

The existing handle table (`handleMap`/`reverseMap` in `javaw-runtime.js`) holds elements strongly and never releases them. A component node handed out through that table pins its whole component, because a shadow-tree node reaches its host through `getRootNode().host`. Components therefore use **scoped handles**:

- **Encoding.** A scoped handle is a negative `i32` from a monotonic counter, so it never collides with a global handle. `getElement` resolves it, so existing DOM imports accept both kinds. Inline `@JS` parameters never apply their string-pointer fallback to negative handles.
- **Storage.** The runtime keeps `scopedHandles: Map<handle, WeakRef<scope>>`. The scope object lives on the JS element, so the element, its scope and its subtree form an ordinary JS object graph with no strong global reference. The finalization callback deletes the scope's entries.
- **Which values are scoped.** Any `Element` value returned to Wasm whose node is a component host, or whose `getRootNode()` is a component's shadow root, is a scoped handle in that component's scope. This covers `host()`, `renderRoot()`, `@Query`, `ref`, `Event.target()`/`currentTarget()`, and traversal inside the subtree. Nodes in the light DOM keep using the global table, with its current lifetime.
- **Release.** On release, the runtime sets `scope.released = true` before calling `runtime.release(peer)`. After that, resolving a handle in the scope throws `IllegalStateException("element released")`.
- **Java references.** A Java reference to a component object does not keep its element alive. Once the element has been finalized, the component is *released*: its lifecycle and DOM calls throw `IllegalStateException("element released")`, while its fields can still be read. Storing scoped handles outside their component is unsupported.
- **Closures.** Closures in template parts are reachable from the peer's `TemplateInstance`. When a part's value is replaced or its DOM is removed, the slot is cleared.

### Callback ownership

An instance-bound callback that is handed to the host holds one `retain` on its closure. The retain is released by the matching remove call, by `listen`'s automatic removal on disconnect, or by a `FinalizationRegistry` on the JS wrapper function, whichever comes first.

A callback added directly to a long-lived JS object (`window`, `document`, a timer, a third-party element) keeps the component alive until it is removed: the global object holds the function, which retains the closure, which holds the peer. Use `listen`, or remove such callbacks in `disconnected()`. The leak tests cover both cases.

### Threads

Custom elements exist only on the main thread. In a module compiled for shared-memory threads, component registration and every component entry point run on the main thread. Calling a component method from a worker throws `IllegalStateException`. A compile-time diagnostic will follow once the compiler can tell that a component is reachable from a worker entry point.

## Generated artifacts and TypeScript interop

The build writes these under `target/`:

- `<module>.components.js`: an ES module that imports the javaw runtime, instantiates the module (or attaches to an existing instance) and defines every tag.
- `custom-elements.json`: the [Custom Elements Manifest](https://github.com/webcomponents/custom-elements-manifest) schema. It records tags, attributes, properties, events, slots found in templates and CSS parts.
- `<module>.components.d.ts`: a class declaration per component, augmentation of `HTMLElementTagNameMap` and `HTMLElementEventMap`, and typed event `detail`.

TypeScript types are derived from [property marshalling](#property-marshalling):

| Java | TypeScript |
| --- | --- |
| `boolean` / `Boolean` | `boolean` / `boolean \| null` |
| `int`, `short`, `byte`, `float`, `double` / their boxes | `number` / `number \| null` |
| `long` / `Long` | `bigint` / `bigint \| null` |
| `char` / `Character` | `string` / `string \| null` |
| `String` | `string \| null` |
| `enum E` | a union of the kebab-case names as string literals, `\| null` |
| record `R` | `interface R`, generated from its components, `\| null` |
| `List<T>`, `T[]` | `T[] \| null` |
| `Map<String, V>` | `Record<string, V> \| null` |
| `JSValue`, `Element` | `unknown`, `Element \| null` |

Typed Java facades generated from a third-party `custom-elements.json` are a later milestone. They would give third-party tags used in Java templates compile-time checks of their attributes, properties and events.

The template parameter is annotated `@Language("HTML")`, which gives IntelliJ HTML highlighting inside text blocks. Completion and refactoring inside `${...}` would need a later IDE plugin. Until then, renaming a field fails the build rather than breaking silently.

## Implementation placement

- **New module `javaw-components`:**
  - the public API;
  - the JavaParser pre-pass, which performs the template rewrite, the field-write rewrite, `@Query` reads and `new X()` rewriting, emits diagnostics and writes the offset map;
  - the build-time HTML tokenizer and template compiler;
  - the manifest and `.d.ts` generators;
  - the component JS runtime, a separate script copied next to `javaw-runtime.js`.
- **`javaw-core` changes:**
  - the [prerequisites](#prerequisites);
  - the [new DOM surface](#api-surface);
  - offset-map translation for diagnostics and source maps;
  - a hook that emits `javaw_wc_templates` and the component exports.
- **Build integration** goes through the existing compile pipeline and `javaw-maven-plugin`.
- **Bytecode path.** This contract targets the source compiler first. Running the rewritten Java through javac and `javaw-bytecode` is a separate claim with its own tests. No `CacheMetadata` revision bump is needed until bytecode admission or runtime semantics change.

## Validation strategy

The JVM has no DOM to compare against, so evidence comes from these layers:

1. **Rewrite tests (JVM).**
   - golden rewritten Java under `target/`;
   - diagnostics at exact positions for every rejected template position, field rule and type rule, with the positions mapped through the offset map;
   - a javac compile of the rewritten output;
   - JVM-versus-Wasm execution comparisons for every write-rewrite form (assignment, compound assignment with narrowing, prefix and postfix `++`/`--`, assignment used as an expression).
2. **Executable tests (Node + happy-dom).** Each test compiles with javaw, runs `wat2wasm`, and loads the module into happy-dom. They cover:
   - upgrading pre-existing elements, including properties set before upgrade;
   - attribute and property round trips;
   - batched updates, lifecycle order and the `Changes` contents;
   - events crossing shadow boundaries;
   - keyed list moves, with DOM node identity preserved;
   - error reporting through a capturing sink;
   - zero host calls for an update in which nothing changed.
3. **Differential tests against lit-html.** The same static strings and sequences of values are rendered by lit-html and by javaw, and the serialized DOM is compared after each step, for every binding kind and for list reconciliation. Java `null` in an attribute part is passed to lit-html as `nothing`. Duplicate-key cases are excluded.
4. **Leak tests.**
   - Create and remove 1,000 components that use event bindings, `e.target()`, `@Query`, `ref` and `listen` on `window`.
   - Then repeat this until the runtime's finalized-peer counter equals the number of removed elements, at most N times: call `gc()` (run Node with `--expose-gc`) and wait at least two macrotask turns.
   - Assert that count explicitly. Then run `__gc_collect` and check that live bytes, heap high-water bytes and handle counts stay level after warm-up.
5. **Chromium smoke test (opt-in).** Checks `adoptedStyleSheets`, `composed` events and real upgrade timing. It runs only when a system property names a local Chrome binary, and is skipped in the default offline suite.
6. **Size budget.** The `.wasm` and JS sizes of `javaw-counter` and `todo-list` are recorded at each milestone.

## Differences from Lit

| Area | Lit | javaw |
| --- | --- | --- |
| Default attribute name | property name lowercased (`maxitems`) | kebab-case (`max-items`) |
| `null` in an attribute part | `""` (only `nothing` removes) | removes the attribute |
| Binding value types | anything; non-strings are stringified | statically typed per kind, enforced by the compiler |
| Attribute converters | declared via `type:` | derived from the Java field type |
| Reactive field visibility | any | `private` only |
| Initial `Changes` | only fields that were set | every reactive field |
| Update scheduling | one microtask per element | a single `__wc_flush` loop per microtask, with a re-render bound |
| Custom event defaults | the author sets them | `bubbles` and `composed` default to `true` |

## Non-goals and limitations

The first version does not include:

- a virtual DOM;
- server-side rendering or a JVM renderer;
- `unsafeHTML`, or bindings inside `css(...)`;
- form-associated elements (`ElementInternals`);
- slot helpers (`@QueryAssignedElements`);
- reactive controllers;
- the context protocol.

Limitations:

- **Not detected:** in-place mutation of collections and objects. Assign a new value or call `requestUpdate()`.
- **Not rewritten:** writes through reflection.
- **Diagnosed:** qualified writes to reactive fields through complex receivers.
- **Best-effort peer release:** a component whose element is never finalized stays alive.
- **Main thread only.**
- **One module instance per page** for all components.

## Open questions

1. **Decided:** test DOM libraries are vendored as pinned, test-only copies with NOTICE entries. happy-dom 21.0.4 is in [`test-vendor/node`](../test-vendor/node/README.md); lit-html will be added the same way for M3. Chromium stays opt-in through a local binary.
2. Should a `@Property` field with no attribute converter need an explicit `attribute = Property.NONE`, or is the implicit JS-property-only behaviour acceptable?
3. Should `updateComplete` also be exposed to JS as a promise on the element, as Lit does, for TypeScript test code?
4. Should `null` in an attribute part follow Lit (`""`) instead of removing the attribute?
