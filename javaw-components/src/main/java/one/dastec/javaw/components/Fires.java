package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event records a component dispatches, in addition to those found in its {@code dispatch(new X(...))}
 * calls. Declared events give {@code @name=${...}} bindings on the component's tag in other Java
 * components the record type.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Fires {
    Class<? extends ComponentEvent>[] value();
}
