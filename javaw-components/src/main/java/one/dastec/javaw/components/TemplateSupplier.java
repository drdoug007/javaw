package one.dastec.javaw.components;

/** A lazily rendered template, for {@code when}. */
@FunctionalInterface
public interface TemplateSupplier {
    Template get();
}
