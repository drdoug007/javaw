package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeClassMetadataTest implements Opcodes {
    @TempDir Path directory;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","--release","17",
                "-d",directory.toString(),file.toString()));
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private String fixtureSource() {
        return """
            public class Metadata {
                interface Root {}
                interface Leaf extends Root {}
                abstract static class Parent implements Root {}
                static class Child extends Parent implements Leaf,Cloneable {}
                private static final class Hidden {}
                public static Class<?> type(int index) {
                    switch(index) {
                        case 0:return Object.class; case 1:return Metadata.class;
                        case 2:return Root.class; case 3:return Leaf.class;
                        case 4:return Parent.class;case 5:return Child.class;
                        case 6:return Hidden.class;case 7:return Class.class;
                        case 8:return int[].class;case 9:return Child[][].class;
                        case 10:return Object[].class;case 11:return Parent[].class;
                        case 12:return boolean.class;case 13:return byte.class;
                        case 14:return char.class;case 15:return short.class;
                        case 16:return int.class;case 17:return long.class;
                        case 18:return float.class;case 19:return double.class;
                        case 20:return void.class;case 21:return Cloneable.class;
                        case 22:return java.io.Serializable.class;case 23:return int[][].class;
                        case 24:class Local {} return Local.class;
                        case 25:return new Object(){}.getClass();
                        default:return String.class;
                    }
                }
                public static String name(int n){return type(n).getName();}
                public static String simple(int n){return type(n).getSimpleName();}
                public static String canonical(int n){return type(n).getCanonicalName();}
                public static String typeName(int n){return type(n).getTypeName();}
                public static String text(int n){return type(n).toString();}
                public static int modifiers(int n){return type(n).getModifiers();}
                public static int flags(int n){Class<?> c=type(n);return (c.isArray()?1:0)+(c.isPrimitive()?2:0)+(c.isInterface()?4:0);}
                public static String parent(int n){Class<?> c=type(n).getSuperclass();return c==null?null:c.getName();}
                public static String component(int n){Class<?> c=type(n).getComponentType();return c==null?null:c.getName();}
                public static int count(int n){return type(n).getInterfaces().length;}
                public static String iface(int n,int i){return type(n).getInterfaces()[i].getName();}
                public static int assignable(int from,int to){return type(to).isAssignableFrom(type(from))?1:0;}
                public static int identity(){
                    Object child=new Child();Class<?> c=child.getClass();
                    return (c==Child.class?1:0)+(c.equals(Child.class)?2:0)+(c==Parent.class?0:4)
                        +(c.getClass()==Class.class?8:0)+(c.hashCode()==System.identityHashCode(c)?16:0)
                        +(new Child[0].getClass()==Child[].class?32:0)+(new int[0].getClass()==int[].class?64:0)
                        +(new String(new char[]{'a'}).getClass()==String.class?128:0);
                }
                public static int defensive(){Class<?>[] interfaces=Child.class.getInterfaces();interfaces[0]=Object.class;
                    return Child.class.getInterfaces()[0]==Leaf.class?1:0;}
                public static int instances(){
                    Object child=new Child(),array=new Child[0];int count=0;
                    if(Parent.class.isInstance(child))count++;
                    if(Root.class.isInstance(child))count+=2;
                    if(Object[].class.isInstance(array))count+=4;
                    if(Cloneable.class.isInstance(array))count+=8;
                    if(!int.class.isInstance(child) && !Child.class.isInstance(null))count+=16;
                    if(Parent.class.cast(child)==child && Child.class.cast(null)==null)count+=32;
                    if(Child.class.asSubclass(Parent.class)==Child.class)count+=64;
                    try{Child.class.cast(new Object());}catch(ClassCastException e){count+=128;}
                    try{Child.class.asSubclass(String.class);}catch(ClassCastException e){count+=256;}
                    try{Child.class.isAssignableFrom(null);}catch(NullPointerException e){count+=512;}
                    return count;
                }
                public static int dormant(){
                    Class<?> c=Dormant.class;
                    return Counts.value+(c==Dormant.class && c.getSuperclass()==Object.class && c.getName().equals("Dormant")
                        && c.getInterfaces()[0]==Sleeping.class?1:0);
                }
                public static int initialize(){new Dormant();return Counts.value;}
                public static int failed(){
                    try {new Failed();}catch(ExceptionInInitializerError e){}
                    Class<?> c=Failed.class;
                    return c==Failed.class && c.getName().equals("Failed") ? Counts.value:0;
                }
            }
            class Counts {static int value;static int bump(){return ++value;}}
            interface Sleeping {int MARKER=Counts.bump();default int method(){return 1;}}
            class Dormant implements Sleeping {static {Counts.value++;}}
            class Failed {static {if(true){Counts.value+=10;throw new RuntimeException();}}}
            """;
    }

    @Test void matchesJvmNamesFlagsHierarchyComponentsAndArrayCovarianceWithoutInitializingTypes() throws Exception {
        byte[] jar=fixture(fixtureSource());StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("dormant","identity","defensive","instances","dormant","initialize","dormant","failed","dormant"))
                checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(jvm.getMethod(method).invoke(null)).append(");\n");
            for(int n=0;n<=26;n++) {
                for(String method:List.of("name","simple","canonical","typeName","text","parent","component"))
                    checks.append("assert.equal(text(e['Metadata.").append(method).append("'](").append(n).append(")),")
                            .append(js((String)jvm.getMethod(method,int.class).invoke(null,n))).append(");\n");
                for(String method:List.of("modifiers","flags","count")) {
                    // The isolated Class runtime exposes its supplied reflection interfaces.
                    if((n==7 || n==26) && method.equals("count"))continue;
                    checks.append("assert.equal(e['Metadata.").append(method).append("'](").append(n).append("),")
                            .append(jvm.getMethod(method,int.class).invoke(null,n)).append(");\n");
                }
                if(n!=7 && n!=26) {
                    int count=(Integer)jvm.getMethod("count",int.class).invoke(null,n);
                    for(int i=0;i<count;i++)checks.append("assert.equal(text(e['Metadata.iface'](").append(n).append(',').append(i)
                            .append(")),").append(js((String)jvm.getMethod("iface",int.class,int.class).invoke(null,n,i))).append(");\n");
                }
                // Bundled String intentionally supplies a smaller interface surface than the host JDK.
                if(n==26)checks.append("assert.equal(e['Metadata.count'](26),3);\n"
                        +"assert.equal(text(e['Metadata.iface'](26,0)), 'java.lang.CharSequence');\n"
                        +"assert.equal(text(e['Metadata.iface'](26,1)), 'java.lang.Comparable');\n"
                        +"assert.equal(text(e['Metadata.iface'](26,2)), 'java.io.Serializable');\n");
            }
            for(int from=0;from<=25;from++)for(int to=0;to<=25;to++)
                checks.append("assert.equal(e['Metadata.assignable'](").append(from).append(',').append(to).append("),")
                        .append(jvm.getMethod("assignable",int.class,int.class).invoke(null,from,to)).append(");\n");
        }
        // Direct isolated Class interfaces: Serializable, GenericDeclaration and Type.
        checks.append("assert.equal(e['Metadata.count'](7),3);\n");
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),checks.toString());
    }

    @Test void internsDynamicUtf16StringsBeforeLiteralsAndConstantValuesAndPreservesObjectTextDispatch() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                public static int intern(){
                    String first=new String(new char[]{'z','Q',0,'\\uD83D','\\uDE00'});
                    String second=new String(new char[]{'z','Q',0,'\\uD83D','\\uDE00'});
                    String canonical=first.intern();
                    return (canonical==first?1:0)+(second.intern()==canonical?2:0)
                        +(canonical=="zQ\\0\\uD83D\\uDE00"?4:0)+(Text.VALUE==canonical?8:0)
                        +(first.equals(second)?16:0)+(first.hashCode()==second.hashCode()?32:0);
                }
                public static String named(){return ((Object)new Named()).toString();}
                public static String custom(){return ((Object)new Custom()).toString();}
                public static String parent(){return new Custom().parent();}
                public static int nulls(){Object value=null;int result=0;
                    try{value.getClass();}catch(NullPointerException e){result++;}
                    try{value.toString();}catch(NullPointerException e){result+=2;}
                    return result;
                }
                public static int empty(){return new String().intern()==""?1:0;}
            }
            class Text {static final String VALUE="zQ\\0\\uD83D\\uDE00";}
            class Named {public int hashCode(){return 0xabcdef10;}}
            class Custom extends Named {
                public String toString(){return "custom";}
                String parent(){return super.toString();}
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("intern","nulls","empty"))checks.append("assert.equal(e['Metadata.").append(method)
                    .append("'](),").append(jvm.getMethod(method).invoke(null)).append(");\n");
            for(String method:List.of("named","custom","parent"))checks.append("assert.equal(text(e['Metadata.").append(method)
                    .append("']()),").append(js((String)jvm.getMethod(method).invoke(null))).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),checks.toString());
    }

    @Test void clonesCloneableObjectsShallowlyWithExactRuntimeClassAndPreservesFailures() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                public static int cloneObjects() throws CloneNotSupportedException {
                    Leaf original=new Leaf(); original.number=9007199254740993L;original.value=2.25;
                    original.child=new Object();original.values=new int[]{7};
                    Leaf copy=(Leaf)original.copy();copy.number=5;
                    return (copy!=original?1:0)+(copy.getClass()==Leaf.class?2:0)
                        +(original.number==9007199254740993L && copy.value==2.25?4:0)
                        +(copy.child==original.child && copy.values==original.values?8:0)
                        +(original.hashCode()==System.identityHashCode(original)?16:0);
                }
                public static int rejected(){try{new NotCloneable().copy();return 0;}catch(CloneNotSupportedException e){return 1;}}
                public static String override(){return new OwnClone().clone().toString();}
            }
            class Parent {long number; Object child;}
            class Leaf extends Parent implements Cloneable {double value;int[] values;Object copy() throws CloneNotSupportedException{return super.clone();}}
            class NotCloneable {Object copy() throws CloneNotSupportedException{return super.clone();}}
            class OwnClone {public Object clone(){return "override";}}
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"""
            assert.equal(e['Metadata.cloneObjects'](),31);
            assert.equal(e['Metadata.rejected'](),1);
            assert.equal(text(e['Metadata.override']()),'override');
            """);
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");assertEquals(31,jvm.getMethod("cloneObjects").invoke(null));
            assertEquals(1,jvm.getMethod("rejected").invoke(null));assertEquals("override",jvm.getMethod("override").invoke(null));
        }
    }

    @Test void rendersArrayAndObjectNamesWithUnsignedHashesAndUnwindsCloneAllocationFailures() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                static int[] numbers=new int[]{1};static Object value=new Object();static Item item=new Item();
                public static String arrayText(){return numbers.toString();}
                public static String objectText(){return value.toString();}
                public static int arrayHash(){return numbers.hashCode();}
                public static int objectHash(){return value.hashCode();}
                public static Object copy() throws CloneNotSupportedException {return item.copy();}
                public static int ready(){return item.number;}
                public static int finalizer() throws Throwable {new Finalized().run();return 1;}
            }
            class Item implements Cloneable {int number=7;Object copy() throws CloneNotSupportedException{return super.clone();}}
            class Finalized {void run() throws Throwable {super.finalize();}}
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"""
            assert.equal(text(e['Metadata.arrayText']()),'[I@'+(e['Metadata.arrayHash']()>>>0).toString(16));
            assert.equal(text(e['Metadata.objectText']()),'java.lang.Object@'+(e['Metadata.objectHash']()>>>0).toString(16));
            assert.equal(e['Metadata.ready'](),7);
            failNext();
            assert.throws(()=>e['Metadata.copy'](),error=>error instanceof WebAssembly.Exception);
            assert.equal(e['Metadata.ready'](),7);assert.notEqual(e['Metadata.copy'](),0);
            assert.equal(e['Metadata.finalizer'](),1);
            """);
    }

    @Test void retriesFailedMirrorAllocationAndNeverAllocatesOrInitializesTypesAtInstantiation() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {public static Class<?> literal(){return Dormant.class;}}
            class Dormant {static {if(true)throw new RuntimeException();}}
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar)),"""
            failNext();
            assert.throws(()=>e['Metadata.literal'](),error=>error instanceof WebAssembly.Exception);
            const mirror=e['Metadata.literal']();assert.notEqual(mirror,0);assert.equal(e['Metadata.literal'](),mirror);
            """);
    }

    @Test void reportsSyntheticAnnotationAndEnumFlagsFromClosedWorldDeclarations() throws Exception {
        Map<String,byte[]> definitions=new LinkedHashMap<>();
        for(String name:List.of("SyntheticType","EnumFlagOnly","AnnotationType")) {
            boolean annotation=name.equals("AnnotationType"),iface=annotation || name.startsWith("java/");
            int flags=ACC_PUBLIC|(iface?ACC_ABSTRACT|ACC_INTERFACE:ACC_SUPER)
                    |(annotation?ACC_ANNOTATION:0)|(name.equals("SyntheticType")?ACC_SYNTHETIC:0)
                    |(name.equals("EnumFlagOnly")?ACC_ENUM:0);
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,flags,name,null,"java/lang/Object",
                    annotation?new String[]{"java/lang/annotation/Annotation"}:null);writer.visitEnd();definitions.put(name,writer.toByteArray());
        }
        ClassWriter entry=new ClassWriter(ClassWriter.COMPUTE_MAXS);entry.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);
        MethodVisitor method=entry.visitMethod(ACC_PUBLIC|ACC_STATIC,"flags","()I",null,null);method.visitCode();
        method.visitLdcInsn(Type.getObjectType("SyntheticType"));method.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Class","isSynthetic","()Z",false);
        method.visitLdcInsn(Type.getObjectType("AnnotationType"));method.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Class","isAnnotation","()Z",false);method.visitInsn(IADD);
        method.visitLdcInsn(Type.getObjectType("EnumFlagOnly"));method.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Class","isEnum","()Z",false);method.visitInsn(IADD);
        method.visitInsn(IRETURN);method.visitMaxs(0,0);method.visitEnd();entry.visitEnd();definitions.put("Metadata",entry.toByteArray());
        ClassLoader loader=new ClassLoader(null){protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes=definitions.get(name.replace('.','/'));if(bytes==null)throw new ClassNotFoundException(name);
            return defineClass(name,bytes,0,bytes.length);
        }};
        assertEquals(2,loader.loadClass("Metadata").getMethod("flags").invoke(null));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(var definition:definitions.entrySet()){jar.putNextEntry(new JarEntry(definition.getKey()+".class"));jar.write(definition.getValue());jar.closeEntry();}
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(bytes.toByteArray())),
                "assert.equal(e['Metadata.flags'](),2);");
    }

    @Test void rejectsUnlinkedInaccessibleClassAndMethodTypeConstantsAndUnsupportedHandles() throws Exception {
        for(Object constant:List.of(Type.getObjectType("missing/Type"),Type.getType("[Lmissing/Type;"),
                Type.getMethodType("(Lmissing/Type;)V"),new Handle(H_INVOKESTATIC,"missing/Type","run","()V",false))) {
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);writer.visit(V17,ACC_PUBLIC,"BadLiteral",null,"java/lang/Object",null);
            MethodVisitor method=writer.visitMethod(ACC_PRIVATE|ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);
            method.visitCode();method.visitLdcInsn(constant);method.visitInsn(ARETURN);method.visitMaxs(0,0);method.visitEnd();writer.visitEnd();
            assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        }
        for(String descriptor:List.of("Lp/Hidden;","[Lp/Hidden;")) {
            ClassWriter hidden=new ClassWriter(0);hidden.visit(V17,ACC_SUPER,"p/Hidden",null,"java/lang/Object",null);hidden.visitEnd();
            ClassWriter caller=new ClassWriter(ClassWriter.COMPUTE_MAXS);caller.visit(V17,ACC_PUBLIC,"q/Caller",null,"java/lang/Object",null);
            MethodVisitor literal=caller.visitMethod(ACC_PRIVATE|ACC_STATIC,"unused","()Ljava/lang/Class;",null,null);
            literal.visitCode();literal.visitLdcInsn(Type.getType(descriptor));literal.visitInsn(ARETURN);
            literal.visitMaxs(0,0);literal.visitEnd();caller.visitEnd();
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(JarOutputStream jar=new JarOutputStream(bytes)) {
                jar.putNextEntry(new JarEntry("p/Hidden.class"));jar.write(hidden.toByteArray());jar.closeEntry();
                jar.putNextEntry(new JarEntry("q/Caller.class"));jar.write(caller.toByteArray());jar.closeEntry();
            }
            String diagnostic=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(
                    new ByteArrayInputStream(bytes.toByteArray()))).getMessage();
            assertTrue(diagnostic.contains("inaccessible"),diagnostic);
        }
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
                // Fail or count allocation requests through whichever allocator the module uses.
                const failNext=()=>{if(e.__gc_fail_allocations)e.__gc_fail_allocations.value=1;else failAllocation=true;},allocated=()=>e.__gc_allocations?.value??allocations;
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        WasmTestSupport.node(script,output);
    }
}
