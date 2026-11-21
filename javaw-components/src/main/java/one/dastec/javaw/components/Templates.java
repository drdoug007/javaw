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

    /**
     * A keyed list: {@code key} identifies each item between renders and {@code template} renders it.
     * Reordering moves the existing DOM of each item; a repeated key is reported and its later item
     * gets its own DOM.
     */
    public static <T> Repeat repeat(Iterable<T> items, KeyFunction<T> key, ItemTemplate<T> template) {
        return null;
    }

    /** {@code trueCase} when {@code condition} holds, otherwise nothing; only the chosen case is rendered. */
    public static Template when(boolean condition, TemplateSupplier trueCase) {
        return null;
    }

    /** {@code trueCase} or {@code falseCase}; only the chosen case is rendered. */
    public static Template when(boolean condition, TemplateSupplier trueCase, TemplateSupplier falseCase) {
        return null;
    }

    /** Renders nothing as child content and removes the attribute in an attribute binding. */
    public static Template nothing() {
        return null;
    }
}
