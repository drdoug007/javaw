package one.dastec.javaw.components;

/**
 * Template functions for classes that do not extend {@link WebComponent}, for example shared row or
 * layout helpers: {@code import static one.dastec.javaw.components.Templates.html;}.
 *
 * <p>Like {@code WebComponent.html}, every call is rewritten by the javaw-components compiler
 * extension, so this method never runs.
 */
public final class Templates {
    private Templates() {
    }

    /** A template; the argument must be a string literal or text block, and {@code ${expr}} marks a binding. */
    public static Template html(String template) {
        throw new UnsupportedOperationException("html(...) must be rewritten by the javaw-components compiler extension");
    }
}
