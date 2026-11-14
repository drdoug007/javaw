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
    private static final String DOM_PACKAGE = "one.dastec.javaw.dom";

    private DomCallbackSupport() {}

    static void addTypes(CompilationUnit cu) {
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
    }

    /** True for add/removeEventListener calls whose listener is not syntactically a handler-name string. */
    static boolean mayBeCallbackListenerCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (!name.equals("addEventListener") && !name.equals("removeEventListener")) return false;
        if (call.getScope().isEmpty() || call.getArguments().size() != 2) return false;
        Expression listener = call.getArgument(1);
        return !listener.isStringLiteralExpr() && !listener.isTextBlockLiteralExpr() && !listener.isBinaryExpr();
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
            if (!imported.isStatic() && !imported.isAsterisk() && TYPES.stream().anyMatch(t -> name.equals(DOM_PACKAGE + "." + t)))
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
