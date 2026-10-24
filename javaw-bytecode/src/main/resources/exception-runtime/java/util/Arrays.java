package java.util;

/** Selected sequential array algorithms for linked dependency bytecode. */
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

    private static void range(int length,int start,int end){
        if(start>end)throw new IllegalArgumentException();
        if(start<0 || end>length)throw new ArrayIndexOutOfBoundsException();
    }
    public static void sort(Object[] values,int start,int end){sort(values,start,end,(Comparator)null);}
    public static <T>T[] copyOf(T[] source,int length){return (T[])copyOf(source,length,source.getClass());}
    public static <T,U>T[] copyOf(U[] source,int length,Class<? extends T[]> type){
        T[] result=(T[])java.lang.reflect.Array.newInstance(type.getComponentType(),length);
        System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;
    }
    public static <T>T[] copyOfRange(T[] source,int start,int end){return (T[])copyOfRange(source,start,end,source.getClass());}
    public static <T,U>T[] copyOfRange(U[] source,int start,int end,Class<? extends T[]> type){
        int length=end-start;if(length<0)throw new IllegalArgumentException();
        T[] result=(T[])java.lang.reflect.Array.newInstance(type.getComponentType(),length);
        int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;
    }
    public static void fill(Object[] values,Object value){fill(values,0,values.length,value);}
    public static void fill(Object[] values,int start,int end,Object value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(Object[] a,Object[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(!Objects.equals(a[i],b[i]))return false;return true;}
    public static int hashCode(Object[] values){if(values==null)return 0;int hash=1;for(Object value:values)hash=31*hash+Objects.hashCode(value);return hash;}
    public static String toString(Object[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(Object[] values,Object key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(Object[] values,int start,int end,Object key){return binarySearch(values,start,end,key,(Comparator)null);}
    public static <T>int binarySearch(T[] values,T key,Comparator<? super T> order){return binarySearch(values,0,values.length,key,order);}
    public static <T>int binarySearch(T[] values,int start,int end,T key,Comparator<? super T> order){
        range(values.length,start,end);int low=start,high=end-1;
        while(low<=high){int mid=(low+high)>>>1;int compared=order==null?((Comparable<T>)values[mid]).compareTo(key):order.compare(values[mid],key);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;
    }
    public static boolean deepEquals(Object[] a,Object[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(!deepElementEquals(a[i],b[i]))return false;return true;}
    static boolean deepElementEquals(Object a,Object b){
        if(a==b)return true;if(a==null || b==null)return false;
        if(a instanceof Object[] && b instanceof Object[])return deepEquals((Object[])a,(Object[])b);
        if(a instanceof boolean[] && b instanceof boolean[])return equals((boolean[])a,(boolean[])b);
        if(a instanceof byte[] && b instanceof byte[])return equals((byte[])a,(byte[])b);
        if(a instanceof short[] && b instanceof short[])return equals((short[])a,(short[])b);
        if(a instanceof char[] && b instanceof char[])return equals((char[])a,(char[])b);
        if(a instanceof int[] && b instanceof int[])return equals((int[])a,(int[])b);
        if(a instanceof long[] && b instanceof long[])return equals((long[])a,(long[])b);
        if(a instanceof float[] && b instanceof float[])return equals((float[])a,(float[])b);
        if(a instanceof double[] && b instanceof double[])return equals((double[])a,(double[])b);
        return a.equals(b);
    }
    public static int deepHashCode(Object[] values){if(values==null)return 0;int hash=1;for(Object value:values)hash=31*hash+deepElementHash(value);return hash;}
    private static int deepElementHash(Object value){
        if(value instanceof Object[])return deepHashCode((Object[])value);
        if(value instanceof boolean[])return hashCode((boolean[])value);
        if(value instanceof byte[])return hashCode((byte[])value);
        if(value instanceof short[])return hashCode((short[])value);
        if(value instanceof char[])return hashCode((char[])value);
        if(value instanceof int[])return hashCode((int[])value);
        if(value instanceof long[])return hashCode((long[])value);
        if(value instanceof float[])return hashCode((float[])value);
        if(value instanceof double[])return hashCode((double[])value);
        return Objects.hashCode(value);
    }
    public static String deepToString(Object[] values){if(values==null)return "null";StringBuilder out=new StringBuilder();deepText(values,out,new ArrayList<Object[]>());return out.toString();}
    private static void deepText(Object[] values,StringBuilder out,ArrayList<Object[]> path){
        for(Object[] prior:path)if(prior==values){out.append("[...]");return;}
        path.add(values);out.append('[');
        for(int i=0;i<values.length;i++){if(i>0)out.append(", ");Object value=values[i];
            if(value instanceof Object[])deepText((Object[])value,out,path);
            else if(value instanceof boolean[])out.append(toString((boolean[])value));
            else if(value instanceof byte[])out.append(toString((byte[])value));
            else if(value instanceof short[])out.append(toString((short[])value));
            else if(value instanceof char[])out.append(toString((char[])value));
            else if(value instanceof int[])out.append(toString((int[])value));
            else if(value instanceof long[])out.append(toString((long[])value));
            else if(value instanceof float[])out.append(toString((float[])value));
            else if(value instanceof double[])out.append(toString((double[])value));
            else out.append(value);
        }
        out.append(']');path.remove(path.size()-1);
    }

    public static boolean[] copyOf(boolean[] source,int length){if(length==source.length)return source.clone();boolean[] result=new boolean[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static boolean[] copyOfRange(boolean[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();boolean[] result=new boolean[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(boolean[] values,boolean value){fill(values,0,values.length,value);}
    public static void fill(boolean[] values,int start,int end,boolean value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(boolean[] a,boolean[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(boolean[] values){if(values==null)return 0;int hash=1;for(boolean value:values)hash=31*hash+(value?1231:1237);return hash;}
    public static String toString(boolean[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}

    public static byte[] copyOf(byte[] source,int length){if(length==source.length)return source.clone();byte[] result=new byte[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static byte[] copyOfRange(byte[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();byte[] result=new byte[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(byte[] values,byte value){fill(values,0,values.length,value);}
    public static void fill(byte[] values,int start,int end,byte value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(byte[] a,byte[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(byte[] values){if(values==null)return 0;int hash=1;for(byte value:values)hash=31*hash+value;return hash;}
    public static String toString(byte[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(byte[] values,byte key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(byte[] values,int start,int end,byte key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=(values[mid]<key? -1:values[mid]==key?0:1);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(byte[] values){sort(values,0,values.length);}
    public static void sort(byte[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){byte value=values[i];int j=i;while(j>start && (values[j-1]<value? -1:values[j-1]==value?0:1)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static short[] copyOf(short[] source,int length){if(length==source.length)return source.clone();short[] result=new short[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static short[] copyOfRange(short[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();short[] result=new short[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(short[] values,short value){fill(values,0,values.length,value);}
    public static void fill(short[] values,int start,int end,short value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(short[] a,short[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(short[] values){if(values==null)return 0;int hash=1;for(short value:values)hash=31*hash+value;return hash;}
    public static String toString(short[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(short[] values,short key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(short[] values,int start,int end,short key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=(values[mid]<key? -1:values[mid]==key?0:1);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(short[] values){sort(values,0,values.length);}
    public static void sort(short[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){short value=values[i];int j=i;while(j>start && (values[j-1]<value? -1:values[j-1]==value?0:1)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static char[] copyOf(char[] source,int length){if(length==source.length)return source.clone();char[] result=new char[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static char[] copyOfRange(char[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();char[] result=new char[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(char[] values,char value){fill(values,0,values.length,value);}
    public static void fill(char[] values,int start,int end,char value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(char[] a,char[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(char[] values){if(values==null)return 0;int hash=1;for(char value:values)hash=31*hash+value;return hash;}
    public static String toString(char[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(char[] values,char key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(char[] values,int start,int end,char key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=(values[mid]<key? -1:values[mid]==key?0:1);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(char[] values){sort(values,0,values.length);}
    public static void sort(char[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){char value=values[i];int j=i;while(j>start && (values[j-1]<value? -1:values[j-1]==value?0:1)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static int[] copyOf(int[] source,int length){if(length==source.length)return source.clone();int[] result=new int[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static int[] copyOfRange(int[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();int[] result=new int[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(int[] values,int value){fill(values,0,values.length,value);}
    public static void fill(int[] values,int start,int end,int value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(int[] a,int[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(int[] values){if(values==null)return 0;int hash=1;for(int value:values)hash=31*hash+value;return hash;}
    public static String toString(int[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(int[] values,int key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(int[] values,int start,int end,int key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=(values[mid]<key? -1:values[mid]==key?0:1);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(int[] values){sort(values,0,values.length);}
    public static void sort(int[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){int value=values[i];int j=i;while(j>start && (values[j-1]<value? -1:values[j-1]==value?0:1)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static long[] copyOf(long[] source,int length){if(length==source.length)return source.clone();long[] result=new long[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static long[] copyOfRange(long[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();long[] result=new long[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(long[] values,long value){fill(values,0,values.length,value);}
    public static void fill(long[] values,int start,int end,long value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(long[] a,long[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
    public static int hashCode(long[] values){if(values==null)return 0;int hash=1;for(long value:values)hash=31*hash+(int)(value^(value>>>32));return hash;}
    public static String toString(long[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(long[] values,long key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(long[] values,int start,int end,long key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=(values[mid]<key? -1:values[mid]==key?0:1);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(long[] values){sort(values,0,values.length);}
    public static void sort(long[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){long value=values[i];int j=i;while(j>start && (values[j-1]<value? -1:values[j-1]==value?0:1)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static float[] copyOf(float[] source,int length){if(length==source.length)return source.clone();float[] result=new float[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static float[] copyOfRange(float[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();float[] result=new float[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(float[] values,float value){fill(values,0,values.length,value);}
    public static void fill(float[] values,int start,int end,float value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(float[] a,float[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(Float.floatToIntBits(a[i])!=Float.floatToIntBits(b[i]))return false;return true;}
    public static int hashCode(float[] values){if(values==null)return 0;int hash=1;for(float value:values)hash=31*hash+Float.floatToIntBits(value);return hash;}
    public static String toString(float[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(float[] values,float key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(float[] values,int start,int end,float key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=Float.compare(values[mid],key);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(float[] values){sort(values,0,values.length);}
    public static void sort(float[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){float value=values[i];int j=i;while(j>start && Float.compare(values[j-1],value)>0){values[j]=values[j-1];j--;}values[j]=value;}}

    public static double[] copyOf(double[] source,int length){if(length==source.length)return source.clone();double[] result=new double[length];System.arraycopy(source,0,result,0,source.length<length?source.length:length);return result;}
    public static double[] copyOfRange(double[] source,int start,int end){if(start==0 && end==source.length)return source.clone();int length=end-start;if(length<0)throw new IllegalArgumentException();double[] result=new double[length];int available=source.length-start;System.arraycopy(source,start,result,0,available<length?available:length);return result;}
    public static void fill(double[] values,double value){fill(values,0,values.length,value);}
    public static void fill(double[] values,int start,int end,double value){range(values.length,start,end);for(int i=start;i<end;i++)values[i]=value;}
    public static boolean equals(double[] a,double[] b){if(a==b)return true;if(a==null || b==null || a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(Double.doubleToLongBits(a[i])!=Double.doubleToLongBits(b[i]))return false;return true;}
    public static int hashCode(double[] values){if(values==null)return 0;int hash=1;for(double value:values)hash=31*hash+Double.hashCode(value);return hash;}
    public static String toString(double[] values){if(values==null)return "null";StringBuilder out=new StringBuilder("[");for(int i=0;i<values.length;i++){if(i>0)out.append(", ");out.append(values[i]);}return out.append(']').toString();}
    public static int binarySearch(double[] values,double key){return binarySearch(values,0,values.length,key);}
    public static int binarySearch(double[] values,int start,int end,double key){range(values.length,start,end);int low=start,high=end-1;while(low<=high){int mid=(low+high)>>>1;int compared=Double.compare(values[mid],key);if(compared<0)low=mid+1;else if(compared>0)high=mid-1;else return mid;}return -low-1;}
    public static void sort(double[] values){sort(values,0,values.length);}
    public static void sort(double[] values,int start,int end){range(values.length,start,end);for(int i=start+1;i<end;i++){double value=values[i];int j=i;while(j>start && Double.compare(values[j-1],value)>0){values[j]=values[j-1];j--;}values[j]=value;}}
}
