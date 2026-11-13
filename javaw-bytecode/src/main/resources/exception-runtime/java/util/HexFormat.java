package java.util;

public final class HexFormat {
    private static final String DIGITS_LOWER="0123456789abcdef";
    private static final String DIGITS_UPPER="0123456789ABCDEF";
    private static final HexFormat DEFAULT=new HexFormat("","","",false);

    private final String delimiter;
    private final String prefix;
    private final String suffix;
    private final boolean upperCase;

    private HexFormat(String delimiter,String prefix,String suffix,boolean upperCase){
        this.delimiter=Objects.requireNonNull(delimiter,"delimiter");
        this.prefix=Objects.requireNonNull(prefix,"prefix");
        this.suffix=Objects.requireNonNull(suffix,"suffix");
        this.upperCase=upperCase;
    }

    private String digits(){return upperCase?DIGITS_UPPER:DIGITS_LOWER;}

    public static HexFormat of(){return DEFAULT;}
    public static HexFormat ofDelimiter(String delimiter){return new HexFormat(delimiter,"","",false);}
    public HexFormat withDelimiter(String delimiter){return new HexFormat(delimiter,prefix,suffix,upperCase);}
    public HexFormat withPrefix(String prefix){return new HexFormat(delimiter,prefix,suffix,upperCase);}
    public HexFormat withSuffix(String suffix){return new HexFormat(delimiter,prefix,suffix,upperCase);}
    public HexFormat withUpperCase(){return new HexFormat(delimiter,prefix,suffix,true);}
    public HexFormat withLowerCase(){return new HexFormat(delimiter,prefix,suffix,false);}

    public String delimiter(){return delimiter;}
    public String prefix(){return prefix;}
    public String suffix(){return suffix;}
    public boolean isUpperCase(){return upperCase;}

    public String formatHex(byte[] bytes){return formatHex(bytes,0,bytes.length);}
    public String formatHex(byte[] bytes,int from,int to){
        Objects.requireNonNull(bytes,"bytes");
        checkRange(from,to,bytes.length);
        if(to==from)return "";
        StringBuilder out=new StringBuilder();
        for(int i=from;i<to;i++){
            if(i>from)out.append(delimiter);
            byte b=bytes[i];
            out.append(prefix).append(toHighHexDigit(b)).append(toLowHexDigit(b)).append(suffix);
        }
        return out.toString();
    }

    public byte[] parseHex(CharSequence string){return parseHex(string,0,string.length());}
    public byte[] parseHex(CharSequence string,int from,int to){
        Objects.requireNonNull(string,"string");
        checkRange(from,to,string.length());
        if(from!=0 || to!=string.length())string=string.subSequence(from,to);
        int length=string.length();
        if(length==0)return new byte[0];
        if(delimiter.isEmpty() && prefix.isEmpty() && suffix.isEmpty()){
            if((length&1)!=0)throw new IllegalArgumentException("string length not even: "+length);
            byte[] bytes=new byte[length/2];
            for(int i=0;i<bytes.length;i++)bytes[i]=(byte)fromHexDigits(string,i*2,i*2+2);
            return bytes;
        }
        int value=prefix.length()+2+suffix.length();
        int stride=value+delimiter.length();
        if((length-value)%stride!=0)
            throw new IllegalArgumentException("extra or missing delimiters or values consisting of prefix, two hexadecimal digits, and suffix");
        checkLiteral(string,0,prefix);
        checkLiteral(string,length-suffix.length(),suffix);
        String between=suffix+delimiter+prefix;
        int count=(length-value)/stride+1;
        byte[] bytes=new byte[count];
        int offset=prefix.length();
        for(int i=0;i<count-1;i++){
            bytes[i]=(byte)fromHexDigits(string,offset,offset+2);
            checkLiteral(string,offset+2,between);
            offset+=2+between.length();
        }
        bytes[count-1]=(byte)fromHexDigits(string,offset,offset+2);
        return bytes;
    }

    public char toLowHexDigit(int value){return digits().charAt(value&0xf);}
    public char toHighHexDigit(int value){return digits().charAt((value>>4)&0xf);}

    public String toHexDigits(byte value){return hex(value,2);}
    public String toHexDigits(char value){return hex(value,4);}
    public String toHexDigits(short value){return hex(value,4);}
    public String toHexDigits(int value){return hex(value,8);}
    public String toHexDigits(long value){return hex(value,16);}
    public String toHexDigits(long value,int digits){
        if(digits<0 || digits>16)throw new IllegalArgumentException("number of digits: "+digits);
        return hex(value,digits);
    }

    private String hex(long value,int digits){
        char[] out=new char[digits];
        String table=digits();
        for(int i=digits-1;i>=0;i--){out[i]=table.charAt((int)value&0xf);value>>>=4;}
        return new String(out);
    }

    public static boolean isHexDigit(int ch){
        if(ch<0 || ch>=256)return false;
        return ch>='0'&&ch<='9' || ch>='a'&&ch<='f' || ch>='A'&&ch<='F';
    }

    public static int fromHexDigit(int ch){
        if(ch>='0'&&ch<='9')return ch-'0';
        if(ch>='a'&&ch<='f')return ch-'a'+10;
        if(ch>='A'&&ch<='F')return ch-'A'+10;
        throw new NumberFormatException("not a hexadecimal digit: \""+(char)ch+"\" = "+ch);
    }

    public static int fromHexDigits(CharSequence string){return fromHexDigits(string,0,string.length());}
    public static int fromHexDigits(CharSequence string,int from,int to){
        Objects.requireNonNull(string,"string");
        checkRange(from,to,string.length());
        if(to-from>8)throw new IllegalArgumentException("string length greater than 8: "+(to-from));
        int value=0;
        for(int i=from;i<to;i++)value=(value<<4)+fromHexDigit(string.charAt(i));
        return value;
    }

    public static long fromHexDigitsToLong(CharSequence string){return fromHexDigitsToLong(string,0,string.length());}
    public static long fromHexDigitsToLong(CharSequence string,int from,int to){
        Objects.requireNonNull(string,"string");
        checkRange(from,to,string.length());
        if(to-from>16)throw new IllegalArgumentException("string length greater than 16: "+(to-from));
        long value=0;
        for(int i=from;i<to;i++)value=(value<<4)+fromHexDigit(string.charAt(i));
        return value;
    }

    public boolean equals(Object other){
        if(this==other)return true;
        if(!(other instanceof HexFormat))return false;
        HexFormat that=(HexFormat)other;
        return upperCase==that.upperCase && delimiter.equals(that.delimiter) && prefix.equals(that.prefix) && suffix.equals(that.suffix);
    }

    public int hashCode(){return 31*Objects.hash(delimiter,prefix,suffix)+Boolean.hashCode(upperCase);}

    public String toString(){
        return escapeNewlines("uppercase: "+upperCase+", delimiter: \""+delimiter+"\", prefix: \""+prefix+"\", suffix: \""+suffix+"\"");
    }

    private static void checkRange(int from,int to,int length){
        if(from<0 || from>to || to>length)
            throw new IndexOutOfBoundsException("Range ["+from+", "+to+") out of bounds for length "+length);
    }

    private static void checkLiteral(CharSequence string,int index,String literal){
        for(int i=0;i<literal.length();i++){
            if(string.charAt(index+i)!=literal.charAt(i)){
                throw new IllegalArgumentException(escapeNewlines("found: \""+string.subSequence(index,index+literal.length())
                        +"\", expected: \""+literal+"\", index: "+index+" ch: "+(int)string.charAt(index+i)));
            }
        }
    }

    private static String escapeNewlines(String string){
        return string.replace("\n","\\n").replace("\r","\\r");
    }
}
