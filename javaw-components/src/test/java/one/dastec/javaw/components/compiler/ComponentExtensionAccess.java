package one.dastec.javaw.components.compiler;

/** Test access to package-private helpers. */
public final class ComponentExtensionAccess {
    private ComponentExtensionAccess() {}

    static {
        com.github.javaparser.StaticJavaParser.getConfiguration()
                .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17);
    }

    /** Runs the extension on {@code source} and returns the rewritten source text. */
    public static String rewrite(String source) {
        var cu = com.github.javaparser.StaticJavaParser.parse(source);
        new ComponentExtension().prepare(cu, (name, payload) -> {});
        return cu.toString();
    }

    /** The processed static strings of a template literal's raw text, as the renderer receives them. */
    public static java.util.List<String> statics(String raw, boolean textBlock) {
        return TemplateScanner.scan(raw, textBlock).statics();
    }

    /** Runs the extension and returns the source of the original (user) types only, plus the sections. */
    public static String rewriteUserTypes(String source, java.util.Map<String, String> sections) {
        var cu = com.github.javaparser.StaticJavaParser.parse(source);
        java.util.Set<String> own = new java.util.HashSet<>();
        cu.getTypes().forEach(t -> own.add(t.getNameAsString()));
        new ComponentExtension().prepare(cu, sections::put);
        cu.getTypes().removeIf(t -> !own.contains(t.getNameAsString()));
        cu.getImports().removeIf(i -> i.getNameAsString().equals("one.dastec.javaw.dom.JSObject"));
        return cu.toString();
    }

    public static String tagProblem(String name) {
        return ComponentExtension.tagProblem(name);
    }
}
