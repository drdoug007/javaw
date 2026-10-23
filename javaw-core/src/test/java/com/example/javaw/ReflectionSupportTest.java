package com.example.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionSupportTest {
    @Test void constructsReadsWritesAndInvokesLinkedSourceMembers() throws Exception {
        byte[] wasm=JavacToWasm.compile("ReflectionApp.java","""
            import java.lang.reflect.*;
            class Person {
                public int age;
                private String name;
                public static int visits;
                public final int fixed=3;
                public Person(int age,String name) { this.age=age; this.name=name; }
                public int add(int amount) { return age+amount; }
                public long add(long amount) { return age+amount; }
                private String secret() { return name; }
                public static int visit(int amount) { visits+=amount; return visits; }
                public void fail() { throw new IllegalStateException("target"); }
            }
            class Child extends Person {
                public Child() { super(10,"child"); }
                public int add(int amount) { return age+amount+100; }
            }
            class ReflectionApp {
                public static int test() {
                    Class<?> type=Person.class;
                    Constructor<?> constructor=type.getDeclaredConstructor(int.class,String.class);
                    Person person=(Person)constructor.newInstance(20,"Ada");
                    Field age=type.getDeclaredField("age");
                    if(age.getType()!=int.class || age.getDeclaringClass()!=type) return -1;
                    age.set(person,Byte.valueOf((byte)22)); if(age.getInt(person)!=22 || age.getLong(person)!=22L) return -2;
                    Method add=type.getDeclaredMethod("add",int.class);
                    if(((Integer)add.invoke(person,3)).intValue()!=25) return -3;
                    if(((Integer)add.invoke(new Child(),2)).intValue()!=112) return -4;
                    if(((Long)type.getDeclaredMethod("add",long.class).invoke(person,3)).longValue()!=25L) return -5;
                    Field name=type.getDeclaredField("name");
                    try { name.get(person); return -6; } catch(IllegalAccessException expected) {}
                    name.setAccessible(true); if(!((String)name.get(person)).equals("Ada")) return -7;
                    name.set(person,"Grace");
                    Method secret=type.getDeclaredMethod("secret"); secret.setAccessible(true);
                    if(!((String)secret.invoke(person)).equals("Grace")) return -8;
                    if(((Integer)type.getDeclaredMethod("visit",int.class).invoke(null,4)).intValue()!=4) return -9;
                    if(type.getDeclaredField("visits").getInt(null)!=4) return -10;
                    try { add.invoke(person,"bad"); return -11; } catch(IllegalArgumentException expected) {}
                    try { add.invoke(null,2); return -12; } catch(NullPointerException expected) {}
                    try { add.invoke(person); return -13; } catch(IllegalArgumentException expected) {}
                    try { type.getDeclaredMethod("fail").invoke(person); return -14; }
                    catch(InvocationTargetException expected) { if(!(expected.getTargetException() instanceof IllegalStateException)) return -15; }
                    try { type.getDeclaredField("fixed").setInt(person,9); return -16; } catch(IllegalAccessException expected) {}
                    try { type.getDeclaredField("absent"); return -17; } catch(NoSuchFieldException expected) {}
                    try { type.getDeclaredMethod("add",double.class); return -18; } catch(NoSuchMethodException expected) {}
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
    @Test void exposesRuntimeAnnotationValuesAndLinkedClassLookup() throws Exception {
        byte[] wasm=JavacToWasm.compile("AnnotatedApp.java","""
            package demo;
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Inherited
            @interface Label { String value(); int rank() default 7; }
            @Retention(RetentionPolicy.SOURCE)
            @interface CompileOnly { String value(); }
            @Label("model") @CompileOnly("hidden")
            class AnnotatedModel {
                @Label(value="field",rank=9) public int number;
                @Label("constructor") public AnnotatedModel() {}
                @Label("method") public int read() { return number; }
            }
            class AnnotatedApp {
                public static int metadata() {
                    Label label=AnnotatedModel.class.getAnnotation(Label.class);
                    if(label==null || !label.value().equals("model") || label.rank()!=7) return -1;
                    if(label.annotationType()!=Label.class) return -2;
                    if(AnnotatedModel.class.getDeclaredAnnotations().length!=1) return -3;
                    if(AnnotatedModel.class.isAnnotationPresent(CompileOnly.class)) return -4;
                    Label field=AnnotatedModel.class.getDeclaredField("number").getAnnotation(Label.class);
                    if(field.rank()!=9 || !field.value().equals("field")) return -5;
                    if(!AnnotatedModel.class.getDeclaredMethod("read").getAnnotation(Label.class).value().equals("method")) return -6;
                    if(!AnnotatedModel.class.getDeclaredConstructor().getAnnotation(Label.class).value().equals("constructor")) return -7;
                    if(Class.forName("demo.AnnotatedModel")!=AnnotatedModel.class) return -8;
                    if(Class.forName("AnnotatedModel")!=AnnotatedModel.class) return -9;
                    try { Class.forName("missing.Type"); return -10; } catch(ClassNotFoundException expected) {}
                    try { Class.forName(null); return -11; } catch(NullPointerException expected) {}
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.metadata(),42);");
    }


    @Test void supportsInheritedNestedAndArrayAnnotations() throws Exception {
        byte[] wasm=JavacToWasm.compile("MetadataApp.java","""
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @interface Nested { String value(); }
            @Retention(RetentionPolicy.RUNTIME) @Inherited @Target({ElementType.TYPE})
            @interface Settings { int[] numbers() default {2,3}; Nested nested() default @Nested("ok"); }
            @Settings class Base {}
            class Derived extends Base {}
            @Settings(numbers=4) class Override extends Base {}
            @Settings interface Tagged {}
            class Implementor implements Tagged {}
            @Deprecated(since="1",forRemoval=true) class Old {}
            class MetadataApp {
                public static int test() {
                    Settings a=Base.class.getAnnotation(Settings.class);
                    Settings b=Derived.class.getAnnotation(Settings.class);
                    if(!a.equals(b) || a.hashCode()!=b.hashCode()) return -1;
                    int[] copy=a.numbers(); copy[0]=99;
                    if(a.numbers()[0]!=2 || !a.nested().value().equals("ok")) return -2;
                    if(Derived.class.getDeclaredAnnotations().length!=0) return -3;
                    if(Override.class.getAnnotation(Settings.class).numbers()[0]!=4) return -4;
                    if(Implementor.class.getAnnotation(Settings.class)!=null) return -5;
                    if(Settings.class.getAnnotation(Retention.class).value()!=RetentionPolicy.RUNTIME) return -6;
                    if(Settings.class.getAnnotation(Target.class).value()[0]!=ElementType.TYPE) return -7;
                    Deprecated old=Old.class.getAnnotation(Deprecated.class);
                    if(!old.since().equals("1") || !old.forRemoval()) return -8;
                    if(!Settings.class.isAnnotation() || !int.class.isPrimitive() || Base.class.isPrimitive()) return -9;
                    if(!int[].class.isArray() || Base.class.isArray()) return -10;
                    if(Class.forName("java.lang.reflect.Field")!=java.lang.reflect.Field.class) return -11;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void enforcesAccessConversionsAndArrayBoundary() throws Exception {
        byte[] wasm=JavacToWasm.compile("BoundaryApp.java","""
            import java.lang.reflect.*;
            import java.util.Arrays;
            abstract class AbstractModel { public AbstractModel() {} }
            class ParentModel { public int inherited; private int hidden; public int read() { return 1; } }
            class Model extends ParentModel {
                private Model() {}
                public Model(int unused) { throw new IllegalStateException(); }
                public String text(String value) { return value; }
                public int array(int[] values) { return values==null?7:values.length; }
                public double number(double value) { return value; }
            }
            class BoundaryApp {
                public static int test() {
                    Constructor<?> ctor=Model.class.getDeclaredConstructor();
                    try { ctor.newInstance(); return -1; } catch(IllegalAccessException expected) {}
                    ctor.setAccessible(true); Model model=(Model)ctor.newInstance();
                    if(Model.class.getField("inherited").getDeclaringClass()!=ParentModel.class) return -2;
                    try { Model.class.getField("hidden"); return -3; } catch(NoSuchFieldException expected) {}
                    if(((Integer)Model.class.getMethod("read").invoke(model)).intValue()!=1) return -4;
                    try { AbstractModel.class.getConstructor().newInstance(); return -5; } catch(InstantiationException expected) {}
                    try { Model.class.getConstructor(int.class).newInstance(1); return -6; }
                    catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalStateException)) return -7; }
                    Method text=Model.class.getMethod("text",String.class);
                    try { text.invoke(model,23); return -8; } catch(IllegalArgumentException expected) {}
                    Object array=new int[]{1,2};
                    try { text.invoke(model,array); return -9; } catch(IllegalArgumentException expected) {}
                    Object copied=Arrays.copyOf(new int[]{1},1);
                    try { text.invoke(model,copied); return -10; } catch(IllegalArgumentException expected) {}
                    Method arrays=Model.class.getMethod("array",int[].class);
                    try { arrays.invoke(model,array); return -11; } catch(UnsupportedOperationException expected) {}
                    if(((Integer)arrays.invoke(model,(Object)null)).intValue()!=7) return -12;
                    Method number=Model.class.getMethod("number",double.class);
                    if(((Double)number.invoke(model,Byte.valueOf((byte)3))).doubleValue()!=3.0) return -13;
                    try { number.invoke(model,(Object)null); return -14; } catch(IllegalArgumentException expected) {}
                    Class<?>[] parameters=number.getParameterTypes(); parameters[0]=int.class;
                    if(number.getParameterTypes()[0]!=double.class || number.getReturnType()!=double.class) return -15;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void preservesNestedArrayAndVarargsAllocations() throws Exception {
        byte[] wasm=JavacToWasm.compile("NestedArrays.java","""
            class NestedArrays {
                static int sum(int... values) { return values[0]+values[1]; }
                static int combine(int... values) { return values[0]*10+values[1]; }
                public static int test() {
                    int[][] values=new int[][]{new int[]{1,2},new int[]{3,4}};
                    return values[0][1]+values[1][0]+combine(sum(1,2),sum(3,4));
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void diagnosesDynamicLoading() {
        var loader=assertThrows(IllegalArgumentException.class,()->JavacToWasm.compile("Loader.java",
            "class Loader { ClassLoader loader; }",false));
        assertTrue(loader.getMessage().contains("Unsupported dynamic class loading"));
        var overload=assertThrows(IllegalArgumentException.class,()->JavacToWasm.compile("Loader.java",
            "class Loader { static Class<?> load() { return Class.forName(\"X\",false,null); } }",false));
        assertTrue(overload.getMessage().contains("Unsupported Class.forName overload"));
    }
}


