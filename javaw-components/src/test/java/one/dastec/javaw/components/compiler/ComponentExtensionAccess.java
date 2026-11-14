package one.dastec.javaw.components.compiler;

/** Test access to package-private helpers. */
public final class ComponentExtensionAccess {
    private ComponentExtensionAccess() {}

    /** Runs the extension on {@code source} and returns the rewritten source text. */
    public static String rewrite(String source) {
        var cu = com.github.javaparser.StaticJavaParser.parse(source);
        new ComponentExtension().prepare(cu, (name, payload) -> {});
        return cu.toString();
    }

    public static String tagProblem(String name) {
        return ComponentExtension.tagProblem(name);
    }
}
