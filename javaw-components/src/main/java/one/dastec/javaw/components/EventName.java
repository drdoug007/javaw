package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The DOM event name of a {@link ComponentEvent} record, instead of its kebab-case simple name. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EventName {
    String value();
}
