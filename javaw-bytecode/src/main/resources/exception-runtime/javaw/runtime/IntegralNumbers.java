package javaw.runtime;

/** Negative accumulation keeps signed minimum values representable. */
public final class IntegralNumbers {
    private IntegralNumbers(){}
    public static long parse(CharSequence text,int start,int end,int radix,long minimum,long maximum){
        if(radix<2 || radix>36 || start==end)throw new NumberFormatException();
        boolean negative=false;char first=text.charAt(start);
        if(first=='-' || first=='+'){negative=first=='-';if(++start==end)throw new NumberFormatException();}
        long limit=negative?minimum:-maximum,multiplyLimit=limit/radix,result=0;
        for(int i=start;i<end;i++){
            int digit=Character.digit(text.charAt(i),radix);
            if(digit<0 || result<multiplyLimit)throw new NumberFormatException();
            result*=radix;if(result<limit+digit)throw new NumberFormatException();result-=digit;
        }
        return negative?result:-result;
    }
    public static String format(long value,int radix){
        if(radix<2 || radix>36)radix=10;
        char[] chars=new char[65];int index=65;boolean negative=value<0;if(!negative)value=-value;
        do{chars[--index]=digit((int)(-(value%radix)));value/=radix;}while(value!=0);
        if(negative)chars[--index]='-';return new String(chars,index,65-index);
    }
    public static String unsigned(long value,int shift){
        char[] chars=new char[64];int index=64,mask=(1<<shift)-1;
        do{chars[--index]=digit((int)value&mask);value>>>=shift;}while(value!=0);return new String(chars,index,64-index);
    }
    private static char digit(int value){return (char)(value<10?'0'+value:'a'+value-10);}
    public static long decode(String text,long minimum,long maximum){
        if(text==null)throw new NullPointerException();int end=text.length(),start=0;boolean negative=false;
        if(end==0)throw new NumberFormatException();char first=text.charAt(0);if(first=='-' || first=='+'){negative=first=='-';start++;}
        int radix=10;if(text.startsWith("0x",start) || text.startsWith("0X",start)){start+=2;radix=16;}else if(text.startsWith("#",start)){start++;radix=16;}else if(text.startsWith("0",start) && start+1<end){start++;radix=8;}
        if(start==end || text.charAt(start)=='+' || text.charAt(start)=='-')throw new NumberFormatException();
        String digits=text.substring(start);if(negative)digits="-".concat(digits);return parse(digits,0,digits.length(),radix,minimum,maximum);
    }
}
