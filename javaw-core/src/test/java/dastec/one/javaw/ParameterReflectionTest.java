package dastec.one.javaw;

import org.junit.jupiter.api.Test;

class ParameterReflectionTest {
    @Test void exposesExecutableParameterAnnotationsNamesAndTypes() throws Exception {
        byte[] wasm=JavacToWasm.compile("ParameterApp.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) @Repeatable(Marks.class)
            @interface Mark { String value(); }
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
            @interface Marks { Mark[] value(); }
            @Retention(RetentionPolicy.SOURCE) @Target(ElementType.PARAMETER) @interface SourceOnly {}
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface TypeOnly {}
            class Parent {
                public void inherited(@Mark("parent") int amount) {}
                public void replaced(@Mark("old") int amount) {}
            }
            class Model extends Parent {
                public Model(@Mark("ctor") int value) {}
                public void replaced(int amount) {}
                public static void inspect(int plain,@Mark("first") @Mark("second") final String title,@SourceOnly int hidden,@TypeOnly String typeUse,@Mark("varargs") int... values) {}
            }
            class ParameterApp {
                public static int test() {
                    Method method=Model.class.getMethod("inspect",int.class,String.class,int.class,String.class,int[].class);
                    Executable executable=method;
                    Annotation[][] rows=executable.getParameterAnnotations();
                    if(rows.length!=5 || rows[0].length!=0 || rows[1].length!=1 || rows[2].length!=0 || rows[3].length!=0 || rows[4].length!=1) return -1;
                    if(!(rows[1][0] instanceof Marks) || ((Marks)rows[1][0]).value().length!=2) return -2;
                    Parameter[] parameters=executable.getParameters();
                    if(parameters.length!=5 || !parameters[1].getName().equals("title") || !parameters[1].isNamePresent()) return -3;
                    if(parameters[1].getType()!=String.class || parameters[4].getType()!=int[].class || !Modifier.isFinal(parameters[1].getModifiers())) return -4;
                    if(parameters[1].getDeclaringExecutable()!=method || parameters[1].isSynthetic() || parameters[1].isImplicit()) return -5;
                    if(parameters[1].isVarArgs() || !parameters[4].isVarArgs()) return -6;
                    Mark[] marks=parameters[1].getAnnotationsByType(Mark.class);
                    if(marks.length!=2 || !marks[0].value().equals("first") || !marks[1].value().equals("second")) return -7;
                    if(parameters[1].getDeclaredAnnotation(Mark.class)!=null || !parameters[1].isAnnotationPresent(Marks.class)) return -8;
                    if(!parameters[4].getAnnotation(Mark.class).value().equals("varargs")) return -9;
                    if(parameters[3].getAnnotation(TypeOnly.class)!=null || parameters[2].getAnnotation(SourceOnly.class)!=null) return -10;
                    rows[1][0]=null; rows[0]=null;
                    if(executable.getParameterAnnotations()[1][0]==null || executable.getParameterAnnotations()[0]==null) return -11;
                    Parameter original=parameters[1]; parameters[1]=null;
                    Parameter again=Model.class.getMethod("inspect",int.class,String.class,int.class,String.class,int[].class).getParameters()[1];
                    if(!again.equals(original) || again.hashCode()!=original.hashCode() || again.equals(executable.getParameters()[0])) return -12;
                    if(!Model.class.getConstructor(int.class).getParameters()[0].getAnnotation(Mark.class).value().equals("ctor")) return -13;
                    if(!Model.class.getMethod("inherited",int.class).getParameters()[0].getAnnotation(Mark.class).value().equals("parent")) return -14;
                    if(Model.class.getMethod("replaced",int.class).getParameterAnnotations()[0].length!=0) return -15;
                    try { original.getDeclaredAnnotationsByType(null); return -16; } catch(NullPointerException expected) {}
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void alignsEnumSyntheticParametersAndRecordCanonicalParameters() throws Exception {
        byte[] wasm=JavacToWasm.compile("SpecialParameters.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PARAMETER,ElementType.RECORD_COMPONENT}) @Repeatable(Names.class)
            @interface Name { String value(); }
            @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PARAMETER,ElementType.RECORD_COMPONENT})
            @interface Names { Name[] value(); }
            enum Level {
                LOW(1);
                Level(@Name("level") int value) {}
            }
            public record Point(@Name("x1") @Name("x2") int x,String label) {}
            public record Checked(@Name("checked") int value) { public Checked { if(value<0) throw new IllegalArgumentException(); } }
            class SpecialParameters {
                public static int test() {
                    Constructor<?> ctor=Level.class.getDeclaredConstructor(String.class,int.class,int.class);
                    Annotation[][] rows=ctor.getParameterAnnotations();
                    if(rows.length!=3 || rows[0].length!=0 || rows[1].length!=0 || rows[2].length!=1) return -1;
                    Parameter[] parameters=ctor.getParameters();
                    if(!parameters[0].isSynthetic() || !parameters[1].isSynthetic() || parameters[2].isSynthetic()) return -2;
                    if(parameters[0].isNamePresent() || !parameters[0].getName().equals("arg0") || parameters[0].getType()!=String.class || parameters[1].getType()!=int.class) return -3;
                    if(!parameters[2].getName().equals("value") || !parameters[2].getAnnotation(Name.class).value().equals("level")) return -4;
                    Parameter factory=Level.class.getMethod("valueOf",String.class).getParameters()[0];
                    if(!factory.isImplicit() || factory.isSynthetic() || !factory.getName().equals("name")) return -5;
                    Constructor<?> point=Point.class.getConstructor(int.class,String.class);
                    Parameter x=point.getParameters()[0];
                    if(!x.getName().equals("x") || !x.isNamePresent() || x.getAnnotationsByType(Name.class).length!=2) return -6;
                    if(point.getParameterAnnotations()[1].length!=0 || x.getDeclaringExecutable()!=point) return -7;
                    if(Point.class.getRecordComponents()[0].getAnnotationsByType(Name.class).length!=2) return -8;
                    if(Checked.class.getConstructor(int.class).getParameters()[0].getAnnotation(Name.class)==null) return -9;
                    if(Point.class.getMethod("x").getParameters().length!=0 || Point.class.getMethod("x").getParameterAnnotations().length!=0) return -10;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
}
