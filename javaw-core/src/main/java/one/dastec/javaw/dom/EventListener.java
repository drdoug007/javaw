package one.dastec.javaw.dom;

/** An instance-bound DOM event listener; see {@link Element#addEventListener(String, EventListener)}. */
@FunctionalInterface
public interface EventListener<E extends Event> {
    void handleEvent(E event);
}
