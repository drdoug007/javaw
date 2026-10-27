package one.dastec.javaw;

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
                    if(((Integer)arrays.invoke(model,array)).intValue()!=2) return -11;
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

    @Test void checksArrayComponentTypesAndPreservesCopies() throws Exception {
        byte[] wasm=JavacToWasm.compile("ArrayReflection.java","""
            import java.lang.reflect.*;
            import java.util.Arrays;
            class Animal {}
            class Dog extends Animal {}
            enum Kind { ONE, TWO }
            class ArrayBox {
                public int[] values;
                public Animal[] animals;
                public ArrayBox(int[] values) { this.values=values; }
                public static int integers(int[] values) { return values[0]+values.length; }
                public static int references(Animal[] values) { return values.length; }
                public static int objects(Object[] values) { return values.length; }
                public static long nested(long[][] values) { return values[0][0]+(long)values.length; }
                public static int varargs(int... values) { return values[0]+values.length; }
                public static int kinds(Kind[] values) { return values.length; }
                public static int strings(String[] values) { return values[0].length(); }
                public static double doubles(double[] values) { return values[0]; }
                public static int chars(char[] values) { return values.length; }
                public static int bytes(byte[] values) { return values.length; }
            }
            class ArrayReflection {
                public static int test() {
                    Method ints=ArrayBox.class.getMethod("integers",int[].class);
                    Object input=new int[]{40,1};
                    if(((Integer)ints.invoke(null,input)).intValue()!=42) return -1;
                    Object copy=Arrays.copyOf((int[])input,2);
                    if(((Integer)ints.invoke(null,copy)).intValue()!=42) return -2;
                    Object range=Arrays.copyOfRange((int[])input,0,2);
                    if(((Integer)ints.invoke(null,range)).intValue()!=42) return -3;
                    try { ints.invoke(null,(Object)new long[]{40,1}); return -4; } catch(IllegalArgumentException expected) {}
                    try { ints.invoke(null,"bad"); return -5; } catch(IllegalArgumentException expected) {}
                    try { ints.invoke(null,(Object)new Integer[]{40,1}); return -6; } catch(IllegalArgumentException expected) {}
                    ArrayBox box=(ArrayBox)ArrayBox.class.getConstructor(int[].class).newInstance(input);
                    Field field=ArrayBox.class.getField("values");
                    field.set(box,copy); if(field.get(box)!=copy) return -7;
                    try { field.set(box,new short[]{1}); return -8; } catch(IllegalArgumentException expected) {}
                    Method refs=ArrayBox.class.getMethod("references",Animal[].class);
                    Object dogs=new Dog[]{new Dog()};
                    if(((Integer)refs.invoke(null,dogs)).intValue()!=1) return -9;
                    Field animals=ArrayBox.class.getField("animals"); animals.set(box,dogs);
                    if(animals.get(box)!=dogs) return -10;
                    try { refs.invoke(null,(Object)new String[]{"bad"}); return -11; } catch(IllegalArgumentException expected) {}
                    Method objects=ArrayBox.class.getMethod("objects",Object[].class);
                    if(((Integer)objects.invoke(null,(Object)new int[][]{new int[]{1}})).intValue()!=1) return -12;
                    try { objects.invoke(null,input); return -13; } catch(IllegalArgumentException expected) {}
                    Object nested=new long[][]{new long[]{41}};
                    if(((Long)ArrayBox.class.getMethod("nested",long[][].class).invoke(null,nested)).longValue()!=42L) return -14;
                    if(((Integer)ArrayBox.class.getMethod("varargs",int[].class).invoke(null,input)).intValue()!=42) return -15;
                    try { ArrayBox.class.getMethod("varargs",int[].class).invoke(null,40,1); return -16; } catch(IllegalArgumentException expected) {}
                    if(((Integer)ArrayBox.class.getMethod("kinds",Kind[].class).invoke(null,(Object)Kind.values())).intValue()!=2) return -17;
                    int[] literal={40,1};
                    if(((Integer)ints.invoke(null,(Object)literal)).intValue()!=42) return -18;
                    Object chars="hi".toCharArray(), bytes="hi".getBytes();
                    if(((Integer)ArrayBox.class.getMethod("chars",char[].class).invoke(null,chars)).intValue()!=2) return -19;
                    if(((Integer)ArrayBox.class.getMethod("bytes",byte[].class).invoke(null,bytes)).intValue()!=2) return -20;
                    try { ArrayBox.class.getMethod("chars",char[].class).invoke(null,bytes); return -21; } catch(IllegalArgumentException expected) {}
                    return 42;
                }
                public static int hostInts(Object input) {
                    try { return ((Integer)ArrayBox.class.getMethod("integers",int[].class).invoke(null,input)).intValue(); }
                    catch(IllegalArgumentException expected) { return -1; }
                }
                public static int hostStrings(Object input) {
                    return ((Integer)ArrayBox.class.getMethod("strings",String[].class).invoke(null,input)).intValue();
                }
                public static double hostDoubles(Object input) {
                    return ((Double)ArrayBox.class.getMethod("doubles",double[].class).invoke(null,input)).doubleValue();
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"""
            assert.equal(e.test(),42);
            const runtime=new globalThis.JavawRuntime();
            runtime.setInstance({exports:e});
            const ints=runtime.writeIntArray([40,1]);
            assert.equal(e.hostInts(ints),42);
            const doubles=runtime.writeDoubleArray([42.5]);
            assert.equal(e.hostDoubles(doubles),42.5);
            assert.equal(e.hostInts(doubles),-1);
            const strings=runtime.writeStringArray(["hello"]);
            assert.equal(e.hostStrings(strings),5);
            assert.equal(e.hostInts(strings),-1);
            const untyped=e.__javaw_array_bytes_alloc(12);
            new DataView(e.memory.buffer).setInt32(untyped,2,true);
            assert.equal(e.hostInts(untyped),-1);
            e.__gc_retain(ints); e.__gc_collect();
            assert.equal(e.hostInts(ints),42);
            e.__gc_release(ints); e.__gc_collect();
            const reused=runtime.writeDoubleArray([42.5]);
            assert.equal(e.hostInts(reused),-1);
            """);
    }

    @Test void reflectsEnumFieldsMethodsAndForbiddenConstruction() throws Exception {
        byte[] wasm=JavacToWasm.compile("EnumReflection.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @interface Label { String value(); }
            interface TaggedTone { public default int marker() { return 5; } }
            @Label("enum") enum Tone implements TaggedTone {
                @Label("constant") LOW(2), HIGH(9);
                private int level;
                Tone(int level) { this.level=level; }
                public int level() { return level; }
                public void fail() { throw new IllegalStateException(); }
            }
            class EnumReflection {
                public static int test() {
                    Field low=Tone.class.getDeclaredField("LOW");
                    if(!low.isEnumConstant() || !Modifier.isPublic(low.getModifiers()) || !Modifier.isStatic(low.getModifiers()) || !Modifier.isFinal(low.getModifiers())) return -1;
                    if(low.get(null)!=Tone.LOW || low.getType()!=Tone.class) return -2;
                    if(!low.getAnnotation(Label.class).value().equals("constant")) return -3;
                    if(!Tone.class.getAnnotation(Label.class).value().equals("enum")) return -4;
                    if(((Integer)Tone.class.getMethod("level").invoke(Tone.HIGH)).intValue()!=9) return -5;
                    Field level=Tone.class.getDeclaredField("level"); level.setAccessible(true); level.setInt(Tone.HIGH,10);
                    if(level.getInt(Tone.HIGH)!=10 || level.isEnumConstant()) return -6;
                    try { low.set(null,Tone.HIGH); return -7; } catch(IllegalAccessException expected) {}
                    Object[] values=(Object[])Tone.class.getMethod("values").invoke(null);
                    if(values.length!=2 || values[0]!=Tone.LOW || values[1]!=Tone.HIGH) return -8;
                    if(Tone.class.getMethod("valueOf",String.class).invoke(null,"HIGH")!=Tone.HIGH) return -9;
                    if(Tone.class.getConstructors().length!=0) return -10;
                    Constructor<?> ctor=Tone.class.getDeclaredConstructor(String.class,int.class,int.class);
                    if(!Modifier.isPrivate(ctor.getModifiers())) return -11;
                    ctor.setAccessible(true);
                    try { ctor.newInstance("OTHER",2,3); return -12; } catch(IllegalArgumentException expected) {}
                    try { Tone.class.getMethod("fail").invoke(Tone.LOW); return -13; } catch(InvocationTargetException expected) {}
                    if(Tone.class.getRecordComponents()!=null || Tone.class.isRecord()) return -14;
                    if(((Integer)Tone.class.getMethod("marker").invoke(Tone.LOW)).intValue()!=5) return -19;
                    try { Tone.class.getMethod("valueOf",String.class).invoke(null,"missing"); return -15; }
                    catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalArgumentException)) return -16; }
                    try { Tone.class.getMethod("valueOf",String.class).invoke(null,(Object)null); return -17; }
                    catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof NullPointerException)) return -18; }
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void constructsAndReflectsObjectBackedRecords() throws Exception {
        byte[] wasm=JavacToWasm.compile("RecordReflection.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @interface Mark { String value(); }
            @Mark("record") record Point(@Mark("component") int x,String label) {
                Point { if(x<0) throw new IllegalArgumentException(); }
                public Point(int x) { this(x,"default"); }
                public int twice() { return x*2; }
            }
            record Empty() {}
            class RecordReflection {
                public static int test() {
                    if(!Point.class.isRecord() || Empty.class.getRecordComponents().length!=0) return -1;
                    Constructor<?> ctor=Point.class.getDeclaredConstructor(int.class,String.class); ctor.setAccessible(true);
                    Point point=(Point)ctor.newInstance(21,"Ada");
                    if(point.x()!=21 || !point.label().equals("Ada")) return -2;
                    if(((Integer)Point.class.getMethod("twice").invoke(point)).intValue()!=42) return -3;
                    if(((Integer)Point.class.getMethod("x").invoke(point)).intValue()!=21) return -4;
                    Field x=Point.class.getDeclaredField("x"); x.setAccessible(true);
                    if(x.getInt(point)!=21 || !Modifier.isFinal(x.getModifiers())) return -5;
                    try { x.setInt(point,99); return -6; } catch(IllegalAccessException expected) {}
                    RecordComponent[] components=Point.class.getRecordComponents();
                    if(components.length!=2 || !components[0].getName().equals("x") || components[0].getType()!=int.class || components[0].getDeclaringRecord()!=Point.class) return -7;
                    if(((Integer)components[0].getAccessor().invoke(point)).intValue()!=21) return -8;
                    if(!components[0].getAnnotation(Mark.class).value().equals("component")) return -9;
                    if(!Point.class.getAnnotation(Mark.class).value().equals("record")) return -10;
                    if(!point.equals(new Point(21,"Ada")) || point.hashCode()!=new Point(21,"Ada").hashCode()) return -11;
                    if(point.equals(new Point(22,"Ada")) || point.equals(null)) return -12;
                    if(!point.toString().equals("Point[x=21, label=Ada]")) return -13;
                    if(((Integer)Point.class.getMethod("hashCode").invoke(point)).intValue()!=point.hashCode()) return -14;
                    if(!((Boolean)Point.class.getMethod("equals",Object.class).invoke(point,new Point(21,"Ada"))).booleanValue()) return -15;
                    if(Point.class.getConstructor(int.class).newInstance(2)==null) return -16;
                    try { ctor.newInstance(-1,"bad"); return -17; } catch(InvocationTargetException expected) { if(!(expected.getCause() instanceof IllegalArgumentException)) return -18; }
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void preservesRecordConstructorsOverridesAndAnnotationTargets() throws Exception {
        byte[] wasm=JavacToWasm.compile("RecordContracts.java","""
            import java.lang.annotation.*;
            import java.lang.reflect.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface ComponentOnly {}
            @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @interface Property {}
            public record Value(@ComponentOnly @Property int amount) {
                public Value(int amount) { this.amount=amount+1; }
                public int amount() { return amount+10; }
                public String toString() { return "custom"; }
            }
            public record Generic<T>(T value) {}
            public record Many(int... values) {}
            public record Pair(double number,String label) {}
            class RecordContracts {
                public static int test() {
                    Value value=(Value)Value.class.getConstructor(int.class).newInstance(1);
                    if(value.amount()!=12 || !value.toString().equals("custom")) return -1;
                    Field field=Value.class.getDeclaredField("amount"); field.setAccessible(true);
                    if(field.getInt(value)!=2 || !field.isAnnotationPresent(Property.class) || field.isAnnotationPresent(ComponentOnly.class)) return -2;
                    RecordComponent component=Value.class.getRecordComponents()[0];
                    if(!component.isAnnotationPresent(ComponentOnly.class) || component.isAnnotationPresent(Property.class)) return -3;
                    if(component.getAccessor().isAnnotationPresent(Property.class)) return -4;
                    Generic generic=(Generic)Generic.class.getConstructor(Object.class).newInstance("hello");
                    if(!generic.value().equals("hello")) return -5;
                    if(Generic.class.getRecordComponents()[0].getType()!=Object.class) return -6;
                    Object array=new int[]{1,2};
                    Many many=(Many)Many.class.getConstructor(int[].class).newInstance(array);
                    if(many.values()!=array || !Many.class.getConstructor(int[].class).isVarArgs()) return -7;
                    double nan=0.0/0.0; Pair first=new Pair(nan,null), second=new Pair(nan,null);
                    if(!first.equals(second) || first.hashCode()!=second.hashCode()) return -8;
                    if(new Pair(0.0,null).equals(new Pair(-0.0,null))) return -9;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void diagnosesEnumConstantBodiesForReflection() {
        var error=assertThrows(IllegalArgumentException.class,()->JavacToWasm.compile("EnumBody.java",
                "enum Mode { ONE { public int code() { return 1; } }; } class App { static void run() { Mode.class.getMethods(); } }",false));
        assertTrue(error.getMessage().contains("enum constant-specific class body"),error.getMessage());
    }
}
