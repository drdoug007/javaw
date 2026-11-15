package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/**
 * T31: String, StringBuilder and StringBuffer index and range failures must carry the exact JDK 21 exception
 * types and messages. Fixtures return "SimpleClassName:message" (plus the state after the failure where it matters).
 * Expected values come from the JVM; see tasks/T31-string-range-messages.md.
 */
class BytecodeStringRangeMessagesTest extends KitDifferential {
    private static final String HELPERS = """
            import java.io.*;
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;
            import java.util.Arrays;

            /** A CharSequence that records the calls StringBuilder makes on it. */
            class Recording implements CharSequence {
                final String text;final StringBuilder calls=new StringBuilder();
                Recording(String text){this.text=text;}
                public int length(){calls.append("L");return text.length();}
                public char charAt(int i){calls.append("c").append(i);return text.charAt(i);}
                public CharSequence subSequence(int b,int e){calls.append("s").append(b).append(",").append(e);return text.subSequence(b,e);}
                public String toString(){calls.append("T");return text;}
            }

            /** A library-style Writer that records the char-array writes it receives. */
            class RecordingWriter extends Writer {
                final StringBuilder calls=new StringBuilder();
                public void write(char[] c,int off,int len){calls.append("w").append(off).append(",").append(len).append(":").append(c,off,len).append(";");}
                public void flush(){}
                public void close(){}
            }

            public class Fixture {
                static String name(Throwable e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                static final char[] XYZ={'x','y','z'};
                static final byte[] ABC={97,98,99};
                static StringBuilder sb(){return new StringBuilder("abc");}
                static char[] dots(int n){char[] c=new char[n];for(int i=0;i<n;i++)c[i]='.';return c;}
                static String getChars(String s,int b,int e,char[] d,int o){try{s.getChars(b,e,d,o);return new String(d);}catch(RuntimeException x){return name(x)+"|"+new String(d);}}
                static String sbGetChars(CharSequence s,int b,int e,char[] d,int o){
                    try{if(s instanceof StringBuilder)((StringBuilder)s).getChars(b,e,d,o);else ((StringBuffer)s).getChars(b,e,d,o);return new String(d);}
                    catch(RuntimeException x){return name(x)+"|"+new String(d);}
                }
            """;

    private void check(String methods, String... names) throws Exception {
        matchesJvm(HELPERS + methods + "\n}\n", names);
    }

    @Test void stringIndexFailures() throws Exception {
        check("""
                public static String charAtPastEnd(){try{return ""+"abc".charAt(5);}catch(RuntimeException e){return name(e);}}
                public static String charAtLength(){try{return ""+"abc".charAt(3);}catch(RuntimeException e){return name(e);}}
                public static String charAtNegative(){try{return ""+"abc".charAt(-1);}catch(RuntimeException e){return name(e);}}
                public static String charAtMinValue(){try{return ""+"abc".charAt(Integer.MIN_VALUE);}catch(RuntimeException e){return name(e);}}
                public static String charAtEmpty(){try{return ""+"".charAt(0);}catch(RuntimeException e){return name(e);}}
                public static String charAtInBounds(){return ""+"abc".charAt(0)+"abc".charAt(2);}
                public static String codePointAtPastEnd(){try{return ""+"abc".codePointAt(3);}catch(RuntimeException e){return name(e);}}
                public static String codePointAtNegative(){try{return ""+"abc".codePointAt(-1);}catch(RuntimeException e){return name(e);}}
                public static String codePointBeforeZero(){try{return ""+"abc".codePointBefore(0);}catch(RuntimeException e){return name(e);}}
                public static String codePointBeforePastEnd(){try{return ""+"abc".codePointBefore(4);}catch(RuntimeException e){return name(e);}}
                public static String codePointBeforeInBounds(){String s="a\\ud83d\\ude00";return s.codePointBefore(3)+","+s.codePointBefore(1)+","+s.codePointBefore(2)+","+s.codePointAt(1);}
                public static String codePointCountInverted(){try{return ""+"abc".codePointCount(2,1);}catch(RuntimeException e){return name(e);}}
                public static String codePointCountNegative(){try{return ""+"abc".codePointCount(-1,1);}catch(RuntimeException e){return name(e);}}
                public static String codePointCountPastEnd(){try{return ""+"abc".codePointCount(0,4);}catch(RuntimeException e){return name(e);}}
                public static String offsetByCodePointsKeepsNullMessages(){
                    StringBuilder out=new StringBuilder();int[][] cases={{4,0},{-1,0},{1,5},{1,-5}};
                    for(int[] c:cases){try{out.append("abc".offsetByCodePoints(c[0],c[1]));}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    return out.append("abc".offsetByCodePoints(1,2)).toString();
                }
                public static String stringIndexExceptionConstructors(){
                    return new StringIndexOutOfBoundsException(7).getMessage()+"|"+new StringIndexOutOfBoundsException(-2).getMessage()+"|"
                        +new StringIndexOutOfBoundsException("m").getMessage()+"|"+new StringIndexOutOfBoundsException().getMessage()+"|"
                        +new IndexOutOfBoundsException(4).getMessage();
                }
                """,
                "charAtPastEnd", "charAtLength", "charAtNegative", "charAtMinValue", "charAtEmpty", "charAtInBounds",
                "codePointAtPastEnd", "codePointAtNegative", "codePointBeforeZero", "codePointBeforePastEnd",
                "codePointBeforeInBounds", "codePointCountInverted", "codePointCountNegative", "codePointCountPastEnd",
                "offsetByCodePointsKeepsNullMessages", "stringIndexExceptionConstructors");
    }

    @Test void stringSubstringAndGetCharsRanges() throws Exception {
        check("""
                public static String substringFromPastEnd(){try{return "abc".substring(4);}catch(RuntimeException e){return name(e);}}
                public static String substringFromNegative(){try{return "abc".substring(-1);}catch(RuntimeException e){return name(e);}}
                public static String substringInverted(){try{return "abc".substring(2,1);}catch(RuntimeException e){return name(e);}}
                public static String substringNegativeBegin(){try{return "abc".substring(-1,2);}catch(RuntimeException e){return name(e);}}
                public static String substringEndPastLength(){try{return "abc".substring(0,4);}catch(RuntimeException e){return name(e);}}
                public static String substringBothPastEnd(){try{return "abc".substring(5,4);}catch(RuntimeException e){return name(e);}}
                public static String substringEdges(){return "["+"abc".substring(3)+"]["+"abc".substring(3,3)+"]["+"abc".substring(0,3)+"]["+"abc".substring(1,2)+"]";}
                public static String subSequenceInverted(){CharSequence s="abc";try{return s.subSequence(2,1).toString();}catch(RuntimeException e){return name(e);}}
                public static String subSequencePastEnd(){try{return "abc".subSequence(1,9).toString();}catch(RuntimeException e){return name(e);}}
                public static String getCharsSourceRange(){return getChars("abc",0,4,dots(5),0)+";"+getChars("abc",2,1,dots(5),0)+";"+getChars("abc",-1,1,dots(5),0);}
                public static String getCharsDestinationRange(){return getChars("abc",0,3,dots(2),0)+";"+getChars("abc",0,3,dots(5),-1)+";"+getChars("abc",0,3,dots(5),3)+";"+getChars("abc",1,3,dots(4),2);}
                public static String getCharsSourceCheckedFirst(){return getChars("abc",0,4,dots(1),7);}
                public static String getCharsEmptyRangeAtEnd(){return getChars("abc",3,3,dots(2),2)+"|"+getChars("abc",1,1,dots(0),0);}
                @SuppressWarnings("deprecation")
                public static String deprecatedGetBytesRanges(){
                    StringBuilder out=new StringBuilder();int[][] cases={{0,4,0,5},{2,1,0,5},{0,3,3,5},{0,3,-1,5},{1,3,0,2}};
                    for(int[] c:cases){byte[] d=new byte[c[3]];try{"abc".getBytes(c[0],c[1],d,c[2]);out.append(Arrays.toString(d));}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    return out.toString();
                }
                """,
                "substringFromPastEnd", "substringFromNegative", "substringInverted", "substringNegativeBegin",
                "substringEndPastLength", "substringBothPastEnd", "substringEdges", "subSequenceInverted",
                "subSequencePastEnd", "getCharsSourceRange", "getCharsDestinationRange", "getCharsSourceCheckedFirst",
                "getCharsEmptyRangeAtEnd", "deprecatedGetBytesRanges");
    }

    @Test void stringConstructorRanges() throws Exception {
        check("""
                public static String charsOffsetCount(){
                    StringBuilder out=new StringBuilder();int[][] cases={{1,5},{-1,1},{1,-1},{4,0},{3,0},{1,Integer.MAX_VALUE},{0,3},{1,2}};
                    for(int[] c:cases){try{out.append("[").append(new String(XYZ,c[0],c[1])).append("]");}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    return out.toString();
                }
                public static String valueOfAndCopyValueOf(){
                    String a,b;
                    try{a=String.valueOf(XYZ,1,5);}catch(RuntimeException e){a=name(e);}
                    try{b=String.copyValueOf(XYZ,2,2);}catch(RuntimeException e){b=name(e);}
                    return a+";"+b+";"+String.valueOf(XYZ,1,2)+String.copyValueOf(XYZ,0,1);
                }
                public static String bytesOffsetCount(){
                    StringBuilder out=new StringBuilder();int[][] cases={{1,5},{-1,1},{1,-1},{4,0},{3,0},{0,3}};
                    for(int[] c:cases){try{out.append("[").append(new String(ABC,c[0],c[1])).append("]");}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    return out.toString();
                }
                public static String bytesWithCharset(){
                    StringBuilder out=new StringBuilder();
                    Charset[] sets={StandardCharsets.UTF_8,StandardCharsets.ISO_8859_1,StandardCharsets.US_ASCII,StandardCharsets.UTF_16BE};
                    for(Charset cs:sets){try{out.append(new String(ABC,2,2,cs));}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    try{out.append(new String(ABC,-1,2,StandardCharsets.UTF_8));}catch(RuntimeException e){out.append(name(e));}
                    return out.append(";").append(new String(ABC,1,2,StandardCharsets.ISO_8859_1)).toString();
                }
                public static String bytesWithCharsetName(){
                    StringBuilder out=new StringBuilder();
                    String[] names={"UTF-8","ISO-8859-1","UTF-16LE"};
                    for(String n:names){try{out.append(new String(ABC,1,3,n));}catch(Exception e){out.append(name(e));}out.append(";");}
                    try{out.append(new String(ABC,0,2,"US-ASCII"));}catch(Exception e){out.append(name(e));}
                    return out.toString();
                }
                public static String bytesNullCharsetBeforeRange(){try{return new String(ABC,9,9,(Charset)null);}catch(RuntimeException e){return e.getClass().getSimpleName();}}
                @SuppressWarnings("deprecation")
                public static String deprecatedHighByte(){
                    StringBuilder out=new StringBuilder();
                    try{out.append(new String(ABC,0,1,5));}catch(RuntimeException e){out.append(name(e));}
                    out.append(";");
                    try{out.append(new String(ABC,1,-1,0));}catch(RuntimeException e){out.append(name(e));}
                    return out.append(";").append(new String(ABC,0,1,2)).toString();
                }
                public static String codePointsOffsetCount(){
                    int[] cps={97,0x1f600,99};StringBuilder out=new StringBuilder();int[][] cases={{1,5},{-1,1},{3,1},{1,2}};
                    for(int[] c:cases){try{out.append(new String(cps,c[0],c[1]).length());}catch(RuntimeException e){out.append(name(e));}out.append(";");}
                    return out.toString();
                }
                """,
                "charsOffsetCount", "valueOfAndCopyValueOf", "bytesOffsetCount", "bytesWithCharset", "bytesWithCharsetName",
                "bytesNullCharsetBeforeRange", "deprecatedHighByte", "codePointsOffsetCount");
    }

    @Test void builderIndexFailures() throws Exception {
        check("""
                public static String builderCharAt(){StringBuilder b=sb();String r;try{r=""+b.charAt(5);}catch(RuntimeException e){r=name(e);}try{r+=";"+b.charAt(-1);}catch(RuntimeException e){r+=";"+name(e);}return r+";"+b.charAt(2);}
                public static String builderCharAtAfterSetLength(){StringBuilder b=new StringBuilder("abcdef");b.setLength(2);try{return ""+b.charAt(2);}catch(RuntimeException e){return name(e);}}
                public static String builderSetCharAt(){StringBuilder b=sb();try{b.setCharAt(3,'x');return "none";}catch(RuntimeException e){return name(e)+"|"+b;}}
                public static String builderDeleteCharAt(){StringBuilder b=sb();String r;try{b.deleteCharAt(3);r="none";}catch(RuntimeException e){r=name(e);}try{b.deleteCharAt(-1);}catch(RuntimeException e){r+=";"+name(e);}return r+"|"+b.deleteCharAt(1);}
                public static String builderCodePoints(){StringBuilder b=sb();StringBuilder out=new StringBuilder();
                    try{out.append(b.codePointAt(3));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.codePointBefore(0));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.codePointCount(2,1));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.offsetByCodePoints(1,5));}catch(RuntimeException e){out.append(name(e));}
                    return out.toString();
                }
                public static String builderSetLengthNegative(){StringBuilder b=sb();try{b.setLength(-1);return "none";}catch(RuntimeException e){return name(e)+"|"+b;}}
                public static String builderSetLengthMinValue(){try{sb().setLength(Integer.MIN_VALUE);return "none";}catch(RuntimeException e){return name(e);}}
                public static String builderSetLengthValid(){StringBuilder b=sb();b.setLength(1);b.setLength(3);return b.length()+":"+(int)b.charAt(2);}
                public static String bufferIndexFailures(){StringBuffer f=new StringBuffer("abc");StringBuilder out=new StringBuilder();
                    try{f.charAt(5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.setCharAt(-2,'q');}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.deleteCharAt(3);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.setLength(-4);}catch(RuntimeException e){out.append(name(e));}
                    return out.append("|").append(f).toString();
                }
                """,
                "builderCharAt", "builderCharAtAfterSetLength", "builderSetCharAt", "builderDeleteCharAt", "builderCodePoints",
                "builderSetLengthNegative", "builderSetLengthMinValue", "builderSetLengthValid", "bufferIndexFailures");
    }

    @Test void builderDeleteReplaceSubstringAndGetChars() throws Exception {
        check("""
                public static String builderDelete(){
                    StringBuilder out=new StringBuilder();int[][] cases={{2,1},{-1,1},{4,5},{1,9},{4,2},{3,3},{3,9},{0,0}};
                    for(int[] c:cases){StringBuilder b=sb();try{out.append("[").append(b.delete(c[0],c[1])).append("]");}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    return out.toString();
                }
                public static String builderReplace(){
                    StringBuilder out=new StringBuilder();int[][] cases={{2,1},{4,5},{-1,1},{1,9},{3,3},{4,2}};
                    for(int[] c:cases){StringBuilder b=sb();try{out.append("[").append(b.replace(c[0],c[1],"XY")).append("]");}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    return out.toString();
                }
                public static String builderReplaceRangeBeforeNull(){try{sb().replace(-1,1,null);return "none";}catch(RuntimeException e){return name(e);}}
                public static String builderReplaceNullInRange(){try{sb().replace(1,2,null);return "none";}catch(RuntimeException e){return e.getClass().getSimpleName();}}
                public static String builderSubstring(){StringBuilder b=sb();StringBuilder out=new StringBuilder();
                    try{out.append(b.substring(4));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.substring(2,1));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.substring(-1,1));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{out.append(b.subSequence(0,4));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append(b.substring(3)).append(b.substring(1,2)).append(b.subSequence(0,1)).toString();
                }
                public static String builderGetChars(){return sbGetChars(sb(),0,4,dots(5),0)+";"+sbGetChars(sb(),2,1,dots(5),0)+";"+sbGetChars(sb(),-1,1,dots(5),0)+";"+sbGetChars(sb(),1,3,dots(3),1);}
                public static String builderGetCharsDestination(){return sbGetChars(sb(),0,3,dots(2),0)+";"+sbGetChars(sb(),0,3,dots(5),-1)+";"+sbGetChars(sb(),1,3,dots(5),4)+";"+sbGetChars(sb(),0,4,dots(1),9);}
                public static String bufferRangeFailures(){StringBuffer f=new StringBuffer("abc");StringBuilder out=new StringBuilder();
                    try{f.substring(2,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.delete(4,5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.replace(-1,0,"q");}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    out.append(sbGetChars(f,0,3,dots(2),0));
                    return out.append("|").append(f).toString();
                }
                """,
                "builderDelete", "builderReplace", "builderReplaceRangeBeforeNull", "builderReplaceNullInRange",
                "builderSubstring", "builderGetChars", "builderGetCharsDestination", "bufferRangeFailures");
    }

    @Test void builderInsertAndAppendRanges() throws Exception {
        check("""
                public static String builderInsertOffset(){StringBuilder out=new StringBuilder();
                    try{sb().insert(4,"x");}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(-1,"x");}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(4,'x');}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(4,XYZ);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(5,(CharSequence)new StringBuilder("q"));}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append(sb().insert(3,"x")).append(sb().insert(0,'y')).toString();
                }
                public static String builderInsertCharArrayRange(){StringBuilder out=new StringBuilder();int[][] cases={{4,0,1},{1,2,2},{1,-1,2},{1,1,-1},{1,1,Integer.MAX_VALUE},{1,3,0},{1,1,2}};
                    for(int[] c:cases){StringBuilder b=sb();try{out.append(b.insert(c[0],XYZ,c[1],c[2]));}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    return out.toString();
                }
                public static String builderInsertSequenceRange(){StringBuilder out=new StringBuilder();
                    try{sb().insert(4,"xy",0,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(1,"xy",1,3);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(1,"xy",2,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(1,"xy",-1,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(1,(CharSequence)null,0,5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().insert(9,(CharSequence)null,0,9);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append(sb().insert(1,"xy",1,2)).append(sb().insert(3,(CharSequence)null,1,3)).toString();
                }
                public static String builderAppendSequenceRange(){StringBuilder out=new StringBuilder();int[][] cases={{1,3},{2,1},{-1,1},{0,2},{2,2}};
                    for(int[] c:cases){StringBuilder b=sb();try{out.append(b.append("xy",c[0],c[1]));}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    try{sb().append((CharSequence)null,0,5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{sb().append(new StringBuilder("pq"),1,4);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append(sb().append((CharSequence)null,1,3)).toString();
                }
                public static String builderAppendCharArrayRange(){StringBuilder out=new StringBuilder();int[][] cases={{2,2},{-1,2},{1,-1},{1,Integer.MAX_VALUE},{4,0},{3,0},{1,2}};
                    for(int[] c:cases){StringBuilder b=sb();try{out.append(b.append(XYZ,c[0],c[1]));}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    return out.toString();
                }
                public static String builderRecordingSequence(){Recording r=new Recording("wxyz");StringBuilder b=sb();StringBuilder out=new StringBuilder();
                    try{b.append(r,2,5);}catch(RuntimeException e){out.append(name(e));}out.append("|").append(r.calls).append("|");r.calls.setLength(0);
                    try{b.append(r,-1,2);}catch(RuntimeException e){out.append(name(e));}out.append("|").append(r.calls).append("|");r.calls.setLength(0);
                    try{b.insert(1,r,3,1);}catch(RuntimeException e){out.append(name(e));}out.append("|").append(r.calls).append("|");r.calls.setLength(0);
                    try{b.insert(7,r);}catch(RuntimeException e){out.append(name(e));}out.append("|").append(r.calls).append("|");r.calls.setLength(0);
                    try{b.insert(7,r,0,1);}catch(RuntimeException e){out.append(name(e));}out.append("|").append(r.calls).append("|");r.calls.setLength(0);
                    b.append(r,1,3);out.append(r.calls).append("|");r.calls.setLength(0);
                    b.insert(0,r,2,4);out.append(r.calls).append("|");r.calls.setLength(0);
                    b.insert(1,r);out.append(r.calls).append("|");r.calls.setLength(0);
                    return out.append(b).toString();
                }
                public static String bufferInsertAppendFailures(){StringBuffer f=new StringBuffer("abc");StringBuilder out=new StringBuilder();
                    try{f.append("xy",2,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.append(XYZ,2,2);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(4,"x");}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(5,1.5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(4,7);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(-1,(Object)"o");}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(1,XYZ,2,2);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{f.insert(1,"xy",0,3);}catch(RuntimeException e){out.append(name(e));}
                    return out.append("|").append(f).toString();
                }
                """,
                "builderInsertOffset", "builderInsertCharArrayRange", "builderInsertSequenceRange", "builderAppendSequenceRange",
                "builderAppendCharArrayRange", "builderRecordingSequence", "bufferInsertAppendFailures");
    }

    @Test void codePointAndRepeatArguments() throws Exception {
        check("""
                public static String appendCodePointInvalid(){StringBuilder out=new StringBuilder();int[] cps={-1,0x110000,Integer.MIN_VALUE,0xABCDEF};
                    for(int cp:cps){StringBuilder b=sb();try{b.appendCodePoint(cp);}catch(RuntimeException e){out.append(name(e)).append("|").append(b);}out.append(";");}
                    return out.append(sb().appendCodePoint(0x10ffff).length()).append(sb().appendCodePoint(0).length()).toString();
                }
                public static String bufferAppendCodePointInvalid(){StringBuffer f=new StringBuffer("q");try{f.appendCodePoint(0x7fffffff);return "none";}catch(RuntimeException e){return name(e)+"|"+f;}}
                public static String codePointsInvalidValue(){StringBuilder out=new StringBuilder();
                    try{new String(new int[]{97,-1},0,2);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{new String(new int[]{0x110000},0,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{new String(new int[]{-1},0,2);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append(new String(new int[]{-1,98,99},1,2)).toString();
                }
                public static String repeatNegative(){StringBuilder out=new StringBuilder();
                    try{"ab".repeat(-1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{"".repeat(Integer.MIN_VALUE);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    return out.append("[").append("ab".repeat(0)).append("][").append("ab".repeat(2)).append("]").toString();
                }
                """,
                "appendCodePointInvalid", "bufferAppendCodePointInvalid", "codePointsInvalidValue", "repeatNegative");
    }

    @Test void writersReachTheseChecks() throws Exception {
        check("""
                public static String stringWriterWriteStringRange(){StringWriter w=new StringWriter();w.write("ab");try{w.write("abc",2,2);return "none";}catch(RuntimeException e){return name(e)+"|"+w;}}
                public static String stringWriterAppendRange(){StringWriter w=new StringWriter();StringBuilder out=new StringBuilder();
                    try{w.append("abc",2,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{w.append(new StringBuilder("abc"),0,4);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{w.append(null,1,5);}catch(RuntimeException e){out.append(name(e));}
                    return out.append("|").append(w.append("abc",1,3).append(null,0,2)).toString();
                }
                public static String charArrayWriterStringRange(){CharArrayWriter w=new CharArrayWriter();StringBuilder out=new StringBuilder();
                    try{w.write("abc",1,5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{w.write("abc",-1,1);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{w.append("abc",-1,1);}catch(RuntimeException e){out.append(name(e));}
                    w.write("abc",1,2);return out.append("|").append(w).append("|").append(w.size()).toString();
                }
                public static String writerSubclassStringRange() throws IOException {RecordingWriter r=new RecordingWriter();StringBuilder out=new StringBuilder();
                    try{r.write("abcdef",2,9);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{r.append("abc",3,2);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    r.write("abcdef",1,2);r.append("xyz",1,3);
                    return out.append("|").append(r.calls).toString();
                }
                public static String printWriterStringRange(){StringWriter s=new StringWriter();PrintWriter p=new PrintWriter(s);StringBuilder out=new StringBuilder();
                    try{p.write("abc",1,5);}catch(RuntimeException e){out.append(name(e));}out.append(";");
                    try{p.append("abc",2,1);}catch(RuntimeException e){out.append(name(e));}
                    p.write("abc",0,2);p.flush();return out.append("|").append(s).append("|").append(p.checkError()).toString();
                }
                """,
                "stringWriterWriteStringRange", "stringWriterAppendRange", "charArrayWriterStringRange",
                "writerSubclassStringRange", "printWriterStringRange");
    }
}
