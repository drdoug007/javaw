package dastec.one.javaw.bytecode;

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

class BytecodeObjectIdentityTest implements Opcodes {
    @TempDir Path directory;

    private byte[] fixture() throws Exception {
        Path source=directory.resolve("Identity.java");
        Files.writeString(source,"""
            public class Identity {
                public static int plain(){
                    Object first=new Object(),second=new Object();
                    Plain child=new Plain();
                    Object inherited=child;
                    int hash=child.hashCode();
                    child.value=29;
                    Object[] garbage=new Object[17];
                    return (first.equals(first)?1:0)+(first.equals(second)?0:2)+(first.equals(null)?0:4)
                        +(inherited.equals(child)?8:0)+(child.hashCode()==hash?16:0)
                        +(hash==System.identityHashCode(child)?32:0)
                        +(first.hashCode()==System.identityHashCode(first)?64:0)
                        +(System.identityHashCode(null)==0?128:0);
                }
                public static int overrides(){
                    Changed one=new Changed(3),two=new Changed(3);
                    Object a=one,b=two;
                    Slot slot=one;
                    return (a.equals(b)?1:0)+(a.hashCode()==one.hashCode()?2:0)
                        +(slot.equals(b)?4:0)+(slot.hashCode()==one.hashCode()?8:0)
                        +(one.hashCode()!=System.identityHashCode(one)?16:0)
                        +(one.baseHash()==System.identityHashCode(one)?32:0)
                        +(one.baseEquals(one)?64:0)+(one.baseEquals(two)?0:128);
                }
                public static int inheritedInterface(){
                    Slot plain=new ImplementsSlot();
                    Object object=plain;
                    return (plain.equals(object)?1:0)+(plain.equals(new Object())?0:2)
                        +(plain.hashCode()==System.identityHashCode(object)?4:0);
                }
                public static int arrays(){
                    int[] ints=new int[2];int[] copy=ints.clone();
                    String[][] nested=new String[1][1];
                    Object object=ints;
                    int hash=ints.hashCode();ints[0]=13;
                    return (ints.equals(ints)?1:0)+(ints.equals(copy)?0:2)
                        +(ints.hashCode()==hash && hash==System.identityHashCode(ints)?4:0)
                        +(object.hashCode()==ints.hashCode()?8:0)+(object.equals(ints)?16:0)
                        +(nested.equals(nested)?32:0)+(nested.hashCode()==System.identityHashCode(nested)?64:0)
                        +(new Object[0].equals(new Object[0])?0:128);
                }
                public static int nulls(){
                    Object missing=null;int result=0;
                    try {missing.equals(null);}catch(NullPointerException e){result++;}
                    try {missing.hashCode();}catch(NullPointerException e){result+=2;}
                    int[] array=null;
                    try {array.hashCode();}catch(NullPointerException e){result+=4;}
                    return result+(System.identityHashCode(missing)==0?8:0);
                }
                public static int strings(){
                    Object text=new String(new char[]{'A','\\0','\\uD83D','\\uDE00'});
                    Object same="A\\0\\uD83D\\uDE00";
                    return (text.equals(same)?1:0)+(text.hashCode()==same.hashCode()?2:0)
                        +(text==same?0:4)+(same=="A\\0\\uD83D\\uDE00"?8:0)
                        +(System.identityHashCode(text)==System.identityHashCode(text)?16:0);
                }
            }
            class Plain { int value; }
            interface Slot { boolean equals(Object other); int hashCode(); }
            class ImplementsSlot implements Slot {}
            class Changed extends Plain implements Slot {
                Changed(int n){value=n;}
                public boolean equals(Object other){return other instanceof Changed && ((Changed)other).value==value;}
                public int hashCode(){return System.identityHashCode(this)^-1;}
                int baseHash(){return super.hashCode();}
                boolean baseEquals(Object other){return super.equals(other);}
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","--release","17",
                "-d",directory.toString(),source.toString()));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> classes;
        try(var files=Files.list(directory)) {
            classes=new ArrayList<>(files.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        if(reverse)Collections.reverse(classes);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:classes){jar.putNextEntry(new JarEntry(file.getFileName().toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    @Test void matchesJvmObjectArrayAndStringIdentityContractsAndPreservesOverrideDispatch() throws Exception {
        byte[] jar=fixture();StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Identity");
            for(String method:List.of("plain","overrides","inheritedInterface","arrays","nulls","strings"))
                checks.append("assert.equal(e['Identity.").append(method).append("'](),")
                        .append(jvm.getMethod(method).invoke(null)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(reverse?jar(true):jar)),checks.toString());
    }

    @Test void resolvesInheritedObjectSlotsOnLinkedArrayMarkerInterfaces() throws Exception {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        writer.visit(V17,ACC_PUBLIC|ACC_SUPER,"ArraySlots",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()I",null,null);
        method.visitCode();method.visitInsn(ICONST_1);method.visitIntInsn(NEWARRAY,T_INT);method.visitVarInsn(ASTORE,0);
        method.visitVarInsn(ALOAD,0);method.visitMethodInsn(INVOKEINTERFACE,"java/lang/Cloneable","hashCode","()I",true);
        method.visitVarInsn(ALOAD,0);method.visitMethodInsn(INVOKESTATIC,"java/lang/System","identityHashCode","(Ljava/lang/Object;)I",false);
        Label mismatch=new Label();method.visitJumpInsn(IF_ICMPNE,mismatch);
        method.visitVarInsn(ALOAD,0);method.visitVarInsn(ALOAD,0);
        method.visitMethodInsn(INVOKEINTERFACE,"java/io/Serializable","equals","(Ljava/lang/Object;)Z",true);
        method.visitInsn(IRETURN);method.visitLabel(mismatch);method.visitInsn(ICONST_0);method.visitInsn(IRETURN);
        method.visitMaxs(0,0);method.visitEnd();writer.visitEnd();byte[] bytes=writer.toByteArray();
        Class<?> jvm=new ClassLoader(null){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
        assertEquals(1,jvm.getMethod("run").invoke(null));
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(output)) {
            jar.putNextEntry(new JarEntry("ArraySlots.class"));jar.write(bytes);jar.closeEntry();

        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(output.toByteArray())),
                "assert.equal(e['ArraySlots.run'](),1);");
    }

    @Test void diagnosesUnsupportedObjectMethodsAndMalformedIdentityReferencesWithoutCallSites() throws Exception {
        for(String owner:List.of("java/lang/Object","[I")) {
            for(int opcode:new int[]{INVOKESTATIC,INVOKEINTERFACE}) {
                byte[] bytes=call(owner,"hashCode","()I",opcode,opcode==INVOKEINTERFACE);
                assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(bytes));
            }
        }
        for(byte[] bytes:List.of(call("java/lang/System","identityHashCode","(Ljava/lang/Object;)I",INVOKESTATIC,true),
                call("java/lang/Object","hashCode","(I)I",INVOKEVIRTUAL,false),
                call("java/lang/Object","wait","()V",INVOKEVIRTUAL,false),
                call("java/lang/Object","notify","()V",INVOKEVIRTUAL,false)))
            assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(bytes));
    }

    @Test void checksVisibilityOfUnusedObjectIdentityOverrides() throws Exception {
        for(int access:new int[]{0,ACC_PROTECTED}) {
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
            writer.visit(V17,ACC_PUBLIC|ACC_SUPER,"HiddenIdentity",null,"java/lang/Object",null);
            MethodVisitor hash=writer.visitMethod(access,"hashCode","()I",null,null);
            hash.visitCode();hash.visitInsn(ICONST_0);hash.visitInsn(IRETURN);hash.visitMaxs(0,0);hash.visitEnd();writer.visitEnd();
            String diagnostic=assertThrows(IllegalArgumentException.class,
                    ()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray())).getMessage();
            assertTrue(diagnostic.contains("visibility"),diagnostic);
        }
    }

    private byte[] call(String owner,String name,String descriptor,int opcode,boolean itf) {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17,ACC_PUBLIC|ACC_SUPER,"BadIdentity",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(ACC_PRIVATE|ACC_STATIC,"unused","(Ljava/lang/Object;)I",null,null);
        method.visitCode();method.visitVarInsn(ALOAD,0);
        method.visitMethodInsn(opcode,owner,name,descriptor,itf);
        method.visitInsn(IRETURN);method.visitMaxs(0,0);method.visitEnd();writer.visitEnd();
        return writer.toByteArray();
    }

    @Test void keepsLowLevelObjectIdentityCallsAsExplicitRuntimeDiagnostics() throws Exception {
        byte[] jar=fixture();
        assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJar(new ByteArrayInputStream(jar)));
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");
        Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("identity.wat"),output=directory.resolve("identity.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    const address=heap;heap+=size;new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
