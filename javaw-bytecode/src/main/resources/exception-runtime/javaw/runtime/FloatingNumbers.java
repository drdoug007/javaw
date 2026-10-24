package javaw.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;

/** ASCII Java floating literal grammar and exact, direct IEEE rounding. */
public final class FloatingNumbers {
    private FloatingNumbers(){}
    public static double parseDouble(String text){return Double.longBitsToDouble(parse(text,false));}
    public static float parseFloat(String text){return Float.intBitsToFloat((int)parse(text,true));}
    private static int digit(char ch,boolean hex){if(ch>='0' && ch<='9')return ch-'0';if(hex && ch>='a' && ch<='f')return ch-'a'+10;if(hex && ch>='A' && ch<='F')return ch-'A'+10;return -1;}
    private static long parse(String text,boolean single){
        if(text==null)throw new NullPointerException();
        int start=0,end=text.length();while(start<end && text.charAt(start)<=32)start++;while(end>start && text.charAt(end-1)<=32)end--;
        boolean negative=false;if(start<end && (text.charAt(start)=='+' || text.charAt(start)=='-'))negative=text.charAt(start++)=='-';
        long sign=negative?(single?0x80000000L:Long.MIN_VALUE):0;
        long infinity=single?0x7f800000L:0x7ff0000000000000L;
        String body=text.substring(start,end);
        if(body.equals("NaN"))return single?0x7fc00000L:0x7ff8000000000000L;
        if(body.equals("Infinity"))return sign|infinity;
        if(start==end)throw new NumberFormatException();
        char tail=text.charAt(end-1);if(tail=='f' || tail=='F' || tail=='d' || tail=='D')end--;
        boolean hex=start+1<end && text.charAt(start)=='0' && (text.charAt(start+1)=='x' || text.charAt(start+1)=='X');if(hex)start+=2;
        StringBuilder digits=new StringBuilder();boolean dot=false,any=false;int fraction=0;
        int i=start;
        while(i<end){char ch=text.charAt(i);int value=digit(ch,hex);
            if(value>=0){any=true;if(dot)fraction++;if(value!=0 || digits.length()!=0)digits.append(ch);i++;}
            else if(ch=='.' && !dot){dot=true;i++;}else break;
        }
        if(!any)throw new NumberFormatException();
        long exponent=0;boolean exponentNegative=false;
        if(i<end && (hex?(text.charAt(i)=='p' || text.charAt(i)=='P'):(text.charAt(i)=='e' || text.charAt(i)=='E'))){
            i++;if(i<end && (text.charAt(i)=='+' || text.charAt(i)=='-'))exponentNegative=text.charAt(i++)=='-';
            int first=i;while(i<end){int value=digit(text.charAt(i++),false);if(value<0)throw new NumberFormatException();if(exponent<100000000000L)exponent=exponent*10+value;}
            if(i==first)throw new NumberFormatException();
        }else if(hex)throw new NumberFormatException();
        if(i!=end)throw new NumberFormatException();if(exponentNegative)exponent=-exponent;
        if(digits.length()==0)return sign;
        if(hex)return sign|hexBits(new BigInteger(digits.toString(),16),exponent-4L*fraction,single);
        long scale=(long)fraction-exponent,order=(long)digits.length()-scale;
        if(order>(single?40:310))return sign|infinity;if(order<(single?-47:-326))return sign;
        // An in-range order implies a representable scale for a memory-bounded input.
        if(scale<Integer.MIN_VALUE || scale>Integer.MAX_VALUE)throw new NumberFormatException();
        BigDecimal decimal=new BigDecimal(new BigInteger(digits.toString()),(int)scale);
        return sign|(single?(long)Float.floatToRawIntBits(decimal.floatValue())&0xffffffffL:Double.doubleToRawLongBits(decimal.doubleValue()));
    }
    private static long hexBits(BigInteger value,long shift,boolean single){
        int precision=single?24:53,maximum=single?127:1023,minimum=1-maximum;
        long exponent=(long)value.bitLength()-1+shift;
        long infinity=single?0x7f800000L:0x7ff0000000000000L;
        if(exponent>maximum)return infinity;if(exponent<minimum-precision)return 0;
        int effective=exponent<minimum?minimum:(int)exponent;
        long move=shift+precision-1-effective;
        BigInteger integral;
        if(move>=0)integral=value.shiftLeft((int)move);
        else{
            int drop=(int)-move;integral=value.shiftRight(drop);
            BigInteger remainder=value.subtract(integral.shiftLeft(drop)),half=BigInteger.ONE.shiftLeft(drop-1);
            int comparison=remainder.compareTo(half);if(comparison>0 || comparison==0 && integral.testBit(0))integral=integral.add(BigInteger.ONE);
        }
        long significand=integral.longValue();
        if(exponent<minimum)return significand;
        if(significand==(1L<<precision)){significand>>>=1;exponent++;}
        if(exponent>maximum)return infinity;
        return ((exponent+maximum)<<(precision-1))|(significand&((1L<<(precision-1))-1));
    }
}
