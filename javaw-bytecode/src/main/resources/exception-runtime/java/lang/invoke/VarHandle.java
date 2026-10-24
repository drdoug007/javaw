package java.lang.invoke;

/** Single-threaded plain byte-array views. No atomic/ordered access is approximated. */
public final class VarHandle {
    // Kinds follow widening order, except that short and char do not convert to each other.
    private final int kind,width;
    private final boolean little,exact;
    VarHandle(Class<?> arrayClass,boolean little,boolean exact) {
        int k;
        if(arrayClass==short[].class)k=0;
        else if(arrayClass==char[].class)k=1;
        else if(arrayClass==int[].class)k=2;
        else if(arrayClass==long[].class)k=3;
        else if(arrayClass==float[].class)k=4;
        else if(arrayClass==double[].class)k=5;
        else throw new UnsupportedOperationException();
        this.kind=k;this.width=k<2?2:k==2 || k==4?4:8;this.little=little;this.exact=exact;
    }
    private VarHandle(VarHandle source,boolean exact) {
        this.kind=source.kind;this.width=source.width;this.little=source.little;this.exact=exact;
    }
    public boolean hasInvokeExactBehavior(){return exact;}
    public VarHandle withInvokeExactBehavior(){return exact?this:new VarHandle(this,true);}
    public VarHandle withInvokeBehavior(){return exact?new VarHandle(this,false):this;}
    public Class<?> varType(){
        if(kind==0)return short.class;if(kind==1)return char.class;if(kind==2)return int.class;
        if(kind==3)return long.class;if(kind==4)return float.class;return double.class;
    }
    private static boolean widens(int from,int to){return from==to || to>=2 && from<to;}
    private void check(int signature,boolean write,boolean objectCoordinate) {
        if(exact ? kind!=signature || objectCoordinate : !widens(write?signature:kind,write?kind:signature))
            throw new WrongMethodTypeException();
    }
    private void bounds(byte[] bytes,int index){
        int length=bytes.length;
        if(index<0 || index>length-width)throw new ArrayIndexOutOfBoundsException();
    }
    private long read(byte[] bytes,int index) {
        bounds(bytes,index);long bits=0;
        for(int i=0;i<width;i++)bits|=(long)(bytes[index+i]&255)<<(8*(little?i:width-1-i));
        return bits;
    }
    private void write(byte[] bytes,int index,long bits) {
        bounds(bytes,index);
        for(int i=0;i<width;i++)bytes[index+i]=(byte)(bits>>>(8*(little?i:width-1-i)));
    }
    private long integral(long bits){return kind==0?(short)bits:kind==1?(char)bits:kind==2?(int)bits:bits;}
    public short javawGetShort(byte[] bytes,int index) {
        check(0,false,false);long bits=read(bytes,index);return (short)integral(bits);
    }
    public void javawSetShort(byte[] bytes,int index,short value) {
        check(0,true,false);write(bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public char javawGetChar(byte[] bytes,int index) {
        check(1,false,false);long bits=read(bytes,index);return (char)integral(bits);
    }
    public void javawSetChar(byte[] bytes,int index,char value) {
        check(1,true,false);write(bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public int javawGetInt(byte[] bytes,int index) {
        check(2,false,false);long bits=read(bytes,index);return (int)integral(bits);
    }
    public void javawSetInt(byte[] bytes,int index,int value) {
        check(2,true,false);write(bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public long javawGetLong(byte[] bytes,int index) {
        check(3,false,false);long bits=read(bytes,index);return (long)integral(bits);
    }
    public void javawSetLong(byte[] bytes,int index,long value) {
        check(3,true,false);write(bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public float javawGetFloat(byte[] bytes,int index) {
        check(4,false,false);long bits=read(bytes,index);return kind==4?Float.intBitsToFloat((int)bits):(float)integral(bits);
    }
    public void javawSetFloat(byte[] bytes,int index,float value) {
        check(4,true,false);write(bytes,index,kind==5?Double.doubleToRawLongBits((double)value):(long)Float.floatToRawIntBits(value));
    }
    public double javawGetDouble(byte[] bytes,int index) {
        check(5,false,false);long bits=read(bytes,index);return kind==5?Double.longBitsToDouble(bits):kind==4?(double)Float.intBitsToFloat((int)bits):(double)integral(bits);
    }
    public void javawSetDouble(byte[] bytes,int index,double value) {
        check(5,true,false);write(bytes,index,Double.doubleToRawLongBits(value));
    }
    public short javawGetShort(Object bytes,int index) {
        check(0,false,true);long bits=read((byte[])bytes,index);return (short)integral(bits);
    }
    public void javawSetShort(Object bytes,int index,short value) {
        check(0,true,true);write((byte[])bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public char javawGetChar(Object bytes,int index) {
        check(1,false,true);long bits=read((byte[])bytes,index);return (char)integral(bits);
    }
    public void javawSetChar(Object bytes,int index,char value) {
        check(1,true,true);write((byte[])bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public int javawGetInt(Object bytes,int index) {
        check(2,false,true);long bits=read((byte[])bytes,index);return (int)integral(bits);
    }
    public void javawSetInt(Object bytes,int index,int value) {
        check(2,true,true);write((byte[])bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public long javawGetLong(Object bytes,int index) {
        check(3,false,true);long bits=read((byte[])bytes,index);return (long)integral(bits);
    }
    public void javawSetLong(Object bytes,int index,long value) {
        check(3,true,true);write((byte[])bytes,index,kind==5?Double.doubleToRawLongBits((double)value):kind==4?(long)Float.floatToRawIntBits((float)value):(long)value);
    }
    public float javawGetFloat(Object bytes,int index) {
        check(4,false,true);long bits=read((byte[])bytes,index);return kind==4?Float.intBitsToFloat((int)bits):(float)integral(bits);
    }
    public void javawSetFloat(Object bytes,int index,float value) {
        check(4,true,true);write((byte[])bytes,index,kind==5?Double.doubleToRawLongBits((double)value):(long)Float.floatToRawIntBits(value));
    }
    public double javawGetDouble(Object bytes,int index) {
        check(5,false,true);long bits=read((byte[])bytes,index);return kind==5?Double.longBitsToDouble(bits):kind==4?(double)Float.intBitsToFloat((int)bits):(double)integral(bits);
    }
    public void javawSetDouble(Object bytes,int index,double value) {
        check(5,true,true);write((byte[])bytes,index,Double.doubleToRawLongBits(value));
    }
}
