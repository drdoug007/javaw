# Instance-bound DOM event listeners

Status: implemented in the source compiler (`javaw-core`). This is the first prerequisite of the [web components track](web-components.md#prerequisites). The bytecode compiler does not lower these calls.

## API

`one.dastec.javaw.dom.Element` has these overloads:

| Method | Listener |
| --- | --- |
| `addEventListener(String type, String handlerMethod)` | Existing form. It calls the static exported function with that name, with no receiver and no event. |
| `<E extends Event> addEventListener(String type, EventListener<E> listener)` | A Java callback that receives the event, wrapped in the listener's event class `E`. |
| `addEventListener(String type, Runnable listener)` | A Java callback that ignores the event. |
| `<E extends Event> removeEventListener(String type, EventListener<E> listener)` and `(String, Runnable)` | Removes a listener that was added with the same type and the same listener object. |

`Event` provides `type()`, `target()`, `currentTarget()`, `preventDefault()`, `defaultPrevented()`, `stopPropagation()` and `js()`. `js()` returns the underlying `JSObject`, for properties that have no typed accessor. `target()` and `currentTarget()` return `null` when the value is not an element. `JSObject.asElement()` performs the same conversion for any JS value.

```java
class Counter {
    private int count;
    private EventListener<Event> listener = e -> { count++; e.preventDefault(); };
    void mount(Element button) {
        button.addEventListener("click", listener);
        button.addEventListener("reset", this::reset);   // zero parameters: the Runnable overload
    }
    void unmount(Element button) { button.removeEventListener("click", listener); }
    private void reset() { count = 0; }
}
```

## Semantics

- **The receiver is kept.** A lambda or bound method reference keeps its captured values and its receiver. Two `Counter` instances each receive their own clicks.
- **Overload selection by shape.** The callback's shape picks the overload. A lambda with no parameters, or a method reference that can only bind zero arguments, uses `Runnable`. One parameter uses `EventListener<E>`, where `E` is the declared parameter type of an explicitly typed lambda or of the referenced method, and `Event` for an implicit lambda (as javac infers). A method reference that matches both shapes is a compile-time error that asks for a lambda.
- **Listener identity.** Identity is the Java listener object, as in the DOM. Adding the same object again for the same type has no effect. `removeEventListener` must be given that same object. A lambda expression creates a new object each time it is evaluated, so store the listener if you need to remove it later.
- **`Event` lifetime.** An `Event` is valid only while its listener runs. If the runtime created the JS handle for the event in order to dispatch it, it releases that handle when the listener returns. Elements that `target()` returns use the global handle table and keep its existing lifetime.
- **Errors.** A Java exception that escapes a listener is passed to `globalThis.reportError` when it exists, and is logged with `console.error` otherwise. Other listeners for the same event still run.

## Ownership

The host import `dom.addEventListenerCallback(element, typePtr, typeLen, closurePtr, kind)` does the following:

- it calls `runtime.retain(closurePtr)` once per new (element, type, listener) registration;
- it creates a JS function that calls the exported dispatcher, `__javaw_runListener` for kind 0 or `__javaw_eventListener` for kind 1;
- it stores that function in a `WeakMap` keyed by the element.

`dom.removeEventListenerCallback` removes the JS function and calls `runtime.release(closurePtr)`. That makes the closure collectable once nothing else references it, and `DomCallbackTest` checks that live bytes return to their baseline.

A `FinalizationRegistry` on the JS function also releases the closure if the function is garbage collected without being removed. Release is best-effort. In practice, an element obtained through the existing global handle table is never collected, because that table holds elements strongly. A listener on such an element therefore stays retained until it is removed. Listeners on nodes with [scoped handles](dom-handle-scopes.md#listeners-inside-a-scope) are released together with their scope instead.

Retaining and releasing only update a counter, so it is safe to do inside an active Wasm call. The collector still runs only at host safepoints ([memory management](memory-management.md)).

## Limitations

- **Only lambdas and method references are accepted.** Pass the lambda or method reference directly, or store it as an `EventListener<Event>` or `EventListener<X>` of a typed event `X`; a wildcard type (`EventListener<? super MouseEvent>`) is a compile-time error, because the wrapper class is chosen statically. A program class named like a typed event (its own `MouseEvent`) cannot be used as a listener type. Passing a value of type `Runnable` (a variable, a field, or an instance of a class that implements `Runnable`) is a compile-time error. The registry's built-in `Runnable` has no source contract, and `Thread.start` dispatches it through a fixed vtable slot. Zero-parameter listener lambdas therefore target an internal interface, `__JavawDomRunnable`.
- **Name conflicts.** A program that uses these listeners cannot also declare its own types named `Event` or `EventListener`; doing so is a compile-time error.
- **Typed listeners.** `E` is `Event`, `MouseEvent`, `PointerEvent`, `KeyboardEvent`, `FocusEvent`, `InputEvent`, `SubmitEvent` or `CustomEvent`, taken from an explicitly typed lambda (`(MouseEvent e) -> ...`), the parameter of a referenced method, or the declared type of a stored `EventListener<KeyboardEvent>`. The compiler passes `E` to the host as the listener kind, and every dispatch is wrapped in `E`, whatever class the JS event has (as template `@event` bindings do); accessors of properties the JS event lacks return their JS defaults. Any other parameter type is a compile-time error. When the event name is a string literal that the [event table](web-components.md#binding-kinds) dispatches with a class unrelated to `E` (`"keydown"` with a `MouseEvent` listener, `"click"` with a `FocusEvent` listener), the call is a compile-time error; names outside the table (custom events, `"input"`) accept any `E`. An implicit lambda (`e -> ...`) receives `Event`, as javac infers; per-name inference for implicit lambdas would accept programs javac rejects. Use `Event.js()` for properties without typed accessors.
- **Main thread only.** Listener dispatch has no worker support.
- **`null` listeners are ignored.** A listener expression that evaluates to `null` adds or removes nothing. Java would throw `NullPointerException` here.
- **Not from static initializers.** Listeners cannot be registered from static initializers. These run from the module start function, before the runtime has the instance it needs to retain the closure.
- **When the support types are added.** `Event`, `EventListener` and the dispatchers are added only in two cases. The first is a program that passes a lambda or method reference to `add`/`removeEventListener`, provided no source type declares methods with those names. The second is a program that imports `one.dastec.javaw.dom.Event` or `EventListener`, either explicitly or through a wildcard import plus use of the name. Source classes that declare their own `addEventListener` keep ordinary Java dispatch.

## Validation

- `DomCallbackTest` runs compiled modules in Node with `javaw-runtime.js` and a minimal `EventTarget`-based fake document. It covers:
  - per-instance receivers;
  - duplicate adds;
  - `Runnable` method references;
  - `Event.type()`, `target()` and `preventDefault()`;
  - a listener reachable only from JS surviving `__gc_collect`;
  - event handles released after 50 dispatches;
  - removal;
  - release of removed closures (live bytes return to the baseline);
  - error reporting while other listeners keep running;
  - diagnostics for ambiguous method references, `Runnable` values and conflicting type names.
- `LambdaSupportTest.selectsFunctionalOverloadsByCallbackArity` covers arity-based overload selection among source overloads, for lambdas and for static, bound and `this::` method references.
