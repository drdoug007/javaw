package one.dastec.javaw.components;

/**
 * A style sheet produced by {@code css("...")}. A {@code static final Css} field of a component class or
 * one of its superclasses adds the sheet to every instance's shadow root (docs/web-components.md, "Styles").
 *
 * <p>This class is a compile-time facade; the javaw-components compiler extension supplies the
 * implementation that runs in WebAssembly.
 */
public final class Css {
    private final String text;

    private Css(String text) {
        this.text = text;
    }

    /** The style text, after Java literal processing. */
    public String cssText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    static Css __of(String text) {
        return new Css(text);
    }
}
