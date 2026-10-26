package javaw.runtime;

/** Exact binary product/sum followed by one ties-to-even IEEE rounding. */
public final class FusedMath {
    private FusedMath() {}

    public static double fma(double a,double b,double c) {
        return Double.longBitsToDouble(fused(Double.doubleToRawLongBits(a),Double.doubleToRawLongBits(b),Double.doubleToRawLongBits(c),false));
    }
    public static float fma(float a,float b,float c) {
        return Float.intBitsToFloat((int)fused(Float.floatToRawIntBits(a)&0xffffffffL,Float.floatToRawIntBits(b)&0xffffffffL,Float.floatToRawIntBits(c)&0xffffffffL,true));
    }

    private static long fused(long a,long b,long c,boolean single) {
        int precision=single?24:53,maximum=single?127:1023;
        long signMask=single?0x80000000L:Long.MIN_VALUE;
        long implicit=1L<<(precision-1),infinity=(2L*maximum+1)<<(precision-1);
        long x=a&~signMask,y=b&~signMask,z=c&~signMask;
        long productSign=(a^b)&signMask,addendSign=c&signMask;
        if(x>infinity || y>infinity || z>infinity)return infinity|(implicit>>1);
        if(x==infinity || y==infinity) {
            if(x==0 || y==0 || z==infinity && productSign!=addendSign)return infinity|(implicit>>1);
            return productSign|infinity;
        }
        if(z==infinity)return c;
        if(x==0 || y==0)return z==0?(productSign&addendSign):c;

        int ax=(int)(x>>>(precision-1)),bx=(int)(y>>>(precision-1)),cx=(int)(z>>>(precision-1));
        long am=(x&(implicit-1))|(ax==0?0:implicit),bm=(y&(implicit-1))|(bx==0?0:implicit);
        long cm=(z&(implicit-1))|(cx==0?0:implicit);
        int productExponent=(ax==0?1:ax)+(bx==0?1:bx)-2*maximum-2*(precision-1);
        int addendExponent=(cx==0?1:cx)-maximum-(precision-1);
        // Alignment spans at most 3172 bits for double and 426 bits for float.
        int base=z==0?productExponent:(productExponent<addendExponent?productExponent:addendExponent);
        int productShift=productExponent-base,addendShift=z==0?0:addendExponent-base;
        int width=productShift+2*precision;
        if(addendShift+precision>width)width=addendShift+precision;
        // Base-65536 digits keep products and carries exact in Java long arithmetic.
        int[] product=new int[8];
        for(int i=0;i<4;i++) {
            long carry=0;
            for(int j=0;j<4;j++) {
                long value=product[i+j]+((am>>>(16*i))&65535)*((bm>>>(16*j))&65535)+carry;
                product[i+j]=(int)value&65535;carry=value>>>16;
            }
            product[i+4]=(int)carry;
        }
        int[] result=new int[(width+15)/16+1],addend=new int[result.length];
        for(int i=0;i<8;i++)putDigit(result,product[i],16*i+productShift);
        for(int i=0;i<4;i++)putDigit(addend,(int)(cm>>>(16*i))&65535,16*i+addendShift);
        long sign=productSign;
        if(productSign==addendSign) {
            int carry=0;
            for(int i=0;i<result.length;i++){int value=result[i]+addend[i]+carry;result[i]=value&65535;carry=value>>>16;}
        } else {
            int comparison=0;
            for(int i=result.length-1;i>=0;i--)if(result[i]!=addend[i]){comparison=result[i]>addend[i]?1:-1;break;}
            if(comparison==0)return 0;
            if(comparison<0){int[] swap=result;result=addend;addend=swap;sign=addendSign;}
            int borrow=0;
            for(int i=0;i<result.length;i++){int value=result[i]-addend[i]-borrow;result[i]=value&65535;borrow=value<0?1:0;}
        }
        return sign|rounded(result,base,precision,maximum,infinity);
    }

    private static void putDigit(int[] target,int digit,int shift) {
        if(digit==0)return;
        int index=shift/16,bits=shift%16;
        target[index]|=(digit<<bits)&65535;
        if(bits!=0)target[index+1]|=digit>>>(16-bits);
    }
    private static int bit(int[] value,int index) {
        return index<0 || index/16>=value.length?0:(value[index/16]>>>(index%16))&1;
    }
    private static boolean below(int[] value,int limit) {
        for(int i=0;i<value.length && 16*i<limit;i++) {
            int count=limit-16*i;
            if((value[i]&(count>=16?65535:(1<<count)-1))!=0)return true;
        }
        return false;
    }
    private static long rounded(int[] value,int base,int precision,int maximum,long infinity) {
        int high=value.length-1;
        while(high>=0 && value[high]==0)high--;
        if(high<0)return 0;
        int length=16*high+32-Integer.numberOfLeadingZeros(value[high]);
        int exponent=base+length-1;
        if(exponent>maximum)return infinity;
        // Round directly on the destination normal/subnormal grid, never through double.
        int unit=exponent-precision+1,minimumUnit=2-maximum-precision;
        if(unit<minimumUnit)unit=minimumUnit;
        int drop=unit-base;
        long significand=0;
        for(int i=length-1;i>=0 && i>=drop;i--)significand=(significand<<1)|bit(value,i);
        if(drop<0)significand<<=-drop;
        if(drop>0 && bit(value,drop-1)!=0 && (below(value,drop-1) || (significand&1)!=0))significand++;
        if(significand==(1L<<precision)){significand>>>=1;unit++;}
        if(significand<(1L<<(precision-1)))return significand;
        int encoded=unit+precision-1+maximum;
        if(encoded>=2*maximum+1)return infinity;
        return ((long)encoded<<(precision-1))|(significand&((1L<<(precision-1))-1));
    }
}
