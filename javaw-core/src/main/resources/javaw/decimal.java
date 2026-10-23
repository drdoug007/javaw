class NumberFormatException extends IllegalArgumentException {
    public NumberFormatException() { super(); }
}
enum RoundingMode { UP, DOWN, CEILING, FLOOR, HALF_UP, HALF_DOWN, HALF_EVEN, UNNECESSARY }
class BigInteger {
    private int[] digits;
    private int sign;
    public static final BigInteger ZERO=new BigInteger("0");
    public static final BigInteger ONE=new BigInteger("1");
    public static final BigInteger TEN=new BigInteger("10");
    public static void __javaw_initialize() { ZERO=new BigInteger("0"); ONE=new BigInteger("1"); TEN=new BigInteger("10"); }
    public BigInteger(String text) {
        if(text==null) throw new NullPointerException();
        int start=0; int len=text.length(); int s=1;
        if(len==0) throw new NumberFormatException();
        if(text.charAt(0)=='-' || text.charAt(0)=='+') { s=text.charAt(0)=='-' ? -1 : 1; start++; }
        if(start==len) throw new NumberFormatException();
        int[] d=new int[len-start];
        for(int i=start;i<len;i++) { int c=text.charAt(i)-'0'; if(c<0 || c>9) throw new NumberFormatException(); d[len-1-i]=c; }
        init(d,s);
    }
    private BigInteger(int[] d,int s) { init(d,s); }
    private void init(int[] d,int s) {
        int n=d.length; while(n>1 && d[n-1]==0) n--;
        digits=new int[n]; for(int i=0;i<n;i++) digits[i]=d[i];
        sign=n==1 && d[0]==0 ? 0 : s;
    }
    public static BigInteger valueOf(long value) { return new BigInteger(""+value); }
    public int signum() { return sign; }
    public int precision() { return digits.length; }
    public BigInteger abs() { return sign<0 ? new BigInteger(digits,1) : this; }
    public BigInteger negate() { return new BigInteger(digits,-sign); }
    private int compareMagnitude(BigInteger other) {
        if(digits.length!=other.digits.length) return digits.length<other.digits.length ? -1 : 1;
        for(int i=digits.length-1;i>=0;i--) if(digits[i]!=other.digits[i]) return digits[i]<other.digits[i] ? -1 : 1;
        return 0;
    }
    public int compareTo(Object value) {
        BigInteger other=(BigInteger)value;
        if(other==null) throw new NullPointerException();
        if(sign!=other.sign) return sign<other.sign ? -1 : 1;
        return sign*compareMagnitude(other);
    }
    public boolean equals(Object other) { return other instanceof BigInteger && compareTo(other)==0; }
    public int hashCode() { int hash=0; for(int i=digits.length-1;i>=0;i--) hash=31*hash+digits[i]; return sign*hash; }
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
        if(other==null) throw new NullPointerException(); if(other.sign==0) throw new ArithmeticException();
        BigInteger divisor=other.abs(); BigInteger rem=ZERO; int[] quotient=new int[digits.length];
        for(int i=digits.length-1;i>=0;i--) {
            rem=rem.timesTen(1).add(valueOf(digits[i])); int q=0;
            while(rem.compareMagnitude(divisor)>=0) { rem=rem.subtract(divisor); q++; } quotient[i]=q;
        }
        BigInteger[] result=new BigInteger[2]; result[0]=new BigInteger(quotient,sign*other.sign); result[1]=sign<0 ? rem.negate() : rem; return result;
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
    public BigInteger timesTen(int exponent) {
        if(exponent<0 || exponent>2147483647-digits.length) throw new ArithmeticException();
        if(sign==0 || exponent==0) return this;
        int[] result=new int[digits.length+exponent]; for(int i=0;i<digits.length;i++) result[i+exponent]=digits[i]; return new BigInteger(result,sign);
    }
    public boolean isOdd() { return (digits[0]&1)!=0; }
    public String toString() {
        StringBuilder out=new StringBuilder(); if(sign<0) out.append('-');
        for(int i=digits.length-1;i>=0;i--) { char digit=(char)('0'+digits[i]); out.append(digit); } return out.toString();
    }
    public long longValue() { long value=0L; for(int i=digits.length-1;i>=0;i--) value=value*10L+digits[i]; return sign<0 ? -value : value; }
    public int intValue() { return (int)longValue(); }
    public long longValueExact() { long result=longValue(); if(!valueOf(result).equals(this)) throw new ArithmeticException(); return result; }
    public int intValueExact() { int result=intValue(); if(!valueOf(result).equals(this)) throw new ArithmeticException(); return result; }
}
class BigDecimal {
    private BigInteger coefficient;
    private int decimalScale;
    public static final BigDecimal ZERO=new BigDecimal(0);
    public static final BigDecimal ONE=new BigDecimal(1);
    public static final BigDecimal TEN=new BigDecimal(10);
    public static void __javaw_initialize() { ZERO=new BigDecimal(0); ONE=new BigDecimal(1); TEN=new BigDecimal(10); }
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
            else { if(c<'0' || c>'9') throw new NumberFormatException(); digits.append(c); count++; if(dot) fraction++; } i++;
        }
        if(count==0) throw new NumberFormatException();
        long exponent=0L; boolean expNegative=false;
        if(i<len) {
            i++; if(i<len && (text.charAt(i)=='-' || text.charAt(i)=='+')) { expNegative=text.charAt(i)=='-'; i++; }
            if(i==len) throw new NumberFormatException();
            while(i<len) { int c=text.charAt(i)-'0'; if(c<0 || c>9 || exponent>2147483648L) throw new NumberFormatException(); exponent=exponent*10L+c; i++; }
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
        int scale=decimalScale>other.decimalScale ? decimalScale : other.decimalScale;
        return new BigDecimal(coefficient.timesTen(scaleChecked((long)scale-decimalScale)).add(other.coefficient.timesTen(scaleChecked((long)scale-other.decimalScale))),scale);
    }
    public BigDecimal subtract(BigDecimal other) { return add(other.negate()); }
    public BigDecimal multiply(BigDecimal other) { return new BigDecimal(coefficient.multiply(other.coefficient),scaleChecked((long)decimalScale+other.decimalScale)); }
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
        long shift=(long)scale+other.decimalScale-decimalScale;
        BigInteger numerator=coefficient; BigInteger denominator=other.coefficient;
        if(shift>=0) numerator=numerator.timesTen(scaleChecked(shift)); else denominator=denominator.timesTen(scaleChecked(-shift));
        return new BigDecimal(rounded(numerator,denominator,mode),scale);
    }
    public BigDecimal divide(BigDecimal other,RoundingMode mode) { return divide(other,decimalScale,mode); }
    public BigDecimal divide(BigDecimal other) {
        if(other.signum()==0) throw new ArithmeticException();
        int preferred=scaleChecked((long)decimalScale-other.decimalScale);
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
        if(mode==null) throw new NullPointerException(); long shift=(long)scale-decimalScale;
        if(shift>=0) return new BigDecimal(coefficient.timesTen(scaleChecked(shift)),scale);
        return new BigDecimal(rounded(coefficient,BigInteger.ONE.timesTen(scaleChecked(-shift)),mode),scale);
    }
    public BigDecimal scaleByPowerOfTen(int n) { return new BigDecimal(coefficient,scaleChecked((long)decimalScale-n)); }
    public BigDecimal movePointLeft(int n) { int scale=scaleChecked((long)decimalScale+n); BigDecimal result=new BigDecimal(coefficient,scale); return scale<0 ? result.setScale(0) : result; }
    public BigDecimal movePointRight(int n) { int scale=scaleChecked((long)decimalScale-n); BigDecimal result=new BigDecimal(coefficient,scale); return scale<0 ? result.setScale(0) : result; }
    public BigDecimal stripTrailingZeros() {
        if(signum()==0) return ZERO; BigInteger value=coefficient; int scale=decimalScale;
        while(value.remainder(BigInteger.TEN).signum()==0) { value=value.divide(BigInteger.TEN); scale=scaleChecked((long)scale-1); }
        return new BigDecimal(value,scale);
    }
    public BigDecimal pow(int n) { if(n<0 || n>999999999) throw new ArithmeticException(); return new BigDecimal(coefficient.pow(n),scaleChecked((long)decimalScale*n)); }
    public int compareTo(Object value) {
        BigDecimal other=(BigDecimal)value; if(other==null) throw new NullPointerException();
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
    public BigInteger toBigInteger() { if(decimalScale<=0) return coefficient.timesTen(scaleChecked(-(long)decimalScale)); return coefficient.divide(BigInteger.ONE.timesTen(decimalScale)); }
    public BigInteger toBigIntegerExact() { return setScale(0).coefficient; }
    public int intValue() { return toBigInteger().intValue(); }
    public long longValue() { return toBigInteger().longValue(); }
    public int intValueExact() { return toBigIntegerExact().intValueExact(); }
    public long longValueExact() { return toBigIntegerExact().longValueExact(); }
    // Stable host ABI: decimal text crosses the bridge, arithmetic stays inside Wasm.
    public static BigDecimal __javaw_parse(String text) { return new BigDecimal(text); }
    public static String __javaw_format(BigDecimal value) { return value.toString(); }
}
