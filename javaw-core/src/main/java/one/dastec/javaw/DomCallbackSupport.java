package one.dastec.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.HashSet;
import java.util.Set;

/**
 * Instance-bound DOM listeners: the source types behind {@code one.dastec.javaw.dom.Event} and
 * {@code EventListener}, and the exported dispatchers the host calls with a retained closure pointer.
 */
final class DomCallbackSupport {
    static final String RUN_EXPORT = "__javaw_runListener";
    static final String EVENT_EXPORT = "__javaw_eventListener";
    /**
     * Zero-parameter listener lambdas and method references target this internal interface. The registry's
     * built-in Runnable has no source contract, and Thread.start dispatches it through a fixed vtable slot.
     */
    static final String RUN_TARGET = "__JavawDomRunnable";
    private static final Set<String> TYPES = Set.of("Event", "EventListener");
    /** Typed event subclasses, each added unless the program declares a type of the same name. */
    private static final java.util.Map<String, String> EVENT_TYPES = new java.util.LinkedHashMap<>();
    static {
        EVENT_TYPES.put("MouseEvent", """
            class MouseEvent extends Event {
                public MouseEvent(JSObject js) { super(js); }
                public double clientX() { return js().getDouble("clientX"); }
                public double clientY() { return js().getDouble("clientY"); }
                public double offsetX() { return js().getDouble("offsetX"); }
                public double offsetY() { return js().getDouble("offsetY"); }
                public int button() { return js().getInt("button"); }
                public int buttons() { return js().getInt("buttons"); }
                public boolean altKey() { return js().getBoolean("altKey"); }
                public boolean ctrlKey() { return js().getBoolean("ctrlKey"); }
                public boolean shiftKey() { return js().getBoolean("shiftKey"); }
                public boolean metaKey() { return js().getBoolean("metaKey"); }
            }
                """);
        EVENT_TYPES.put("PointerEvent", """
            class PointerEvent extends MouseEvent {
                public PointerEvent(JSObject js) { super(js); }
                public int pointerId() { return js().getInt("pointerId"); }
                public String pointerType() { return js().getString("pointerType"); }
                public double pressure() { return js().getDouble("pressure"); }
                public boolean isPrimary() { return js().getBoolean("isPrimary"); }
            }
                """);
        EVENT_TYPES.put("KeyboardEvent", """
            class KeyboardEvent extends Event {
                public KeyboardEvent(JSObject js) { super(js); }
                public String key() { return js().getString("key"); }
                public String code() { return js().getString("code"); }
                public boolean repeat() { return js().getBoolean("repeat"); }
                public boolean altKey() { return js().getBoolean("altKey"); }
                public boolean ctrlKey() { return js().getBoolean("ctrlKey"); }
                public boolean shiftKey() { return js().getBoolean("shiftKey"); }
                public boolean metaKey() { return js().getBoolean("metaKey"); }
            }
                """);
        EVENT_TYPES.put("FocusEvent", """
            class FocusEvent extends Event {
                public FocusEvent(JSObject js) { super(js); }
                public Element relatedTarget() { return js().get("relatedTarget").asElement(); }
            }
                """);
        EVENT_TYPES.put("InputEvent", """
            class InputEvent extends Event {
                public InputEvent(JSObject js) { super(js); }
                public String data() { return js().getString("data"); }
                public String inputType() { return js().getString("inputType"); }
                public boolean isComposing() { return js().getBoolean("isComposing"); }
            }
                """);
        EVENT_TYPES.put("SubmitEvent", """
            class SubmitEvent extends Event {
                public SubmitEvent(JSObject js) { super(js); }
                public Element submitter() { return js().get("submitter").asElement(); }
            }
                """);
        EVENT_TYPES.put("CustomEvent", """
            class CustomEvent extends Event {
                public CustomEvent(JSObject js) { super(js); }
                public JSObject detail() { return js().get("detail"); }
            }
                """);
    }
    private static final String DOM_PACKAGE = "one.dastec.javaw.dom";

    private DomCallbackSupport() {}

    static void addTypes(CompilationUnit cu) {
        if (usesDom(cu))
            // The runtime calls this export when a released scoped handle is used, so the failure is a Java exception.
            LibrarySupport.add(cu, DOM_PACKAGE, """
                class __JavawDomErrors {
                    public static void __javaw_elementReleased() { throw new IllegalStateException("element released"); }
                }
                """);
        if (!needed(cu)) return;
        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class))
            if (TYPES.contains(type.getNameAsString()))
                throw JavaSourcePreparation.error(type, "Type name '" + type.getNameAsString()
                        + "' conflicts with one.dastec.javaw.dom." + type.getNameAsString() + ", which instance-bound DOM listeners use");
        LibrarySupport.add(cu, "one.dastec.javaw.dom", """
            class Event {
                private JSObject __js;
                public Event(JSObject js) { this.__js = js; }
                public String type() { return __js.getString("type"); }
                public Element target() { return __js.get("target").asElement(); }
                public Element currentTarget() { return __js.get("currentTarget").asElement(); }
                public void preventDefault() { JSObject ignored = __js.call("preventDefault"); }
                public boolean defaultPrevented() { return __js.getBoolean("defaultPrevented"); }
                public void stopPropagation() { JSObject ignored = __js.call("stopPropagation"); }
                public JSObject js() { return __js; }
            }
            interface EventListener<E> { public void handleEvent(E event); }
            interface __JavawDomRunnable { public void run(); }
            class __JavawHostCallbacks {
                public static void __javaw_runListener(__JavawDomRunnable listener) { listener.run(); }
                public static void __javaw_eventListener(EventListener<Event> listener, JSObject event) {
                    listener.handleEvent(new Event(event));
                }
            }
            """);
        Set<String> declared = new HashSet<>();
        cu.findAll(TypeDeclaration.class).forEach(t -> declared.add(t.getNameAsString()));
        StringBuilder events = new StringBuilder();
        for (var entry : EVENT_TYPES.entrySet()) {
            boolean ownParent = entry.getKey().equals("PointerEvent") && declared.contains("MouseEvent");
            if (!declared.contains(entry.getKey()) && !ownParent) events.append(entry.getValue()).append('\n');
        }
        if (events.length() > 0) LibrarySupport.add(cu, DOM_PACKAGE, events.toString());
    }

    /** True for add/removeEventListener calls whose listener is not syntactically a handler-name string. */
    static boolean mayBeCallbackListenerCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (!name.equals("addEventListener") && !name.equals("removeEventListener")) return false;
        if (call.getScope().isEmpty() || call.getArguments().size() != 2) return false;
        Expression listener = call.getArgument(1);
        return !listener.isStringLiteralExpr() && !listener.isTextBlockLiteralExpr() && !listener.isBinaryExpr();
    }

    private static boolean usesDom(CompilationUnit cu) {
        return cu.getImports().stream().anyMatch(i -> i.getNameAsString().equals(DOM_PACKAGE) || i.getNameAsString().startsWith(DOM_PACKAGE + "."));
    }

    /**
     * Inject only for programs that use DOM listener callbacks: a lambda or method reference passed to
     * add/removeEventListener when no source type declares those methods, or an explicit import of the
     * dom Event/EventListener types (a wildcard dom import counts when the names are used and not declared).
     */
    private static boolean needed(CompilationUnit cu) {
        boolean sourceListenerMethods = cu.findAll(MethodDeclaration.class).stream()
                .anyMatch(m -> m.getNameAsString().equals("addEventListener") || m.getNameAsString().equals("removeEventListener"));
        if (!sourceListenerMethods)
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class))
                if (mayBeCallbackListenerCall(call) && (call.getArgument(1).isLambdaExpr() || call.getArgument(1).isMethodReferenceExpr()))
                    return true;
        for (ImportDeclaration imported : cu.getImports()) {
            String name = imported.getNameAsString();
            if (!imported.isStatic() && !imported.isAsterisk() && (TYPES.stream().anyMatch(t -> name.equals(DOM_PACKAGE + "." + t))
                    || EVENT_TYPES.keySet().stream().anyMatch(t -> name.equals(DOM_PACKAGE + "." + t))))
                return true;
        }
        boolean wildcard = cu.getImports().stream().anyMatch(i -> !i.isStatic() && i.isAsterisk() && i.getNameAsString().equals(DOM_PACKAGE));
        if (!wildcard) return false;
        Set<String> declared = new HashSet<>();
        cu.findAll(TypeDeclaration.class).forEach(t -> declared.add(t.getNameAsString()));
        for (ClassOrInterfaceType type : cu.findAll(ClassOrInterfaceType.class))
            if (TYPES.contains(type.getNameAsString()) && !declared.contains(type.getNameAsString())) return true;
        return false;
    }
}
