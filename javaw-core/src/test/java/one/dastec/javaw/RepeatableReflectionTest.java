package one.dastec.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepeatableReflectionTest {
    @Test void expandsRepeatedAnnotationsAndPreservesJavaInheritanceRules() throws Exception {
        byte[] wasm=JavacToWasm.compile("RepeatedApp.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @Inherited @Repeatable(Tags.class)
            @interface Tag { String value(); }
            @Retention(RetentionPolicy.RUNTIME) @Inherited
            @interface Tags { Tag[] value(); String note() default "default"; }
            @Tag("first") @Tag("second") class Base {}
            class Child extends Base {}
            @Tag("own") class Single extends Base {}
            @Tags({}) class Empty extends Base {}
            @Tag("direct") @Tags({@Tag("nested1"),@Tag("nested2")}) class Mixed {}
            @Tag("iface1") @Tag("iface2") interface Tagged {}
            class Implementor implements Tagged {}
            class Model {
                @Tag("field1") @Tag("field2") public int value;
                @Tag("ctor1") @Tag("ctor2") public Model() {}
                @Tag("method1") @Tag("method2") public void read() {}
            }
            record Data(@Tag("component1") @Tag("component2") int value) {}
            class RepeatedApp {
                public static int test() {
                    Tag[] tags=Base.class.getDeclaredAnnotationsByType(Tag.class);
                    if(tags.length!=2 || !tags[0].value().equals("first") || !tags[1].value().equals("second")) return -1;
                    if(Base.class.getDeclaredAnnotation(Tag.class)!=null || Base.class.isAnnotationPresent(Tag.class)) return -2;
                    Tags container=Base.class.getDeclaredAnnotation(Tags.class);
                    if(container==null || container.value().length!=2 || !container.note().equals("default")) return -3;
                    if(Base.class.getDeclaredAnnotations().length!=1) return -4;
                    if(Child.class.getDeclaredAnnotationsByType(Tag.class).length!=0 || Child.class.getAnnotationsByType(Tag.class).length!=2) return -5;
                    if(Child.class.getDeclaredAnnotation(Tags.class)!=null || Child.class.getAnnotation(Tags.class)==null) return -6;
                    if(Single.class.getAnnotationsByType(Tag.class).length!=1 || !Single.class.getAnnotationsByType(Tag.class)[0].value().equals("own")) return -7;
                    if(Empty.class.getDeclaredAnnotationsByType(Tag.class).length!=0 || Empty.class.getAnnotationsByType(Tag.class).length!=2) return -8;
                    if(Implementor.class.getAnnotationsByType(Tag.class).length!=0) return -9;
                    Tag[] mixed=Mixed.class.getAnnotationsByType(Tag.class);
                    if(mixed.length!=3 || !mixed[0].value().equals("direct") || !mixed[2].value().equals("nested2")) return -10;
                    if(!Model.class.getField("value").getAnnotationsByType(Tag.class)[1].value().equals("field2")) return -11;
                    if(!Model.class.getConstructor().getDeclaredAnnotationsByType(Tag.class)[0].value().equals("ctor1")) return -12;
                    Method read=Model.class.getMethod("read");
                    if(read.getAnnotation(Tag.class)!=null || read.getDeclaredAnnotation(Tags.class)==null || read.getAnnotationsByType(Tag.class).length!=2) return -13;
                    if(Data.class.getRecordComponents()[0].getDeclaredAnnotationsByType(Tag.class).length!=2) return -14;
                    tags[0]=null; if(Base.class.getDeclaredAnnotationsByType(Tag.class)[0]==null) return -15;
                    try { Base.class.getAnnotationsByType(null); return -16; } catch(NullPointerException expected) {}
                    try { read.getDeclaredAnnotationsByType(null); return -17; } catch(NullPointerException expected) {}
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }


    @Test void exposesRuntimeContainersForClassRetainedElements() throws Exception {
        byte[] wasm=JavacToWasm.compile("RetentionApp.java","""
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.CLASS) @Repeatable(Notes.class) @interface Note { int value(); }
            @Retention(RetentionPolicy.RUNTIME) @interface Notes { Note[] value(); }
            @Note(1) @Note(2) class Repeated {}
            @Note(3) class Single {}
            class RetentionApp {
                public static int test() {
                    if(Repeated.class.getDeclaredAnnotation(Note.class)!=null) return -1;
                    Note[] notes=Repeated.class.getDeclaredAnnotationsByType(Note.class);
                    if(notes.length!=2 || notes[0].value()!=1 || notes[1].value()!=2) return -2;
                    if(Single.class.getAnnotationsByType(Note.class).length!=0) return -3;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void diagnosesInvalidRepeatableContainersAndDuplicateUses() {
        rejects("""
            @Retention(RetentionPolicy.RUNTIME) @Repeatable(Tags.class) @interface Tag {}
            @Retention(RetentionPolicy.RUNTIME) @interface Tags { String[] value(); }
            @Tag @Tag class Model {}
            ""","container value() must return Tag[]");
        rejects("""
            @Retention(RetentionPolicy.RUNTIME) @interface Tag {}
            @Tag @Tag class Model {}
            ""","requires @Repeatable");
        rejects("""
            @Retention(RetentionPolicy.RUNTIME) @Repeatable(Tags.class) @interface Tag {}
            @Retention(RetentionPolicy.RUNTIME) @interface Tags { Tag[] value(); }
            @Tag @Tag @Tags({}) class Model {}
            ""","explicit container");
        rejects("""
            @Retention(RetentionPolicy.RUNTIME) @Repeatable(Tags.class) @interface Tag {}
            @interface Tags { Tag[] value(); }
            @Tag @Tag class Model {}
            ""","runtime-retained container");
    }

    private static void rejects(String source,String diagnostic) {
        var error=assertThrows(IllegalArgumentException.class,()->JavacToWasm.compile("BadRepeat.java",
                "import java.lang.annotation.*; "+source+" class App { static void run() { Model.class.getAnnotations(); } }",false));
        assertTrue(error.getMessage().contains(diagnostic),error.getMessage());
        assertTrue(error.getMessage().contains("BadRepeat.java:"),error.getMessage());
    }
}
