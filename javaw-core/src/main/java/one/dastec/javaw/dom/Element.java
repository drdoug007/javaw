package one.dastec.javaw.dom;

public class Element {
    public void setInnerText(String text) {
    }

    public String getInnerText() {
        return null;
    }

    public void setAttribute(String name, String value) {
    }

    public String getAttribute(String name) {
        return null;
    }

    public void appendChild(Element child) {
    }

    public void removeChild(Element child) {
    }

    public void remove() {
    }

    public Element getParent() {
        return null;
    }

    public String getValue() {
        return null;
    }

    public void setValue(String value) {
    }

    public boolean isChecked() {
        return false;
    }

    public void setChecked(boolean checked) {
    }

    public void setStyle(String name, String value) {
    }

    public String getStyle(String name) {
        return null;
    }

    public void addClass(String className) {
    }

    public void removeClass(String className) {
    }

    public boolean hasClass(String className) {
        return false;
    }

    public void toggleClass(String className) {
    }

    public ClassList getClassList() {
        return null;
    }

    public CanvasRenderingContext2D getContext2D() {
        return null;
    }

    public CanvasRenderingContext2D getContext(String contextType) {
        return null;
    }

    public WebGLRenderingContext getWebGLContext() {
        return null;
    }

    public int getWidth() {
        return 0;
    }

    public void setWidth(int width) {
    }

    public int getHeight() {
        return 0;
    }

    public void setHeight(int height) {
    }

    /** The first descendant matching {@code selector}, or {@code null}. */
    public Element querySelector(String selector) {
        return null;
    }

    /** All descendants matching {@code selector}, in document order. */
    public Element[] querySelectorAll(String selector) {
        return new Element[0];
    }

    /** Calls the static exported function named {@code handlerMethod}, without a receiver or event. */
    public void addEventListener(String event, String handlerMethod) {
    }

    /**
     * Registers an instance-bound listener. The listener keeps its captured
     * receiver and values, and the runtime retains it until it is removed.
     * Adding the same listener object for the same type again has no effect.
     */
    public void addEventListener(String type, EventListener<Event> listener) {
    }

    /** Registers an instance-bound listener that does not need the event. */
    public void addEventListener(String type, Runnable listener) {
    }

    /** Removes a listener added with the same type and the same listener object. */
    public void removeEventListener(String type, EventListener<Event> listener) {
    }

    /** Removes a listener added with the same type and the same listener object. */
    public void removeEventListener(String type, Runnable listener) {
    }
}
