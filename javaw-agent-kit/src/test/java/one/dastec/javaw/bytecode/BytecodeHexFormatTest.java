package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T12: java.util.HexFormat. Expected values come from the JVM; see tasks/T12-hex-format.md. */
@Tag("agent-pending")
class BytecodeHexFormatTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.Arrays;
        import java.util.HexFormat;
        public class Fixture {
            static String name(Throwable t){return t.getClass().getSimpleName();}

            public static String basics(){
                byte[] b={0,1,15,16,127,-128,-1,(byte)0xab};
                return HexFormat.of().formatHex(b)+"|"+HexFormat.of().withUpperCase().formatHex(b)+"|"+HexFormat.of().formatHex(new byte[0]).length();
            }
            public static String range(){
                byte[] b={1,2,3,4,5};
                return HexFormat.of().formatHex(b,1,4)+"|"+HexFormat.of().formatHex(b,2,2).isEmpty();
            }
            public static String rangeFailures(){
                byte[] b={1,2,3};StringBuilder out=new StringBuilder();
                try{HexFormat.of().formatHex(b,1,5);}catch(RuntimeException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.of().formatHex(b,2,1);}catch(RuntimeException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.of().formatHex(b,-1,1);}catch(RuntimeException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.of().formatHex(null);}catch(RuntimeException e){out.append(name(e)).append(';');}
                return out.toString();
            }
            public static String delimited(){
                byte[] b={10,-6,0};
                HexFormat f=HexFormat.ofDelimiter(":");
                return f.formatHex(b)+"|"+f.withUpperCase().formatHex(b)+"|"+HexFormat.ofDelimiter(", ").withPrefix("0x").withSuffix("h").formatHex(b)
                        +"|"+f.formatHex(new byte[]{7})+"|"+HexFormat.of().withPrefix("#").formatHex(b);
            }
            public static String roundTrip(){
                byte[] all=new byte[256];for(int i=0;i<256;i++)all[i]=(byte)i;
                HexFormat f=HexFormat.ofDelimiter(" ").withUpperCase();
                String s=f.formatHex(all);
                return s.length()+","+s.substring(0,20)+","+Arrays.equals(all,f.parseHex(s))+","+Arrays.equals(all,HexFormat.of().parseHex(HexFormat.of().formatHex(all)));
            }

            public static String parse(){
                byte[] a=HexFormat.of().parseHex("00ff7FaB");
                byte[] c=HexFormat.ofDelimiter(":").parseHex("0a:FA:00");
                byte[] d=HexFormat.ofDelimiter(", ").withPrefix("0x").withSuffix("h").parseHex("0x0ah, 0xfah");
                byte[] e=HexFormat.of().parseHex("");
                byte[] r=HexFormat.of().parseHex("zz0102zz",2,6);
                return Arrays.toString(a)+Arrays.toString(c)+Arrays.toString(d)+e.length+Arrays.toString(r);
            }
            public static String parseFailures(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"abc","0g","g0"," 1"}){
                    try{HexFormat.of().parseHex(s);out.append("ok;");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                try{HexFormat.ofDelimiter(":").parseHex("0a:fa:");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.ofDelimiter(":").parseHex("0a-fa");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.of().withPrefix("[").parseHex("(0a");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.of().parseHex(null);}catch(NullPointerException e){out.append(name(e)).append(';');}
                return out.toString();
            }

            public static String digits(){
                HexFormat f=HexFormat.of();
                return f.toHexDigits((byte)-1)+f.toHexDigits((byte)10)+"|"+f.toHexDigits('A')+"|"+f.toHexDigits((short)-2)+"|"+f.toHexDigits(0x1234abcd)
                        +"|"+f.toHexDigits(-1)+"|"+f.toHexDigits(0x0123456789abcdefL)+"|"+f.withUpperCase().toHexDigits(255)+"|"+f.toHexDigits(0xabcL,4)
                        +"|"+f.toHexDigits(-1L,0)+"|"+f.toLowHexDigit(0x3c)+f.toHighHexDigit(0x3c)+"|"+f.withUpperCase().toHexDigits(0xfedcba9876543210L,16);
            }
            public static String digitCountFailure(){
                try{HexFormat.of().toHexDigits(1L,17);return "none";}catch(IllegalArgumentException e){return name(e)+":"+e.getMessage();}
            }
            public static String hexDigitChecks(){
                StringBuilder out=new StringBuilder();
                for(int c:new int[]{'0','9','a','f','A','F','g','G','/',':','@','`',-1,256,0xff10,'a'+256})out.append(HexFormat.isHexDigit(c)?'1':'0');
                out.append('|');
                for(int c:new int[]{'0','9','a','f','A','F'})out.append(HexFormat.fromHexDigit(c)).append(',');
                return out.toString();
            }
            public static String fromHexDigitFailures(){
                StringBuilder out=new StringBuilder();
                for(int c:new int[]{'g',' '}){
                    try{HexFormat.fromHexDigit(c);}catch(NumberFormatException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                for(int c:new int[]{256,-1,'a'+256}){
                    try{HexFormat.fromHexDigit(c);}catch(NumberFormatException e){out.append(name(e)).append(';');}
                }
                return out.toString();
            }
            public static String fromDigits(){
                return HexFormat.fromHexDigits("7fffffff")+"|"+HexFormat.fromHexDigits("ffffffff")+"|"+HexFormat.fromHexDigits("")+"|"+HexFormat.fromHexDigits("x1Az",1,3)
                        +"|"+HexFormat.fromHexDigitsToLong("ffffffffffffffff")+"|"+HexFormat.fromHexDigitsToLong("0123456789ABCDEF")+"|"+HexFormat.fromHexDigitsToLong("--10--",2,4);
            }
            public static String fromDigitsFailures(){
                StringBuilder out=new StringBuilder();
                try{HexFormat.fromHexDigits("123456789");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.fromHexDigitsToLong("12345678901234567");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{HexFormat.fromHexDigits("1x");}catch(IllegalArgumentException e){out.append(name(e)).append(';');}
                try{HexFormat.fromHexDigits("12",1,3);}catch(IndexOutOfBoundsException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                return out.toString();
            }

            public static String accessors(){
                HexFormat f=HexFormat.ofDelimiter("-").withPrefix("<").withSuffix(">").withUpperCase();
                return f.delimiter()+f.prefix()+f.suffix()+f.isUpperCase()+f.withLowerCase().isUpperCase()+HexFormat.of().isUpperCase()
                        +"|"+HexFormat.of().delimiter().isEmpty()+"|"+f+"|"+HexFormat.of()+"|"+HexFormat.ofDelimiter("\\n");
            }
            public static String equality(){
                HexFormat a=HexFormat.ofDelimiter(":").withUpperCase(),b=HexFormat.of().withDelimiter(":").withUpperCase();
                return a.equals(b)+","+(a.hashCode()==b.hashCode())+","+a.equals(b.withLowerCase())+","+a.equals(b.withPrefix("x"))+","+a.equals(null)
                        +","+a.equals("x")+","+HexFormat.of().equals(HexFormat.of())+","+a.hashCode()+","+HexFormat.of().hashCode()+","+(HexFormat.of()==HexFormat.of());
            }
            public static String nullArguments(){
                StringBuilder out=new StringBuilder();
                try{HexFormat.ofDelimiter(null);}catch(NullPointerException e){out.append("d:").append(e.getMessage()).append(';');}
                try{HexFormat.of().withPrefix(null);}catch(NullPointerException e){out.append("p:").append(e.getMessage()).append(';');}
                try{HexFormat.of().withSuffix(null);}catch(NullPointerException e){out.append("s:").append(e.getMessage()).append(';');}
                return out.toString();
            }
        }
        """;

    @Test void formatsBytes() throws Exception {
        matchesJvm(FIXTURE, "basics", "range", "rangeFailures", "delimited", "roundTrip");
    }

    @Test void parsesHexStrings() throws Exception {
        matchesJvm(FIXTURE, "parse", "parseFailures");
    }

    @Test void convertsSingleValuesAndDigits() throws Exception {
        matchesJvm(FIXTURE, "digits", "digitCountFailure", "hexDigitChecks", "fromHexDigitFailures", "fromDigits", "fromDigitsFailures");
    }

    @Test void exposesSettingsEqualityAndNullChecks() throws Exception {
        matchesJvm(FIXTURE, "accessors", "equality", "nullArguments");
    }

    @Test void leavesAppendableAndCharArrayOverloadsUnlinked() {
        rejectsUnusedCall("java/util/HexFormat", "formatHex", "(Ljava/lang/Appendable;[B)Ljava/lang/Appendable;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/HexFormat", "toHexDigits", "(Ljava/lang/Appendable;B)Ljava/lang/Appendable;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/HexFormat", "parseHex", "([CII)[B", INVOKEVIRTUAL, false);
    }
}
