package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Differential JVM-versus-Wasm harness for agent-kit task tests.
 *
 * <p>{@link #matchesJvm} compiles a Java 17 fixture with javac, runs the named methods on this JVM to get the
 * expected values, compiles the same classes with the bundled bytecode runtime, and checks that Wasm returns the
 * same values. It does this for both JAR entry orders and two fresh instances that start from dirty (0x7f-filled)
 * memory. {@link #rejectsUnusedCall} checks that admission rejects a call to a member the runtime does not declare.
 *
 * <p>Agents must not edit this class. A failure message that starts with "FIX THE RUNTIME" means the runtime
 * sources need work; one that starts with "TEST PROBLEM" means the test itself is wrong and must be reported.
 */
public abstract class KitDifferential implements Opcodes {
    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)");
    private static final long TIMEOUT_SECONDS = 120;

    @TempDir protected Path directory;

    /**
     * Compiles {@code source} (one public top-level class in the default package, plus any helper classes), runs each
     * named {@code public static} no-argument method on the JVM, and asserts that Wasm returns the same value.
     * Methods must return {@code String}, {@code int}, {@code long}, {@code boolean} or {@code char}, and must catch
     * their own exceptions.
     */
    protected final void matchesJvm(String source, String... methods) throws Exception {
        assertTrue(methods.length>0, "TEST PROBLEM: name at least one fixture method");
        Path classes=compileFixture(source);
        String type=publicClass(source);
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{classes.toUri().toURL()},null)) {
            Class<?> fixture=loader.loadClass(type);
            for(String name:methods)assertions.append(assertion(type,name,expected(fixture,name)));
        }
        for(boolean reverse:new boolean[]{false,true}) {
            String wat;
            try {
                wat=BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(classes,reverse)));
            } catch(IllegalArgumentException|java.io.IOException rejected) {
                throw new AssertionError("FIX THE RUNTIME: the bytecode compiler rejected the fixture. Usually a runtime "
                        +"class or member is missing, or a runtime source uses an unsupported construct (see "
                        +"javaw-agent-kit/RULES.md).\n"+rejected.getMessage(), rejected);
            }
            execute(wat,assertions.toString());
        }
    }

    /**
     * Asserts that admission rejects an otherwise unused static method that calls {@code owner.name descriptor}
     * with {@code opcode}, and that the diagnostic names the caller's source line. Use it for members a task
     * deliberately leaves out.
     */
    protected final void rejectsUnusedCall(String owner, String name, String descriptor, int opcode, boolean isInterface) {
        ClassWriter writer=new ClassWriter(0);
        writer.visit(V17,ACC_PUBLIC,"UnusedCall",null,"java/lang/Object",null);
        writer.visitSource("UnusedCall.java",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()V",null,null);
        method.visitCode();
        Label line=new Label();method.visitLabel(line);method.visitLineNumber(41,line);
        int slots=0;
        if(opcode!=INVOKESTATIC){method.visitInsn(ACONST_NULL);slots++;}
        for(org.objectweb.asm.Type argument:org.objectweb.asm.Type.getArgumentTypes(descriptor)){pushDefault(method,argument);slots+=argument.getSize();}
        method.visitMethodInsn(opcode,owner,name,descriptor,isInterface);
        org.objectweb.asm.Type result=org.objectweb.asm.Type.getReturnType(descriptor);
        if(result.getSize()==1)method.visitInsn(POP);else if(result.getSize()==2)method.visitInsn(POP2);
        method.visitInsn(RETURN);
        method.visitMaxs(Math.max(slots,2),0);method.visitEnd();writer.visitEnd();
        Throwable failure=assertThrows(Throwable.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()),
                "Admission accepted a call to "+owner+"."+name+descriptor+", which this task says must stay unlinked. Remove that member from the runtime.");
        assertTrue(String.valueOf(failure.getMessage()).contains("UnusedCall.java:41"),
                "The rejection must name the caller's source line; got: "+failure.getMessage());
    }

    private static void pushDefault(MethodVisitor method, org.objectweb.asm.Type type) {
        switch(type.getSort()) {
            case org.objectweb.asm.Type.LONG -> method.visitInsn(LCONST_0);
            case org.objectweb.asm.Type.FLOAT -> method.visitInsn(FCONST_0);
            case org.objectweb.asm.Type.DOUBLE -> method.visitInsn(DCONST_0);
            case org.objectweb.asm.Type.OBJECT, org.objectweb.asm.Type.ARRAY -> method.visitInsn(ACONST_NULL);
            default -> method.visitInsn(ICONST_0);
        }
    }

    private Path compileFixture(String source) throws Exception {
        String type=publicClass(source);
        Path sources=Files.createDirectories(directory.resolve("fixture-src")),classes=Files.createDirectories(directory.resolve("fixture-classes"));
        Path file=sources.resolve(type+".java");
        Files.writeString(file,source);
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        int status=ToolProvider.getSystemJavaCompiler().run(null,output,output,
                "-g","-parameters","--release","17","-d",classes.toString(),file.toString());
        assertEquals(0,status,"TEST PROBLEM: the fixture does not compile with javac --release 17\n"+output);
        return classes;
    }

    private static String publicClass(String source) {
        Matcher matcher=PUBLIC_CLASS.matcher(source);
        if(!matcher.find())throw new AssertionError("TEST PROBLEM: the fixture needs one public top-level class");
        return matcher.group(1);
    }

    private static Object expected(Class<?> fixture, String name) throws Exception {
        Method method;
        try {
            method=fixture.getMethod(name);
        } catch(NoSuchMethodException missing) {
            throw new AssertionError("TEST PROBLEM: fixture has no public static method "+name+"()");
        }
        try {
            return method.invoke(null);
        } catch(InvocationTargetException thrown) {
            throw new AssertionError("TEST PROBLEM: fixture method "+name+"() threw on the JVM: "+thrown.getCause(),thrown.getCause());
        }
    }

    private static String assertion(String type, String name, Object expected) {
        String method=type+"."+name;
        return "{let value;try{value=e['"+method+"']();}catch(thrown){throw new Error("+js(method+"() threw an uncaught exception in Wasm but returned normally on the JVM. JVM result: ")
                +"+"+js(String.valueOf(expected))+"+' (thrown: '+thrown+')');}\n"+comparison(type,name,expected)+"}\n";
    }

    private static String comparison(String type, String name, Object expected) {
        String call="value";
        String label=js(type+"."+name+"() returned a different value in Wasm than on the JVM");
        if(expected==null||expected instanceof String)return "assert.equal(text("+call+"),"+js((String)expected)+","+label+");\n";
        if(expected instanceof Integer||expected instanceof Short||expected instanceof Byte)return "assert.equal("+call+","+expected+","+label+");\n";
        if(expected instanceof Character c)return "assert.equal("+call+","+(int)c+","+label+");\n";
        if(expected instanceof Boolean b)return "assert.equal("+call+","+(b?1:0)+","+label+");\n";
        if(expected instanceof Long l)return "assert.equal("+call+","+l+"n,"+label+");\n";
        throw new AssertionError("TEST PROBLEM: "+name+"() returns "+expected.getClass().getName()+"; use String, int, long, boolean or char");
    }

    private static String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }

    private static byte[] jar(Path classes, boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(classes)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\','/')));
                jar.write(Files.readAllBytes(file));
                jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void execute(String wat, String assertions) throws Exception {
        Path input=directory.resolve("fixture.wat"),output=directory.resolve("fixture.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);
        run(directory.resolve("wat2wasm.log"),Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS),
                "FIX THE RUNTIME: wat2wasm rejected the generated module (a compiler problem; report it if the runtime sources look fine)");
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}\n");
        run(directory.resolve("node.log"),List.of("node",script.toString(),output.toString()),
                "FIX THE RUNTIME: Wasm execution differs from the JVM (or trapped)");
    }

    private static void run(Path log, List<String> command, String problem) throws Exception {
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(TIMEOUT_SECONDS,TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail(problem+": timed out after "+TIMEOUT_SECONDS+" s (infinite loop?)");
        }
        if(process.exitValue()!=0)fail(problem+"\n"+Files.readString(log));
    }
}
