package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;

class BytecodeCompatibilityTest {
    @TempDir Path directory;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Fixture.java"); Files.writeString(file,source);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","-d",directory.toString(),file.toString()));
        return Files.readAllBytes(directory.resolve("Fixture.class"));
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("module.wat"), output=directory.resolve("module.wasm");
        Files.writeString(input,wat);
        run("wat2wasm",input.toString(),"-o",output.toString());
        Path script=directory.resolve("run.cjs");
        Files.writeString(script,"""
            const assert=require('node:assert/strict');
            const fs=require('node:fs');
            const instance=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),
                {env:{memory:new WebAssembly.Memory({initial:2,maximum:20}),__heap_alloc:()=>{throw Error('unexpected allocation')}}});
            const e=instance.exports;
            """+assertions);
        run("node",script.toString(),output.toString());
    }

    private void run(String... command) throws Exception {
        Path log=directory.resolve("command.log");
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(20,TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Timed out: "+String.join(" ",command)); }
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    @Test void executesJavacBranchesLoopsJoinsAndStaticCalls() throws Exception {
        String wat=BytecodeCompiler.compileClass(fixture("""
            public class Fixture {
                public static final int CONSTANT=42;
                private static int twice(int n) { return n*2; }
                private static int twice(boolean n) { return n ? 1 : 0; }
                public static int sum(int n) {
                    int sum=0; for(int i=0;i<=n;i++) { if((i&1)==0) sum+=twice(i); else sum-=i; }
                    return sum+CONSTANT;
                }
                public static int choose(int n) { return n<0 ? n*2 : n+3; }
                public static int countdown(int n) { while(n>0) n--; return n; }
                public static int divide(int a,int b) { return a/b; }
                public static int narrowing(int n) { return (byte)n+(short)n+(char)n; }
            }
            """));
        execute(wat,"""
            for(let n=-3;n<30;n++) {
                let sum=42; for(let i=0;i<=n;i++) sum+=(i&1)===0 ? i*2 : -i;
                assert.equal(e['Fixture.sum'](n),sum);
                assert.equal(e['Fixture.choose'](n),n<0?n*2:n+3);
                assert.equal(e['Fixture.countdown'](n),Math.min(n,0));
            }
            assert.equal(e['Fixture.narrowing'](65535),65533);
            assert.equal(e['Fixture.divide'](-2147483648,-1),-2147483648);
            assert.equal(e['Fixture.divide'](-7,2),-3);
            """);
    }

    @Test void linksSupportedClassesWithinOneJar() throws Exception {
        fixture("""
            public class Fixture {
                public static int calculate(int n) { return Helper.doubleValue(n)+1; }
            }
            class Helper {
                static int stored;
                static int doubleValue(int n) { stored=n; return stored*2; }
            }
            """);
        var bytes=new java.io.ByteArrayOutputStream();
        try(var jar=new java.util.jar.JarOutputStream(bytes)) {
            for(String name:new String[]{"Fixture","Helper"}) {
                jar.putNextEntry(new java.util.jar.JarEntry(name+".class"));
                jar.write(Files.readAllBytes(directory.resolve(name+".class"))); jar.closeEntry();
            }
        }
        execute(BytecodeCompiler.compileJar(new java.io.ByteArrayInputStream(bytes.toByteArray())),
                "assert.equal(e['Fixture.calculate'](20),41); assert.equal(e['Fixture.calculate'](-3),-5);");
    }

    @Test void rejectsUnsupportedJavacConstructsWithSourceMethodAndRemediation() throws Exception {
        for(String body:new String[]{
                "public static final String TEXT=\"text\"; public static int f(){ return 1; }",
                "public static Runnable f() { return () -> {}; }",
                "public static String f(int n) { return \"value=\"+n; }",
                "public static int f(int n) { switch(n) { case 1:return 2; case 5:return 3; default:return 0; } }",
                "public static int f() { try { return 1; } catch(RuntimeException e) { return 0; } }",
                "public static native int f();",
                "public static synchronized int f() { return 1; }",
                "public static long f() { return 4L; }",
                "public static int f() { return Math.abs(-4); }",
                "public static Class<?> f() { return Fixture.class; }",
                "public static int[] f() { return new int[2]; }",
                "static int x=compute(); static int compute(){ return 1; }",
                "public static int f(int n){ return n; } public static int f(){ return 1; }"}) {
            byte[] bytes=fixture("public class Fixture {\n"+body+"\n}");
            var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(bytes),body);
            assertTrue(error.getMessage().startsWith("Fixture.java:"),error.getMessage());
            assertTrue(error.getMessage().contains("Fixture"),error.getMessage());
            if (body.contains("try {")) assertTrue(error.getMessage().contains("Fixture.java:2:"),error.getMessage());
            assertTrue(error.getMessage().contains("unsupported"),error.getMessage());
            assertTrue(error.getMessage().contains("; "),error.getMessage());
        }
    }

    @Test void diagnosesOpcodeAndMissingDebugInformation() {
        ClassWriter writer=new ClassWriter(0);
        writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"NoDebug",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"dynamic","()V",null,null);
        method.visitCode();
        method.visitInvokeDynamicInsn("run","()V",new Handle(Opcodes.H_INVOKESTATIC,"Bootstrap","bootstrap","()V",false));
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(0,0); method.visitEnd(); writer.visitEnd();
        var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(writer.toByteArray()));
        assertTrue(error.getMessage().contains("INVOKEDYNAMIC"),error.getMessage());
        assertTrue(error.getMessage().contains("line unavailable"),error.getMessage());
        assertTrue(error.getMessage().contains("NoDebug.dynamic()V"),error.getMessage());
    }

    @Test void checksActualMavenDependencyJarsAndReportsUnsupportedFeatures() throws Exception {
        for(Class<?> library:new Class<?>[]{org.objectweb.asm.Type.class,org.junit.jupiter.api.Assertions.class}) {
            var jar=Path.of(library.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
            assertTrue(jar.getName().endsWith(".jar"));
            var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJar(jar));
            assertTrue(error.getMessage().contains("unsupported"),error.getMessage());
            assertTrue(error.getMessage().contains("; "),error.getMessage());
        }
    }
}
