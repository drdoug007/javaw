package java.math;

/** Decimal-magnitude arithmetic compiled into the isolated bytecode runtime. */
public class BigInteger extends Number implements Comparable<BigInteger>,java.io.Serializable {
    private int[] digits;
    private int sign;
    public static final BigInteger ZERO=new BigInteger("0");
    public static final BigInteger ONE=new BigInteger("1");
    public static final BigInteger TEN=new BigInteger("10");
    public BigInteger(String text) { this(text,10); }
    public BigInteger(String text,int radix) {
        if(text==null)throw new NullPointerException();
        if(radix<2 || radix>36 || text.length()==0)throw new NumberFormatException();
        int start=0,sign=1;char first=text.charAt(0);
        if(first=='-' || first=='+'){sign=first=='-'?-1:1;start++;}
        if(start==text.length())throw new NumberFormatException();
        // Decimal accumulation is independent of the source and bytecode heap layouts.
        int[] buffer=new int[Math.addExact(Math.multiplyExact(text.length()-start,2),1)];int size=1;
        for(int i=start;i<text.length();i++){
            int digit=Character.digit(text.charAt(i),radix);if(digit<0)throw new NumberFormatException();
            int carry=digit;for(int j=0;j<size;j++){int value=buffer[j]*radix+carry;buffer[j]=value%10;carry=value/10;}
            while(carry!=0){buffer[size++]=carry%10;carry/=10;}
        }
        init(java.util.Arrays.copyOf(buffer,size),sign);
    }
    public BigInteger(byte[] bytes) {
        if(bytes==null)throw new NullPointerException();if(bytes.length==0)throw new NumberFormatException();
        boolean negative=bytes[0]<0;byte[] magnitude=bytes.clone();
        if(negative){int carry=1;for(int i=magnitude.length-1;i>=0;i--){int value=(~magnitude[i]&255)+carry;magnitude[i]=(byte)value;carry=value>>>8;}}
        fromMagnitude(negative?-1:1,magnitude);
    }
    public BigInteger(int sign,byte[] magnitude) {
        if(magnitude==null)throw new NullPointerException();if(sign<-1 || sign>1)throw new NumberFormatException();
        fromMagnitude(sign,magnitude);
    }
    private void fromMagnitude(int sign,byte[] magnitude){
        BigInteger value=ZERO,base=valueOf(256);for(int i=0;i<magnitude.length;i++)value=value.multiply(base).add(valueOf(magnitude[i]&255));
        if(sign==0 && value.sign!=0)throw new NumberFormatException();init(value.digits,sign);
    }
    private BigInteger(int[] d,int s) { init(d,s); }
    private void init(int[] d,int s) {
        int n=d.length; while(n>1 && d[n-1]==0) n--;
        digits=new int[n]; for(int i=0;i<n;i++) digits[i]=d[i];
        sign=n==1 && d[0]==0 ? 0 : s;
    }
    public static BigInteger valueOf(long value) { return new BigInteger(""+value); }
    public int signum() { return sign; }
    int precision() { return digits.length; }
    public BigInteger abs() { return sign<0 ? new BigInteger(digits,1) : this; }
    public BigInteger negate() { return new BigInteger(digits,-sign); }
    private int compareMagnitude(BigInteger other) {
        if(digits.length!=other.digits.length) return digits.length<other.digits.length ? -1 : 1;
        for(int i=digits.length-1;i>=0;i--) if(digits[i]!=other.digits[i]) return digits[i]<other.digits[i] ? -1 : 1;
        return 0;
    }
    public int compareTo(BigInteger other) {
        if(other==null) throw new NullPointerException();
        if(sign!=other.sign) return sign<other.sign ? -1 : 1;
        return sign*compareMagnitude(other);
    }
    public boolean equals(Object other) { return other instanceof BigInteger && compareTo((BigInteger)other)==0; }
    public int hashCode() {
        byte[] bytes=abs().toByteArray();int start=bytes[0]==0?1:0,hash=0;
        int group=(bytes.length-start)%4;if(group==0)group=4;
        while(start<bytes.length){int word=0;for(int i=0;i<group;i++)word=(word<<8)|(bytes[start++]&255);hash=31*hash+word;group=4;}
        return sign*hash;
    }
    public BigInteger add(BigInteger other) {
        if(other==null) throw new NullPointerException();
        if(sign==0) return other; if(other.sign==0) return this;
        if(sign!=other.sign) return subtract(other.negate());
        int n=digits.length>other.digits.length ? digits.length : other.digits.length;
        int[] result=new int[n+1]; int carry=0;
        for(int i=0;i<n;i++) { int a=i<digits.length ? digits[i] : 0; int b=i<other.digits.length ? other.digits[i] : 0; int sum=a+b+carry; result[i]=sum%10; carry=sum/10; }
        result[n]=carry; return new BigInteger(result,sign);
    }
    public BigInteger subtract(BigInteger other) {
        if(other==null) throw new NullPointerException();
        if(other.sign==0) return this; if(sign==0) return other.negate();
        if(sign!=other.sign) return add(other.negate());
        int cmp=compareMagnitude(other); if(cmp==0) return ZERO;
        BigInteger a=cmp>0 ? this : other; BigInteger b=cmp>0 ? other : this;
        int[] result=new int[a.digits.length]; int borrow=0;
        for(int i=0;i<result.length;i++) { int v=a.digits[i]-borrow-(i<b.digits.length ? b.digits[i] : 0); borrow=v<0 ? 1 : 0; result[i]=v<0 ? v+10 : v; }
        return new BigInteger(result,cmp>0 ? sign : -sign);
    }
    public BigInteger multiply(BigInteger other) {
        if(other==null) throw new NullPointerException();
        int[] result=new int[digits.length+other.digits.length];
        for(int i=0;i<digits.length;i++) { int carry=0; for(int j=0;j<other.digits.length;j++) { int k=i+j; int v=result[k]+digits[i]*other.digits[j]+carry; result[k]=v%10; carry=v/10; } result[i+other.digits.length]=carry; }
        return new BigInteger(result,sign*other.sign);
    }
    public BigInteger[] divideAndRemainder(BigInteger other) {
        if(other==null)throw new NullPointerException();if(other.sign==0)throw new ArithmeticException();
        int[] remainder=new int[Math.addExact(other.digits.length,1)],quotient=new int[digits.length];int size=1;
        // Each decimal step needs at most nine in-place subtractions; temporary objects
        // are allocated only for the final quotient and remainder, including gcd loops.
        for(int i=digits.length-1;i>=0;i--){
            if(size!=1 || remainder[0]!=0){for(int j=size;j>0;j--)remainder[j]=remainder[j-1];size++;}remainder[0]=digits[i];
            int count=0;
            while(true){
                int comparison=Integer.compare(size,other.digits.length);
                if(comparison==0)for(int j=size-1;j>=0;j--)if(remainder[j]!=other.digits[j]){comparison=Integer.compare(remainder[j],other.digits[j]);break;}
                if(comparison<0)break;
                int borrow=0;for(int j=0;j<size;j++){int value=remainder[j]-borrow-(j<other.digits.length?other.digits[j]:0);borrow=value<0?1:0;remainder[j]=value<0?value+10:value;}
                while(size>1 && remainder[size-1]==0)size--;count++;
            }
            quotient[i]=count;
        }
        return new BigInteger[]{new BigInteger(quotient,sign*other.sign),new BigInteger(remainder,sign)};
    }
    public BigInteger divide(BigInteger other) { return divideAndRemainder(other)[0]; }
    public BigInteger remainder(BigInteger other) { return divideAndRemainder(other)[1]; }
    public BigInteger gcd(BigInteger other) {
        BigInteger a=abs(); BigInteger b=other.abs(); while(b.sign!=0) { BigInteger next=a.remainder(b); a=b; b=next; } return a;
    }
    public BigInteger pow(int exponent) {
        if(exponent<0) throw new ArithmeticException(); BigInteger base=this; BigInteger result=ONE;
        while(exponent>0) { if((exponent&1)!=0) result=result.multiply(base); exponent=exponent/2; if(exponent>0) base=base.multiply(base); } return result;
    }
    BigInteger timesTen(int exponent) {
        if(exponent<0 || exponent>2147483647-digits.length) throw new ArithmeticException();
        if(sign==0 || exponent==0) return this;
        int[] result=new int[digits.length+exponent]; for(int i=0;i<digits.length;i++) result[i+exponent]=digits[i]; return new BigInteger(result,sign);
    }
    boolean isOdd() { return (digits[0]&1)!=0; }
    public String toString() {
        StringBuilder out=new StringBuilder(); if(sign<0) out.append('-');
        for(int i=digits.length-1;i>=0;i--) { char digit=(char)('0'+digits[i]); out.append(digit); } return out.toString();
    }
    public long longValue() { long value=0L; for(int i=digits.length-1;i>=0;i--) value=value*10L+digits[i]; return sign<0 ? -value : value; }
    public int intValue() { return (int)longValue(); }
    public long longValueExact() { long result=longValue(); if(!valueOf(result).equals(this)) throw new ArithmeticException(); return result; }
    public int intValueExact() { int result=intValue(); if(!valueOf(result).equals(this)) throw new ArithmeticException(); return result; }

    public String toString(int radix){
        if(radix<2 || radix>36)radix=10;if(radix==10)return toString();
        int[] work=digits.clone();char[] out=new char[Math.addExact(Math.multiplyExact(digits.length,4),1)];int index=out.length,size=work.length;
        do{int remainder=0;for(int i=size-1;i>=0;i--){int value=remainder*10+work[i];work[i]=value/radix;remainder=value%radix;}out[--index]=Character.forDigit(remainder,radix);while(size>1 && work[size-1]==0)size--;}while(size!=1 || work[0]!=0);
        if(sign<0)out[--index]='-';return new String(out,index,out.length-index);
    }
    public byte[] toByteArray(){
        int[] work=digits.clone();byte[] out=new byte[digits.length/2+2];int index=out.length,size=work.length;
        do{int remainder=0;for(int i=size-1;i>=0;i--){int value=remainder*10+work[i];work[i]=value/256;remainder=value%256;}out[--index]=(byte)remainder;while(size>1 && work[size-1]==0)size--;}while(size!=1 || work[0]!=0);
        if(out[index]<0)out[--index]=0;
        if(sign<0){int carry=1;for(int i=out.length-1;i>=index;i--){int value=(~out[i]&255)+carry;out[i]=(byte)value;carry=value>>>8;}}
        while(index<out.length-1 && ((out[index]==0 && out[index+1]>=0)||(out[index]==-1 && out[index+1]<0)))index++;
        return java.util.Arrays.copyOfRange(out,index,out.length);
    }
    private int magnitudeBits(){byte[] bytes=abs().toByteArray();int start=bytes[0]==0?1:0;return start==bytes.length?0:(bytes.length-start-1)*8+32-Integer.numberOfLeadingZeros(bytes[start]&255);}
    public int bitLength(){return sign<0?negate().subtract(ONE).magnitudeBits():magnitudeBits();}
    public boolean testBit(int index){if(index<0)throw new ArithmeticException();byte[] bytes=toByteArray();int position=bytes.length-1-index/8;return position<0?sign<0:(bytes[position]&(1<<(index&7)))!=0;}
    public BigInteger shiftLeft(int distance){
        if(sign==0 || distance==0)return this;
        if(distance==Integer.MIN_VALUE)return sign<0?valueOf(-1):ZERO;
        if(distance<0)return shiftRight(-distance);
        if((long)magnitudeBits()+distance>Integer.MAX_VALUE)throw new ArithmeticException();
        return multiply(valueOf(2).pow(distance));
    }
    public BigInteger shiftRight(int distance){
        if(sign==0 || distance==0)return this;
        if(distance==Integer.MIN_VALUE)throw new ArithmeticException();
        if(distance<0)return shiftLeft(-distance);
        if(distance>=magnitudeBits())return sign<0?valueOf(-1):ZERO;
        BigInteger[] qr=abs().divideAndRemainder(valueOf(2).pow(distance));
        return sign<0?(qr[1].sign==0?qr[0]:qr[0].add(ONE)).negate():qr[0];
    }
    // Operands are aligned at their least significant bytes and sign-extended.
    private BigInteger bitwise(BigInteger other,int operation){
        if(other==null)throw new NullPointerException();
        byte[] a=toByteArray(),b=other.toByteArray();
        int length=Math.max(a.length,b.length);byte[] result=new byte[length];
        for(int i=0;i<length;i++){
            int ai=a.length-length+i,bi=b.length-length+i;
            int x=ai<0?(sign<0?-1:0):a[ai],y=bi<0?(other.sign<0?-1:0):b[bi];
            result[i]=(byte)(operation==0?x&y:operation==1?x|y:operation==2?x^y:x&~y);
        }
        return new BigInteger(result);
    }
    public BigInteger and(BigInteger other){return bitwise(other,0);}
    public BigInteger or(BigInteger other){return bitwise(other,1);}
    public BigInteger xor(BigInteger other){return bitwise(other,2);}
    public BigInteger andNot(BigInteger other){return bitwise(other,3);}
    public BigInteger not(){
        byte[] result=toByteArray();for(int i=0;i<result.length;i++)result[i]=(byte)~result[i];
        return new BigInteger(result);
    }
    public BigInteger min(BigInteger other){return compareTo(other)<=0?this:other;}
    public BigInteger max(BigInteger other){return compareTo(other)>=0?this:other;}
    public BigInteger mod(BigInteger modulus){if(modulus.sign<=0)throw new ArithmeticException();BigInteger result=remainder(modulus);return result.sign<0?result.add(modulus):result;}
    public byte byteValueExact(){int value=intValueExact();if((byte)value!=value)throw new ArithmeticException();return (byte)value;}
    public short shortValueExact(){int value=intValueExact();if((short)value!=value)throw new ArithmeticException();return (short)value;}
    private long floatingBits(int precision,int maximumExponent){
        if(sign==0)return 0;
        byte[] bytes=abs().toByteArray();int bits=magnitudeBits(),exponent=bits-1;long significand=0;
        int count=Math.min(bits,precision);
        for(int i=bits-1;i>=bits-count;i--)significand=(significand<<1)|((bytes[bytes.length-1-i/8]>>>(i&7))&1);
        if(bits>precision){int next=bits-precision-1;boolean guard=(bytes[bytes.length-1-next/8]&(1<<(next&7)))!=0,sticky=false;
            for(int i=next-1;i>=0;i--)if((bytes[bytes.length-1-i/8]&(1<<(i&7)))!=0){sticky=true;break;}
            if(guard && (sticky || (significand&1)!=0)){significand++;if(significand==(1L<<precision)){significand>>>=1;exponent++;}}}
        else significand<<=precision-bits;
        int fractionBits=precision-1;long signBit=sign<0?(maximumExponent==127?0x80000000L:Long.MIN_VALUE):0;
        if(exponent>maximumExponent)return signBit|((long)(2*maximumExponent+1)<<fractionBits);
        return signBit|((long)(exponent+maximumExponent)<<fractionBits)|(significand&((1L<<fractionBits)-1));
    }
    public double doubleValue(){return Double.longBitsToDouble(floatingBits(53,1023));}
    public float floatValue(){return Float.intBitsToFloat((int)floatingBits(24,127));}
}
