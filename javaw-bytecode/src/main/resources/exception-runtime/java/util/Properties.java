package java.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.*;

/**
 * A permanent set of key/value pairs whose entries live in the inherited Hashtable. The JDK 9+
 * implementation keeps its entries in a private concurrent map; here the observable behaviour that
 * library callers depend on (defaults chain, String-only lookups, the null rules copied from that
 * map, and which overridable methods are called) is matched, while multi-entry iteration order
 * follows the Hashtable instead of the concurrent map.
 */
public class Properties extends Hashtable<Object,Object>{
    protected Properties defaults;

    public Properties(){super();}
    public Properties(int initialCapacity){
        super();
        if(initialCapacity<0)throw new IllegalArgumentException();
    }
    public Properties(Properties defaults){
        super();
        this.defaults=defaults;
    }

    public Object setProperty(String key,String value){return put(key,value);}
    public String getProperty(String key){
        Object value=super.get(key);
        String text=value instanceof String?(String)value:null;
        return text==null && defaults!=null?defaults.getProperty(key):text;
    }
    public String getProperty(String key,String defaultValue){
        String value=getProperty(key);
        return value==null?defaultValue:value;
    }

    public Enumeration<?> propertyNames(){
        Hashtable<String,Object> h=new Hashtable<String,Object>();
        enumerate(h);
        return h.keys();
    }
    public Set<String> stringPropertyNames(){
        Map<String,String> h=new HashMap<String,String>();
        enumerateStringProperties(h);
        return Collections.unmodifiableSet(h.keySet());
    }

    public void putAll(Map<?,?> t){
        t.size();
        for(Map.Entry<?,?> e:t.entrySet())super.put(e.getKey(),e.getValue());
    }
    public boolean remove(Object key,Object value){
        if(key==null)throw new NullPointerException();
        return value!=null && super.remove(key,value);
    }
    public Object merge(Object key,Object value,BiFunction<? super Object,? super Object,?> remappingFunction){
        if(key==null || value==null || remappingFunction==null)throw new NullPointerException();
        return super.merge(key,value,remappingFunction);
    }
    public void forEach(BiConsumer<? super Object,? super Object> action){
        Objects.requireNonNull(action);
        for(Object key:new ArrayList<Object>(super.keySet())){
            Object value=super.get(key);
            if(value!=null)action.accept(key,value);
        }
    }

    public String toString(){
        StringBuilder out=new StringBuilder("{");
        boolean first=true;
        for(Map.Entry<Object,Object> e:super.entrySet()){
            if(!first)out.append(", ");
            first=false;
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.append('}').toString();
    }
    public boolean equals(Object o){
        if(!(o instanceof Map))return false;
        Map<?,?> m=(Map<?,?>)o;
        for(Map.Entry<Object,Object> e:super.entrySet()){
            Object value=e.getValue();
            Object v=m.get(e.getKey());
            if(v==null || (v!=value && !v.equals(value)))return false;
        }
        for(Map.Entry<?,?> e:m.entrySet()){
            Object key=e.getKey();
            Object value=e.getValue();
            if(key==null || value==null)return false;
            Object mine=super.get(key);
            if(mine==null || (value!=mine && !value.equals(mine)))return false;
        }
        return true;
    }

    private void enumerate(Map<String,Object> h){
        if(defaults!=null)defaults.enumerate(h);
        for(Map.Entry<Object,Object> e:entrySet())h.put((String)e.getKey(),e.getValue());
    }
    private void enumerateStringProperties(Map<String,String> h){
        if(defaults!=null)defaults.enumerateStringProperties(h);
        for(Map.Entry<Object,Object> e:entrySet()){
            Object k=e.getKey();
            Object v=e.getValue();
            if(k instanceof String && v instanceof String)h.put((String)k,(String)v);
        }
    }

    /**
     * Reads a property list (key and element pairs) from the input character stream, in the
     * simple line-oriented format: leading ' ', '\t' and '\f' are skipped, a line whose first
     * char is '#' or '!' is a comment, '\r', '\n' or '\r\n' end a natural line, a trailing
     * backslash on a non-empty line continues it (the backslash is dropped, leading whitespace
     * of the next line skipped), and escapes are resolved by loadConvert. The source is left
     * open and the existing entries are kept.
     */
    public void load(Reader reader) throws IOException{
        Objects.requireNonNull(reader,"reader parameter is null");
        load0(new LineReader(reader));
    }
    public void load(InputStream inStream) throws IOException{
        Objects.requireNonNull(inStream,"inStream parameter is null");
        load0(new LineReader(inStream));
    }

    private void load0(LineReader lr) throws IOException{
        StringBuilder outBuffer=new StringBuilder();
        int limit,keyLen,valueStart;
        boolean hasSep,precedingBackslash;
        while((limit=lr.readLine())>=0){
            keyLen=0;
            valueStart=limit;
            hasSep=false;
            precedingBackslash=false;
            while(keyLen<limit){
                char c=lr.lineBuf[keyLen];
                if((c=='='||c==':')&&!precedingBackslash){
                    valueStart=keyLen+1;
                    hasSep=true;
                    break;
                }else if((c==' '||c=='\t'||c=='\f')&&!precedingBackslash){
                    valueStart=keyLen+1;
                    break;
                }
                precedingBackslash=(c=='\\')?!precedingBackslash:false;
                keyLen++;
            }
            while(valueStart<limit){
                char c=lr.lineBuf[valueStart];
                if(c!=' '&&c!='\t'&&c!='\f'){
                    if(!hasSep&&(c=='='||c==':'))hasSep=true;
                    else break;
                }
                valueStart++;
            }
            String key=loadConvert(lr.lineBuf,0,keyLen,outBuffer);
            String value=loadConvert(lr.lineBuf,valueStart,limit-valueStart,outBuffer);
            put(key,value);
        }
    }

    private static String loadConvert(char[] in,int off,int len,StringBuilder out){
        char aChar;
        int end=off+len;
        int start=off;
        while(off<end){
            aChar=in[off++];
            if(aChar=='\\')break;
        }
        if(off==end)return new String(in,start,len);
        out.setLength(0);
        off--;
        out.append(in,start,off-start);
        while(off<end){
            aChar=in[off++];
            if(aChar=='\\'){
                aChar=in[off++];
                if(aChar=='u'){
                    if(off>end-4)throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                    int value=0;
                    for(int i=0;i<4;i++){
                        aChar=in[off++];
                        if(aChar>='0'&&aChar<='9')value=(value<<4)+aChar-'0';
                        else if(aChar>='a'&&aChar<='f')value=(value<<4)+10+aChar-'a';
                        else if(aChar>='A'&&aChar<='F')value=(value<<4)+10+aChar-'A';
                        else throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                    }
                    out.append((char)value);
                }else{
                    if(aChar=='t')aChar='\t';
                    else if(aChar=='r')aChar='\r';
                    else if(aChar=='n')aChar='\n';
                    else if(aChar=='f')aChar='\f';
                    out.append(aChar);
                }
            }else{
                out.append(aChar);
            }
        }
        return out.toString();
    }

    /* Turn the input into logical lines, skipping comments and blank lines and stripping the
     * leading whitespace of every natural line; a line ends at '\r', '\n' or '\r\n', and an
     * odd number of trailing backslashes continues the line. readLine returns the length of
     * the next logical line (stored in lineBuf[0..len)) or -1 at the end of the input. */
    private static final class LineReader{
        char[] lineBuf=new char[1024];
        InputStream inStream;
        Reader reader;
        byte[] inByteBuf;
        char[] inCharBuf;
        int inLimit=0;
        int inOff=0;

        LineReader(InputStream inStream){
            this.inStream=inStream;
            inByteBuf=new byte[8192];
        }
        LineReader(Reader reader){
            this.reader=reader;
            inCharBuf=new char[8192];
        }

        int readLine() throws IOException{
            int len=0;
            int off=inOff;
            int limit=inLimit;
            boolean skipWhiteSpace=true;
            boolean appendedLineBegin=false;
            boolean precedingBackslash=false;
            boolean fromStream=inStream!=null;
            byte[] byteBuf=inByteBuf;
            char[] charBuf=inCharBuf;
            char[] lineBuf=this.lineBuf;
            char c;
            while(true){
                if(off>=limit){
                    inLimit=limit=fromStream?inStream.read(byteBuf):reader.read(charBuf);
                    if(limit<=0){
                        if(len==0)return -1;
                        return precedingBackslash?len-1:len;
                    }
                    off=0;
                }
                c=fromStream?(char)(byteBuf[off++]&0xFF):charBuf[off++];
                if(skipWhiteSpace){
                    if(c==' '||c=='\t'||c=='\f')continue;
                    if(!appendedLineBegin&&(c=='\r'||c=='\n'))continue;
                    skipWhiteSpace=false;
                    appendedLineBegin=false;
                }
                if(len==0&&(c=='#'||c=='!')){
                    boolean atLineEnd=false;
                    while(!atLineEnd){
                        if(fromStream){
                            byte b;
                            while(off<limit){
                                b=byteBuf[off++];
                                if(b<='\r'&&(b=='\r'||b=='\n')){atLineEnd=true;break;}
                            }
                            if(!atLineEnd&&off==limit){
                                inLimit=limit=inStream.read(byteBuf);
                                if(limit<=0)return -1;
                                off=0;
                            }
                        }else{
                            while(off<limit){
                                c=charBuf[off++];
                                if(c<='\r'&&(c=='\r'||c=='\n')){atLineEnd=true;break;}
                            }
                            if(!atLineEnd&&off==limit){
                                inLimit=limit=reader.read(charBuf);
                                if(limit<=0)return -1;
                                off=0;
                            }
                        }
                    }
                    skipWhiteSpace=true;
                    continue;
                }
                if(c!='\n'&&c!='\r'){
                    lineBuf[len++]=c;
                    if(len==lineBuf.length){
                        this.lineBuf=lineBuf=Arrays.copyOf(lineBuf,len*2);
                    }
                    precedingBackslash=(c=='\\')?!precedingBackslash:false;
                }else{
                    if(len==0){
                        skipWhiteSpace=true;
                        continue;
                    }
                    if(off>=limit){
                        inLimit=limit=fromStream?inStream.read(byteBuf):reader.read(charBuf);
                        off=0;
                        if(limit<=0)return precedingBackslash?len-1:len;
                    }
                    if(precedingBackslash){
                        len-=1;
                        skipWhiteSpace=true;
                        appendedLineBegin=true;
                        precedingBackslash=false;
                        if(c=='\r'){
                            if(fromStream){
                                if(byteBuf[off]=='\n')off++;
                            }else{
                                if(charBuf[off]=='\n')off++;
                            }
                        }
                    }else{
                        inOff=off;
                        return len;
                    }
                }
            }
        }
    }
}
