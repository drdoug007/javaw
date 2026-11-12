package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import javax.tools.ToolProvider;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeInitializationTest {
    @TempDir Path directory;

    private byte[] fixture(String source) throws Exception {
        Path file = directory.resolve("Init.java");
        Files.writeString(file, source);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-g", "--release", "17", "-d", directory.toString(), file.toString()));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> classes;
        try (var files = Files.list(directory)) {
            classes = new ArrayList<>(files.filter(path -> path.toString().endsWith(".class")).sorted().toList());
        }
        if (reverse) Collections.reverse(classes);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (Path file : classes) {
                jar.putNextEntry(new JarEntry(file.getFileName().toString()));
                jar.write(Files.readAllBytes(file)); jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void compare(byte[] jar, String... methods) throws Exception {
        StringBuilder assertions = new StringBuilder();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("Init");
            for (String method : methods) {
                int expected = (Integer) type.getMethod(method).invoke(null);
                assertions.append("assert.equal(e['Init.").append(method).append("'](),")
                        .append(expected).append(");\n");
            }
        }
        for (boolean reverse : new boolean[]{false, true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(reverse ? jar(true) : jar)),
                    assertions.toString());
    }

    @Test void ordersSuperclassDefaultsAndActiveUsesWithoutInitializingUnusedOrInheritedOwners() throws Exception {
        byte[] jar = fixture("""
            public class Init {
                static int log;
                static int tick(int n) { log=log*10+n; return n; }
                public static int run() {
                    int a=Child.value; // symbolic subclass must only initialize Base
                    int b=Child.get();
                    int before=log;
                    new Child();
                    int after=log;
                    new Child();
                    Child.value=8;
                    return before*100000+after*10+log+(a+b-2)*1000000;
                }
                public static int interfaces() { return OnlyChild.value*100000+log; }
                public static int unused() { return log; }
            }
            class Base { static int value=Init.tick(1); static int get(){return value;} }
            interface Root { int x=Init.tick(2); default int root(){return 1;} }
            interface Empty extends Root { int x=Init.tick(9); }
            interface Other extends Root { int x=Init.tick(3); default int other(){return 2;} }
            class Child extends Base implements Empty,Other { static int own=Init.tick(4); }
            interface OnlyParent { int value=Init.tick(8); default int method(){return 0;} }
            interface OnlyChild extends OnlyParent { int value=Init.tick(5); }
            class Unused { static int value=Init.tick(9); }
            """);
        compare(jar, "unused", "run", "run", "interfaces", "unused");
    }

    @Test void preparesConstantsAndDefaultsBeforeRecursiveInitializationAndKeepsModuleStateIsolated() throws Exception {
        byte[] jar = fixture("""
            public class Init {
                static int calls;
                static int value=cycle();
                static int cycle(){ calls++; return Peer.value+10; }
                public static int run(){return value*100+Peer.value*10+calls;}
                public static int constants(){ return Constants.text()==Constants.TEXT && Constants.LONG==9007199254740993L ? 1:0; }
            }
            class Peer { static int value=Init.value+2; }
            class Constants {
                static final String TEXT="constant";
                static final long LONG=9007199254740993L;
                static String other=TEXT;
                static String text(){return other;}
            }
            """);
        compare(jar, "run", "run", "constants");
    }

    @Test void wrapsFirstExceptionPreservesErrorsAndPoisonsFailedClassesAndDependents() throws Exception {
        byte[] jar = fixture("""
            public class Init {
                static Throwable original;
                public static int first(){
                    try { return Dependent.value; }
                    catch(ExceptionInInitializerError e){return e.getCause()==original && e.getException()==original ? 1:0;}
                }
                public static int again(){
                    try {return Broken.value;}
                    catch(NoClassDefFoundError e){return e.getCause() instanceof ExceptionInInitializerError ? 2:0;}
                }
                public static int dependent(){try{return Dependent.value;}catch(NoClassDefFoundError e){return 3;}}
                public static int error(){try{return ErrorBroken.value;}catch(LinkageError e){return e==original ? 4:0;}}
                public static int errorAgain(){try{return ErrorBroken.value;}catch(NoClassDefFoundError e){return 5;}}
                static int fail(){original=new RuntimeException("failure");throw (RuntimeException)original;}
                static int failError(){original=new LinkageError();throw (LinkageError)original;}
            }
            class Broken { static int value=Init.fail(); }
            class Dependent extends Broken { static int value=7; }
            class ErrorBroken { static int value=Init.failError(); }
            """);
        compare(jar, "first", "again", "dependent", "error", "errorAgain");
    }

    @Test void triggersPutstaticNewAndExportedStaticEntriesBeforeTheirBodies() throws Exception {
        byte[] jar = fixture("""
            public class Init {
                static int ticks;
                static int tick(int n){ticks=ticks*10+n;return n;}
                public static int put(){Put.value=9;return ticks*10+Put.value;}
                public static int allocate(){new Made();return ticks;}
                public static int entry(){return Entry.run()*100+ticks;}
            }
            class Put { static int value=Init.tick(1); }
            class Made { static { Init.tick(2); } Made(){Init.tick(3);} }
            class Entry { static { Init.tick(4); } public static int run(){return Init.ticks;} }
            """);
        compare(jar, "put", "allocate", "entry", "put");
        // Direct host export follows the same active-use rule as an invokestatic.
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),
                "assert.equal(e['Entry.run'](),4);assert.equal(e['Entry.run'](),4);");
    }

    @Test void preservesForeignHostFailuresAndDoesNotRetryTheirInitializers() throws Exception {
        byte[] jar = fixture("""
            public class Init {
                public static int run(){
                    try {return Victim.data.length;}
                    catch(NoClassDefFoundError e){return e.getCause()==null ? 8:0;}
                }
                public static int trace(){return new RuntimeException().getStackTrace().length;}
            }
            class Victim { static int[] data=new int[1]; }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar),BytecodeCompiler.Memory.hostAllocator()), """
            failAllocation=true;
            assert.throws(()=>e['Init.run'](), error=>error===hostError);
            assert.equal(e['Init.run'](),8);
            assert.equal(e['Init.run'](),8);
            assert.equal(e['Init.trace'](),1);
            """);
    }

    @Test void executesLegacyInitializerWithoutStaticFlagAndNeverExportsClinit() throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "LegacyInit", null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_STATIC, "value", "I", null, null).visitEnd();
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<clinit>", "()V", null, null);
        init.visitCode();init.visitIntInsn(Opcodes.BIPUSH, 42);
        init.visitFieldInsn(Opcodes.PUTSTATIC, "LegacyInit", "value", "I");
        init.visitInsn(Opcodes.RETURN);init.visitMaxs(0,0);init.visitEnd();
        MethodVisitor run = writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, "run", "()I", null, null);
        run.visitCode();run.visitFieldInsn(Opcodes.GETSTATIC, "LegacyInit", "value", "I");
        run.visitInsn(Opcodes.IRETURN);run.visitMaxs(0,0);run.visitEnd();writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Class<?> jvm = new ClassLoader(null) { Class<?> define(){return defineClass(null,bytes,0,bytes.length);} }.define();
        assertEquals(42,jvm.getMethod("run").invoke(null));
        execute(BytecodeCompiler.compileClassWithExceptionRuntime(bytes),
                "assert.equal(e['LegacyInit.run'](),42);assert.equal(e['LegacyInit.<clinit>'],undefined);");
    }

    @Test void preparesStringAndNarrowNumericConstantValuesWithoutAnInitializerBody() throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Prepared", null, "java/lang/Object", null);
        String[] descriptors={"Z","B","C","S","I"};
        int[] values={2,257,65537,65535,42};
        for(int i=0;i<descriptors.length;i++) {
            writer.visitField(Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"value"+i,descriptors[i],null,values[i]).visitEnd();
            MethodVisitor getter=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"get"+i,"()I",null,null);
            getter.visitCode();getter.visitFieldInsn(Opcodes.GETSTATIC,"Prepared","value"+i,descriptors[i]);
            getter.visitInsn(Opcodes.IRETURN);getter.visitMaxs(0,0);getter.visitEnd();
        }
        writer.visitField(Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"TEXT","Ljava/lang/String;",null,"a\0\uD83D\uDE00").visitEnd();
        MethodVisitor text=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"text","()I",null,null);
        text.visitCode();text.visitFieldInsn(Opcodes.GETSTATIC,"Prepared","TEXT","Ljava/lang/String;");
        text.visitLdcInsn("a\0\uD83D\uDE00");
        org.objectweb.asm.Label different=new org.objectweb.asm.Label();
        text.visitJumpInsn(Opcodes.IF_ACMPNE,different);text.visitInsn(Opcodes.ICONST_1);text.visitInsn(Opcodes.IRETURN);
        text.visitLabel(different);text.visitFrame(Opcodes.F_SAME,0,null,0,null);
        text.visitInsn(Opcodes.ICONST_0);text.visitInsn(Opcodes.IRETURN);text.visitMaxs(0,0);text.visitEnd();writer.visitEnd();
        byte[] bytes=writer.toByteArray();
        Class<?> jvm=new ClassLoader(null){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
        StringBuilder checks=new StringBuilder();
        for(int i=0;i<descriptors.length;i++)checks.append("assert.equal(e['Prepared.get").append(i)
                .append("'](),").append(jvm.getMethod("get"+i).invoke(null)).append(");");
        assertEquals(1,jvm.getMethod("text").invoke(null));
        checks.append("assert.equal(e['Prepared.text'](),1);assert.equal(e['Prepared.text'](),1);");
        execute(BytecodeCompiler.compileClassWithExceptionRuntime(bytes),checks.toString());
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");
        Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("init.wat"), output=directory.resolve("init.wasm"), script=directory.resolve("run.cjs");
        Files.writeString(input,wat);
        WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            // Run twice to establish independent initialization state and no eager host allocations.
            for(let iteration=0;iteration<2;iteration++) {
                const memory=new WebAssembly.Memory({initial:2}); let heap=1024, allocations=0, failAllocation=false;
                const hostError=new Error("foreign allocator failure");
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    if(failAllocation){failAllocation=false;throw hostError;}
                    allocations++;const result=heap;heap+=size;
                    new Uint8Array(memory.buffer,result,size).fill(0x7f);return result;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;
                heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """+assertions+"\n}");
        WasmTestSupport.node(script,output);
    }
}
