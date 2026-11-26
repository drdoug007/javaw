# Web components from Java classes (design contract)

Status: **partly implemented.** Both prerequisites and M1 to M5 are done, and M6 (manifest, `.d.ts`, ES module, `@Language`, the opt-in Chromium smoke test and the size budget) is implemented; see [Implementation status](#implementation-status). The rest of this document records the agreed authoring model and the runtime contracts an implementation must satisfy. Milestones and their evidence are tracked in [Todo.md section 15](../Todo.md#15-web-components-from-java-classes-browser-interop-track). The track sits outside the AGENTS.md goal order and does not take priority over compiler correctness or library execution.

The aim is a Lit-like authoring experience in Java. A class annotated `@CustomElement` becomes a native browser custom element with reactive fields, declarative templates, scoped styles and typed events. Lit relies on TypeScript; this design relies on the Java type system and the javaw whole-program compiler instead.

Agreed decisions:

1. Templates are written as `html("""...${expr}...""")` text blocks. A source-to-source pre-pass rewrites them into plain, typed Java before compilation.
2. javaw has its own renderer. lit-html serves as the behavioural reference and as a test oracle; it is not a runtime dependency.
3. Reactivity comes from annotated fields (`@Property`, `@State`). The pre-pass rewrites writes to those fields into change notifications.

Semantics follow Lit wherever Lit has an equivalent. Each deliberate difference is listed in [Differences from Lit](#differences-from-lit).

## Implementation status

Implemented (M1 subset; evidence in `javaw-components` `CustomElementTest` and `ComponentDiagnosticsTest`):
- **Module and hook.** A `javaw-components` module holds the API and a compiler extension. The extension plugs into `javaw-core` through the `one.dastec.javaw.spi.SourceExtension` `ServiceLoader` hook. A program that imports `one.dastec.javaw.components` without the extension on the compiler classpath gets a compile error.
- **Annotations and diagnostics.**
  - `@CustomElement(value, observedAttributes)` is checked at compile time: the tag name rules, duplicate tags, a `WebComponent` superclass, a concrete class, a no-argument constructor, lowercase observed attribute names, and the reserved type names.
  - Writes to `@Query` fields and the field rules are also diagnosed.
- **`WebComponent` hooks.** It provides `connected`, `disconnected`, `adopted`, `attributeChanged`, `host()`, `renderRoot()` (a `ShadowRoot`) and `createElement(tag)`.
- **`@Query` fields.** Reads are lowered to `renderRoot().querySelector(selector)`.
  - Java scoping decides which names are reads of the field. Locals declared earlier in an enclosing block, for, for-each and try-resource variables, catch parameters and lambda parameters all shadow it.
  - A pattern variable with the same name, `other.field`, and reads from nested classes are compile errors.
- **Registration.** The runtime defines each tag from the `javaw_wc_components` custom section. Each element's constructor creates its Java object and a handle scope that retains it. The open shadow root is attached on first connection and kept across reconnection. Elements that are parsed, created, or already in the document (upgraded) all work.
- **`new X()`** creates the element through the registry and returns its Java object.
  - A Java exception thrown by the component's constructor reaches the caller's `catch`. Browsers report a custom element constructor exception inside `createElement` instead of rethrowing it, so the runtime rethrows the original exception itself.
  - Constructor references (`X::new`) and `new Sub()` for a `WebComponent` subclass without `@CustomElement` are compile errors.
  - A component created with `new X()` stays alive until it is first connected, even if only Java references it. A created component that is never attached is therefore never reclaimed. After its first connection it follows normal element lifetime. Once a component is detached and unreachable from JS, Java-held references see `IllegalStateException("element released")`.
  - `new X();` as a statement is allowed.
- **Errors in lifecycle hooks** go to `reportError` without affecting other components.
- **Reclaiming removed components.** After 300 create, click and remove cycles, every scope is finalized. Live bytes and the global handle table stay level.
  - Nodes from `createElement` that are attached outside the shadow tree (a tooltip appended to the body) keep working while the component lives. They do not pin it once it is removed: such nodes and their listeners refer to the scope only weakly.
- **`define` failures are isolated.** If one definition fails, for example because another script already took the tag or an upgraded element's constructor throws, the failure is reported and the remaining tags are still defined.
- **Fully qualified API names** (`@one.dastec.javaw.components.CustomElement`, `extends one.dastec.javaw.components.WebComponent`) work without imports.
- **The extension stays out of unrelated code.** It activates only for programs that import or name the components package. A program with its own `CustomElement` annotation type or `WebComponent` class is left alone.
- **Fixes in `javaw-core`.** M1 needed these changes:
  - `Element.querySelector` and `querySelectorAll` on elements and shadow roots;
  - JLS 15.18.1 `null` String concatenation, including `+ null` and `s += null` (`NullStringConcatenationTest`);
  - DOM string literals with escape sequences, which compiled to `""` before;
  - lambdas that call inherited methods without qualification (`() -> host()`) now keep their lexical receiver (`LambdaSupportTest`).

Differences from the plan below, decided during M1:
- **Constructor rule.** It is enforced by `host()` and `renderRoot()` being `null` during the Java constructor, not by a runtime DOM-access check. The constructor therefore cannot reach its own element.
- **`createElement`.** Use `WebComponent.createElement(tag)` instead of `DOM.createElement`. It gives the node a handle in the component's scope even while it is detached. `DOM.createElement` would enter the node in the global table and pin the whole component.
- **Registration and runtime placement.** Elements are registered by `javaw-runtime.js` from a custom section. The component runtime lives in `javaw-runtime.js` rather than a separate script. The generated `<module>.components.js` ES module, manifest and `.d.ts` remain M6.
- **Nested component classes** are rejected for now. Only top-level classes are supported.

Implemented (M2 subset; evidence in `ReactiveFieldsTest`):
- **Reactive fields.** `@Property` and `@State` go on private, non-static, non-final fields. Writes as `f` or `this.f` outside constructors and initializers (assignment, compound assignment, prefix and postfix `++`/`--`, nested assignments, and writes inside lambdas, including lambdas created in a constructor or initializer) become generated setters. The setters record the first old value per cycle and schedule an update. Change detection follows the table in [Change detection](#change-detection).
- **Update cycle.** It runs once per microtask for each component that requested an update, through `shouldUpdate`, `willUpdate`, reflection, `update(Changes)`, `firstUpdated` (first time only) and `updated`. Writes in `willUpdate` join the cycle; writes in `update` neither schedule nor extend it, as in Lit; writes in `updated` start a new cycle. A declined or failed update discards its changes. Updates start once the element is connected, and the first update lists every reactive field. A component that requests more than 100 updates in one flush is reported and its pending cycle is dropped; a later write starts a new one.
- **Attributes.** `@Property` fields of type `String`, `boolean`, `int` and `double` get an observed attribute: the kebab-case field name, or `attribute = "..."`. Attribute values are converted as in [Attribute conversion](#attribute-conversion), and `reflect = true` writes them back during the update. Two properties may not share an attribute.
- **JS properties.** Element properties get and set those fields with type checks; a mismatched value throws `TypeError`. `int` properties narrow numbers as Java's `(int)` cast does (truncate, saturate, `NaN` to 0). Properties set before upgrade are replayed on the first connect, after the initial attributes, so the property value wins as in Lit.
- **`requestUpdate()`** handles changes the fields cannot see.

Differences from the plan, decided for this subset:
- **`update(Changes)` stands in for `render()` until M3** as the imperative rendering hook.
- **No writes through other references.** Only `f` and `this.f` writes are rewritten. Without type resolution, `other.f` is a compile error only when `other` is declared with the component's own type; same-named fields of other classes, including nested classes, are left alone.
- **Heap during a flush.** Memory is collected only at explicit safepoints, so an update loop that allocates on every cycle can exhaust the fixed heap before the 100-update limit (the post-flush collector call is open).
- **Limited `@Property` types.** Other types must use `@State` until property marshalling for records, lists and enums arrives.
- **`updateComplete` is not implemented yet.**

Implemented (M3 subset; evidence in `TemplateTest`, `TemplateDiagnosticsTest`, `TemplateRewriteTest` and `TemplateDifferentialTest`):
- **`render()` and `html(...)`.** The default `update(Changes)` renders `render()` into the shadow root; a component that overrides `update` without calling `super` keeps imperative rendering. `html(...)` takes one string literal or text block, in a class that extends `WebComponent` directly or through other classes of the module, in classes nested in one, or written `WebComponent.html(...)`. Other classes, such as shared row or layout helpers, use `import static one.dastec.javaw.components.Templates.html` (or `Templates.*`) or write `Templates.html(...)`. A class that declares its own `html` method keeps it, as Java resolves the call to that method.
- **Scanning and rewrite.** Bindings are found in the raw literal text, so a binding is its verbatim source. Static parts get javac's processing (incidental indentation stripped over the whole raw block, then escapes). Each call becomes `Template.of(id, Parts.kind(expr), ...)`, and binding expressions keep their source positions, so the compiler's own errors (for example `?hidden=${someString}`) point at the binding. One-line `html("...")` calls nested in bindings are rewritten recursively. The rewritten source compiles with javac against the public API stubs.
- **Positions and diagnostics.** Bindings in element position (until M4, which admits `ref`), tag names, attribute names, closing tags, comments, raw-text content (`<script>`, `<style>`, `<textarea>`, `<title>`, `<xmp>`, `<iframe>`, `<noembed>`, `<noframes>`, `<noscript>`, `<plaintext>`, also when written self-closing) and `<template>` content are rejected at the binding's `${`. The tokenizer uses HTML whitespace and ends comments at `-->` or `--!>`, as the browser does. So are unquoted values that mix text with a binding, `?`/`.`/`@` attributes with more than one binding or static text, `null` in a `?` binding, unterminated or empty bindings, invalid expressions, non-literal arguments, templates that end inside a tag or comment, and the reserved text `wc$<digit>`.
- **Parts.** Child content (`String`, primitives, boxes and enums through `String.valueOf`, nested `Template`, `null` for nothing); attribute values, including several bindings in one quoted value (`null` in any of them removes the attribute); `?boolean` attributes; `.property` values (`String`, `boolean`, `int`, `char`, `double`, `null`), set as JS properties on elements that are not Java components; `@event` listeners (zero-parameter lambdas, one-parameter lambdas and method references, selected by arity). The listener's event type follows the event name as in the [event table](#binding-kinds): `MouseEvent` for `click` and the other mouse events, `PointerEvent` for `pointer*`, `KeyboardEvent`, `FocusEvent`, `InputEvent` for `beforeinput`, `SubmitEvent`, and `Event` otherwise.
- **Templates section and preparation.** Each template's HTML with markers and its part list go into `javaw_wc_templates`. On first use the runtime parses the HTML once into a `<template>`, finds the markers after parsing (so implied `<tbody>` and similar parser fix-ups cannot misplace a part), and caches each part's child-index path. Instances clone the content and resolve their nodes from the paths.
- **Renderer.** Each rendered template is a Java object in Wasm holding every binding's previous value. Only changed bindings cross to JS: an update in which nothing changed makes no host calls, and changing one field commits only the bindings that use it. Text nodes are updated in place. A nested template with the same id is updated in place; a different template, text or `null` replaces it and releases the old instance.
- **Events.** An event part has one stable JS listener that calls the binding's current Java listener, so a render that creates a new lambda makes no host call. A `null` listener removes the JS listener. Listeners of released instances do nothing. The `Event` is valid only while the listener runs.
- **Lifetime.** Template instances belong to the component's handle scope and are dropped with it. Removing 200 rendered components with event parts finalizes all of them.
- **Differential check.** The same static strings and ten value steps rendered by lit-html 3.3.2 (vendored for tests) and by javaw give the same serialized DOM after every step, for child, multi-part attribute, boolean, property and nested-template parts. Java `null` in an attribute is passed to lit-html as `nothing`.
- **Properties of Java components.** `.name=${x}` on an element whose tag is a `@CustomElement` of the same module must name one of its `@Property` fields; another name is a compile error that lists the fields, and the value must have the field's type (checked by the compiler at the binding). The value is set through the field's generated setter on the element's Java object, with no JS marshalling, so the child's change detection and batched update apply; unchanged values make no host calls.
- **Typed DOM events.** `one.dastec.javaw.dom` has `MouseEvent` (positions, buttons, modifier keys), `PointerEvent`, `KeyboardEvent` (`key`, `code`, `repeat`, modifiers), `FocusEvent` (`relatedTarget`), `InputEvent` (`data`, `inputType`, `isComposing`), `SubmitEvent` (`submitter`) and `CustomEvent` (`detail`). String accessors read a missing value as `""`. A program that declares its own type with one of these names keeps it, and that typed event is then not available.
- **Core fix.** Stores into arrays reached through another object's field (`inst.values[i] = 1.5`) used `i32.store` for `double` and `long` elements (`ArrayFieldStoreTest`).

Differences from the plan, decided for this subset:
- **Descriptors are ids.** `Template.of` receives an int template id instead of a generated descriptor field; the HTML and part list live only in the custom section.
- **Typed component properties without `PropertyKey` objects.** `.name=${x}` on a component of the module becomes a generated `Target.__wc_prop_name(x)` call instead of `Parts.prop(Target.$NAME, x)`; the check is the same. Such rewritten sources reference generated methods, so they do not compile with javac against the API stubs alone.
- **No offset-map file.** Rewritten nodes carry their original positions, so diagnostics and source maps need no translation step. Rewritten sources are written to `target/generated-sources/javaw-components/` only by tests.
- **Source maps.** Mappings come only from the user's own types, including lambdas written in bindings (their generated classes map to where the lambda is written). Code that compiler extensions and library support add from source strings is not mapped; before this change its positions were mapped onto unrelated lines of the user's file.
- **Event listener values.** Lambdas and method references work. A value whose static type is `java.lang.Runnable` passes javac against the API stubs but has no matching `Parts.on` overload in javaw, because the source compiler does not model `Runnable`; it is a compile error at the binding.
- **Number properties** are compared with `Object.is` semantics, so an unchanged `NaN` makes no host call and `0.0` to `-0.0` is committed (Lit commits `NaN` every time and never commits `-0`); the DOM result is the same.

Implemented (M4 subset; evidence in `TemplateListTest`):
- **Keyed lists.** `repeat(items, keyFn, templateFn)` (on `WebComponent` and `Templates`) renders one item per element, keyed by `keyFn` with `equals`/`hashCode`. Updates use lit-html's reconciliation (head and tail matching, then key maps built on demand), so a surviving item keeps its DOM and is moved, and the serialized DOM matches lit-html's `repeat` over a sequence of reorderings, insertions and removals. A repeated key is reported through the error sink and the later item gets its own DOM. Removed items release their template instances.
- **Plain lists.** An `Iterable` bound as child content renders each element like a single child value (text, template, nested list or nothing) and reuses items by index.
- **`when(condition, trueCase[, falseCase])`** renders only the chosen case, given as a `TemplateSupplier` lambda. **`nothing()`** renders nothing as child content and removes the attribute in an attribute binding.
- **Core fixes found on the way** (`ObjectTextTest`, `StringIndexOfTest`): references held as `Object` (boxes, enums, user objects) printed their address in string concatenation and boxes had no `toString`; `equals(Object)` did not box a primitive argument; `instanceof` did not follow superinterfaces (`ArrayList` was not an `Iterable`); classes without fields whose instance methods capture `this` compiled their methods without a receiver; `indexOf(str, fromIndex)` ignored the index.

Implemented (rest of M4; evidence in `TemplateDirectiveTest`, `TemplateDiagnosticsTest`, `ForEachCallArgumentTest`):
- **`classMap(Map<String, Boolean>)`** (on `WebComponent` and `Templates`) must be the only binding of a `class` attribute; static text around it is allowed (`class="card ${classMap(m)}"`). As in lit-html, the first commit writes the attribute (static text plus the `true` keys), and later commits add and remove single classes, so the attribute's static classes and classes added by other code are kept. A `null` value counts as `false`; `classMap(null)` throws `NullPointerException`.
- **`styleMap(Map<String, String>)`** must be the only binding of a `style` attribute. Keys with a dash are CSS property names (including custom properties such as `--gap`); other keys are camelCase style names (`fontSize`, `webkitTransform` as `-webkit-transform` in the first commit's text). A `null` or missing value removes the property, and a value ending in ` !important` is set with priority, as lit-html does.
- **Diffing in Wasm.** Both maps are compared with the previous commit in Java: an unchanged map makes no host call, and one added class is one host call. Unlike lit-html, which writes every non-null style property on each commit, an unchanged style value is not written again, so a value changed by other code is not restored.
- **`ref(Ref<T>)` in element position** (`<input ${ref(field)}>`, `Parts.element(x)`). `Ref<T extends Element>` (`new Ref<Element>()`, `value()`) receives the element when the binding is first committed (before a nested template is inserted, as in lit-html). Following lit-html's `ref`: binding another `Ref` clears the old one; the element is assigned again if the `Ref` was changed elsewhere; removing the template clears the `Ref` unless it holds another element by then. The element is a scoped handle of the component even while its template is still a detached fragment. `null` (`${null}` or `ref(null)`) clears the previous `Ref`.
- **Placement.** Element position now accepts bindings. A direct `classMap(...)`/`styleMap(...)` call elsewhere than as the only binding of its attribute, a direct `ref(...)` call outside element position, and literals, operators, lambdas or method references in element position are compile errors at the binding. Other expressions are checked by type (`Parts.element` accepts only `ref(...)` results; `<p ${s}>` is "No applicable overload" at the expression). A `ClassMap`/`StyleMap` reached through a variable in another attribute is reported at runtime with lit-html's message, and as child content throws `IllegalArgumentException`.
- **Differential check.** Eight steps of class and style maps (static classes, `false` values, removal, custom properties, camelCase and vendor-prefixed names, `!important`) give the same DOM as lit-html 3.3.2's `classMap`/`styleMap`; Java `HashMap` iterates in hash order and lit-html objects in insertion order, so class tokens and style declarations are compared as sorted sets, and a one-key render is compared exactly. The same `ref` steps (swap, template removal and re-creation) give the same `Ref` values as lit-html's `ref`.
- **Core fix.** An enhanced `for` over a method call with a `char` or floating-point literal argument (`for (String p : split(s, ','))`) compiled as an array loop over the returned list (`ForEachCallArgumentTest`).

Implemented (M5 styles subset; evidence in `StylesTest`, `TextBlockTest`):
- **`css(literal)`** (on `WebComponent` and `Templates`) returns a `Css` value whose `cssText()` and `toString()` give the processed text. A concatenation, variable or other non-literal argument is a diagnostic (there is no `unsafeCSS`), and so is `${` in the text (no bindings; Lit's `css` admits only nested `CSSResult`s and numbers). Every call is qualified as `Templates.css` for the source compiler, which has no static method imports.
- **Style fields.** A `static final Css` field of any access in the component class or a superclass (component or not) is a style. Its initializer is a direct `css(...)` call or a simple (`BASE`) or type-qualified (`Theme.SHARED`) reference to another `static final Css` field of the module (a reference to an interface field is diagnosed as unsupported, since the core compiler cannot yet initialize object-typed interface constants), followed transitively. Diagnostics: a `Css` field of a component class or superclass that is not `static final`, a `Css[]` field, a missing initializer (for example one assigned in a static block), any other initializer (calls, conditionals), and self-reference. `css(...)` outside style fields is allowed and is not adopted.
- **Order and sharing.** The list is superclass fields first, then declaration order. A call site listed twice is kept at its *last* position, as Lit's `finalizeStyles` keeps the last occurrence, so it keeps its place in the cascade. Each call site's text goes into the `javaw_wc_styles` section and each component definition lists its call-site ids; the runtime caches one `CSSStyleSheet` per call site, so all instances in a document of a class and of its subclasses share the same sheet objects. The cache holds one document at a time: attaching an instance in another document rebuilds that call site's sheet, so instances alternating between two documents do not share sheets. The sheets are assigned to `adoptedStyleSheets` once, when the shadow root is attached on first connection, and are not touched again on reconnection.
- **Fallback.** Without constructable sheets (no `CSSStyleSheet` with `replaceSync`, or no `ShadowRoot.prototype.adoptedStyleSheets`), each sheet becomes a `<style>` element appended to each shadow root before the rendered content, as Lit does (Lit renders before the style elements; the cascade is the same).
- **Core fix.** Text blocks outside templates were rejected by the source compiler ("Unsupported expression TextBlockLiteralExpr"). They are now lowered to string literals after the source extensions run, with javac's incidental-whitespace stripping and escape translation (`TextBlockTest`, compared with the JVM's values).
- **Differences from Lit.** Every style field of the class chain applies; Lit's subclass `static styles` replaces the superclass's unless it lists `super.styles`. There is no `static styles` array: each sheet is its own field. No `nonce` is set on fallback `<style>` elements. lit 3.3.2's `css` lives in `@lit/reactive-element`, which is not vendored (only `lit-html` is), so there is no differential test; the ordering rule follows Lit's source.

Implemented (M5 events; evidence in `EventsTest`, `LongTextTest`, `CallbackOverloadArgumentTest`):
- **Event records.** A record that implements `ComponentEvent` is an event. `dispatch(event)` (not available in the constructor) dispatches a `CustomEvent` from the host element and returns `dispatchEvent`'s result, `!defaultPrevented`. The name is the simple name in kebab-case without an `Event` suffix (`RemainingChanged` is `remaining-changed`, `URLChanged` is `url-changed`), or `@EventName("...")`. `bubbles` and `composed` default to `true` and `cancelable` to `false`; `@EventOptions(bubbles = ..., composed = ..., cancelable = ...)` overrides them.
- **Records as classes.** The source compiler represents records as flattened values that cannot be held as `Object` (Todo stage 1). An event record must be held by reference while it is dispatched, so the extension lowers each `ComponentEvent` record to a final class with the record's private final fields, canonical constructor (a compact constructor's body runs first), accessors, and record-style `equals` (floating-point components compared like `Double.compare`), `hashCode` and `toString` (`Name[a=1, b=x]`). Declared accessors and methods are kept. The text, equality and compact-constructor behaviour match JVM records of the same source. Other records are unchanged.
- **Detail.** `detail` is a plain object built from the components: numbers for `byte`/`short`/`int`/`float`/`double`, booleans, a `bigint` for `long`, a one-character string for `char`, strings, enums as their kebab-case names (`VERY_HIGH` is `very-high`), the element for `Element`, a nested object for a component that is another event record, and `null` for `null`, including boxes. A component of any other type (lists, maps, arrays, plain records) is a compile error at the component.
- **Declared events.** A component declares the event records of its `dispatch(new X(...))` calls, `dispatch(x)` calls on a local or parameter declared as an event record, calls written in its templates' bindings, and `@Fires({X.class, ...})`, also from component superclasses. `@Fires` entries that are not event records of the module, and two declared events with the same name, are compile errors.
- **Typed bindings across components.** `@name=${...}` on the tag of a Java component of the module that declares an event `name` gives a one-parameter listener the event record itself (the same instance that was dispatched; `Todo.CloseRequestEvent e` in a method reference); zero-parameter listeners work as before. The `CustomEvent` carries a hidden token for the record only while `dispatch` runs, so a same-named event dispatched from JS (which has no record) does not call the typed Java listener; JS listeners see every event. Other event names on such tags keep the [event table](#binding-kinds) types.
- **`listen(target, type, listener)`** for `Element` and `JSObject` targets, with an `EventListener<Event>` or a zero-parameter listener. `window()` and `document()` give the runtime document's window and document. A registration is added while the component is connected, removed on disconnect, and added again on reconnect, so a removed component is not kept alive by the target: 100 components that listen on `window` and `document` are all finalized after removal. Call `listen` once per registration (in the constructor or `firstUpdated`, not in `connected()`), because registrations persist across reconnection.
- **Core fixes.** `String.valueOf(long)` and `StringBuilder.append(long)` used the 32-bit conversion and produced invalid WAT for `long` values (`LongTextTest`). Callback overloads with the same shape that differ in another parameter's type, such as the two `listen` overloads, were an "Ambiguous callback target" error; the other arguments' static types now select the overload (`CallbackOverloadArgumentTest`).
- **Differences and limitations.** Lit has no dispatch helper or event defaults (they are `CustomEvent`'s, all `false`), so there is no differential test against lit-html. happy-dom does not retarget `event.target` across shadow boundaries at the document, so retargeting and `composed` across real shadow roots are checked in the Chromium smoke test (M6). Declared events are written to the manifest (M6). Typed bindings see only components of the same compilation unit. `listen` listeners receive a plain `Event` (no typed subclasses) and there is no call to remove a single registration. Event record simple names must be unique in the module, and event records cannot be generic or have varargs components.


Remaining limitations: map order follows the map's iteration order, and the source compiler has `HashMap` but no `LinkedHashMap` or `Map.of`, so the order of classes in the first commit is hash order. `ref` takes a `Ref` object only (no callback form), and refs are not cleared when the component is disconnected, only when their template is removed. Fully qualified `java.util` type names in declarations or `new` (`new java.util.ArrayList<String>()`) do not compile correctly in the source compiler; import the types. List items must be classes: records passed as `Object` are not supported by the source compiler yet (Todo stage 1). The 200-component finalization test runs without collection inside a 20-page shared memory, so per-part memory has little headroom; directive parts reuse the existing value field.

Implemented (M6 artifacts subset; evidence in `ArtifactsTest`):
- **Artifacts.** For a module that defines components, the extension contributes three files through `SourceExtension.Output.artifact`. `JavacToWasm.compileFiles` (and so `javaw-maven-plugin`, with `javaw-components` added as a plugin dependency) writes them next to the `.wasm`; `JavacToWasm.compile` returns them from `CompilationResult.getArtifacts()`. `<module>` is the `.wasm` stem.
- **`<module>.custom-elements.json`** (named per module so two component modules compiled into one directory do not overwrite each other's manifest; point `package.json` `"customElements"` at the module's file; schema 2.1.0, one `javascript-module` whose path is `<module>.components.js`). Each class has `tagName`, the class Javadoc as `description`, `superclass: HTMLElement`, and one `custom-element-definition` export. `attributes` are the observed attributes in registration order (explicit `observedAttributes` typed `string`, then `@Property` attributes typed `string`/`boolean`/`number`, with `fieldName`). `members` are the `@Property` JS properties with their type, `attribute`, `reflects`, the field Javadoc and literal initializers as `default` (JS text, e.g. `"\"Todos\""`). `@State` fields are internal and omitted. `events` are the declared events (`dispatch(new X(...))` and `@Fires`, through component superclasses), typed `CustomEvent<XDetail>`. `slots` and `cssParts` are the literal `<slot name>` and `part` values in the class's `html(...)` templates; HTML comments and bound (`${...}`) names are skipped.
- **`<module>.components.d.ts`.** A readonly `XDetail` interface per event record with the [detail](#events) types (numbers, `bigint` for `long`, `string` for `char`, `\| null` for boxes, `String`, `Element` and nested records, kebab-case string unions for enums); per component a `<Class>EventMap extends HTMLElementEventMap` and an `interface <Class> extends HTMLElement` with the typed properties and `addEventListener`/`removeEventListener` overloads; and `declare global` entries in `HTMLElementTagNameMap` and `HTMLElementEventMap` (a union when two components declare different details for one name). Event names the DOM already declares (`change`, `input`, `click`, `toggle`, `close`, ...) are left out of the global `HTMLElementEventMap`, because the augmentation would retype the native event on every element; they are typed only on the component's `<Class>EventMap`, so `addEventListener('change', ...)` on the component itself sees the detail while an `<input>` keeps `Event`. It also types the ES module's exports. A strict TypeScript 5.9 consumer, including `@ts-expect-error` checks for wrong property and detail types, type-checks against it in the opt-in `typeScriptConsumerTypeChecks` test (`-Djavaw.tsc=<path>/typescript/lib/tsc.js`); the offline suite has no TypeScript and skips it.
- **`<module>.components.js`.** An ES module that imports `./javaw-runtime.js` (copied next to it), exports `tagNames`, `wasmUrl` (`./<module>.wasm` relative to the module), `load({source, imports, document})` (instantiates once and defines every tag; later calls return the same promise; a failed load is not cached, so the next call retries), `attach(instance)` (adopts an instance already created with `instantiateJavaw`) and `whenDefined()`. A Node test imports it, loads the module into happy-dom and renders a component.
- **`@Language`.** `html(...)` parameters (on `WebComponent` and `Templates`) carry `@org.intellij.lang.annotations.Language("HTML")` and `css(...)` parameters `@Language("CSS")`, from `org.jetbrains:annotations` (provided scope, class retention).

Differences from the plan, decided for this subset:
- **Interfaces, not classes, in the `.d.ts`.** The ES module exports no element classes (the runtime builds them at define time), so the declarations are interfaces; `document.createElement('todo-list')` and `querySelector` give the typed element.
- **No import side effect.** Importing `<module>.components.js` does not instantiate the module; call `load()`. This keeps `attach` possible for pages that instantiate the module themselves.
- **Types follow what the runtime defines.** Only the component's own `@Property` fields are JS properties and attributes (inherited reactive fields are not), so the manifest lists only those. Slots and parts from shared helper classes outside the component's class chain are not found.

Implemented (M6 Chromium smoke test and size budget):
- **`ChromiumSmokeTest` (opt-in).** Runs only when `-Djavaw.chrome=<binary>` or `JAVAW_CHROME` names a local Chrome or Chromium executable (for example Playwright's `chrome-headless-shell`); nothing is downloaded and the offline suite skips it. A configured path that is not an executable file fails the test rather than skipping it; for a macOS `.app` bundle pass the inner binary, e.g. `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`. The test serves the compiled module from a loopback `HttpServer` with `Cross-Origin-Opener-Policy`/`Cross-Origin-Embedder-Policy`, because javaw memory is shared and needs a cross-origin isolated page. Headless Chrome loads the page, which POSTs its results back. It checks:
  - upgrade timing: an element parsed before `load()` gets `attributeChanged("mode", ...)` before `connected()` when it is upgraded, which happy-dom skips;
  - `adopted()`: moving an element into `document.implementation.createHTMLDocument()` runs `disconnected`, `adopted`, then `connected`;
  - events: a composed `dispatch` from a component inside another component's shadow root is seen as the inner host in the outer root and retargeted to the outer host at the document; `composedPath()[0]` is the inner host; an `@EventOptions(composed = false)` event never reaches the document;
  - styles: one constructable sheet per class in `adoptedStyleSheets`, shared between instances, no `<style>` fallback, and the computed colours of `:host`, shadow content and a nested component;
  - the `javaw-counter` and `todo-list` examples, each in its own module, loaded on one page through their `components.js` modules: clicks, typed input, keyed list updates, `?disabled`, reflected attributes and event details.
  It passed on Playwright's chrome-headless-shell (build 1243) and Google Chrome 158 on macOS.
- **Size budget.** [`examples/components`](../examples/components) holds `JavawCounter.java` (`javaw-counter`) and `TodoList.java` (`todo-list`, the authoring-model example within the implemented subset: a `Todo` class instead of a record and `@State` items instead of a `List` `@Property`). `SizeBudgetTest` compiles each at `-O0` and fails when the `.wasm`, the `<module>.components.js` or `javaw-runtime.js` exceeds its budget in `size-budget.properties` (about 5% headroom). Recorded at M6:

  | Example | `.wasm` | `components.js` | `javaw-runtime.js` (shared) |
  | --- | --- | --- | --- |
  | `javaw-counter` | 223,295 B | 2,122 B | 180,886 B |
  | `todo-list` | 227,840 B | 2,106 B | 180,886 B |

  Most of each module is the runtime support linked into every javaw module; the component code adds about 4.5 KB between the two.
- **Core fixes.** `ArrayList` had no `ArrayList(Collection)` constructor, and a diamond whose target is a generic supertype (`List<String> x = new ArrayList<>()`) inferred `ArrayList<Object>`; the target's arguments now bind the class variables first (`ArrayListCopyConstructorTest`, against the JVM).

Not yet implemented in M6: record, list, enum and map property types (the [marshalling table](#generated-artifacts-and-typescript-interop) applies once those properties exist); CSS custom properties in the manifest.

Known compiler gaps that affect components (Todo section 13): `+=` on a String field concatenates only for locals and statics (reactive fields are unaffected, because their writes go through setters).

Known M1 limitations:
- **Scope growth.** A live component's scope keeps every node it has handed to Java, and those nodes' listener closures, until the component is released. A long-lived component that repeatedly replaces its own DOM therefore grows (see [DOM handle scopes](dom-handle-scopes.md#limitations)). M3's renderer will release handles for the DOM it removes.
- **`adopted` is covered only in Chromium.** happy-dom 21.0.4 never calls `adoptedCallback`; the opt-in Chromium smoke test checks it.
- **Multi-file diagnostics.** In a build with several files, the "extension missing" diagnostic for an import names the first file.

Not yet implemented in M1:
- typed event subclasses (`MouseEvent` and others);
- a compile-time diagnostic for components used from worker entry points;

## Contents

- [Implementation status](#implementation-status)

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
| `window()`, `document()` | `protected static` `JSObject` targets for `listen`. |
| `ComponentEvent`, `@EventName`, `@EventOptions`, `@Fires` | Event records; see [Events](#events). |

New DOM surface in `javaw-core` package `one.dastec.javaw.dom` (M1 unless noted otherwise):

- `Event`, with `target()`, `currentTarget()`, `type()`, `preventDefault()`, `defaultPrevented()` and `stopPropagation()`;
- `MouseEvent`, `PointerEvent`, `KeyboardEvent`, `FocusEvent`, `InputEvent`, `SubmitEvent` and `CustomEvent`;
- `EventListener<E extends Event>`;
- `Element.querySelector` and `Element.querySelectorAll`, so lookups can be scoped to an element;
- `ShadowRoot`;
- the instance-bound overload `Element.addEventListener(String, EventListener<E>)` and its `removeEventListener` counterpart, covered by the [first prerequisite](#prerequisites); the listener's event type selects the wrapper class and is checked against literal event names ([typed listeners](dom-event-listeners.md)).

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

Each `css(...)` call site becomes one shared `CSSStyleSheet`, created once. A shadow root's `adoptedStyleSheets` lists the sheets superclass first, then in declaration order, with duplicates removed by call site (the last occurrence is kept, as in Lit). Where constructable stylesheets are unavailable, each sheet falls back to a `<style>` element in each shadow root.

## Events

A record that implements `ComponentEvent` is dispatched as a `CustomEvent`:

- **Name.** The record's simple name in kebab-case, with any `Event` suffix removed: `RemainingChanged` becomes `remaining-changed`. `@EventName("...")` overrides it.
- **Options.** `bubbles` and `composed` default to `true`, and `cancelable` defaults to `false`. `@EventOptions(bubbles, composed, cancelable)` overrides them. `dispatch` returns `!event.defaultPrevented`, so it can only return `false` for a cancelable event.
- **Detail.** `detail` is the record, marshalled as described in [Property marshalling](#property-marshalling). Java listeners bound in a template with `@remaining-changed=${...}` receive the original record instance. The `CustomEvent` carries a hidden reference to it.
- **Declared events.** A component's events are the record types in its `dispatch(new X(...))` calls, plus any listed in `@Fires(X.class)`. They are recorded in the manifest and give event bindings in other Java components a static type.
- **Listeners on long-lived targets.** For listeners on `window`, `document` or other objects that outlive the component, use `listen(target, type, listener)` with `window()`, `document()` or an `Element`. It adds the listener on `connected` and removes it on `disconnected`, and adds it again when the component is reconnected; call it once per registration. See [callback ownership](#callback-ownership).
- **Implementation.** Event records are lowered to final classes, and the detail types are limited; see [Implementation status](#implementation-status).

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
- `<module>.custom-elements.json`: the [Custom Elements Manifest](https://github.com/webcomponents/custom-elements-manifest) schema. It records tags, attributes, properties, events, slots found in templates and CSS parts.
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
