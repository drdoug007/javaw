import one.dastec.javaw.components.ComponentEvent;
import one.dastec.javaw.components.Css;
import one.dastec.javaw.components.CustomElement;
import one.dastec.javaw.components.Property;
import one.dastec.javaw.components.Template;
import one.dastec.javaw.components.WebComponent;

/** A button that counts its clicks. Size-budget example (docs/web-components.md, Validation strategy). */
@CustomElement("javaw-counter")
public class JavawCounter extends WebComponent {
    static final Css STYLES = css("""
        :host { display: inline-block; font: 14px system-ui; }
        button { min-width: 4em; }
        """);

    /** Fired after every click with the new count. */
    public record CountChanged(int count) implements ComponentEvent {}

    /** The current count. */
    @Property(reflect = true) private int count;
    /** The label shown before the count. */
    @Property private String label = "Clicks";

    @Override protected Template render() {
        return html("""
            <button part="button" @click=${this::increment}>${label}: ${count}</button>
            """);
    }

    private void increment() {
        count = count + 1;
        dispatch(new CountChanged(count));
    }
}
