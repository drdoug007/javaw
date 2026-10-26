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

class BytecodeTextFormatTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private byte[] pinnedCarrier;
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

    @Test void localeIdentities() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {
                public static int run(int group) {
                    String[] languages={"EN","he","iw","yi","ji","id","in","","und","tr"};
                    String[] countries={"us","","gb"}; int h=1;
                    for(String language:languages)for(String country:countries)for(String variant:new String[]{"","V"}) {
                        Locale l=new Locale(language,country,variant);
                        h=31*h+l.getLanguage().hashCode();h=31*h+l.getCountry().hashCode();
                        h=31*h+l.getVariant().hashCode();h=31*h+l.toString().hashCode();h=31*h+l.hashCode();
                        h=31*h+(l.equals(l.clone())?1:0);h=31*h+(l!=l.clone()?1:0);
                    }
                    h=31*h+Locale.getDefault().hashCode();h=31*h+(Locale.PRC==Locale.CHINA?1:0);
                    try {new Locale(null);return 0;}catch(NullPointerException expected){return h;}
                }
            }
            """);compare(1);
    }

    @Test void unicodeUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {
                public static int run(int group) {
                    int h=1,start=group*1024;
                    for(int cp=start;cp<start+1024 && cp<=0x10ffff;cp++) {
                        String text=new String(Character.toChars(cp));
                        String upper=text.toUpperCase(Locale.ROOT);
                        h=31*h+upper.hashCode();h=31*h+(upper==text?1:0);
                    }
                    return h;
                }
            }
            """);compare(1088);
    }

    @Test void turkicUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {public static int run(int group){int h=1;
                for(String lang:new String[]{"tr","az","TR","AZ","en"}) {
                    Locale l=new Locale(lang,"tr");
                    for(int cp=group*256;cp<(group+1)*256;cp+=17){String s="i"+new String(Character.toChars(cp))+"ıİi";h=31*h+s.toUpperCase(l).hashCode();}
                }
                try {"".toUpperCase((Locale)null);return 0;}catch(NullPointerException expected){return h;}
            }}
            """);compare(256);
    }

    @Test void lithuanianUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {public static int run(int group){int h=1;Locale l=new Locale("lt");
                int[] bases={105,106,303,616,1110,1112,7522,7725,7883,8305,97,304,0x10428,0xd800};
                for(int base:bases)for(int cc=group*256;cc<(group+1)*256;cc+=13){
                    String s=new String(Character.toChars(base))+new String(Character.toChars(cc))+"̇̇";
                    h=31*h+s.toUpperCase(l).hashCode();
                }
                for(int base:bases)h=31*h+(new String(Character.toChars(base))+"̇").toUpperCase(l).hashCode();
                return h;
            }}
            """);compare(256);
    }

    @Test void defaultUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            import java.util.function.Function;
            public class Metadata {public static int run(int group){int h=1;Function<String,String> f=String::toUpperCase;
                for(String s:new String[]{"","abc","ABC","Straße","iıİ","ﬃ","ᾀ","𐐨",new String(new char[]{(char)0xd800,120,(char)0xdc00}),"ȧi̇"}) {
                    h=31*h+s.toUpperCase().hashCode();h=31*h+f.apply(s).hashCode();h=31*h+(s==s.toUpperCase()?1:0);
                    h=31*h+(s.toUpperCase().equals(s.toUpperCase(Locale.ROOT))?1:0);
                }return h;
            }}
            """);compare(1);
    }

    @Test void textFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
              static int failure(String s){try{String.format(s,"x");return 0;}
               catch(DuplicateFormatFlagsException e){return 1+e.getFlags().hashCode();}
               catch(MissingFormatWidthException e){return 2+e.getFormatSpecifier().hashCode();}
               catch(UnknownFormatConversionException e){return 3+e.getConversion().hashCode();}
               catch(IllegalFormatPrecisionException e){return 4+e.getPrecision();}
               catch(IllegalFormatFlagsException e){return 5+e.getFlags().hashCode();}
               catch(MissingFormatArgumentException e){return 6+e.getFormatSpecifier().hashCode();}
               catch(IllegalFormatWidthException e){return 7+e.getWidth();}
               catch(FormatFlagsConversionMismatchException e){return 8+e.getFlags().hashCode()+e.getConversion();}}
              public static int run(int group){int h=1;
               for(String f:new String[]{"literal","%s","%S","%10.3s","%-10.3S","%2$s %s %<S %1$s","%s%% %n%5%", "%1$s %1$S"})
                for(Object v:new Object[]{null,"Straße","iıİ","𐐨",123})h=31*h+String.format(f,v,"other").hashCode();
               h=31*h+String.format("%100$s %s",(Object[])null).hashCode();
               for(String f:new String[]{"%--s","%-s","%q","%","%.s","%.2%","%0%","%<s","%2$s","%#s","%+s","%5n","%-n","%00s","%2147483648s","%.2147483648s"})h=31*h+failure(f);
               try{String.format((String)null,"x");return 0;}catch(NullPointerException expected){return h;}
              }}
            """);compare(1);
    }

    @Test void generalFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             static int fail(String f,Object value){try{String.format(f,value);return 0;}
              catch(IllegalFormatConversionException e){return e.getConversion()+e.getArgumentClass().getName().hashCode();}
              catch(IllegalFormatCodePointException e){return e.getCodePoint();}
              catch(IllegalFormatPrecisionException e){return e.getPrecision()+19;}
              catch(FormatFlagsConversionMismatchException e){return e.getFlags().hashCode()+e.getConversion();}}
             public static int run(int group){int h=1;
              for(String f:new String[]{"%b/%B","%10.3h/%-10.2H","%.0B %.4H","%1$S %1$B %1$H"})
               for(Object value:new Object[]{null,true,false,0,1,-1,"iıİß"})h=31*h+String.format(f,value,value).hashCode();
              for(Object value:new Object[]{null,(byte)65,(short)105,0x10428,(char)0xd800,0x10ffff})
               for(String f:new String[]{"%c","%C","%5c/%-5C"})h=31*h+String.format(f,value,value).hashCode();
              for(Object value:new Object[]{-1,0x110000,1L,1.0,"x",true})h=31*h+fail("%c",value);
              for(String f:new String[]{"%#b","%+h","%.2c","%0c"})h=31*h+fail(f,65);
              return h;
             }}
            """);compare(1);
    }

    @Test void decimalFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             static int fail(String f,Object value){try{String.format(f,value);return 0;}
              catch(IllegalFormatConversionException e){return e.getConversion()+e.getArgumentClass().getName().hashCode();}
              catch(IllegalFormatPrecisionException e){return e.getPrecision()+19;}
              catch(MissingFormatWidthException e){return e.getFormatSpecifier().hashCode();}
              catch(IllegalFormatFlagsException e){return e.getFlags().hashCode();}
              catch(FormatFlagsConversionMismatchException e){return e.getFlags().hashCode()+e.getConversion();}}
             public static int run(int group){int h=1;
              Object[] values={null,(byte)-128,(short)-32768,Integer.MIN_VALUE,Long.MIN_VALUE,Long.MAX_VALUE,-1234567890123L,0,123,1234567};
              String[] formats={"%d","%25d","%-25d","%025d","%+025d","% 25d","%(25d","%(025d","%,d","%+,025d","% (25d","%+(25d"};
              for(String f:formats)h=31*h+String.format(f,values[group]).hashCode();
              for(String f:new String[]{"%#d","%.1d","%0d","%-0d","%+ d","%-05d","%+ 5d"})h=31*h+fail(f,1);
              for(Object v:new Object[]{1.5,"1",true,(char)65})h=31*h+fail("%d",v);
              return h;
             }}
            """);compare(10);
    }

    @Test void radixFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             static int fail(String f,Object value){try{String.format(f,value);return 0;}
              catch(IllegalFormatConversionException e){return e.getConversion()+e.getArgumentClass().getName().hashCode();}
              catch(IllegalFormatPrecisionException e){return e.getPrecision()+19;}
              catch(MissingFormatWidthException e){return e.getFormatSpecifier().hashCode();}
              catch(IllegalFormatFlagsException e){return e.getFlags().hashCode();}
              catch(FormatFlagsConversionMismatchException e){return e.getFlags().hashCode()+e.getConversion();}}
             public static int run(int group){int h=1;
              Object[] values={null,(byte)-1,(short)-1,-1,-1L,Long.MIN_VALUE,Long.MAX_VALUE,(byte)-128,(short)-32768,Integer.MIN_VALUE,0,123456};
              for(String f:new String[]{"%o","%x","%X","%#o","%#x","%#X","%25x","%-25o","%025X","%#025o","%#025x","%#025X"})h=31*h+String.format(f,values[group]).hashCode();
              for(String f:new String[]{"%+x","%(o","% x","%,x","%.1o","%0X","%-0x","%+ x","%-05o","%+ 5X"})h=31*h+fail(f,values[group]);
              for(Object v:new Object[]{1.5,"1",true,(char)65})h=31*h+fail("%x",v);
              return h;
             }}
            """);compare(12);
    }

    @Test void localeAndInstanceFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             for(Locale l:new Locale[]{null,Locale.ROOT,Locale.ENGLISH,Locale.US,Locale.UK})
              for(String f:new String[]{"%s %S %b %H", "%+,025d %#025x %c", "%n%5% %3$s %<S"})h=31*h+String.format(l,f,1234567,1234567,105,255).hashCode();
             for(String lang:new String[]{"tr","az","lt","de","ja"})h=31*h+String.format(new Locale(lang),"%S/%C","iıİß",105).hashCode();
             for(String f:new String[]{"%s","%1$d:%1$#x:%1$c","%s %% %n %<S"})h=31*h+f.formatted(65).hashCode();
             return h;
            }}
            """);compare(1);
    }

    @Test void pinnedJacksonInjectFormatting() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-annotations/2.15.4/jackson-annotations-2.15.4.jar");
        assertEquals("f204ebbd552614a22b8531ffe350d47f8fd42c45bb60517c07974dc27a5a1dd3",
            HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        try(JarFile jar=new JarFile(artifact.toFile())){
            for(String name:List.of("JacksonInject","JacksonInject$Value","JacksonAnnotationValue","JacksonAnnotation","OptBoolean")) {
                String entry="com/fasterxml/jackson/annotation/"+name+".class";Path file=directory.resolve(entry);
                Files.createDirectories(file.getParent());Files.write(file,jar.getInputStream(jar.getJarEntry(entry)).readAllBytes());
            }
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import com.fasterxml.jackson.annotation.JacksonInject.Value;
            public class Metadata {
             public static class Item extends Value {public Item(Object id,Boolean use){super(id,use);}}
             public static int run(int group){int h=1;
              for(Object id:new Object[]{null,"","name","Straße",123})for(Boolean use:new Boolean[]{null,false,true})
               h=31*h+new Item(id,use).toString().hashCode();
              return h;}}
            """);
        // Only Wasm uses the carrier; the external JVM oracle executes the original complete classes.
        ClassNode original=new ClassNode();new ClassReader(Files.readAllBytes(directory.resolve("com/fasterxml/jackson/annotation/JacksonInject$Value.class"))).accept(original,0);
        ClassWriter carrier=new ClassWriter(0);carrier.visit(V17,ACC_PUBLIC,original.name,null,"java/lang/Object",null);
        for(FieldNode field:original.fields)if(field.name.equals("_id") || field.name.equals("_useInput"))field.accept(carrier);
        for(MethodNode method:original.methods)if(method.name.equals("<init>") || method.name.equals("toString"))method.accept(carrier);
        carrier.visitEnd();pinnedCarrier=carrier.toByteArray();compare(1);
    }

    @Test void unsupportedFormattingIsExplicit() throws Exception {
        fixture("""
            import java.util.*;
            import java.math.BigInteger;
            public class Metadata {public static int run(int group){int h=0;
             for(String f:new String[]{"%f","%e","%g","%a","%tY"})try{String.format(f,1.0);throw new Error();}catch(UnsupportedOperationException expected){h++;}
             try{String.format(Locale.GERMANY,"%d",1000);throw new Error();}catch(UnsupportedOperationException expected){h++;}
             return h;}}
            """);
        // Excluded valid forms deliberately differ from the JDK formatter; verify the diagnostic contract.
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"[6]");
    }

    @Test void formattingDiagnostics() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){
             String[] formats={"%0$s","%2147483648$s","%2$<s","%1$-s","%0-5d","% +5d","%+B","%#C","%#H","%Q","%01$s","%1$<s","%2$-s","%3$-10s","%-#s","%0-c","%.2c","%-0d","%#-s","%+0x","%.s","%1$","%1$2","%--5s","%--5q","%1$s %q","%3$s %q"};
             try{return String.format(formats[group],1).hashCode();}
             catch(IllegalFormatException e){int h=e.getClass().getName().hashCode();
              if(e instanceof MissingFormatArgumentException)h=31*h+((MissingFormatArgumentException)e).getFormatSpecifier().hashCode();
              if(e instanceof MissingFormatWidthException)h=31*h+((MissingFormatWidthException)e).getFormatSpecifier().hashCode();
              if(e instanceof IllegalFormatFlagsException)h=31*h+((IllegalFormatFlagsException)e).getFlags().hashCode();
              if(e instanceof FormatFlagsConversionMismatchException){h=31*h+((FormatFlagsConversionMismatchException)e).getFlags().hashCode();h=31*h+((FormatFlagsConversionMismatchException)e).getConversion();}
              if(e instanceof UnknownFormatConversionException)h=31*h+((UnknownFormatConversionException)e).getConversion().hashCode();
              return h;
             }
            }}
            """);compare(27);
    }

    @Test void malformedTextDescriptors() {
        for(int fixture=0;fixture<3;fixture++){
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadText",null,"java/lang/Object",null);
            MethodVisitor m=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()Ljava/lang/Object;",null,null);m.visitCode();
            if(fixture==0){m.visitLdcInsn("x");m.visitInsn(ACONST_NULL);m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String","toUpperCase","(Ljava/lang/Object;)Ljava/lang/String;",false);}
            else if(fixture==1){m.visitLdcInsn("x");m.visitInsn(ACONST_NULL);m.visitMethodInsn(INVOKESTATIC,"java/lang/String","format","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",false);}
            else m.visitFieldInsn(GETSTATIC,"java/util/Locale","ROOT","Ljava/lang/String;");
            m.visitInsn(ARETURN);m.visitMaxs(2,0);m.visitEnd();writer.visitEnd();
            assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        }
    }

    @Test void bigIntegerDecimalFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             String[] values={"0","1","-1","1234567890123456789012345678901234567890","-9999999999999999999999999999999999999999999999999999999999999999"};
             BigInteger v=new BigInteger(values[group]);
             for(String f:new String[]{"%d","%+,d","%(,d","%+080d","%(080d","%-80d","% 80d","%+,080d","%1$d %<d"})h=31*h+String.format(Locale.ROOT,f,v).hashCode();
             return h;}}

            """);compare(5);
    }

    @Test void bigIntegerOctalFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             BigInteger v=new BigInteger(new String[]{"0","1","-1","123456789012345678901234567890","-9999999999999999999999999999999999999999999999999999"}[group]);
             for(String f:new String[]{"%o","%#o","%+o","% (80o","%+#080o","%(#080o","%-#80o","% 80o"})h=31*h+String.format(Locale.ROOT,f,v).hashCode();
             return h;}}
            """);compare(5);
    }

    @Test void bigIntegerHexFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             BigInteger v=new BigInteger(new String[]{"0","1","-1","123456789012345678901234567890","-9999999999999999999999999999999999999999999999999999"}[group]);
             for(String f:new String[]{"%x","%X","%#x","%+x","% (80x","%+#080x","%(#080X","%-#80X","% 80x"})h=31*h+String.format(Locale.ROOT,f,v).hashCode();
             return h;}}
            """);compare(5);
    }

    @Test void bigDecimalFixedFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             String[] values={"0","0.000","-0.000","1.2345","-1.2345","0.000000001","-0.000000001","999999999999999999999999999999.9995","-999.995","1E+30","1E-30"};
             Object v=group==values.length?null:new BigDecimal(values[group]);
             for(String f:new String[]{"%f","%.0f","%#.0f","%+.3f","% 40.2f","%(,045.2f","%-40.9f","%,.5f","%.12f"})h=31*h+String.format(Locale.ROOT,f,v).hashCode();
             return h;}}
            """);compare(12);
    }

    @Test void bigDecimalScientificFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             Object[] values={null,new BigDecimal("0"),new BigDecimal("0.000"),new BigDecimal("0E+8"),new BigDecimal("9.9995"),new BigDecimal("-0.0000099995"),new BigDecimal("123456789012345678901234567890.123456789"),new BigDecimal("1E-100"),new BigDecimal("-1E+100"),new BigDecimal(BigInteger.ONE,Integer.MAX_VALUE),new BigDecimal(BigInteger.ONE,Integer.MIN_VALUE)};
             for(String f:new String[]{"%e","%E","%.0e","%#.0e","%+.3e","% 40.2E","%(045.2e","%-40.9e","%.12e"})h=31*h+String.format(Locale.ROOT,f,values[group]).hashCode();
             return h;}}
            """);compare(11);
    }

    @Test void bigDecimalGeneralFormatting() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             Object[] values={null,new BigDecimal("0"),new BigDecimal("0.000"),new BigDecimal("0E+8"),new BigDecimal("9.9995"),new BigDecimal("-0.0000099995"),new BigDecimal("999999.5"),new BigDecimal("0.00009999995"),new BigDecimal("123456789012345678901234567890.123456789"),new BigDecimal("1E-100"),new BigDecimal("-1E+100"),new BigDecimal(BigInteger.ONE,Integer.MAX_VALUE),new BigDecimal(BigInteger.ONE,Integer.MIN_VALUE)};
             for(String f:new String[]{"%g","%G","%.0g","%.1g","%+.3g","% 40.2G","%(045.2g","%-40.9g","%,.12g"})h=31*h+String.format(Locale.ROOT,f,values[group]).hashCode();
             return h;}}
            """);compare(13);
    }

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
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
                if(pinnedCarrier!=null && entry.startsWith("com/fasterxml/") && !entry.endsWith("JacksonInject$Value.class"))continue;
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(pinnedCarrier!=null && entry.endsWith("JacksonInject$Value.class")?pinnedCarrier:Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString(),"CaseOracle",fixtureClassName,Integer.toString(groups));
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
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
