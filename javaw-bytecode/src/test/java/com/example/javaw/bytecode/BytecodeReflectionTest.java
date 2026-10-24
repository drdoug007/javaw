package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeReflectionTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        options.add(file.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    @Test void preservesProtectedReceiversAndHiddenConstructorParameters() throws Exception {
        Path base=directory.resolve("Base.java"),sub=directory.resolve("Sub.java");
        Files.writeString(base,"package p; public class Base {protected int value=7;protected int method(){return value;}private int secret(){return 3;}}");
        Files.writeString(sub,"""
            package q;import java.lang.reflect.*;
            public class Sub extends p.Base {
                public static int test() throws Exception {
                    Method method=p.Base.class.getDeclaredMethod("method");Field field=p.Base.class.getDeclaredField("value");Sub sub=new Sub();
                    int score=(Integer)method.invoke(sub)+(Integer)field.get(sub);
                    try{method.invoke(new p.Base());}catch(IllegalAccessException expected){score+=100;}
                    try{field.get(new p.Base());}catch(IllegalAccessException expected){score+=1000;}
                    Method secret=p.Base.class.getDeclaredMethod("secret");if(!secret.canAccess(sub))score+=10000;
                    return score;
                }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",directory.toString(),base.toString(),sub.toString()));
        fixtureClasspath=directory.toString();
        fixture("""
            import java.lang.annotation.*;import java.lang.reflect.*;
            public class Metadata {
                @Retention(RetentionPolicy.RUNTIME) @interface Mark {}
                class Inner {Inner(@Mark int value){}}
                public static int run() throws Exception {
                    Constructor<?> constructor=Inner.class.getDeclaredConstructor(Metadata.class,int.class);
                    int score=q.Sub.test();if(constructor.getParameterAnnotations()[0].length==0 && constructor.getParameterAnnotations()[1].length==1)score+=100000;
                    if(constructor.getAnnotatedReceiverType().getType()==Metadata.class)score+=1000000;
                    if(Metadata.class.getDeclaredConstructor().getAnnotatedReceiverType()==null)score+=10000000;
                    return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void executesIsolatedUnchangedPinnedSpringReflectionInvocationBodies() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));Path artifact=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertEquals("476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import org.springframework.util.ReflectionUtils;import java.lang.reflect.*;
            public class Metadata {
                static RuntimeException runtime=new IllegalArgumentException("target");static Error error=new Error("target");static Exception checked=new Exception("target");
                public static long target(int mode,long value) throws Exception {if(mode==1)throw runtime;if(mode==2)throw error;if(mode==3)throw checked;return value+7;}
                public static int run() throws Exception {
                    Method method=Metadata.class.getMethod("target",int.class,long.class);int score=((Long)ReflectionUtils.invokeMethod(method,null,0,11L))==18?1:0;
                    try{ReflectionUtils.invokeMethod(method,null,1,0L);}catch(RuntimeException failure){if(failure==runtime)score+=10;}
                    try{ReflectionUtils.invokeMethod(method,null,2,0L);}catch(Error failure){if(failure==error)score+=100;}
                    try{ReflectionUtils.invokeMethod(method,null,3,0L);}catch(UndeclaredThrowableException failure){if(failure.getUndeclaredThrowable()==checked)score+=1000;}
                    return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{artifact.toUri().toURL(),directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        // Only this caller's method closure is selected. Instruction bodies, handlers and source lines are copied unchanged.
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/springframework/util/ReflectionUtils.class")).readAllBytes()).accept(original,0);
            ClassNode selected=new ClassNode();selected.version=original.version;selected.access=original.access;selected.name=original.name;selected.superName=original.superName;selected.sourceFile=original.sourceFile;
            for(MethodNode method:original.methods)if(method.name.equals("invokeMethod") && method.desc.equals("(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;") || Set.of("handleReflectionException","handleInvocationTargetException","rethrowRuntimeException").contains(method.name))selected.methods.add(method);
            assertEquals(4,selected.methods.size());ClassWriter writer=new ClassWriter(0);selected.accept(writer);Path output=directory.resolve("org/springframework/util/ReflectionUtils.class");Files.createDirectories(output.getParent());Files.write(output,writer.toByteArray());
            String annotation="org/springframework/lang/Nullable.class";output=directory.resolve(annotation);Files.createDirectories(output.getParent());Files.write(output,jar.getInputStream(jar.getJarEntry(annotation)).readAllBytes());
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void preservesTypeUseAndTypeVariableAnnotationPaths() throws Exception {
        fixture("""
            import java.lang.annotation.*;import java.lang.reflect.*;
            public class Metadata {
                @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE_USE,ElementType.TYPE_PARAMETER}) @interface Mark {int value();}
                static class Generic<@Mark(1) T extends @Mark(2) Number>{}
                static class Outer<T>{class Inner<U>{}}
                static class Model {
                    @Mark(3) String @Mark(4) [] array;
                    Generic<@Mark(5) ? extends @Mark(6) Number> wildcard;
                    Outer<@Mark(7) String>.@Mark(8) Inner<@Mark(9) Integer> nested;
                    @Mark(10) String method(@Mark(11) String value) throws @Mark(12) Exception{return value;}
                }
                static int value(AnnotatedElement element){Mark mark=element.getAnnotation(Mark.class);return mark==null?0:mark.value();}
                public static int run() throws Exception {
                    int score=value(Generic.class.getTypeParameters()[0])+value(Generic.class.getTypeParameters()[0].getAnnotatedBounds()[0])*10;
                    AnnotatedArrayType array=(AnnotatedArrayType)Model.class.getDeclaredField("array").getAnnotatedType();score+=value(array)*100+value(array.getAnnotatedGenericComponentType())*1000;
                    AnnotatedParameterizedType generic=(AnnotatedParameterizedType)Model.class.getDeclaredField("wildcard").getAnnotatedType();AnnotatedWildcardType wildcard=(AnnotatedWildcardType)generic.getAnnotatedActualTypeArguments()[0];score+=value(wildcard)*10000+value(wildcard.getAnnotatedUpperBounds()[0])*100000;
                    AnnotatedParameterizedType nested=(AnnotatedParameterizedType)Model.class.getDeclaredField("nested").getAnnotatedType();score+=value(nested)*1000000+value(nested.getAnnotatedActualTypeArguments()[0])*10000000;
                    if(value(((AnnotatedParameterizedType)nested.getAnnotatedOwnerType()).getAnnotatedActualTypeArguments()[0])==7)score+=100000000;
                    Method method=Model.class.getDeclaredMethod("method",String.class);if(value(method.getAnnotatedReturnType())==10 && value(method.getAnnotatedParameterTypes()[0])==11 && value(method.getAnnotatedExceptionTypes()[0])==12)score+=1000000000;
                    return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void preservesInitializationFailuresAccessAndIndirectCallerIdentity() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                static int events;
                public static class StaticCall {static {events++;}public static void method(int value){}}
                public static class Construction {static {events+=10;}public Construction(int value){}}
                public static class StaticField {static {events+=100;}public static int value;}
                public static class Broken {static {if(true)throw new IllegalArgumentException();}public static void method(){}}
                public static abstract class Abstract {public Abstract(){}}
                interface Call {Object apply(Object receiver,Object[] arguments) throws Exception;}
                private int secret(int value){return value+7;}
                public static int run() throws Exception {
                    int score=0;try{StaticCall.class.getMethod("method",int.class).invoke(null,"bad");}catch(IllegalArgumentException expected){score+=events;}
                    try{Construction.class.getConstructor(int.class).newInstance("bad");}catch(IllegalArgumentException expected){score+=events*10;}
                    try{StaticField.class.getField("value").set(null,"bad");}catch(IllegalArgumentException expected){score+=events*100;}
                    try{Broken.class.getMethod("method").invoke(null);}catch(ExceptionInInitializerError expected){score+=100000;}
                    try{Broken.class.getMethod("method").invoke(null);}catch(NoClassDefFoundError expected){score+=1000000;}
                    try{Abstract.class.getConstructor().newInstance();}catch(InstantiationException expected){score+=10000000;}
                    Method method=Metadata.class.getDeclaredMethod("secret",int.class);Call call=method::invoke;
                    if((Integer)call.apply(new Metadata(),new Object[]{3})==10 && method.canAccess(new Metadata()))score+=100000000;
                    if(Object.class.getConstructor().newInstance().getClass()==Object.class)score+=1000000000;
                    return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void throwsLazyTypedMalformedAnnotationAndGenericFailures() throws Exception {
        fixture("""
            import java.lang.annotation.*;import java.lang.reflect.*;
            public class Metadata {
                @Retention(RetentionPolicy.RUNTIME) @interface Mark {int value();}
                @Mark(7) static class Subject {}
                public static class Generic<T>{public T value;}
                public static int run() throws Exception {
                    int score=0;Mark mark=Subject.class.getAnnotation(Mark.class);
                    try{mark.value();}catch(AnnotationTypeMismatchException expected){if(expected.element().getName().equals("value"))score++;}
                    try{Generic.class.getField("value").getGenericType();}catch(GenericSignatureFormatError expected){score+=10;}
                    return score;
                }
            }
            """);
        Path subject=directory.resolve("Metadata$Subject.class");ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(subject)).accept(type,0);type.visibleAnnotations.get(0).values.set(1,"wrong");ClassWriter writer=new ClassWriter(0);type.accept(writer);Files.write(subject,writer.toByteArray());
        Path generic=directory.resolve("Metadata$Generic.class");type=new ClassNode();new ClassReader(Files.readAllBytes(generic)).accept(type,0);type.fields.get(0).signature="!";writer=new ClassWriter(0);type.accept(writer);Files.write(generic,writer.toByteArray());
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void exposesGenericSignaturesAndNamedAnnotatedParameters() throws Exception {
        fixture("""
            import java.lang.annotation.*;import java.lang.reflect.*;
            public class Metadata {
                @Retention(RetentionPolicy.RUNTIME) @interface Mark {int value();}
                public static class Box<T extends Number & Comparable<T>> {
                    public T[] values;public Box<? extends Number> upper;public Box<? super Integer> lower;
                    public <X extends Exception> T method(@Mark(7) final T value,String... names) throws X {return value;}
                }
                public static int run() throws Exception {
                    TypeVariable<?> variable=Box.class.getTypeParameters()[0];int score=variable.getName().equals("T") && variable.getBounds().length==2?1:0;
                    Type bound=variable.getBounds()[1];if(bound instanceof ParameterizedType && ((ParameterizedType)bound).getActualTypeArguments()[0].equals(variable))score+=10;
                    GenericArrayType array=(GenericArrayType)Box.class.getField("values").getGenericType();if(array.getGenericComponentType().equals(variable))score+=100;
                    ParameterizedType upper=(ParameterizedType)Box.class.getField("upper").getGenericType();WildcardType wildcard=(WildcardType)upper.getActualTypeArguments()[0];if(wildcard.getUpperBounds()[0]==Number.class)score+=1000;
                    ParameterizedType lower=(ParameterizedType)Box.class.getField("lower").getGenericType();if(((WildcardType)lower.getActualTypeArguments()[0]).getLowerBounds()[0]==Integer.class)score+=10000;
                    Method method=Box.class.getMethod("method",Number.class,String[].class);if(method.getGenericReturnType().equals(variable) && method.getTypeParameters()[0].getBounds()[0]==Exception.class)score+=100000;
                    Parameter[] parameters=method.getParameters();if(parameters[0].getName().equals("value") && parameters[0].isNamePresent() && Modifier.isFinal(parameters[0].getModifiers()) && parameters[1].isVarArgs())score+=1000000;
                    if(parameters[0].getAnnotation(Mark.class).value()==7 && method.getParameterAnnotations()[0].length==1)score+=10000000;
                    return score;
                }
                public static String typeText() throws Exception {return Box.class.getField("upper").getGenericType().getTypeName();}
            }
            """);
        String checks;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){Class<?> jvm=loader.loadClass("Metadata");checks="assert.equal(e['Metadata.run'](),"+jvm.getMethod("run").invoke(null)+");\nassert.equal(text(e['Metadata.typeText']()),"+js((String)jvm.getMethod("typeText").invoke(null))+");\n";}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks);
    }

    @Test void readsUnchangedPinnedJpaAnnotationDeclarationsAndValues() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar");
        assertEquals("475389446d35c6f46c565728b756dc508c284644ea2690644e0d8e7e339d42fd",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import jakarta.persistence.*;
            @Entity(name="Widget") @Table(name="widgets",indexes={@Index(name="by_name",columnList="name",unique=true)})
            public class Metadata {
                @Column(name="display_name",nullable=false,length=42) private String name;
                public static int run() throws Exception {
                    int score=Metadata.class.getAnnotation(Entity.class).name().equals("Widget")?1:0;
                    Table table=Metadata.class.getAnnotation(Table.class);if(table.name().equals("widgets") && table.indexes()[0].unique() && table.indexes()[0].columnList().equals("name"))score+=10;
                    Column column=Metadata.class.getDeclaredField("name").getAnnotation(Column.class);if(!column.nullable() && column.length()==42 && column.precision()==0)score+=100;
                    if((Integer)Column.class.getDeclaredMethod("length").getDefaultValue()==255)score+=1000;
                    return score;
                }
            }
            """);
        Map<Path,byte[]> originals=new LinkedHashMap<>();try(JarFile jar=new JarFile(artifact.toFile())){for(String name:List.of("Entity","Table","Column","UniqueConstraint","Index")){String entry="jakarta/persistence/"+name+".class";Path output=directory.resolve(entry);Files.createDirectories(output.getParent());byte[] bytes=jar.getInputStream(jar.getJarEntry(entry)).readAllBytes();Files.write(output,bytes);originals.put(output,bytes);}}
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
        for(var entry:originals.entrySet())assertArrayEquals(entry.getValue(),Files.readAllBytes(entry.getKey()));
    }

    @Test void readsRuntimeAnnotationsDefaultsInheritanceRepeatsAndArrays() throws Exception {
        fixture("""
            import java.lang.annotation.*;import java.lang.reflect.*;
            public class Metadata {
                @Retention(RetentionPolicy.RUNTIME) @Inherited @interface Flag {String value() default "default";int[] numbers() default {1,2};Class<?> type() default String.class;}
                @Retention(RetentionPolicy.RUNTIME) @Repeatable(Labels.class) @interface Label {String value();}
                @Retention(RetentionPolicy.RUNTIME) @interface Labels {Label[] value();}
                @Flag(value="parent",numbers={3,4}) static class Parent {}
                @Label("a") @Label("b") static class Child extends Parent {
                    @Flag public int field;
                    @Flag(value="method") public void method(@Label("parameter") int argument){}
                }
                public static int run() throws Exception {
                    Flag inherited=Child.class.getAnnotation(Flag.class);int score=inherited.value().equals("parent")?1:0;
                    if(Child.class.getDeclaredAnnotation(Flag.class)==null)score+=10;
                    int[] values=inherited.numbers();values[0]=99;if(inherited.numbers()[0]==3)score+=100;
                    if(inherited.equals(Parent.class.getAnnotation(Flag.class)) && inherited.hashCode()==Parent.class.getAnnotation(Flag.class).hashCode())score+=1000;
                    Label[] labels=Child.class.getAnnotationsByType(Label.class);if(labels.length==2 && labels[0].value().equals("a") && labels[1].value().equals("b"))score+=10000;
                    Flag field=Child.class.getField("field").getAnnotation(Flag.class);if(field.value().equals("default") && field.type()==String.class)score+=100000;
                    Method method=Child.class.getMethod("method",int.class);if(method.getAnnotation(Flag.class).value().equals("method"))score+=1000000;
                    Object defaults=Flag.class.getDeclaredMethod("numbers").getDefaultValue();if(((int[])defaults)[0]==1)score+=10000000;
                    return score;
                }
                public static int arrays(){
                    Object array=Array.newInstance(int.class,new int[]{2,3});Object row=Array.get(array,0);Array.set(row,1,Byte.valueOf((byte)7));
                    Object strings=Array.newInstance(String.class,2);Array.set(strings,0,"hello");Array.setByte(row,2,(byte)9);int score=(int)Array.getDouble(row,2);
                    try{Array.setInt(strings,0,3);}catch(IllegalArgumentException expected){score+=10;}
                    try{Array.getByte(row,0);}catch(IllegalArgumentException expected){score+=100;}
                    try{Array.getInt(null,0);}catch(NullPointerException expected){score+=1000;}
                    try{Array.setLong(row,0,3);}catch(IllegalArgumentException expected){score+=10000;}
                    boolean[] bools={true};if(Array.getBoolean(bools,0))score+=100000;Array.setBoolean(bools,0,false);
                    return score+Array.getLength(array)+Array.getLength(row)+(Integer)Array.get(row,1)+((String)Array.get(strings,0)).length();
                }
            }
            """);
        StringBuilder checks=new StringBuilder();try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> jvm=loader.loadClass("Metadata");for(String name:List.of("run","arrays"))checks.append("assert.equal(e['Metadata.").append(name).append("'](),").append(jvm.getMethod(name).invoke(null)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void invokesMethodsConstructorsAndFieldsWithAccessAndConversions() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                public static class Parent {public int inherited(){return 4;}}
                public static class Subject extends Parent {
                    public long value;public static int shared;private int secret;
                    private Subject(long value){this.value=value;}
                    public long add(int n){return value+n;}
                    public static int fail(){throw new IllegalArgumentException("target");}
                    public void array(String... args){secret=args.length;}
                }
                public static long run() throws Exception {
                    Constructor<Subject> constructor=Subject.class.getDeclaredConstructor(long.class);
                    Subject subject=constructor.newInstance(Integer.valueOf(7));
                    Field field=Subject.class.getField("value");field.setInt(subject,11);
                    Method add=Subject.class.getMethod("add",int.class);long result=(Long)add.invoke(subject,Byte.valueOf((byte)3));
                    result+=field.getLong(subject)+(Integer)Subject.class.getMethod("inherited").invoke(subject);
                    Subject.class.getDeclaredMethod("array",String[].class).invoke(subject,(Object)new String[]{"a","b"});
                    result+=Subject.class.getDeclaredField("secret").getInt(subject);
                    Subject.class.getField("shared").set(null,Byte.valueOf((byte)5));return result+Subject.shared;
                }
                public static int failures() throws Exception {
                    int score=0;Method method=Subject.class.getMethod("fail");try{method.invoke(null);}catch(InvocationTargetException error){if(error.getCause() instanceof IllegalArgumentException)score++;}
                    Field field=Other.class.getDeclaredField("secret");Other other=new Other();
                    try{field.get(other);}catch(IllegalAccessException expected){score+=10;}
                    field.setAccessible(true);field.set(other,7);score+=(Integer)field.get(other);
                    try{Subject.class.getField("value").getInt(new Subject(3));}catch(IllegalArgumentException expected){score+=100;}
                    try{Subject.class.getMethod("add",int.class).invoke(new Subject(1),Long.valueOf(2));}catch(IllegalArgumentException expected){score+=1000;}
                    return score;
                }
            }
            class Other {private int secret;}
            """);
        StringBuilder checks=new StringBuilder();try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> jvm=loader.loadClass("Metadata");for(String name:List.of("run","failures")){Object expected=jvm.getMethod(name).invoke(null);checks.append("assert.equal(e['Metadata.").append(name).append("'](),").append(expected).append(expected instanceof Long?"n);\n":");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void discoversThroughMethodReferencesWithoutDirectDiscoveryCalls() throws Exception {
        fixture("""
            import java.lang.reflect.Method;
            public class Metadata {
                interface Discovery {Method[] run();}
                private static class Subject {public void target(){}}
                public static int run(){Discovery discovery=Subject.class::getDeclaredMethods;Method[] methods=discovery.run();
                    return methods.length==1 && methods[0].getName().equals("target")?1:0;}
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void discoversUnchangedPinnedSpringBootRecordDeclarations() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/springframework/boot/spring-boot-autoconfigure/3.2.4/spring-boot-autoconfigure-3.2.4.jar");
        assertEquals("14b4694e1a782ea946264a7676da2936d2474abf0f875739b1af5da3bb7ad85b",
            java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();
        fixture("""
            import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails.Address;
            import java.lang.reflect.Method;
            public class Metadata {
                public static int run() throws Exception {
                    Method[] methods=Address.class.getDeclaredMethods();int score=methods.length;
                    Method host=Address.class.getDeclaredMethod("host");Method port=Address.class.getDeclaredMethod("port",(Class<?>[])null);
                    if(host.getReturnType()==String.class && host.getModifiers()==1)score+=10;
                    if(port.getReturnType()==int.class && port.getParameterCount()==0)score+=100;
                    if(Address.class.getDeclaredMethod("equals",Object.class).getReturnType()==boolean.class)score+=1000;
                    return score;
                }
            }
            """);
        String name="org/springframework/boot/autoconfigure/amqp/RabbitConnectionDetails$Address.class";byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry(name)).readAllBytes();}
        Path output=directory.resolve(name);Files.createDirectories(output.getParent());Files.write(output,original);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
        assertArrayEquals(original,Files.readAllBytes(output));
    }

    @Test void discoversOriginalDeclarationsWithoutInitializingTypes() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                static int initialized;
                private static class Hidden {static {initialized+=10;}}
                public static class Parent {public void inherited(){}}
                public static class Subject extends Parent {
                    static {initialized++;}
                    public Subject(){}private Subject(int value){}
                    public static long wide(int i,long j,double d) throws IllegalStateException,Exception {return j;}
                    private Hidden hidden(Hidden[] values){return null;}
                    protected void varargs(String... values){}
                }
                public interface Contract {
                    default int value(){return 1;}
                    static void statik(){}
                    private void secret(){}
                }
                public static int discovery() throws Exception {
                    Method[] methods=Subject.class.getDeclaredMethods();int flags=methods.length;
                    for(Method method:methods) {
                        if(method.getName().equals("wide"))flags+=method.getModifiers()==9?10:0;
                        if(method.getName().equals("hidden"))flags+=method.getModifiers()==2 && method.getReturnType()==Hidden.class && method.getParameterTypes()[0]==Hidden[].class?100:0;
                        if(method.getName().equals("varargs"))flags+=method.isVarArgs()?1000:0;
                        if(method.getDeclaringClass()!=Subject.class)return -1;
                    }
                    return flags+initialized*10000;
                }
                public static int copies() throws Exception {
                    Method first=Subject.class.getDeclaredMethod("wide",int.class,long.class,double.class);
                    Method second=Subject.class.getDeclaredMethod("wide",int.class,long.class,double.class);
                    Class<?>[] exceptions=first.getExceptionTypes();exceptions[0]=null;
                    Class<?>[] parameters=first.getParameterTypes();parameters[0]=null;
                    Method[] methods=Subject.class.getDeclaredMethods();methods[0]=null;
                    return (first.equals(second)?1:0)+(first.hashCode()==second.hashCode()?2:0)
                        +(first.getExceptionTypes()[0]==IllegalStateException.class && first.getExceptionTypes()[1]==Exception.class?4:0)
                        +(first.getParameterTypes()[0]==int.class?8:0)+(Subject.class.getDeclaredMethods()[0]!=null?16:0);
                }
                public static int interfaces(){int score=0;for(Method method:Contract.class.getDeclaredMethods()){
                    if(method.getName().equals("value") && method.isDefault())score+=1;
                    if(method.getName().equals("statik") && !method.isDefault())score+=2;
                    if(method.getName().equals("secret") && !method.isDefault())score+=4;
                }return score;}
                public static int absent(int mode) throws Exception {
                    try{
                        if(mode==0)Subject.class.getDeclaredMethod("inherited");
                        if(mode==1)Subject.class.getDeclaredMethod("<init>");
                        if(mode==2)Subject.class.getDeclaredMethod("wide",Integer.class,long.class,double.class);
                        if(mode==3)Subject.class.getDeclaredMethod("wide",new Class<?>[]{null,long.class,double.class});
                        if(mode==4)Subject.class.getDeclaredMethod(null);
                        if(mode==5)return int.class.getDeclaredMethods().length+String[].class.getDeclaredMethods().length;
                        return 0;
                    }catch(NoSuchMethodException expected){return 1;}catch(NullPointerException expected){return 2;}
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("discovery","copies","interfaces","discovery"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(jvm.getMethod(method).invoke(null)).append(");\n");
            for(int mode=0;mode<6;mode++)checks.append("assert.equal(e['Metadata.absent'](").append(mode).append("),").append(jvm.getMethod("absent",int.class).invoke(null,mode)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void selectsCovariantReturnAndKeepsBridgeAndSyntheticDeclarations() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                interface Parent<T>{T value();}
                static class Child implements Parent<String>{public String value(){return "value";}}
                public static int run() throws Exception {
                    Method found=Child.class.getDeclaredMethod("value",(Class<?>[])null);int score=found.getReturnType()==String.class?1:0;
                    for(Method method:Child.class.getDeclaredMethods()){
                        if(method.isBridge() && method.isSynthetic() && method.getReturnType()==Object.class)score+=10;
                        if(!method.isBridge() && method.getReturnType()==String.class)score+=100;
                    }return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void rejectsUndeliveredPlatformMetadataAndAllowsDiscoveredInvocation() throws Exception {
        byte[] bytes=fixture("""
            public class Metadata {
                public static void target(){}
                public static int run() throws Exception {
                    int score=0;try{String.class.getDeclaredMethods();}catch(UnsupportedOperationException expected){score++;}
                    try{Metadata.class.getDeclaredMethod("target").invoke(null);}catch(UnsupportedOperationException expected){score+=10;}
                    return score;
                }
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(bytes)),"assert.equal(e['Metadata.run'](),1);\n");
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
