package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A reactive public property: a private instance field whose writes schedule an update, exposed to
 * JavaScript as an element property and, for {@code String}, {@code boolean}, {@code int} and
 * {@code double} fields, as an observed attribute.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Property {
    /** Attribute name; defaults to the field name in kebab-case ({@code maxItems} becomes {@code max-items}). */
    String attribute() default "";

    /** Whether changes are written back to the attribute during the update. */
    boolean reflect() default false;
}
