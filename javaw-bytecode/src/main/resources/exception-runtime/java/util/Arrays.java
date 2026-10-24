package java.util;

/** The primitive lexicographic comparators required by the pinned H2 Bits class. */
public final class Arrays {
    public static <T>void sort(T[] values,Comparator<? super T> comparator){sort(values,0,values.length,comparator);}
    public static void sort(Object[] values){sort(values,(Comparator)null);}
    public static <T>void sort(T[] values,int start,int end,Comparator<? super T> comparator){if(start>end)throw new IllegalArgumentException();if(start<0 || end>values.length)throw new ArrayIndexOutOfBoundsException();for(int i=start+1;i<end;i++){T value=values[i];int j=i;while(j>start && (comparator==null?((Comparable<T>)values[j-1]).compareTo(value):comparator.compare(values[j-1],value))>0){values[j]=values[j-1];j--;}values[j]=value;}}
    private Arrays(){}
    public static <T>List<T> asList(T... values){if(values==null)throw new NullPointerException();return new ArrayView<T>(values);}
    private static final class ArrayView<E> extends AbstractList<E> implements RandomAccess,java.io.Serializable{
        final E[] values;ArrayView(E[] values){this.values=values;}public int size(){return values.length;}
        public E get(int index){index(index);return values[index];}public E set(int index,E value){index(index);E old=values[index];values[index]=value;return old;}
    }

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
