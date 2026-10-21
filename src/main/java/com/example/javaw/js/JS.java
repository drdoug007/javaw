package com.example.javaw.js;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining inline JavaScript code for Java native methods or classes.
 * Inspired by GraalVM Web Image and modern Java-to-WebAssembly interop.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JS {
    /**
     * Inline JavaScript code snippet or expression to execute.
     */
    String value() default "";

    /**
     * Optional sub-annotation to request automatic type coercion across WASM/JS boundary.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Coerce {}

    /**
     * Global JavaScript class or object binding.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Import {
        String value() default "";
    }

    /**
     * Export Java class or function to JavaScript global scope.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Export {
        String value() default "";
    }

    /**
     * Embedded JavaScript source code block or module inclusion.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Code.List.class)
    @interface Code {
        String value() default "";

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface Include {
            String[] value() default {};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface List {
            Code[] value();
        }
    }

    /**
     * Embedded static web resource (CSS, JSON, HTML, text, binary asset).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    @Repeatable(Resource.List.class)
    @interface Resource {
        String value() default "";
        String path() default "";

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
        @interface List {
            Resource[] value();
        }
    }
}
