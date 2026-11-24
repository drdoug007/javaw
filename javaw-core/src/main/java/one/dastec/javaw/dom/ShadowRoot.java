package one.dastec.javaw.dom;

/** A shadow root, such as {@code WebComponent.renderRoot()}: a node tree attached to a host element. */
public class ShadowRoot {
    public void appendChild(Element child) {
    }

    public void removeChild(Element child) {
    }

    /** The first descendant matching {@code selector}, or {@code null}. */
    public Element querySelector(String selector) {
        return null;
    }

    /** All descendants matching {@code selector}, in document order. */
    public Element[] querySelectorAll(String selector) {
        return new Element[0];
    }
}
