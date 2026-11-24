package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A private {@code Element} field of a {@link WebComponent} whose every read is
 * {@code renderRoot().querySelector(selector)}. Writing the field is a compile-time error.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Query {
    String value();
}
