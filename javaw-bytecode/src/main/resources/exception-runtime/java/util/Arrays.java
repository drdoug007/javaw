package java.util;

/** The primitive lexicographic comparators required by the pinned H2 Bits class. */
public final class Arrays {
    private Arrays(){}
    public static int compare(char[] left,char[] right){
        if(left==right)return 0;if(left==null)return -1;if(right==null)return 1;
        int length=left.length<right.length?left.length:right.length;
        for(int i=0;i<length;i++)if(left[i]!=right[i])return left[i]-right[i];
        return left.length-right.length;
    }
    public static int compare(byte[] left,byte[] right){
        if(left==right)return 0;if(left==null)return -1;if(right==null)return 1;
        int length=left.length<right.length?left.length:right.length;
        for(int i=0;i<length;i++)if(left[i]!=right[i])return left[i]-right[i];
        return left.length-right.length;
    }
    public static int compareUnsigned(byte[] left,byte[] right){
        if(left==right)return 0;if(left==null)return -1;if(right==null)return 1;
        int length=left.length<right.length?left.length:right.length;
        for(int i=0;i<length;i++)if(left[i]!=right[i])return (left[i]&255)-(right[i]&255);
        return left.length-right.length;
    }
}
