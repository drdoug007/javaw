package dastec.one.javaw.bytecode;

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

class BytecodeBigDecimalTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
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

    @Test void constructionTextScaleAndValueContracts() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"0","-0.00","1","-1.2300","123456789012345678901234567890.00012","1E+9","0.00000001","0E-9","0E+5",".125","5.","+１２٣.٤٥E+２","1e000000000000000001"}){
 BigDecimal a=new BigDecimal(text);hash=31*hash+a.toString().hashCode();hash=31*hash+a.toPlainString().hashCode();hash+=a.scale()+a.precision()+a.signum()+a.hashCode();
 if(!a.equals(new BigDecimal(a.toString())))throw new Error();if(!a.equals(new BigDecimal(a.unscaledValue(),a.scale())))throw new Error();
 hash=31*hash+a.ulp().toString().hashCode();hash+=a.plus()==a?1:0;
 char[] chars=("x"+text+"z").toCharArray();BigDecimal copy=new BigDecimal(chars,1,chars.length-2);chars[1]='7';if(!a.equals(copy))throw new Error();hash+=new BigDecimal(text.toCharArray()).hashCode();}
 hash+=new BigDecimal(17).hashCode()+new BigDecimal(-9223372036854775808L).hashCode();hash+=new BigDecimal(BigInteger.TEN).hashCode()+BigDecimal.valueOf(77,3).hashCode();
 if(new BigDecimal("1.0").equals(new BigDecimal("1.00")))throw new Error();return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void arithmeticAndPreferredScales() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;String[] values={"0.00","1.00","-3.125","12345678901234567890.123","0.00005","1E+3"};
 for(String text:values){BigDecimal a=new BigDecimal(text);for(String other:values){BigDecimal b=new BigDecimal(other);
 hash=31*hash+a.add(b).toString().hashCode();hash=31*hash+a.subtract(b).toString().hashCode();hash=31*hash+a.multiply(b).toString().hashCode();
 hash+=a.compareTo(b)+a.min(b).hashCode()+a.max(b).hashCode();}
 hash=31*hash+a.pow(3).toString().hashCode();hash+=a.abs().hashCode()+a.negate().hashCode();
 for(int n=-3;n<=3;n++){hash=31*hash+a.movePointLeft(n).toString().hashCode();hash=31*hash+a.movePointRight(n).toString().hashCode();hash=31*hash+a.scaleByPowerOfTen(n).toString().hashCode();}
 hash=31*hash+a.stripTrailingZeros().toString().hashCode();}return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void allRoundingModesAndScaleChanges() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"2.5","3.5","-2.5","-3.5",".125","-.125","1.2345","-1.2345","0.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(text);for(int scale:new int[]{-1,0,1,3})try{BigDecimal value=a.setScale(scale,mode);hash=31*hash+value.toString().hashCode();if(!value.equals(a.setScale(scale,mode.ordinal())))throw new Error();}catch(ArithmeticException e){hash+=7;}}
 for(int mode=0;mode<8;mode++)hash=31*hash+RoundingMode.valueOf(mode).name().hashCode();
 try{RoundingMode.valueOf(-1);throw new Error();}catch(IllegalArgumentException e){hash++;}
 try{BigDecimal.ONE.setScale(0,(RoundingMode)null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void exactRoundedAndIntegralDivision() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"0.00","1.00","-7.5","15","1E+2"})for(String other:new String[]{"2","0.125","3","-2.0","1E+1"}){
 BigDecimal a=new BigDecimal(text),b=new BigDecimal(other);
 try{hash=31*hash+a.divide(b).toString().hashCode();}catch(ArithmeticException e){hash+=7;}
 BigDecimal[] qr=a.divideAndRemainder(b);hash=31*hash+qr[0].toString().hashCode();hash=31*hash+qr[1].toString().hashCode();
 if(qr[0].multiply(b).add(qr[1]).compareTo(a)!=0)throw new Error();hash+=a.remainder(b).hashCode()+a.divideToIntegralValue(b).hashCode();}
 for(RoundingMode mode:RoundingMode.values())for(String text:new String[]{"1","-1","10.50"}){
 BigDecimal a=new BigDecimal(text),b=new BigDecimal("8");
 try{hash=31*hash+a.divide(b,2,mode).toString().hashCode();hash=31*hash+a.divide(b,mode).toString().hashCode();hash+=a.divide(b,2,mode.ordinal()).hashCode()+a.divide(b,mode.ordinal()).hashCode();}catch(ArithmeticException e){hash+=11;}}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void integerConversionsAndScaleExtremes() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"0.00","127","128","-128","-129","32767","32768","2147483647","2147483648","-9223372036854775808","9223372036854775808","1.0","1.01","-1.99","123456789012345678901234567890.5"}){
 BigDecimal a=new BigDecimal(text);hash+=a.byteValue()+a.shortValue()+a.intValue()+Long.hashCode(a.longValue());hash=31*hash+a.toBigInteger().hashCode();
 try{hash+=a.byteValueExact();}catch(ArithmeticException e){hash+=7;}try{hash+=a.shortValueExact();}catch(ArithmeticException e){hash+=11;}
 try{hash+=a.intValueExact();}catch(ArithmeticException e){hash+=13;}try{hash+=Long.hashCode(a.longValueExact());}catch(ArithmeticException e){hash+=17;}
 try{hash=31*hash+a.toBigIntegerExact().hashCode();}catch(ArithmeticException e){hash+=19;}}
 for(String text:new String[]{"1e2147483648","1.0e2147483648","1.0e2147483649","1.00e2147483650"}){BigDecimal a=new BigDecimal(text);hash=31*hash+a.toString().hashCode()+a.scale();}
 for(int scale:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE})for(BigInteger coefficient:new BigInteger[]{BigInteger.ZERO,BigInteger.ONE,BigInteger.ONE.negate()}){
 BigDecimal a=new BigDecimal(coefficient,scale);hash=31*hash+a.toString().hashCode();hash+=a.intValue()+Long.hashCode(a.longValue());
 try{hash+=a.intValueExact();}catch(ArithmeticException e){hash+=23;}
 try{hash=31*hash+a.scaleByPowerOfTen(1).toString().hashCode();}catch(ArithmeticException e){hash+=29;}
 if(a.signum()==0)hash+=a.setScale(0).hashCode()+a.toBigIntegerExact().hashCode();}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void decimalToBinaryRoundingAndDoubleConstruction() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"0","-0.00","0.1","-0.1","16777217","9007199254740993","1.7976931348623157E308","1.7976931348623159E308","2.2250738585072014E-308","4.9406564584124654E-324","2.4703282292062327E-324","-1E-400","1E400","3.4028234663852886E38","1.1754943508222875E-38","1.401298464324817E-45","-1E-50"}){
 BigDecimal a=new BigDecimal(text);hash=31*hash+Double.hashCode(a.doubleValue());hash=31*hash+Float.hashCode(a.floatValue());}
 for(double value:new double[]{0.0,-0.0,0.1,-1.25,1.23456789012345,Double.MIN_NORMAL,Double.MIN_VALUE,Double.MAX_VALUE}){
 BigDecimal exact=new BigDecimal(value);hash=31*hash+exact.toString().hashCode();hash=31*hash+exact.hashCode();hash=31*hash+BigDecimal.valueOf(value).toString().hashCode();}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void typedFailuresAndMalformedText() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(String text:new String[]{"","+","-",".","1.2.3","1e","1e+","1e2147483649","1e-2147483648","1.0e2147483650"," 1","1_0","NaN","Infinity"})try{new BigDecimal(text);throw new Error();}catch(NumberFormatException e){hash++;}
 try{new BigDecimal((String)null);throw new Error();}catch(NullPointerException e){hash++;}
 try{new BigDecimal((BigInteger)null);throw new Error();}catch(NullPointerException e){hash++;}
 try{new BigDecimal(new char[]{'1'},-1,1);throw new Error();}catch(NumberFormatException e){hash++;}
 try{new BigDecimal(Double.NaN);throw new Error();}catch(NumberFormatException e){hash++;}
 try{BigDecimal.valueOf(Double.POSITIVE_INFINITY);throw new Error();}catch(NumberFormatException e){hash++;}
 try{BigDecimal.ONE.divide(BigDecimal.ZERO);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigDecimal.ONE.divide(BigDecimal.ZERO,2,RoundingMode.DOWN);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigDecimal.ONE.remainder(BigDecimal.ZERO);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigDecimal.ONE.pow(-1);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigDecimal.ONE.add(null);throw new Error();}catch(NullPointerException e){hash++;}
 try{new BigDecimal("1.2").setScale(0);throw new Error();}catch(ArithmeticException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
        fixture("public class Metadata {public static String run(){return java.math.BigDecimal.ONE.sqrt(java.math.MathContext.DECIMAL32).toString();}}");
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))));
        assertTrue(failure.getMessage().contains("java/math/BigDecimal.sqrt"),failure.getMessage());
    }

    @Test void engineeringNotationPreservesScaleAndExponentGroups() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(String coefficient:new String[]{"0","1","-1","12","-123","1234","-10000","999999999999999999999999999999"})
                    for(int scale=-18;scale<=20;scale++){
                        BigDecimal value=new BigDecimal(new BigInteger(coefficient),scale);
                        String text=value.toEngineeringString();hash=31*hash+text.hashCode();
                        if(value.compareTo(new BigDecimal(text))!=0)throw new Error();
                    }
                for(int scale:new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1,Integer.MIN_VALUE+2,Integer.MAX_VALUE-2,Integer.MAX_VALUE-1,Integer.MAX_VALUE})
                    for(long coefficient:new long[]{0,1,-12,1234}){
                        String text=BigDecimal.valueOf(coefficient,scale).toEngineeringString();hash=31*hash+text.hashCode();
                    }
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void executesSelectedPinnedHibernateDecimalCoercions() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path hibernate=repository.resolve("org/hibernate/orm/hibernate-core/6.4.4.Final/hibernate-core-6.4.4.Final.jar");
        Path persistence=repository.resolve("jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar");
        assertDigest(hibernate,"a1324b7c80c800826c9a5d74b61b0de768141f967c2b082650cb7bf4675570a7");
        assertDigest(persistence,"475389446d35c6f46c565728b756dc508c284644ea2690644e0d8e7e339d42fd");
        fixtureClasspath=hibernate+File.pathSeparator+persistence;
        fixture("""
            import java.math.*;import org.hibernate.type.descriptor.java.*;
            public class Metadata {public static int run(){
                int hash=1;
                for(String text:new String[]{"0.00","127","-128","32767","-32768","2147483647","-2147483648","9223372036854775807","-9223372036854775808","123456789012345678901234567890","1.5","-1.00001"}){
                    BigDecimal value=new BigDecimal(text);
                    try{hash=31*hash+CoercionHelper.toByte(value);}catch(CoercionException e){if(!(e.getCause() instanceof ArithmeticException))throw new Error();hash++;}
                    try{hash=31*hash+CoercionHelper.toShort(value);}catch(CoercionException e){if(!(e.getCause() instanceof ArithmeticException))throw new Error();hash++;}
                    try{hash=31*hash+CoercionHelper.toInteger(value);}catch(CoercionException e){if(!(e.getCause() instanceof ArithmeticException))throw new Error();hash++;}
                    try{hash=31*hash+CoercionHelper.toLong(value).hashCode();}catch(CoercionException e){if(!(e.getCause() instanceof ArithmeticException))throw new Error();hash++;}
                    try{hash=31*hash+CoercionHelper.toBigInteger(value).hashCode();}catch(CoercionException e){if(!(e.getCause() instanceof ArithmeticException))throw new Error();hash++;}
                    hash=31*hash+CoercionHelper.toDouble(value).hashCode();
                }
                try{CoercionHelper.toLong((BigDecimal)null);throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;
            }}
            """);
        try(JarFile jar=new JarFile(hibernate.toFile())){
            String name="org/hibernate/type/descriptor/java/CoercionHelper";
            ClassNode selected=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes()).accept(selected,0);
            // Preserve original bodies for decimal overloads and their shared error wrapper only.
            selected.methods.removeIf(method->!method.desc.startsWith("(Ljava/math/BigDecimal;)") && !method.name.equals("coerceWrappingError"));
            assertEquals(7,selected.methods.size());
            ClassWriter writer=new ClassWriter(0);selected.accept(writer);writeClass(name,writer.toByteArray());
            for(String support:new String[]{name+"$Coercer","org/hibernate/type/descriptor/java/CoercionException","org/hibernate/HibernateException"})
                writeClass(support,jar.getInputStream(jar.getJarEntry(support+".class")).readAllBytes());
        }
        try(JarFile jar=new JarFile(persistence.toFile())){
            String name="jakarta/persistence/PersistenceException";writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
        }
        compare(List.of("run"));
    }

    @Test void mathContextValueContractsAndFailures() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(MathContext c:new MathContext[]{MathContext.UNLIMITED,MathContext.DECIMAL32,MathContext.DECIMAL64,MathContext.DECIMAL128,new MathContext(Integer.MAX_VALUE)}){
                    hash=31*hash+c.toString().hashCode();hash+=c.getPrecision()+c.getRoundingMode().ordinal();
                    if(!c.equals(new MathContext(c.toString())) || c.equals(null) || c.equals("x"))throw new Error();
                    if(c.hashCode()!=c.getPrecision()+59*c.getRoundingMode().hashCode())throw new Error();
                }
                for(RoundingMode m:RoundingMode.values())for(int p:new int[]{0,1,7,34}){
                    MathContext c=new MathContext(p,m);if(!c.equals(new MathContext(p,m)) || c.hashCode()!=new MathContext(p,m).hashCode())throw new Error();
                    hash=31*hash+new MathContext(c.toString()).toString().hashCode();
                }
                for(String s:new String[]{"precision=+2 roundingMode=UP","precision=００２ roundingMode=DOWN","precision=-0 roundingMode=HALF_EVEN"})hash=31*hash+new MathContext(s).toString().hashCode();
                for(String s:new String[]{"","2","precision= roundingMode=UP","precision=-1 roundingMode=UP","precision=2147483648 roundingMode=UP","precision=2  roundingMode=UP","precision=2 roundingMode=up","precision=2 roundingMode=UP ","precision=2"})try{new MathContext(s);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{new MathContext((String)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{new MathContext(1,null);throw new Error();}catch(NullPointerException e){hash++;}
                try{new MathContext(-1,null);throw new Error();}catch(IllegalArgumentException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void precisionRoundingCarriesTiesAndScaleBoundaries() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(String s:new String[]{"0.000","1.2345","-1.2345","2.50","-3.50","9.99","-99.95","999999999999999999999999999999999995","1.0000","1E+100","0.000000125"})
                for(RoundingMode mode:RoundingMode.values())for(int precision:new int[]{0,1,2,3,7,34}){
                    BigDecimal a=new BigDecimal(s);MathContext c=new MathContext(precision,mode);
                    try{BigDecimal result=a.round(c);hash=31*hash+result.toString().hashCode()+result.scale()+result.precision();
                        if(!result.equals(a.plus(c)) || !result.equals(new BigDecimal(s,c)))throw new Error();
                        hash=31*hash+a.abs(c).toString().hashCode();hash=31*hash+a.negate(c).toString().hashCode();
                    }catch(ArithmeticException e){hash+=7;}
                }
                for(int scale:new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1,Integer.MAX_VALUE})for(long raw:new long[]{0,1,99,999,-999}){
                    BigDecimal a=BigDecimal.valueOf(raw,scale);
                    try{hash=31*hash+a.round(new MathContext(1)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}
                }
                try{BigDecimal.ONE.round(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{BigDecimal.ZERO.plus(null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextConstructorsPreserveExactInputBeforeRounding() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(MathContext c:new MathContext[]{MathContext.UNLIMITED,new MathContext(1),new MathContext(5,RoundingMode.HALF_EVEN)}){
                    hash=31*hash+new BigDecimal(12345,c).toString().hashCode();hash=31*hash+new BigDecimal(Long.MIN_VALUE,c).toString().hashCode();
                    hash=31*hash+new BigDecimal(new BigInteger("999999999999999999999"),c).toString().hashCode();
                    hash=31*hash+new BigDecimal(new BigInteger("999999999999999999999"),8,c).toString().hashCode();
                    hash=31*hash+new BigDecimal("123.450".toCharArray(),c).toString().hashCode();
                    hash=31*hash+new BigDecimal("x-99.95z".toCharArray(),1,6,c).toString().hashCode();
                    for(double d:new double[]{0.0,-0.0,0.1,-0.1,1.23456789,Double.MIN_VALUE,Double.MAX_VALUE})hash=31*hash+new BigDecimal(d,c).toString().hashCode();
                }
                try{new BigDecimal(0,(MathContext)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{new BigDecimal("1.1",new MathContext(1,RoundingMode.UNNECESSARY));throw new Error();}catch(ArithmeticException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextConstructorsAvoidSubclassCallbacksAndUnaryDispatchMatchesJdk() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {
                static class Decimal extends BigDecimal {
                    Decimal(){super("1.2345",new MathContext(2));}
                    public BigDecimal round(MathContext c){throw new Error("constructor called round");}
                    public BigDecimal plus(MathContext c){return new BigDecimal("7.89");}
                }
                static class PlusDecimal extends BigDecimal {
                    PlusDecimal(){super("1.2345");}
                    public BigDecimal plus(MathContext c){return BigDecimal.TEN;}
                }
                public static int run(){Decimal value=new Decimal();
                    if(!value.toString().equals("1.2"))throw new Error();
                    if(!value.abs(MathContext.DECIMAL32).toString().equals("7.89"))throw new Error();
                    if(new PlusDecimal().round(MathContext.DECIMAL32)!=BigDecimal.TEN)throw new Error();
                    return value.hashCode();
                }
            }
            """);compare(List.of("run"));
    }

    @Test void executesSelectedPinnedH2RoundingInstructionSequence() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("com/h2database/h2/2.2.224/h2-2.2.224.jar");
        assertDigest(artifact,"b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497");
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/h2/expression/function/TruncateValueFunction.class")).readAllBytes()).accept(original,0);
            MethodNode method=original.methods.stream().filter(m->m.name.equals("getValue")).findFirst().orElseThrow();
            // Lift just the first NEW MathContext ... BigDecimal.round sequence, with original
            // operands and local 5 unchanged. The adapter supplies the decimal already on stack.
            ClassWriter adapter=new ClassWriter(ClassWriter.COMPUTE_MAXS);
            adapter.visit(V17,ACC_PUBLIC,"SelectedH2Rounding",null,"java/lang/Object",null);
            MethodVisitor out=adapter.visitMethod(ACC_PUBLIC|ACC_STATIC,"round","(Ljava/math/BigDecimal;IIIIJ)Ljava/math/BigDecimal;",null,null);
            out.visitCode();out.visitVarInsn(ALOAD,0);
            boolean copying=false;int count=0;
            for(AbstractInsnNode instruction:method.instructions){
                if(instruction instanceof TypeInsnNode type && type.getOpcode()==NEW && type.desc.equals("java/math/MathContext"))copying=true;
                if(copying && instruction.getOpcode()>=0){instruction.accept(out);count++;}
                if(copying && instruction instanceof MethodInsnNode call && call.owner.equals("java/math/BigDecimal") && call.name.equals("round"))break;
            }
            assertEquals(6,count);out.visitInsn(ARETURN);out.visitMaxs(0,0);out.visitEnd();adapter.visitEnd();
            writeClass("SelectedH2Rounding",adapter.toByteArray());
            ClassNode helper=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/h2/util/MathUtils.class")).readAllBytes()).accept(helper,0);
            helper.fields.clear();helper.methods.removeIf(m->!m.name.equals("convertLongToInt"));assertEquals(1,helper.methods.size());
            ClassWriter writer=new ClassWriter(0);helper.accept(writer);writeClass(helper.name,writer.toByteArray());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(String s:new String[]{"0.00","1.2345","-1.2345","999.95","123456789012345678901234567890"})for(long p:new long[]{0,1,2,7,34,Long.MAX_VALUE,-1,Long.MIN_VALUE}){
                    try{BigDecimal a=SelectedH2Rounding.round(new BigDecimal(s),0,0,0,0,p);hash=31*hash+a.toString().hashCode()+a.scale();}catch(IllegalArgumentException e){hash+=7;}
                }return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextAddition() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){
int hash=1;
 for(String x:new String[]{"0.00","9.995","-9.995","1200"})for(String y:new String[]{"0.0","-9.994","3.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal(y);
 MathContext context=new MathContext(3,mode);
 try{BigDecimal result=a.add(b,context);hash=31*hash+result.toString().hashCode();hash+=result.scale();}catch(ArithmeticException e){hash=31*hash+7;}
 }
 for(int precision:new int[]{0,1,2,6}){BigDecimal a=new BigDecimal("1000.00"),b=new BigDecimal("3");
 try{hash=31*hash+a.add(b,new MathContext(precision)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
 try{BigDecimal.ONE.add(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.add(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;}public static String extremes(){StringBuilder out=new StringBuilder();
 for(String x:new String[]{"1E+2147483647","0E-2147483647","1.00","-1.00"})for(String y:new String[]{"0E+2147483647","1E-2147483647","-1E-2147483647"})for(RoundingMode mode:RoundingMode.values()){
 try{out.append(x).append("+").append(y).append(mode).append("=").append(new BigDecimal(x).add(new BigDecimal(y),new MathContext(2,mode))).append(";");}catch(ArithmeticException e){out.append("failure;");}}
 return out.toString();
            }}
            """);compare(List.of("run","extremes"));
    }

    @Test void contextSubtraction() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){
int hash=1;
 for(String x:new String[]{"0.00","9.995","-9.995","1200"})for(String y:new String[]{"0.0","-9.994","3.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal(y);
 MathContext context=new MathContext(3,mode);
 try{BigDecimal result=a.subtract(b,context);hash=31*hash+result.toString().hashCode();hash+=result.scale();}catch(ArithmeticException e){hash=31*hash+7;}
 }
 for(int precision:new int[]{0,1,2,6}){BigDecimal a=new BigDecimal("1000.00"),b=new BigDecimal("3");
 try{hash=31*hash+a.subtract(b,new MathContext(precision)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
 try{BigDecimal.ONE.subtract(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.subtract(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextMultiplication() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){
int hash=1;
 for(String x:new String[]{"0.00","9.995","-9.995","1200"})for(String y:new String[]{"0.0","-9.994","3.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal(y);
 MathContext context=new MathContext(3,mode);
 try{BigDecimal result=a.multiply(b,context);hash=31*hash+result.toString().hashCode();hash+=result.scale();}catch(ArithmeticException e){hash=31*hash+7;}
 }
 for(int precision:new int[]{0,1,2,6}){BigDecimal a=new BigDecimal("1000.00"),b=new BigDecimal("3");
 try{hash=31*hash+a.multiply(b,new MathContext(precision)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
 try{BigDecimal.ONE.multiply(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.multiply(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextIntegralDivision() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){
int hash=1;
 for(String x:new String[]{"0.00","9.995","-9.995","1200"})for(String y:new String[]{"0.0","-9.994","3.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal(y);
 MathContext context=new MathContext(3,mode);
 try{BigDecimal result=a.divideToIntegralValue(b,context);hash=31*hash+result.toString().hashCode();hash+=result.scale();}catch(ArithmeticException e){hash=31*hash+7;}
 }
 for(int precision:new int[]{0,1,2,6}){BigDecimal a=new BigDecimal("1000.00"),b=new BigDecimal("3");
 try{hash=31*hash+a.divideToIntegralValue(b,new MathContext(precision)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
 try{BigDecimal.ONE.divideToIntegralValue(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.divideToIntegralValue(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 for(String x:new String[]{"1000.00","1200","-1200","0.000"})for(int p:new int[]{1,2,3})for(RoundingMode mode:RoundingMode.values()){
 try{hash=31*hash+new BigDecimal(x).divideToIntegralValue(new BigDecimal("1.0"),new MathContext(p,mode)).toString().hashCode();}catch(ArithmeticException e){hash+=17;}}
 return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextRemainders() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){
int hash=1;
 for(String x:new String[]{"0.00","9.995","-9.995","1200"})for(String y:new String[]{"0.0","-9.994","3.00"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal(y);
 MathContext context=new MathContext(3,mode);
 try{BigDecimal result=a.remainder(b,context);hash=31*hash+result.toString().hashCode();hash+=result.scale();BigDecimal[] qr=a.divideAndRemainder(b,context);if(!result.equals(qr[1]) || qr[0].multiply(b).add(qr[1]).compareTo(a)!=0)throw new Error();hash=31*hash+qr[0].toString().hashCode();}catch(ArithmeticException e){hash=31*hash+7;}
 }
 for(int precision:new int[]{0,1,2,6}){BigDecimal a=new BigDecimal("1000.00"),b=new BigDecimal("3");
 try{hash=31*hash+a.remainder(b,new MathContext(precision)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
 try{BigDecimal.ONE.remainder(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.remainder(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 for(String x:new String[]{"123.4567","-123.4567","0.000"})for(RoundingMode mode:RoundingMode.values()){
 BigDecimal a=new BigDecimal(x),b=new BigDecimal("100"),expected=new BigDecimal(x.startsWith("-")?"-23.4567":x.startsWith("0")?"0.000":"23.4567");
 MathContext context=new MathContext(1,mode);BigDecimal[] qr=a.divideAndRemainder(b,context);
 if(!qr[1].equals(expected) || !a.remainder(b,context).equals(expected))throw new Error();
 hash=31*hash+qr[0].toString().hashCode()+qr[1].toString().hashCode();}
 try{BigDecimal.ONE.divideAndRemainder(BigDecimal.ZERO,MathContext.UNLIMITED);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigDecimal.ONE.divideAndRemainder(BigDecimal.ONE,null);throw new Error();}catch(NullPointerException e){hash++;}
 try{BigDecimal.ONE.divideAndRemainder(null,MathContext.UNLIMITED);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextDivision() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(String x:new String[]{"0.00","1.00","-9.995","999.95"})for(String y:new String[]{"0.0","3.00","-2"})for(RoundingMode mode:RoundingMode.values()){
                    try{BigDecimal result=new BigDecimal(x).divide(new BigDecimal(y),new MathContext(3,mode));hash=31*hash+result.toString().hashCode()+result.scale();}catch(ArithmeticException e){hash=31*hash+7;}}
                for(String x:new String[]{"1E+2147483647","1E-2147483647","0E-2147483647"})for(String y:new String[]{"1E+2147483647","1E-2147483647","10"}){
                    try{hash=31*hash+new BigDecimal(x).divide(new BigDecimal(y),MathContext.DECIMAL32).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
                for(int p:new int[]{0,1,2,8})for(String x:new String[]{"1.00","1000","2.50","1001","9999"})for(String y:new String[]{"2.0","3","0.125","1000"}){
                    try{hash=31*hash+new BigDecimal(x).divide(new BigDecimal(y),new MathContext(p)).toString().hashCode();}catch(ArithmeticException e){hash+=13;}}
                try{BigDecimal.ONE.divide(null,MathContext.DECIMAL32);throw new Error();}catch(NullPointerException e){hash++;}
                try{BigDecimal.ONE.divide(BigDecimal.ONE,(MathContext)null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void contextPowers() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata {public static int run(){int hash=1;
                for(String x:new String[]{"1.005","-2.5"})for(int n:new int[]{2,-3})for(RoundingMode mode:RoundingMode.values()){
                    try{BigDecimal result=new BigDecimal(x).pow(n,new MathContext(3,mode));hash=31*hash+result.toString().hashCode()+result.scale();}catch(ArithmeticException e){hash=31*hash+7;}}
                for(int n:new int[]{-1000000000,-999999999,-10,-1,0,1,10,999999999,1000000000})for(int p:new int[]{0,1,9}){
                    try{hash=31*hash+BigDecimal.ONE.pow(n,new MathContext(p)).toString().hashCode();}catch(ArithmeticException e){hash+=11;}}
                for(int n:new int[]{-1,0,1,3})try{hash+=new BigDecimal("0.00").pow(n,MathContext.DECIMAL32).hashCode();}catch(ArithmeticException e){hash+=13;}
                try{BigDecimal.ONE.pow(1,new MathContext(Integer.MAX_VALUE));throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{BigDecimal.ONE.pow(0,null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    private void writeClass(String name,byte[] bytes) throws Exception {
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
    }
    private void assertDigest(Path artifact,String expected) throws Exception {
        assertEquals(expected,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
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
