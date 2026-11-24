package one.dastec.javaw.components;

/**
 * A rendered template value: a template id plus the current value of each binding.
 *
 * <p>Produced by the compiler's rewrite of {@code html("...")}; authors never build one directly.
 * This class is a compile-time facade; the javaw-components compiler extension supplies the
 * implementation that runs in WebAssembly.
 */
public final class Template {
    private Template() {
    }

    /** Rewrite target of {@code html(...)}; not for direct use. */
    public static Template of(int id, TemplatePart... parts) {
        return new Template();
    }
}
