package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * T23: Properties.load(Reader) and Properties.load(InputStream) with the full JDK line format. Expected values come
 * from the JVM; see tasks/T23-properties-load.md. Every fixture reports the puts in the order load makes them (through
 * a Properties subclass that overrides put), so no output depends on hash order.
 */
@Tag("agent-pending")
class BytecodePropertiesLoadTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.*;
        import java.util.*;

        /** Logs every put that load makes, in order, with control and non-ASCII chars shown as <code>. */
        class Recorder extends Properties {
            final StringBuilder log=new StringBuilder();
            Recorder(){}
            Recorder(Properties defaults){super(defaults);}
            public Object put(Object k,Object v){log.append(PropertiesLoadFixture.show((String)k)).append('=').append(PropertiesLoadFixture.show((String)v)).append(' ');return super.put(k,v);}
            String take(){String s=log.toString().trim();log.setLength(0);return s;}
        }
        /** A Reader that hands out at most step chars per call and logs read(char[]) as a<len> and read(char[],int,int) as len@off. Returns 0 on call zeroAt and throws IOException("boom") on call failAt (1-based, 0 = never). */
        class Chunks extends Reader {
            final String data; final int step; int pos; int calls; int zeroAt; int failAt; boolean closed;
            final StringBuilder log=new StringBuilder();
            Chunks(String data,int step){this.data=data;this.step=step;}
            public int read(char[] b) throws IOException {log.append('a').append(b.length).append(' ');return super.read(b);}
            public int read(char[] b,int off,int len) throws IOException {
                log.append(len).append('@').append(off).append(' ');
                calls++;
                if(calls==failAt)throw new IOException("boom");
                if(calls==zeroAt)return 0;
                if(pos>=data.length())return -1;
                int n=Math.min(Math.min(len,step),data.length()-pos);
                data.getChars(pos,pos+n,b,off);pos+=n;return n;
            }
            public void close(){closed=true;log.append("close ");}
            String take(){String s=log.toString().trim();log.setLength(0);return s;}
        }
        /** The InputStream twin of Chunks: logs read(byte[]) as a<len>, read(byte[],int,int) as len@off and read() as r. */
        class ByteChunks extends InputStream {
            final byte[] data; final int step; int pos; int calls; int zeroAt; boolean closed;
            final StringBuilder log=new StringBuilder();
            ByteChunks(byte[] data,int step){this.data=data;this.step=step;}
            public int read(){log.append("r ");return pos<data.length?data[pos++]&0xff:-1;}
            public int read(byte[] b) throws IOException {log.append('a').append(b.length).append(' ');return super.read(b);}
            public int read(byte[] b,int off,int len){
                log.append(len).append('@').append(off).append(' ');
                calls++;
                if(calls==zeroAt)return 0;
                if(pos>=data.length)return -1;
                int n=Math.min(Math.min(len,step),data.length-pos);
                System.arraycopy(data,pos,b,off,n);pos+=n;return n;
            }
            public void close(){closed=true;log.append("close ");}
            String take(){String s=log.toString().trim();log.setLength(0);return s;}
        }

        public class PropertiesLoadFixture {
            static String show(String s){
                StringBuilder out=new StringBuilder("[");
                for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<32||c>126)out.append('<').append((int)c).append('>');else out.append(c);}
                return out.append(']').toString();
            }
            static String load(String text){
                Recorder p=new Recorder();
                try{p.load(new StringReader(text));}catch(IOException|RuntimeException e){p.log.append("| ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());}
                return p.take();
            }
            static String loadBytes(byte[] bytes){
                Recorder p=new Recorder();
                try{p.load(new ByteArrayInputStream(bytes));}catch(IOException|RuntimeException e){p.log.append("| ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());}
                return p.take();
            }
            static byte[] latin1(String s){byte[] b=new byte[s.length()];for(int i=0;i<b.length;i++)b[i]=(byte)s.charAt(i);return b;}
            static String sorted(Properties p){List<String> keys=new ArrayList<>();for(Object k:p.keySet())keys.add((String)k);Collections.sort(keys);StringBuilder out=new StringBuilder();for(String k:keys)out.append(show(k)).append('=').append(show((String)p.get(k))).append(' ');return out.toString().trim();}

            // separators and whitespace
            public static String separators(){return load("a=1\\nb:2\\nc 3\\nd\\te\\nf\\fg\\nh  =  i\\nj\\t:\\tk\\nv\\013w=1\\nx\\240y=2\\n\\013z=3");}
            public static String whitespaceAroundValue(){return load("  key  =  value  \\nk = = v\\nk2  :x\\nk3=:x\\nk4 :=x\\nk5 x y\\n\\t\\f  k6=z");}
            public static String emptyKeysAndValues(){return load("lonely\\nx=\\n=v\\n:\\n   =   \\ntrail   \\n");}
            // comments and blank lines
            public static String comments(){return load("#c\\n!c\\n  # indented\\n\\t!indented\\na=1 # not a comment\\n\\n   \\n\\t\\f\\nb=2!x\\n#x\\\\\\nc=3\\n#\\\\u12\\nd=4");}
            public static String commentOnlyInput(){return load("# just a comment")+"|"+load("")+"|"+load("\\n\\n\\r\\n  ")+"|"+load("#a\\r#b\\r\\n!c");}
            public static String duplicateKeys(){Recorder p=new Recorder();String r;try{p.load(new StringReader("a=1\\na=2\\nb=3\\na=4"));r=sorted(p);}catch(IOException e){r=e.toString();}return p.take()+"|"+r+"|"+p.size();}
            // line ends
            public static String lineEnds(){return load("a=1\\rb=2\\r\\nc=3\\n\\nd=4\\n\\re=5\\r\\rf=6");}
            public static String finalLineWithoutTerminator(){return load("a=1\\nlast=x")+"|"+load("only")+"|"+load("k=v ");}
            // continuation lines
            public static String continuation(){return load("a=one\\\\\\n    two\\\\\\n\\tthree\\nb=after");}
            public static String backslashCounts(){return load("b=x\\\\\\\\\\nc=x\\\\\\\\\\\\\\ny\\nd=\\\\\\\\\\\\\\\\\\ne=z");}
            public static String continuationLineEnds(){return load("d=1\\\\\\r\\n  2\\r\\ne=1\\\\\\r  2\\rf=1\\\\\\n\\r  2\\n");}
            public static String continuationEdgeCases(){return load("f=1\\\\\\n\\n g=2\\nh=1\\\\")+"|"+load("i=\\\\\\n#not a comment\\nlo\\\\\\n  ng=v\\nj=1\\\\\\n   \\\\\\n  2\\nk\\\\\\n=x");}
            public static String continuationOfComment(){return load("#comment\\\\\\nx=1")+"|"+load("a=\\\\\\n\\\\\\n\\\\\\n");}
            // escapes
            public static String escapes(){return load("t=\\\\t\\\\n\\\\r\\\\f|\\nq=\\\\b\\\\q\\\\\\\\\\\\\\"\\\\'\\n\\\\t\\\\n=key\\n");}
            public static String unicodeEscapes(){return load("u=\\\\u0041\\\\u00e9\\\\u20AC\\\\uFFFF\\\\u0000x\\nc=\\\\uabcdz\\n\\\\u0041\\\\u0042=AB");}
            public static String escapedTerminators(){return load("a\\\\=b\\\\:c\\\\ d=e\\nk=\\\\ lead\\n\\\\#notcomment=1\\n\\\\!x=2\\nsp\\\\ ace\\\\ =v\\nm=a\\\\=b");}
            public static String malformedUnicode(){return load("a=1\\nb=\\\\u12")+"|"+load("\\\\u00G0=x")+"|"+load("\\\\u41=1")+"|"+load("ok=1\\nx=\\\\u")+"|"+load("y=\\\\u004")+"|"+load("z=\\\\u00410");}
            public static String malformedInKeyBeforeValue(){return load("k\\\\uZZZZ=\\\\u0041")+"|"+load("v=\\\\u 041");}
            // nulls and existing state
            public static String nullSources(){
                Properties p=new Properties();String a,b;
                try{p.load((Reader)null);a="no exception";}catch(Exception e){a=e.getClass().getSimpleName()+": "+e.getMessage();}
                try{p.load((InputStream)null);b="no exception";}catch(Exception e){b=e.getClass().getSimpleName()+": "+e.getMessage();}
                return a+"|"+b;
            }
            public static String loadKeepsExistingEntries() throws IOException {
                Properties d=new Properties();d.setProperty("x","dx");d.setProperty("y","dy");
                Properties p=new Properties(d);p.setProperty("a","old");p.put("n",42);
                p.load(new StringReader("a=new\\nx=own\\nb=2"));
                return sorted(d)+"|"+p.getProperty("a")+p.getProperty("x")+p.getProperty("y")+p.getProperty("b")+"|"+p.get("n")+"|"+p.size();
            }
            // InputStream: ISO-8859-1
            public static String streamLatin1(){return loadBytes(new byte[]{'k','=',(byte)0xe9,(byte)0xff,(byte)0x80,(byte)0xa0,'\\n',(byte)0xc3,(byte)0xa9,'=',(byte)0xc3,(byte)0xa9});}
            public static String streamFormat(){return loadBytes(latin1("# c\\u00ff\\n!\\u0080\\r\\n  a = 1 \\\\\\r\\n   2\\rb:\\\\u00e9\\\\t\\nc\\\\=d e\\\\\\\\\\n\\\\u00ffkey=v"));}
            public static String streamMalformed(){return loadBytes(latin1("a=1\\nb=\\\\uXYZW\\nc=3"));}
            public static String streamHighByteComment(){return loadBytes(new byte[]{'#',(byte)0x8d,(byte)0x8a,(byte)0xfd,'\\n','a','=','1','\\r','!',(byte)0x0d});}
            // long lines and long input
            public static String longValue() throws IOException {
                StringBuilder text=new StringBuilder("big=");for(int i=0;i<3000;i++)text.append((char)('a'+i%26));
                text.append("\\nkey");for(int i=0;i<1500;i++)text.append('k');text.append("=v\\\\\\n   ");for(int i=0;i<1100;i++)text.append('w');
                Properties p=new Properties();p.load(new StringReader(text.toString()));
                String big=p.getProperty("big");String key=null;for(Object k:p.keySet())if(((String)k).startsWith("key"))key=(String)k;
                String v=p.getProperty(key);
                return big.length()+"|"+big.substring(2990)+"|"+key.length()+"|"+v.length()+"|"+v.substring(0,3)+"|"+p.size();
            }
            public static String manyLines() throws IOException {
                StringBuilder text=new StringBuilder();
                for(int i=0;i<1500;i++){text.append(i%3==0?"# comment line ":"key").append(i).append(i%2==0?"=":" : ").append("value").append(i);text.append(i%4==0?"\\r\\n":i%4==1?"\\r":"\\n");}
                Chunks r=new Chunks(text.toString(),100000);Properties p=new Properties();p.load(r);
                int calls=r.calls;
                return text.length()+"|"+p.size()+"|"+p.getProperty("key1")+"|"+p.getProperty("key1499")+"|"+p.getProperty("key3")+"|"+p.getProperty("key1000")+"|"+calls;
            }
            // what a Reader or InputStream subclass sees
            public static String readerCallPattern() throws IOException {
                StringBuilder out=new StringBuilder();
                for(String text:new String[]{"a=1\\n","a=1","","#only","a=1\\n\\n\\n"}){
                    Chunks r=new Chunks(text,8192);Recorder p=new Recorder();p.load(r);out.append(r.take()).append(" -> ").append(p.take()).append('|');
                }
                return out.toString();
            }
            public static String readerSmallChunks() throws IOException {
                StringBuilder out=new StringBuilder();
                for(int step:new int[]{1,2,3,5}){
                    Chunks r=new Chunks("a=1\\r\\nb=2\\\\\\r\\n c\\r\\n#x\\\\\\r\\nd\\\\\\r",step);Recorder p=new Recorder();p.load(r);out.append(p.take()).append(' ').append(r.calls).append('|');
                }
                return out.toString();
            }
            public static String readerReturnsZero() throws IOException {
                StringBuilder out=new StringBuilder();
                for(int zeroAt=1;zeroAt<=8;zeroAt++){
                    Chunks r=new Chunks("a=1\\n\\nb=2\\n",1);r.zeroAt=zeroAt;Recorder p=new Recorder();p.load(r);
                    out.append(zeroAt).append(':').append(p.take()).append(" pos=").append(r.pos).append(" calls=").append(r.calls).append('|');
                }
                return out.toString();
            }
            public static String readerThrows(){
                StringBuilder out=new StringBuilder();
                for(int failAt:new int[]{1,2,3}){
                    Chunks r=new Chunks("a=1\\nb=2\\n",4);r.failAt=failAt;Recorder p=new Recorder();
                    try{p.load(r);out.append("no exception");}catch(IOException e){out.append(e.getMessage());}
                    out.append(' ').append(p.take()).append(' ').append(r.calls).append('|');
                }
                return out.toString();
            }
            public static String readerLeftOpen() throws IOException {
                StringReader r=new StringReader("a=1");Properties p=new Properties();p.load(r);
                boolean ready=r.ready();int next=r.read();
                ByteArrayInputStream in=new ByteArrayInputStream(new byte[]{'a','=','1','\\n','b'});p.load(in);
                return ready+"|"+next+"|"+in.available()+"|"+in.read()+"|"+p.getProperty("b").length();
            }
            public static String streamCallPattern() throws IOException {
                StringBuilder out=new StringBuilder();
                for(int step:new int[]{8192,3}){
                    ByteChunks in=new ByteChunks(latin1("a=1\\r\\n#\\u00e9\\nb=\\\\\\n  2"),step);Recorder p=new Recorder();p.load(in);out.append(in.take()).append(" -> ").append(p.take()).append(" closed=").append(in.closed).append('|');
                }
                for(int zeroAt:new int[]{3,6}){
                    ByteChunks z=new ByteChunks(latin1("a=1\\n\\nb=2\\n"),1);z.zeroAt=zeroAt;Recorder q=new Recorder();q.load(z);
                    out.append(zeroAt).append(':').append(q.take()).append(" pos=").append(z.pos).append(" calls=").append(z.calls).append('|');
                }
                return out.toString();
            }
        }
        """;

    @Test void lineFormat() throws Exception {
        matchesJvm(FIXTURE, "separators", "whitespaceAroundValue", "emptyKeysAndValues", "comments", "commentOnlyInput", "duplicateKeys", "loadKeepsExistingEntries");
    }

    @Test void lineEndsAndContinuations() throws Exception {
        matchesJvm(FIXTURE, "lineEnds", "finalLineWithoutTerminator", "continuation", "backslashCounts", "continuationLineEnds", "continuationEdgeCases", "continuationOfComment");
    }

    @Test void escapesAndFailures() throws Exception {
        matchesJvm(FIXTURE, "escapes", "unicodeEscapes", "escapedTerminators", "malformedUnicode", "malformedInKeyBeforeValue", "nullSources");
    }

    @Test void inputStreamIsLatin1() throws Exception {
        matchesJvm(FIXTURE, "streamLatin1", "streamFormat", "streamMalformed", "streamHighByteComment");
    }

    @Test void longLinesAndLongInput() throws Exception {
        matchesJvm(FIXTURE, "longValue", "manyLines");
    }

    @Test void sourceCallPatterns() throws Exception {
        matchesJvm(FIXTURE, "readerCallPattern", "readerSmallChunks", "readerReturnsZero", "readerThrows", "readerLeftOpen", "streamCallPattern");
    }

    /** store writes the current date, which the runtime cannot provide (RULES.md), so it stays unlinked. */
    @Test void leavesStoreUnlinked() {
        rejectsUnusedCall("java/util/Properties", "store", "(Ljava/io/Writer;Ljava/lang/String;)V", INVOKEVIRTUAL, false);
    }
}
