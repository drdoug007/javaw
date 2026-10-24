package javaw.runtime;

/** Exact interval digit generation for Java's shortest floating decimal representation.
 * Limbs use base 2^16; the largest binary64 scaling fits in 80 limbs. No host formatter.
 */
public final class Decimal {
    private Decimal() {}
    public static String doubleText(double value) {
        long bits=ExceptionSupport.doubleBits(value),fraction=bits&0xfffffffffffffL;
        int exponent=(int)((bits>>>52)&2047);boolean negative=bits<0;
        if(exponent==2047)return fraction!=0?"NaN":negative?"-Infinity":"Infinity";
        if(exponent==0 && fraction==0)return negative?"-0.0":"0.0";
        long significand=exponent==0?fraction:fraction|(1L<<52);
        return render(significand,(exponent==0?1:exponent)-1075,negative,fraction==0 && exponent>1);
    }
    public static String floatText(float value) {
        int bits=ExceptionSupport.floatBits(value),fraction=bits&0x7fffff;
        int exponent=(bits>>>23)&255;boolean negative=bits<0;
        if(exponent==255)return fraction!=0?"NaN":negative?"-Infinity":"Infinity";
        if(exponent==0 && fraction==0)return negative?"-0.0":"0.0";
        long significand=exponent==0?fraction:fraction|(1<<23);
        return render(significand,(exponent==0?1:exponent)-150,negative,fraction==0 && exponent>1);
    }
    private static String render(long significand,int exponent,boolean negative,boolean asymmetric) {
        int[] numerator=new int[80],denominator=new int[80],low=new int[80],high=new int[80],scratch=new int[80];
        set(numerator,significand*4);set(denominator,1);set(low,asymmetric?1:2);set(high,2);
        if(exponent>=2){shift(numerator,exponent-2);shift(low,exponent-2);shift(high,exponent-2);}
        else shift(denominator,2-exponent);
        int decimalExponent=0;
        if(compare(numerator,denominator)<0) {
            do {multiply(numerator,10);multiply(low,10);multiply(high,10);decimalExponent--;}
            while(compare(numerator,denominator)<0);
        } else {
            copy(denominator,scratch);multiply(scratch,10);
            while(compare(numerator,scratch)>=0) {
                copy(scratch,denominator);multiply(scratch,10);decimalExponent++;
            }
        }
        boolean even=(significand&1)==0;
        char[] digits=new char[24];int count=0;
        int minimum=decimalExponent < -3 || decimalExponent >= 7 ? 2 : 1;
        for(;;) {
            int digit=0;while(compare(numerator,denominator)>=0){subtract(numerator,denominator);digit++;}
            digits[count++]=(char)('0'+digit);
            int lower=compare(numerator,low);boolean down=lower<0 || even && lower==0;
            copy(denominator,scratch);subtract(scratch,numerator);
            int upper=compare(high,scratch);boolean up=upper>0 || even && upper==0;
            if(count>=minimum && (down || up)) {
                boolean round=up && !down;
                if(up && down) {
                    copy(numerator,scratch);multiply(scratch,2);int middle=compare(scratch,denominator);
                    round=middle>0 || middle==0 && (digit&1)!=0;
                }
                if(round) {
                    int index=count-1;
                    while(index>=0 && digits[index]=='9'){digits[index]='0';index--;}
                    if(index<0){digits[0]='1';decimalExponent++;}
                    else digits[index]++;
                }
                break;
            }
            multiply(numerator,10);multiply(low,10);multiply(high,10);
        }
        char[] result=new char[32];int at=0;if(negative)result[at++]='-';
        if(decimalExponent>=-3 && decimalExponent<7) {
            if(decimalExponent<0) {
                result[at++]='0';result[at++]='.';
                for(int i=-1;i>decimalExponent;i--)result[at++]='0';
                for(int i=0;i<count;i++)result[at++]=digits[i];
            } else {
                for(int i=0;i<=decimalExponent;i++)result[at++]=i<count?digits[i]:'0';
                result[at++]='.';
                if(count<=decimalExponent+1)result[at++]='0';
                else for(int i=decimalExponent+1;i<count;i++)result[at++]=digits[i];
            }
        } else {
            result[at++]=digits[0];result[at++]='.';
            if(count==1)result[at++]='0';else for(int i=1;i<count;i++)result[at++]=digits[i];
            result[at++]='E';if(decimalExponent<0){result[at++]='-';decimalExponent=-decimalExponent;}
            if(decimalExponent>=100)result[at++]=(char)('0'+decimalExponent/100);
            if(decimalExponent>=10)result[at++]=(char)('0'+decimalExponent/10%10);
            result[at++]=(char)('0'+decimalExponent%10);
        }
        char[] exact=new char[at];System.arraycopy(result,0,exact,0,at);return new String(exact);
    }
    private static void set(int[] number,long value) {
        int size=0;while(value!=0){number[++size]=(int)(value&65535);value>>>=16;}number[0]=size;
    }
    private static void copy(int[] from,int[] to) {
        int old=to[0];for(int i=0;i<=from[0];i++)to[i]=from[i];
        for(int i=from[0]+1;i<=old;i++)to[i]=0;
    }
    private static int compare(int[] left,int[] right) {
        if(left[0]!=right[0])return left[0]<right[0]?-1:1;
        for(int i=left[0];i>0;i--)if(left[i]!=right[i])return left[i]<right[i]?-1:1;
        return 0;
    }
    private static void multiply(int[] number,int factor) {
        int carry=0;for(int i=1;i<=number[0];i++){int value=number[i]*factor+carry;number[i]=value&65535;carry=value>>>16;}
        if(carry!=0)number[++number[0]]=carry;
    }
    private static void shift(int[] number,int bits) {
        if(number[0]==0)return;
        int words=bits/16,rest=bits%16,size=number[0];
        for(int i=size;i>0;i--)number[i+words]=number[i];
        for(int i=1;i<=words;i++)number[i]=0;number[0]=size+words;
        if(rest!=0)multiply(number,1<<rest);
    }
    private static void subtract(int[] number,int[] other) {
        int borrow=0;for(int i=1;i<=number[0];i++) {
            int value=number[i]-(i<=other[0]?other[i]:0)-borrow;
            if(value<0){value+=65536;borrow=1;}else borrow=0;number[i]=value;
        }
        while(number[0]>0 && number[number[0]]==0)number[0]--;
    }
}
