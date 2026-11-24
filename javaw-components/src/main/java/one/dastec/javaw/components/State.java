package one.dastec.javaw.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Internal reactive state: a private instance field whose writes schedule an update. No attribute or JS property. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface State {
}
