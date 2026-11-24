package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link WebComponent} subclass as a browser custom element.
 *
 * <p>The tag must be a valid custom element name: it starts with an ASCII lowercase letter, contains a
 * hyphen, has no ASCII uppercase letters, and is not a reserved name. Each tag may be claimed once per module.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CustomElement {
    /** The custom element name, for example {@code "todo-list"}. */
    String value();

    /** Attribute names whose changes call {@link WebComponent#attributeChanged}. */
    String[] observedAttributes() default {};
}
