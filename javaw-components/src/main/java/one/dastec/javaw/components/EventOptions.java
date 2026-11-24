package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code CustomEvent} options of a {@link ComponentEvent} record. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EventOptions {
    boolean bubbles() default true;

    boolean composed() default true;

    /** Only a cancelable event can make {@link WebComponent#dispatch(ComponentEvent)} return {@code false}. */
    boolean cancelable() default false;
}
