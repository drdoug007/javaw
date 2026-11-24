package one.dastec.javaw.components;

/**
 * A component event: a record that implements this interface is dispatched with
 * {@link WebComponent#dispatch(ComponentEvent)} as a {@code CustomEvent}. The event name is the record's
 * simple name in kebab-case without an {@code Event} suffix ({@code RemainingChanged} is
 * {@code remaining-changed}) unless {@link EventName} gives one; {@link EventOptions} sets
 * {@code bubbles}, {@code composed} and {@code cancelable}. {@code detail} is a plain object built from
 * the record's components, and Java listeners bound with {@code @name=${...}} receive the record itself.
 */
public interface ComponentEvent {
}
