package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodePropertyLookupTest implements Opcodes {
    @TempDir Path directory;
    private final String fixtureClassName="Metadata";
    @Test void isolatedProperties() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             public static int run(int group){int h=1;String key="javaw.test.properties.key";
              Function<String,String> read=System::getProperty,clear=System::clearProperty;
              BiFunction<String,String,String> write=System::setProperty,defaults=System::getProperty;
              h=31*h+(read.apply(key)==null?1:0);
              h=31*h+defaults.apply(key,"fallback").hashCode();
              h=31*h+(write.apply(key,"first")==null?1:0);
              h=31*h+write.apply(new String(key),"second").hashCode();
              h=31*h+read.apply(key).hashCode();h=31*h+clear.apply(key).hashCode();
              h=31*h+(System.clearProperty(key)==null?1:0);
              for(int i=0;i<4;i++)System.setProperty(key+i,"v"+i);
              for(int i:new int[]{2,3,0,1})h=31*h+System.clearProperty(key+i).hashCode();
              for(String invalid:new String[]{null,""}){
               try{System.getProperty(invalid);throw new Error();}catch(NullPointerException e){h+=3;}catch(IllegalArgumentException e){h+=5;}
               try{System.getProperty(invalid,null);throw new Error();}catch(NullPointerException e){h+=7;}catch(IllegalArgumentException e){h+=11;}
               try{System.setProperty(invalid,null);throw new Error();}catch(NullPointerException e){h+=13;}catch(IllegalArgumentException e){h+=17;}
               try{System.clearProperty(invalid);throw new Error();}catch(NullPointerException e){h+=19;}catch(IllegalArgumentException e){h+=23;}
              }
              try{System.setProperty(key,null);throw new Error();}catch(NullPointerException e){h++;}
              System.setProperty(key,"");h=31*h+System.getProperty(key,"ignored").length();System.clearProperty(key);
              return h;
             }
             public static int checkDefault(){return System.getProperty("java.version")==null?1:0;}
             public static int checkIsolation(){String previous=System.setProperty("javaw.test.isolation","left");return previous==null?1:0;}
            }
            """);compare(1);
    }

    @Test void booleanProperty() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;String key="javaw.test.boolean";
             Predicate<String> read=Boolean::getBoolean;
             for(String value:new String[]{null,"","true","TRUE","TrUe","false"," true","true ","1","yes","TRUE\u0000"}){
              System.clearProperty(key);if(value!=null)System.setProperty(key,value);
              h=31*h+(Boolean.getBoolean(key)?1:0);h=31*h+(read.test(key)?1:0);
             }
             h=31*h+(read.test(null)?1:0);h=31*h+(read.test("")?1:0);System.clearProperty(key);return h;}}
            """);compare(1);
    }

    @Test void integer_property() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             static int hash(Integer value){return value==null?17:31*value.hashCode()+(value==Integer.valueOf(value.intValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.int";Function<String,Integer> read=Integer::getInteger;
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Integer.getInteger(key));h=31*h+hash(read.apply(key));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.apply(bad));
              System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void integer_primitive_default() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {interface PrimitiveLookup{Integer get(String key,int fallback);}
             static int hash(Integer value){return value==null?17:31*value.hashCode()+(value==Integer.valueOf(value.intValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.int";PrimitiveLookup read=Integer::getInteger;int fallback=Integer.MIN_VALUE;
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Integer.getInteger(key,fallback));h=31*h+hash(read.get(key,fallback));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.get(bad,fallback));
              System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void integer_boxed_default() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             static int hash(Integer value){return value==null?17:31*value.hashCode()+(value==Integer.valueOf(value.intValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.int";BiFunction<String,Integer,Integer> read=Integer::getInteger;Integer fallback=new Integer(1000);
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Integer.getInteger(key,fallback));h=31*h+hash(read.apply(key,fallback));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.apply(bad,fallback));
              h=31*h+(Integer.getInteger("",fallback)==fallback?1:0);h=31*h+(Integer.getInteger(null,(Integer)null)==null?1:0);System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void long_property() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             static int hash(Long value){return value==null?17:31*value.hashCode()+(value==Long.valueOf(value.longValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.long";Function<String,Long> read=Long::getLong;
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Long.getLong(key));h=31*h+hash(read.apply(key));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.apply(bad));
              System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void long_primitive_default() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {interface PrimitiveLookup{Long get(String key,long fallback);}
             static int hash(Long value){return value==null?17:31*value.hashCode()+(value==Long.valueOf(value.longValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.long";PrimitiveLookup read=Long::getLong;long fallback=Long.MIN_VALUE;
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Long.getLong(key,fallback));h=31*h+hash(read.get(key,fallback));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.get(bad,fallback));
              System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void long_boxed_default() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             static int hash(Long value){return value==null?17:31*value.hashCode()+(value==Long.valueOf(value.longValue())?1:0);}
             public static int run(int group){int h=1;String key="javaw.test.long";BiFunction<String,Long,Long> read=Long::getLong;Long fallback=new Long(1000);
              for(String text:new String[]{null,"","0","-0","+17","-17","077","0x7f","0X7F","#ff","-#80","+0x11","09","0x","++1"," 1","1 ","1L","0x-1","\u0661\u0662","2147483647","-2147483648","2147483648","-2147483649","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","-0x8000000000000000","0x7fffffffffffffff"}){
               System.clearProperty(key);if(text!=null)System.setProperty(key,text);
               h=31*h+hash(Long.getLong(key,fallback));h=31*h+hash(read.apply(key,fallback));
              }
              for(String bad:new String[]{null,"","javaw.test.missing"," javaw.test.missing "})h=31*h+hash(read.apply(bad,fallback));
              h=31*h+(Long.getLong("",fallback)==fallback?1:0);h=31*h+(Long.getLong(null,(Long)null)==null?1:0);System.clearProperty(key);return h;
             }
            }
            """);compare(1);
    }

    @Test void pinnedSpringPropertyInitialization() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",System.getProperty("user.home")+"/.m2/repository"))
            .resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertEquals("476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",
            HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        try(JarFile jar=new JarFile(artifact.toFile())) {
            for(String name:List.of("DefaultNamingPolicy","NamingPolicy","Predicate")) {
                String entry="org/springframework/cglib/core/"+name+".class";
                Path output=directory.resolve(entry);Files.createDirectories(output.getParent());
                Files.write(output,jar.getInputStream(jar.getJarEntry(entry)).readAllBytes());
            }
        }
        for(boolean stress:new boolean[]{false,true}) {
            fixture("""
                import org.springframework.cglib.core.DefaultNamingPolicy;
                public class Metadata {public static int run(int group){
                 String key="org.springframework.cglib.test.stressHashCodes";
                 System.setProperty(key,"STRESS");
                 DefaultNamingPolicy policy=DefaultNamingPolicy.INSTANCE;int h=1;
                 for(String prefix:new String[]{null,"java.lang.String","sample.Base"}){
                  String first=policy.getClassName(prefix,"demo.Emitter","stable-key",candidate->false);
                  h=31*h+first.hashCode();
                  int[] calls={0};String collisions=policy.getClassName(prefix,"demo.Emitter","stable-key",candidate->calls[0]++<2);
                  h=31*h+collisions.hashCode();h=31*h+calls[0];
                  System.setProperty(key,"CHANGED");
                  h=31*h+(first.equals(policy.getClassName(prefix,"demo.Emitter","stable-key",candidate->false))?1:0);
                 }
                 h=31*h+policy.hashCode();h=31*h+(policy.equals(new DefaultNamingPolicy())?1:0);
                 h=31*h+(policy.equals(null)?1:0);System.clearProperty(key);return h;
                }}
                """.replace("STRESS",Boolean.toString(stress)).replace("CHANGED",Boolean.toString(!stress)));
            compare(1);
        }
    }

    @Test void malformedPropertyAdmission() throws Exception {
        for(String name:List.of("getProperty","setProperty","clearProperty")) {
            String valid=name.equals("setProperty")?"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":"(Ljava/lang/String;)Ljava/lang/String;";
            for(int variant=0;variant<6;variant++) {
                ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadPropertyCalls",null,"java/lang/Object",null);
                writer.visitSource("PropertyCalls.java",null);
                MethodVisitor m=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);
                m.visitCode();Label line=new Label();m.visitLabel(line);m.visitLineNumber(41,line);
                if(variant<3) {
                    m.visitInsn(ACONST_NULL);m.visitInsn(ACONST_NULL);m.visitInsn(ACONST_NULL);
                    m.visitMethodInsn(variant==1?INVOKEVIRTUAL:INVOKESTATIC,"java/lang/System",name,variant==0?"(I)Ljava/lang/String;":valid,variant==2);
                } else {
                    Handle bad=new Handle(variant==3?H_INVOKEVIRTUAL:H_INVOKESTATIC,"java/lang/System",name,variant==4?"(I)Ljava/lang/String;":valid,variant==5);
                    m.visitLdcInsn(bad);
                }
                m.visitInsn(ARETURN);m.visitMaxs(4,0);m.visitEnd();writer.visitEnd();
                var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
                assertTrue(failure.getMessage().contains("PropertyCalls.java:41"),failure.getMessage());
                assertTrue(failure.getMessage().contains("unused"),failure.getMessage());
                assertTrue(failure.getMessage().contains("exact standard static System."+name),failure.getMessage());
            }
        }
        Handle malformed=new Handle(H_INVOKESTATIC,"java/lang/System","getProperty","(I)Ljava/lang/String;",false);
        ClassNode nested=new ClassNode();nested.version=V17;nested.access=ACC_PUBLIC;nested.name="BadNestedProperty";nested.superName="java/lang/Object";
        MethodNode method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"unused","()V",null,null);
        Handle bootstrap=new Handle(H_INVOKESTATIC,"java/lang/invoke/ConstantBootstraps","nullConstant","(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",false);
        method.instructions.add(new LdcInsnNode(new ConstantDynamic("bad","Ljava/lang/Object;",bootstrap,malformed)));
        method.instructions.add(new InsnNode(POP));method.instructions.add(new InsnNode(RETURN));method.maxStack=1;nested.methods.add(method);
        var nestedFailure=assertThrows(IllegalArgumentException.class,()->BytecodeSystemProperties.normalize(new ArrayList<>(List.of(nested))));
        assertTrue(nestedFailure.getMessage().contains("exact standard static System.getProperty"));
        MethodNode indy=new MethodNode(ACC_PUBLIC|ACC_STATIC,"dynamic","()V",null,null);
        indy.instructions.add(new InvokeDynamicInsnNode("bad","()Ljava/lang/Object;",bootstrap,malformed));nested.methods.clear();nested.methods.add(indy);
        assertThrows(IllegalArgumentException.class,()->BytecodeSystemProperties.normalize(new ArrayList<>(List.of(nested))));

        for(String owner:List.of("java/lang/Boolean","java/lang/Integer","java/lang/Long")) {
            String name=owner.endsWith("Boolean")?"getBoolean":owner.endsWith("Integer")?"getInteger":"getLong";
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadWrapperProperty",null,"java/lang/Object",null);
            MethodVisitor m=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);m.visitCode();m.visitInsn(ACONST_NULL);
            m.visitMethodInsn(INVOKESTATIC,owner,name,"(Ljava/lang/String;)Ljava/lang/Object;",false);m.visitInsn(ARETURN);m.visitMaxs(1,0);m.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains(name),failure.getMessage());
        }
        ClassWriter reserved=new ClassWriter(0);reserved.visit(V17,ACC_PUBLIC,"javaw/runtime/SystemProperties",null,"java/lang/Object",null);reserved.visitEnd();
        var collision=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(reserved.toByteArray()));
        assertTrue(collision.getMessage().contains("runtime redefinition"),collision.getMessage());
        fixture("public class Metadata {public static int run(int group){return System.getProperties().size();}}");
        assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))));
    }

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
        options.addAll(List.of("-classpath",directory.toString()));
        options.add(file.toString());options.add(oracle.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){
                String entry=directory.relativize(file).toString();
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString(),"CaseOracle",fixtureClassName,Integer.toString(groups));
        String expected=Files.readString(directory.resolve("command.log")).trim();
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),expected);
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"if(e['Metadata.checkDefault'])assert.equal(e['Metadata.checkDefault'](),1);\nif(e['Metadata.checkIsolation'])assert.equal(e['Metadata.checkIsolation'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        WasmTestSupport.node(script,output);
    }
}
