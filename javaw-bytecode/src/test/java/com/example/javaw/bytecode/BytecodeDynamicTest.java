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

class BytecodeDynamicTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","--release",release,
                "-d",directory.toString(),file.toString()));
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

    @Test void executesCapturedAndConstructorLambdasAndConcatAgainstJvm() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                interface Wide { long apply(long n); }
                interface Factory { Item create(int n); }
                interface Generic { Object apply(Object n); }
                interface IntFn { int apply(int n); }
                interface Mapper<T> { T apply(T value); }
                interface Unbound { int apply(Item receiver,int value); }
                interface Widen { double apply(int n); }
                static class Item { int n;Item(int n){this.n=n;} int plus(int value){return n+value;} }
                static long offset=10;
                private static long add(long a,long b){return a+b+offset;}
                public static long captured(long base,long input){Wide f=n->add(base,n);return f.apply(input);}
                public static int constructor(){Factory factory=Item::new;return factory.create(71).n;}
                public static int bound(){IntFn f=new Item(40)::plus;return f.apply(2);}
                public static int unbound(){Unbound f=Item::plus;return f.apply(new Item(40),2);}
                public static int nullBound(){try{Item item=null;IntFn f=item::plus;return 0;}catch(NullPointerException expected){return 1;}}
                public static String generic(){Mapper<String> f=Metadata::duplicate;return f.apply("g");}
                private static String duplicate(String s){return s.concat(s);}
                public static double widen(){Widen f=Metadata::longValue;return f.apply(31);}
                private static long longValue(long n){return n+1;}
                public static int reference(){IntFn f=Metadata::increment;return f.apply(5);}
                private static int increment(int n){return n+1;}
                public static String concat(int n,long wide,boolean flag,char ch){return "A:"+n+":"+wide+":"+flag+":"+ch+":"+(Object)null;}
                static class Text {public String toString(){return "custom";}}
                public static String object(){return "["+new Text()+"]";}
                public static int fresh(){String s="abc";return (""+s)==s?0:1;}
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            checks.append("assert.equal(e['Metadata.captured'](9007199254740993n,7n),")
                .append(jvm.getMethod("captured",long.class,long.class).invoke(null,9007199254740993L,7L)).append("n);\n");
            for(String name:List.of("constructor","reference","fresh","bound","unbound","nullBound","widen"))checks.append("assert.equal(e['Metadata.").append(name).append("'](),")
                .append(jvm.getMethod(name).invoke(null)).append(");\n");
            checks.append("assert.equal(text(e['Metadata.concat'](-2147483648,-9223372036854775808n,1,55357)),")
                .append(js((String)jvm.getMethod("concat",int.class,long.class,boolean.class,char.class).invoke(null,Integer.MIN_VALUE,Long.MIN_VALUE,true,'\uD83D'))).append(");\n");
            checks.append("assert.equal(text(e['Metadata.generic']()),").append(js((String)jvm.getMethod("generic").invoke(null))).append(");\n");
            checks.append("assert.equal(text(e['Metadata.object']()),").append(js((String)jvm.getMethod("object").invoke(null))).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),checks.toString());
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(true))),checks.toString());
    }

    @Test void preservesInitializationArgumentOrderAndPerSiteFactories() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                interface Value {long get();}
                static int events;
                static class Target {static int initialized=mark(7);static long value(){return initialized;}}
                static int mark(int value){events=events*10+value;return value;}
                static Value factory(){return Target::value;}
                static class Text {int n;Text(int n){this.n=n;}public String toString(){mark(n);return null;}}
                public static int initialized(){events=0;Value f=factory();int before=events;
                    long result=f.get();return before*1000+events*10+(int)result;}
                public static int sites(){Value first=factory(),second=factory();return first.getClass()==second.getClass()?1:0;}
                public static String order(){events=0;String s=""+new Text(1)+new Text(2);return s+":"+events;}
                public static long wideCapture(){double wide=2.25;long large=9007199254740993L;
                    Value f=()->wide==2.25?large:0;return f.get();}
                public static int wrongGeneric(){Mapper<String> typed=s->s;Mapper raw=typed;
                    try{raw.apply(new Object());return 0;}catch(ClassCastException expected){return 1;}}
                interface Mapper<T> {T apply(T value);}
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String name:List.of("initialized","sites","wrongGeneric"))checks.append("assert.equal(e['Metadata.").append(name).append("'](),")
                .append(jvm.getMethod(name).invoke(null)).append(");\n");
            checks.append("assert.equal(text(e['Metadata.order']()),").append(js((String)jvm.getMethod("order").invoke(null))).append(");\n");
            checks.append("assert.equal(e['Metadata.wideCapture'](),").append(jvm.getMethod("wideCapture").invoke(null)).append("n);\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),checks.toString());
    }

    @Test void executesJava8PrivateLambdaTargetsWithoutWeakeningOriginalDeclarations() throws Exception {
        release="8";
        byte[] jar=fixture("""
            public class Metadata {
                interface Fn {int value(int n);}
                public static int run(int capture){Fn f=n->n+capture;return f.value(7);}
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"assert.equal(e['Metadata.run'](35),42);");
    }

    @Test void executesAlternateMarkerAndBridgeFactories() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                interface Fn {int apply(int n);}
                interface Marker {}
                interface A {Object get();}
                interface B {String get();}
                public static int marker(){Fn f=(Fn & Marker)(n->n+1);return (f instanceof Marker?40:0)+f.apply(1);}
                public static String bridge(){A f=(A & B)(()->"bridged");return (String)f.get();}
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"""
            assert.equal(e['Metadata.marker'](),42);
            assert.equal(text(e['Metadata.bridge']()),'bridged');
            """);
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");assertEquals(42,jvm.getMethod("marker").invoke(null));
            assertEquals("bridged",jvm.getMethod("bridge").invoke(null));
        }
    }

    @Test void rejectsFloatingConcatAndSerializableFactoriesExplicitly() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {public static String text(double value){return "value="+value;}}
            """);
        String failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar))).getMessage();
        assertTrue(failure.contains("floating-point"),failure);
        byte[] serialized=fixture("""
            public class Metadata {
                interface Fn {int get();}
                public static Object lambda(){return (Fn & java.io.Serializable)(()->1);}
            }
            """);
        failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(serialized))).getMessage();
        assertTrue(failure.contains("serializable lambda"),failure);
    }

    private void addConstant(String methodName,String name,String descriptor,String bootstrapName,Object... arguments) throws Exception {
        Path file=directory.resolve("Metadata.class");ClassNode type=new ClassNode();
        new ClassReader(Files.readAllBytes(file)).accept(type,0);
        String bootstrapDescriptor="(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;"+
                (arguments.length==1?"Ljava/lang/Class;":"")+")"+(bootstrapName.equals("primitiveClass")?"Ljava/lang/Class;":"Ljava/lang/Object;");
        Handle bootstrap=new Handle(H_INVOKESTATIC,"java/lang/invoke/ConstantBootstraps",bootstrapName,bootstrapDescriptor,false);
        MethodNode code=new MethodNode(ACC_PUBLIC|ACC_STATIC,methodName,"()"+descriptor,null,null);
        code.visitCode();code.visitLdcInsn(new ConstantDynamic(name,descriptor,bootstrap,arguments));
        code.visitInsn(Type.getType(descriptor).getOpcode(IRETURN));code.visitMaxs(2,0);code.visitEnd();type.methods.add(code);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);Files.write(file,writer.toByteArray());
    }

    @Test void resolvesConstantFieldsAndPrimitiveMirrorsLazilyAndCachesInitializationErrors() throws Exception {
        fixture("""
            public class Metadata {
                static int events;
                static class Mutable {static int VALUE=123;}
                static class Target {private static final long VALUE=initialize();static long initialize(){events+=7;return 9007199254740993L;}}
                static class Failed {private static final int VALUE=initialize();static int initialize(){events+=100;throw new Error("init");}}
                public static int events(){return events;}
                public static int isLong(Class<?> type){return type==long.class?1:0;}
                public static int initializationError(Throwable error){return error.getClass()==Error.class?1:0;}
                public static int fieldError(Throwable error){return error instanceof IncompatibleClassChangeError?1:0;}
                public static int constantError(Throwable error){return error instanceof BootstrapMethodError && error.getCause() instanceof IllegalArgumentException?1:0;}
            }
            """);
        addConstant("longValue","VALUE","J","getStaticFinal",Type.getObjectType("Metadata$Target"));
        addConstant("failedValue","VALUE","I","getStaticFinal",Type.getObjectType("Metadata$Failed"));
        addConstant("mutableValue","VALUE","I","getStaticFinal",Type.getObjectType("Metadata$Mutable"));
        addConstant("invalidNull","value","I","nullConstant");
        addConstant("mirror","J","Ljava/lang/Class;","primitiveClass");
        byte[] jar=jar(false);
        String wat=BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar));
        execute(wat,"""
            assert.equal(e['Metadata.events'](),0);
            assert.equal(e['Metadata.isLong'](e['Metadata.mirror']()),1);
            assert.equal(e['Metadata.events'](),0);
            assert.equal(e['Metadata.longValue'](),9007199254740993n);
            assert.equal(e['Metadata.events'](),7);
            assert.equal(e['Metadata.longValue'](),9007199254740993n);
            let first;
            for(let attempt=0;attempt<2;attempt++) {
                try{e['Metadata.failedValue']();assert.fail('initialization succeeded');}
                catch(error){const pointer=error.getArg(e.__java_exception,0);
                    assert.equal(e['Metadata.initializationError'](pointer),1);
                    if(attempt===0)first=pointer;else assert.equal(pointer,first);}
            }
            assert.equal(e['Metadata.events'](),107);
            for(let attempt=0;attempt<2;attempt++) {
                try{e['Metadata.mutableValue']();assert.fail('nonfinal constant succeeded');}
                catch(error){assert.equal(e['Metadata.fieldError'](error.getArg(e.__java_exception,0)),1);}
                try{e['Metadata.invalidNull']();assert.fail('primitive null succeeded');}
                catch(error){assert.equal(e['Metadata.constantError'](error.getArg(e.__java_exception,0)),1);}
            }
            """);
        execute(wat,"""
            assert.equal(e['Metadata.events'](),0);
            failAllocation=true;const before=allocations;let first;
            for(let attempt=0;attempt<2;attempt++) {
                try{e['Metadata.mirror']();assert.fail('failed constant resolved');}
                catch(error){const pointer=error.getArg(e.__java_exception,0);
                    if(attempt===0)first=pointer;else assert.equal(pointer,first);}
            }
            assert.equal(allocations,before+1);
            """);
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");assertEquals(0,jvm.getMethod("events").invoke(null));
            assertSame(long.class,jvm.getMethod("mirror").invoke(null));assertEquals(0,jvm.getMethod("events").invoke(null));
            assertEquals(9007199254740993L,jvm.getMethod("longValue").invoke(null));assertEquals(7,jvm.getMethod("events").invoke(null));
            for(int attempt=0;attempt<2;attempt++)assertInstanceOf(Error.class,
                assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("failedValue").invoke(null)).getCause());
            assertEquals(107,jvm.getMethod("events").invoke(null));
            for(int attempt=0;attempt<2;attempt++) {
                assertInstanceOf(IncompatibleClassChangeError.class,assertThrows(java.lang.reflect.InvocationTargetException.class,
                    ()->jvm.getMethod("mutableValue").invoke(null)).getCause());
                Throwable error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("invalidNull").invoke(null)).getCause();
                assertInstanceOf(BootstrapMethodError.class,error);assertInstanceOf(IllegalArgumentException.class,error.getCause());
            }
        }
    }

    private static final Handle CONCAT=new Handle(H_INVOKESTATIC,"java/lang/invoke/StringConcatFactory","makeConcatWithConstants",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",false);
    private byte[] dynamic(Handle bootstrap,String recipe) {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES);writer.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);
        for(String name:List.of("value","other")) {
            MethodVisitor code=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,name,"()Ljava/lang/String;",null,null);
            code.visitCode();code.visitInvokeDynamicInsn("text","()Ljava/lang/String;",bootstrap,recipe);code.visitInsn(ARETURN);
            code.visitMaxs(0,0);code.visitEnd();
        }
        MethodVisitor code=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"caught","()I",null,null);
        Label start=new Label(),end=new Label(),handler=new Label();code.visitTryCatchBlock(start,end,handler,"java/lang/BootstrapMethodError");
        code.visitCode();code.visitLabel(start);code.visitMethodInsn(INVOKESTATIC,"Metadata","value","()Ljava/lang/String;",false);
        code.visitInsn(POP);code.visitLabel(end);code.visitInsn(ICONST_0);code.visitInsn(IRETURN);
        code.visitLabel(handler);code.visitMethodInsn(INVOKEVIRTUAL,"java/lang/BootstrapMethodError","getCause","()Ljava/lang/Throwable;",false);
        code.visitTypeInsn(INSTANCEOF,"java/lang/invoke/StringConcatException");code.visitInsn(IRETURN);
        code.visitMaxs(0,0);code.visitEnd();writer.visitEnd();return writer.toByteArray();
    }
    @Test void memoizesCatchableBootstrapFailuresAndDoesNotLinkAtModuleInstantiation() throws Exception {
        byte[] bytes=dynamic(CONCAT,"\u0001");
        execute(BytecodeCompiler.compileClassWithExceptionRuntime(bytes),"""
            assert.equal(e['Metadata.caught'](),1);
            let first;
            for(let attempt=0;attempt<2;attempt++) {
                try {e['Metadata.value']();assert.fail('bootstrap succeeded');}
                catch(error){assert.ok(error instanceof WebAssembly.Exception);const pointer=error.getArg(e.__java_exception,0);
                    if(attempt===0)first=pointer;else assert.equal(pointer,first);}
            }
            try{e['Metadata.other']();assert.fail('bootstrap succeeded');}
            catch(error){assert.notEqual(error.getArg(e.__java_exception,0),first);}
            """);
        Class<?> jvm=new ClassLoader(null){Class<?> define(){return defineClass("Metadata",bytes,0,bytes.length);}}.define();
        for(int attempt=0;attempt<2;attempt++)assertInstanceOf(BootstrapMethodError.class,
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("value").invoke(null)).getCause());
    }
    @Test void supportsNullDynamicConstantsAndRejectsUnknownBootstrapsWithSourceContext() throws Exception {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);writer.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);
        Handle bootstrap=new Handle(H_INVOKESTATIC,"java/lang/invoke/ConstantBootstraps","nullConstant",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",false);
        MethodVisitor code=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"value","()Ljava/lang/String;",null,null);
        code.visitCode();code.visitLdcInsn(new ConstantDynamic("nothing","Ljava/lang/String;",bootstrap));code.visitInsn(ARETURN);
        code.visitMaxs(0,0);code.visitEnd();writer.visitEnd();
        execute(BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()),"assert.equal(e['Metadata.value'](),0);");
        Handle unknown=new Handle(H_INVOKESTATIC,"custom/Bootstrap","bootstrap",CONCAT.getDesc(),false);
        String failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(dynamic(unknown,"x"))).getMessage();
        assertTrue(failure.contains("Metadata.value"),failure);assertTrue(failure.contains("custom/Bootstrap"),failure);
    }
    @Test void executesRecipeFreeConcatenation() throws Exception {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);writer.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);
        MethodVisitor code=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"concat","(ICJ)Ljava/lang/String;",null,null);
        code.visitCode();code.visitVarInsn(ILOAD,0);code.visitVarInsn(ILOAD,1);code.visitVarInsn(LLOAD,2);
        code.visitInvokeDynamicInsn("concat","(ICJ)Ljava/lang/String;",new Handle(H_INVOKESTATIC,"java/lang/invoke/StringConcatFactory","makeConcat",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false));
        code.visitInsn(ARETURN);code.visitMaxs(0,0);code.visitEnd();writer.visitEnd();byte[] bytes=writer.toByteArray();
        Class<?> jvm=new ClassLoader(null){Class<?> define(){return defineClass("Metadata",bytes,0,bytes.length);}}.define();
        String expected=(String)jvm.getMethod("concat",int.class,char.class,long.class).invoke(null,-12,'a',Long.MIN_VALUE);
        execute(BytecodeCompiler.compileClassWithExceptionRuntime(bytes),"assert.equal(text(e['Metadata.concat'](-12,97,-9223372036854775808n)),"+js(expected)+");");
    }

    @Test void linksMalformedLambdaImplementationArityAsTypedCachedJavaFailure() throws Exception {
        fixture("""
            public class Metadata {
                interface Fn {int get();}
                private static int target(int n){return n;}
                public static int isFailure(Throwable error){return error instanceof BootstrapMethodError &&
                    error.getCause() instanceof java.lang.invoke.LambdaConversionException?1:0;}
            }
            """);
        Path file=directory.resolve("Metadata.class");ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(type,0);
        MethodNode factory=new MethodNode(ACC_PUBLIC|ACC_STATIC,"factory","()LMetadata$Fn;",null,null);
        factory.visitCode();factory.visitInvokeDynamicInsn("get","()LMetadata$Fn;",new Handle(H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false),
            Type.getMethodType("()I"),new Handle(H_INVOKESTATIC,"Metadata","target","(I)I",false),Type.getMethodType("()I"));
        factory.visitInsn(ARETURN);factory.visitMaxs(1,0);factory.visitEnd();type.methods.add(factory);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);Files.write(file,writer.toByteArray());
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),"""
            let first;
            for(let attempt=0;attempt<2;attempt++) {
                try{e['Metadata.factory']();assert.fail('invalid factory linked');}
                catch(error){const pointer=error.getArg(e.__java_exception,0);assert.equal(e['Metadata.isFailure'](pointer),1);
                    if(attempt===0)first=pointer;else assert.equal(pointer,first);}
            }
            """);
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int attempt=0;attempt<2;attempt++) {
                Throwable error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("factory").invoke(null)).getCause();
                assertInstanceOf(BootstrapMethodError.class,error);
                // HotSpot may reconstruct the sticky error without its original cause on later invocations.
                if(attempt==0)assertEquals(1,jvm.getMethod("isFailure",Throwable.class).invoke(null,error));
            }
        }
    }

    @Test void cachesLambdaInterfaceInitializationFailuresAndRetriesInstanceAllocations() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                static int events;
                static int mark(){events+=7;return 7;}
                interface Good {int START=mark();int get();default int extra(){return START;}}
                interface Bad {int START=fail();static int fail(){throw new Error("interface");}
                    int get();default int extra(){return START;}}
                public static Object good(){return (Good)()->3;}
                public static Object broken(){return (Bad)()->5;}
                public static int value(Object fn){return ((Good)fn).get();}
                public static int events(){return events;}
                public static int isError(Throwable error){return error instanceof InternalError && error.getCause()!=null && error.getCause().getClass()==Error.class?1:0;}
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"""
            assert.equal(e['Metadata.events'](),0);
            failAllocation=true;
            assert.throws(()=>e['Metadata.good'](),WebAssembly.Exception);
            assert.equal(e['Metadata.value'](e['Metadata.good']()),3);
            assert.equal(e['Metadata.events'](),7);
            let first;
            for(let attempt=0;attempt<2;attempt++) {
                try{e['Metadata.broken']();assert.fail('interface initialized');}
                catch(error){const pointer=error.getArg(e.__java_exception,0);assert.equal(e['Metadata.isError'](pointer),1);
                    if(attempt===0)first=pointer;else assert.equal(pointer,first);}
            }
            """);
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");assertEquals(0,jvm.getMethod("events").invoke(null));
            assertEquals(3,jvm.getMethod("value",Object.class).invoke(null,jvm.getMethod("good").invoke(null)));
            assertEquals(7,jvm.getMethod("events").invoke(null));
            for(int attempt=0;attempt<2;attempt++) {
                Throwable error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("broken").invoke(null)).getCause();
                assertEquals(InternalError.class,error.getClass());
                if(attempt==0)assertEquals(Error.class,error.getCause().getClass());
            }
        }
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
