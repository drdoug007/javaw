package java.math;

/** Exact decimal arithmetic for the isolated bundled bytecode runtime. */
public class BigDecimal extends Number implements Comparable<BigDecimal>,java.io.Serializable {
    private BigInteger coefficient;
    private int decimalScale;
    public static final BigDecimal ZERO=new BigDecimal(0);
    public static final BigDecimal ONE=new BigDecimal(1);
    public static final BigDecimal TEN=new BigDecimal(10);
    public BigDecimal(BigInteger value,int scale) { if(value==null) throw new NullPointerException(); coefficient=value; decimalScale=scale; }
    public BigDecimal(BigInteger value) { this(value,0); }
    public BigDecimal(int value) { this(BigInteger.valueOf(value),0); }
    public BigDecimal(long value) { this(BigInteger.valueOf(value),0); }
    public BigDecimal(double value) {
        if(value-value!=0.0) throw new NumberFormatException();
        boolean negative=value<0.0; double x=negative ? -value : value;
        if(x==0.0) { coefficient=BigInteger.ZERO; decimalScale=0; return; }
        int exponent=0;
        while(x>=2.0) { x=x/2.0; exponent++; }
        while(x<1.0) { x=x*2.0; exponent--; }
        long mantissa=0L;
        for(int i=0;i<53;i++) { int bit=x>=1.0 ? 1 : 0; mantissa=mantissa*2L+bit; x=(x-bit)*2.0; }
        int shift=exponent-52;
        while(shift<0 && mantissa%2L==0L) { mantissa=mantissa/2L; shift++; }
        BigInteger raw=BigInteger.valueOf(mantissa);
        if(shift>=0) { coefficient=raw.multiply(BigInteger.valueOf(2).pow(shift)); decimalScale=0; }
        else { coefficient=raw.multiply(BigInteger.valueOf(5).pow(-shift)); decimalScale=-shift; }
        if(negative) coefficient=coefficient.negate();
    }
    public BigDecimal(String text) {
        if(text==null) throw new NullPointerException();
        int len=text.length(); int i=0; boolean negative=false;
        if(len==0) throw new NumberFormatException();
        if(text.charAt(0)=='-' || text.charAt(0)=='+') { negative=text.charAt(0)=='-'; i++; }
        StringBuilder digits=new StringBuilder(); boolean dot=false; int fraction=0; int count=0;
        while(i<len && text.charAt(i)!='e' && text.charAt(i)!='E') {
            char c=text.charAt(i); if(c=='.') { if(dot) throw new NumberFormatException(); dot=true; }
            else { int digit=Character.digit(c,10);if(digit<0) throw new NumberFormatException(); digits.append((char)('0'+digit)); count++; if(dot) fraction++; } i++;
        }
        if(count==0) throw new NumberFormatException();
        long exponent=0L; boolean expNegative=false;
        if(i<len) {
            i++; if(i<len && (text.charAt(i)=='-' || text.charAt(i)=='+')) { expNegative=text.charAt(i)=='-'; i++; }
            if(i==len) throw new NumberFormatException();
            while(i<len) { int c=Character.digit(text.charAt(i),10); if(c<0) throw new NumberFormatException(); exponent=exponent*10L+c;if(exponent>4294967295L)throw new NumberFormatException(); i++; }
        }
        long scale=(long)fraction-(expNegative ? -exponent : exponent);
        if(scale<-2147483648L || scale>2147483647L) throw new NumberFormatException();
        coefficient=new BigInteger((negative ? "-" : "")+digits.toString()); decimalScale=(int)scale;
    }
    public static BigDecimal valueOf(long value) { return new BigDecimal(value); }
    public static BigDecimal valueOf(long value,int scale) { return new BigDecimal(BigInteger.valueOf(value),scale); }
    public int scale() { return decimalScale; }
    public int precision() { return coefficient.precision(); }
    public int signum() { return coefficient.signum(); }
    public BigInteger unscaledValue() { return coefficient; }
    private static int scaleChecked(long value) { if(value<-2147483648L || value>2147483647L) throw new ArithmeticException(); return (int)value; }
    public BigDecimal negate() { return new BigDecimal(coefficient.negate(),decimalScale); }
    public BigDecimal abs() { return signum()<0 ? negate() : this; }
    public BigDecimal add(BigDecimal other) {
        if(other==null) throw new NullPointerException();
        int scale=decimalScale>other.decimalScale ? decimalScale : other.decimalScale;
        return new BigDecimal(coefficient.timesTen(scaleChecked((long)scale-decimalScale)).add(other.coefficient.timesTen(scaleChecked((long)scale-other.decimalScale))),scale);
    }
    public BigDecimal subtract(BigDecimal other) { return add(other.negate()); }
    public BigDecimal multiply(BigDecimal other) { if(other==null) throw new NullPointerException(); return new BigDecimal(coefficient.multiply(other.coefficient),checkedScale((long)decimalScale+other.decimalScale)); }
    private static BigInteger rounded(BigInteger numerator,BigInteger denominator,RoundingMode mode) {
        if(mode==null) throw new NullPointerException();
        BigInteger[] qr=numerator.divideAndRemainder(denominator); if(qr[1].signum()==0) return qr[0];
        int sign=numerator.signum()*denominator.signum(); boolean increment=false;
        if(mode==RoundingMode.UNNECESSARY) throw new ArithmeticException();
        if(mode==RoundingMode.UP) increment=true;
        else if(mode==RoundingMode.CEILING) increment=sign>0;
        else if(mode==RoundingMode.FLOOR) increment=sign<0;
        else if(mode!=RoundingMode.DOWN) {
            int cmp=qr[1].abs().multiply(BigInteger.valueOf(2)).compareTo(denominator.abs());
            increment=cmp>0 || cmp==0 && (mode==RoundingMode.HALF_UP || mode==RoundingMode.HALF_EVEN && qr[0].isOdd());
        }
        return increment ? qr[0].add(BigInteger.valueOf(sign)) : qr[0];
    }
    public BigDecimal divide(BigDecimal other,int scale,RoundingMode mode) {
        if(other==null) throw new NullPointerException();
        long shift=(long)scale+other.decimalScale-decimalScale;
        BigInteger numerator=coefficient; BigInteger denominator=other.coefficient;
        if(shift>=0) numerator=numerator.timesTen(scaleChecked(shift)); else denominator=denominator.timesTen(scaleChecked(-shift));
        return new BigDecimal(rounded(numerator,denominator,mode),scale);
    }
    public BigDecimal divide(BigDecimal other,RoundingMode mode) { return divide(other,decimalScale,mode); }
    public BigDecimal divide(BigDecimal other) {
        if(other==null) throw new NullPointerException();
        if(other.signum()==0) throw new ArithmeticException();
        int preferred=clampedScale((long)decimalScale-other.decimalScale);
        if(signum()==0)return new BigDecimal(BigInteger.ZERO,preferred);
        BigInteger gcd=coefficient.gcd(other.coefficient); BigInteger denominator=other.coefficient.abs().divide(gcd);
        BigInteger two=BigInteger.valueOf(2); BigInteger five=BigInteger.valueOf(5); int twos=0; int fives=0;
        while(denominator.remainder(two).signum()==0) { denominator=denominator.divide(two); twos++; }
        while(denominator.remainder(five).signum()==0) { denominator=denominator.divide(five); fives++; }
        if(!denominator.equals(BigInteger.ONE)) throw new ArithmeticException();
        int extra=twos>fives ? twos : fives;
        return divide(other,scaleChecked((long)preferred+extra),RoundingMode.UNNECESSARY);
    }
    public BigDecimal setScale(int scale) { return setScale(scale,RoundingMode.UNNECESSARY); }
    public BigDecimal setScale(int scale,RoundingMode mode) {
        if(mode==null) throw new NullPointerException();if(signum()==0)return new BigDecimal(BigInteger.ZERO,scale); long shift=(long)scale-decimalScale;
        if(shift>=0) return new BigDecimal(coefficient.timesTen(scaleChecked(shift)),scale);
        return new BigDecimal(rounded(coefficient,BigInteger.ONE.timesTen(scaleChecked(-shift)),mode),scale);
    }
    public BigDecimal scaleByPowerOfTen(int n) { return new BigDecimal(coefficient,checkedScale((long)decimalScale-n)); }
    public BigDecimal movePointLeft(int n) { int scale=checkedScale((long)decimalScale+n); BigDecimal result=new BigDecimal(coefficient,scale); return scale<0 ? result.setScale(0) : result; }
    public BigDecimal movePointRight(int n) { int scale=checkedScale((long)decimalScale-n); BigDecimal result=new BigDecimal(coefficient,scale); return scale<0 ? result.setScale(0) : result; }
    public BigDecimal stripTrailingZeros() {
        if(signum()==0) return ZERO; BigInteger value=coefficient; int scale=decimalScale;
        while(value.remainder(BigInteger.TEN).signum()==0) { value=value.divide(BigInteger.TEN); scale=scaleChecked((long)scale-1); }
        return new BigDecimal(value,scale);
    }
    public BigDecimal pow(int n) { if(n<0 || n>999999999) throw new ArithmeticException(); int scale=checkedScale((long)decimalScale*n); return new BigDecimal(coefficient.pow(n),scale); }
    public int compareTo(BigDecimal other) { if(other==null) throw new NullPointerException();
        if(signum()!=other.signum()) return signum()<other.signum() ? -1 : 1;
        if(signum()==0) return 0;
        long a=(long)precision()-decimalScale; long b=(long)other.precision()-other.decimalScale;
        if(a!=b) return (a<b ? -1 : 1)*signum();
        int scale=decimalScale>other.decimalScale ? decimalScale : other.decimalScale;
        return coefficient.timesTen(scaleChecked((long)scale-decimalScale)).compareTo(other.coefficient.timesTen(scaleChecked((long)scale-other.decimalScale)));
    }
    public boolean equals(Object other) { if(!(other instanceof BigDecimal)) return false; BigDecimal value=(BigDecimal)other; return decimalScale==value.decimalScale && coefficient.equals(value.coefficient); }
    public int hashCode() { return 31*coefficient.hashCode()+decimalScale; }
    public BigDecimal min(BigDecimal other) { return compareTo(other)<=0 ? this : other; }
    public BigDecimal max(BigDecimal other) { return compareTo(other)>=0 ? this : other; }
    public String toPlainString() {
        if(decimalScale==Integer.MIN_VALUE && signum()!=0)throw new ArithmeticException();
        String digits=coefficient.abs().toString(); StringBuilder out=new StringBuilder();
        if(signum()<0) out.append('-');
        if(decimalScale<=0) { out.append(digits); if(signum()!=0) for(long i=0;i<-(long)decimalScale;i++) out.append('0'); }
        else if(decimalScale>=digits.length()) { out.append("0."); for(int i=digits.length();i<decimalScale;i++) out.append('0'); out.append(digits); }
        else { out.append(digits.substring(0,digits.length()-decimalScale)); out.append('.'); out.append(digits.substring(digits.length()-decimalScale)); }
        return out.toString();
    }
    public String toString() {
        long exponent=(long)precision()-decimalScale-1;
        if(decimalScale>=0 && exponent>=-6) return toPlainString();
        String digits=coefficient.abs().toString(); StringBuilder out=new StringBuilder(); if(signum()<0) out.append('-');
        char first=digits.charAt(0); out.append(first); if(digits.length()>1) { out.append('.'); out.append(digits.substring(1)); }
        out.append('E'); if(exponent>=0) out.append('+'); out.append(""+exponent); return out.toString();
    }
    public String toEngineeringString() {
        long adjusted=(long)precision()-decimalScale-1;
        if(decimalScale>=0 && adjusted>=-6)return toPlainString();
        long exponent=adjusted-Math.floorMod(adjusted,3);
        int integerDigits=(int)(adjusted-exponent)+1;
        String digits=coefficient.abs().toString();
        StringBuilder out=new StringBuilder();
        if(signum()<0)out.append('-');
        if(signum()==0){
            // Zero keeps enough fractional zeros to preserve its original scale.
            if(integerDigits==1)out.append('0');
            else {out.append("0.");for(int i=integerDigits;i<4;i++)out.append('0');exponent+=3;}
        } else {
            for(int i=0;i<integerDigits;i++)out.append(i<digits.length()?digits.charAt(i):'0');
            if(digits.length()>integerDigits){out.append('.');out.append(digits.substring(integerDigits));}
        }
        if(exponent!=0){out.append('E');if(exponent>0)out.append('+');out.append(exponent);}
        return out.toString();
    }
    public BigInteger toBigInteger() { if(signum()==0 || decimalScale>=precision())return BigInteger.ZERO; if(decimalScale<=0) return coefficient.timesTen(scaleChecked(-(long)decimalScale)); return coefficient.divide(BigInteger.ONE.timesTen(decimalScale)); }
    public BigInteger toBigIntegerExact() { if(signum()==0)return BigInteger.ZERO;if(decimalScale>=precision())throw new ArithmeticException();return setScale(0).coefficient; }
    public int intValue() { return decimalScale<=-32?0:toBigInteger().intValue(); }
    public long longValue() { return decimalScale<=-64?0:toBigInteger().longValue(); }
    public int intValueExact() { if(signum()!=0 && (long)precision()-decimalScale>10)throw new ArithmeticException();return toBigIntegerExact().intValueExact(); }
    public long longValueExact() { if(signum()!=0 && (long)precision()-decimalScale>19)throw new ArithmeticException();return toBigIntegerExact().longValueExact(); }

    public BigDecimal(char[] value){this(new String(value));}
    public BigDecimal(char[] value,int offset,int length){this(slice(value,offset,length));}
    private static String slice(char[] value,int offset,int length){if(value==null)throw new NullPointerException();if(offset<0 || length<0 || offset>value.length-length)throw new NumberFormatException();return new String(value,offset,length);}
    public static BigDecimal valueOf(double value){return new BigDecimal(Double.toString(value));}
    public BigDecimal plus(){return this;}
    private BigDecimal roundedContext(MathContext context){
        int target=context.getPrecision();
        if(target==0)return this;
        BigInteger value=coefficient;int scale=decimalScale;
        int drop=value.precision()-target;
        // A carry (999 -> 1000) can require one more digit to be removed.
        while(drop>0){
            scale=scaleChecked((long)scale-drop);
            value=rounded(value,BigInteger.ONE.timesTen(drop),context.getRoundingMode());
            drop=value.precision()-target;
        }
        return new BigDecimal(value,scale);
    }
    public BigDecimal round(MathContext context){return plus(context);}
    public BigDecimal plus(MathContext context){return roundedContext(context);}
    public BigDecimal negate(MathContext context){return negate().plus(context);}
    public BigDecimal abs(MathContext context){return signum()<0?negate(context):plus(context);}
    private void applyContext(MathContext context){BigDecimal result=roundedContext(context);coefficient=result.coefficient;decimalScale=result.decimalScale;}
    public BigDecimal(String value,MathContext context){this(value);applyContext(context);}
    public BigDecimal(char[] value,MathContext context){this(value);applyContext(context);}
    public BigDecimal(char[] value,int offset,int length,MathContext context){this(value,offset,length);applyContext(context);}
    public BigDecimal(BigInteger value,MathContext context){this(value);applyContext(context);}
    public BigDecimal(BigInteger value,int scale,MathContext context){this(value,scale);applyContext(context);}
    public BigDecimal(int value,MathContext context){this(value);applyContext(context);}
    public BigDecimal(long value,MathContext context){this(value);applyContext(context);}
    public BigDecimal(double value,MathContext context){this(value);applyContext(context);}

    public BigDecimal add(BigDecimal other,MathContext context){
        if(context==null)throw new NullPointerException();
        int precision=context.getPrecision();
        if(precision==0)return add(other);
        if(other==null)throw new NullPointerException();
        if(signum()==0 || other.signum()==0){
            int preferred=Math.max(decimalScale,other.decimalScale);
            if(signum()==0 && other.signum()==0)return new BigDecimal(BigInteger.ZERO,preferred);
            BigDecimal result=(signum()==0?other:this).roundedContext(context);
            long padded=(long)result.decimalScale+Math.max(0,precision-result.precision());
            return result.setScale((int)Math.min(preferred,padded));
        }
        BigDecimal large=this,small=other;
        long order=(long)precision()-decimalScale,otherOrder=(long)other.precision()-other.decimalScale;
        if(order<otherOrder){large=other;small=this;long swap=order;order=otherOrder;otherOrder=swap;}
        // Keep two guard digits and a signed sticky digit rather than expanding
        // an arbitrarily distant addend. Opposite signs still round correctly.
        long padding=Math.max(0L,(long)precision+2-large.precision());
        long stickyScale=(long)large.decimalScale+padding;
        if(otherOrder < -stickyScale && stickyScale<=Integer.MAX_VALUE){
            small=new BigDecimal(BigInteger.valueOf(small.signum()),(int)stickyScale);
            return large.add(small).roundedContext(context);
        }
        return add(other).roundedContext(context);
    }

    public BigDecimal subtract(BigDecimal other,MathContext context){
        if(context==null)throw new NullPointerException();
        if(other==null)throw new NullPointerException();
        return add(other.negate(),context);
    }

    public BigDecimal multiply(BigDecimal other,MathContext context){
        if(context==null)throw new NullPointerException();
        return multiply(other).roundedContext(context);
    }

    public BigDecimal ulp(){return new BigDecimal(BigInteger.ONE,decimalScale);}
    private static int clampedScale(long scale){return scale<Integer.MIN_VALUE?Integer.MIN_VALUE:scale>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)scale;}
    private int checkedScale(long scale){return signum()==0?clampedScale(scale):scaleChecked(scale);}
    public byte byteValueExact(){int value=intValueExact();if((byte)value!=value)throw new ArithmeticException();return (byte)value;}
    public short shortValueExact(){int value=intValueExact();if((short)value!=value)throw new ArithmeticException();return (short)value;}
    public BigDecimal divideToIntegralValue(BigDecimal other){
        if(other==null)throw new NullPointerException();if(other.signum()==0)throw new ArithmeticException();
        int preferred=clampedScale((long)decimalScale-other.decimalScale);
        if(signum()==0 || abs().compareTo(other.abs())<0)return new BigDecimal(BigInteger.ZERO,preferred);
        long shift=(long)other.decimalScale-decimalScale;BigInteger numerator=coefficient,denominator=other.coefficient;
        if(shift>=0)numerator=numerator.timesTen(scaleChecked(shift));else denominator=denominator.timesTen(scaleChecked(-shift));
        BigDecimal result=new BigDecimal(numerator.divide(denominator));
        if(preferred>=0)return result.setScale(preferred);
        result=result.stripTrailingZeros();return result.setScale(Math.max(result.scale(),preferred));
    }
    public BigDecimal[] divideAndRemainder(BigDecimal other){BigDecimal quotient=divideToIntegralValue(other);return new BigDecimal[]{quotient,subtract(quotient.multiply(other))};}
    public BigDecimal remainder(BigDecimal other){return divideAndRemainder(other)[1];}
    private long floatingBits(int precision,int maximumExponent){
        if(signum()==0)return 0;
        boolean negative=signum()<0;int fractionBits=precision-1;
        long sign=negative?(maximumExponent==127?0x80000000L:Long.MIN_VALUE):0;
        long order=(long)precision()-decimalScale;int high=maximumExponent==127?40:310,low=maximumExponent==127?-47:-326;
        if(order>high)return sign|((long)(2*maximumExponent+1)<<fractionBits);if(order<low)return sign;
        BigInteger numerator=coefficient.abs(),denominator=BigInteger.ONE;
        if(decimalScale>=0)denominator=denominator.timesTen(decimalScale);else numerator=numerator.timesTen(scaleChecked(-(long)decimalScale));
        int exponent=numerator.bitLength()-denominator.bitLength();
        int comparison=exponent>=0?numerator.compareTo(denominator.shiftLeft(exponent)):numerator.shiftLeft(-exponent).compareTo(denominator);
        if(comparison<0)exponent--;
        if(exponent>maximumExponent)return sign|((long)(2*maximumExponent+1)<<fractionBits);
        int minimumExponent=1-maximumExponent,shift=fractionBits-Math.max(exponent,minimumExponent);
        if(shift>=0)numerator=numerator.shiftLeft(shift);else denominator=denominator.shiftLeft(-shift);
        BigInteger[] qr=numerator.divideAndRemainder(denominator);long value=qr[0].longValue();
        int halfway=qr[1].add(qr[1]).compareTo(denominator);if(halfway>0 || halfway==0 && (value&1)!=0)value++;
        if(exponent<minimumExponent)return sign|value;
        if(value==(1L<<precision)){value>>>=1;exponent++;}
        if(exponent>maximumExponent)return sign|((long)(2*maximumExponent+1)<<fractionBits);
        return sign|((long)(exponent+maximumExponent)<<fractionBits)|(value&((1L<<fractionBits)-1));
    }
    public double doubleValue(){return Double.longBitsToDouble(floatingBits(53,1023));}
    public float floatValue(){return Float.intBitsToFloat((int)floatingBits(24,127));}
    public static final int ROUND_UP=0,ROUND_DOWN=1,ROUND_CEILING=2,ROUND_FLOOR=3,ROUND_HALF_UP=4,ROUND_HALF_DOWN=5,ROUND_HALF_EVEN=6,ROUND_UNNECESSARY=7;
    public BigDecimal setScale(int scale,int mode){return setScale(scale,RoundingMode.valueOf(mode));}
    public BigDecimal divide(BigDecimal value,int scale,int mode){return divide(value,scale,RoundingMode.valueOf(mode));}
    public BigDecimal divide(BigDecimal value,int mode){return divide(value,RoundingMode.valueOf(mode));}
}
