package java.io;

/** Big-endian data writes over a sink; counts the bytes written, matching java.io.DataOutputStream. */
public class DataOutputStream extends FilterOutputStream implements DataOutput {
    protected int written;
    private byte[] bytearr;
    private final byte[] writeBuffer=new byte[8];
    public DataOutputStream(OutputStream out){super(out);}
    private void incCount(int value){
        int temp=written+value;
        if(temp<0)temp=Integer.MAX_VALUE;
        written=temp;
    }
    public void write(int b) throws IOException{out.write(b);incCount(1);}
    public void write(byte[] b,int off,int len) throws IOException{out.write(b,off,len);incCount(len);}
    public void flush() throws IOException{out.flush();}
    private void putInt(int v){
        writeBuffer[0]=(byte)(v>>>24);
        writeBuffer[1]=(byte)(v>>>16);
        writeBuffer[2]=(byte)(v>>>8);
        writeBuffer[3]=(byte)v;
    }
    private void putLong(long v){
        writeBuffer[0]=(byte)(v>>>56);
        writeBuffer[1]=(byte)(v>>>48);
        writeBuffer[2]=(byte)(v>>>40);
        writeBuffer[3]=(byte)(v>>>32);
        writeBuffer[4]=(byte)(v>>>24);
        writeBuffer[5]=(byte)(v>>>16);
        writeBuffer[6]=(byte)(v>>>8);
        writeBuffer[7]=(byte)v;
    }
    public final void writeBoolean(boolean v) throws IOException{out.write(v?1:0);incCount(1);}
    public final void writeByte(int v) throws IOException{out.write(v);incCount(1);}
    public final void writeShort(int v) throws IOException{
        writeBuffer[0]=(byte)(v>>>8);
        writeBuffer[1]=(byte)v;
        out.write(writeBuffer,0,2);
        incCount(2);
    }
    public final void writeChar(int v) throws IOException{
        writeBuffer[0]=(byte)(v>>>8);
        writeBuffer[1]=(byte)v;
        out.write(writeBuffer,0,2);
        incCount(2);
    }
    public final void writeInt(int v) throws IOException{putInt(v);out.write(writeBuffer,0,4);incCount(4);}
    public final void writeLong(long v) throws IOException{putLong(v);out.write(writeBuffer,0,8);incCount(8);}
    public final void writeFloat(float v) throws IOException{putInt(Float.floatToIntBits(v));out.write(writeBuffer,0,4);incCount(4);}
    public final void writeDouble(double v) throws IOException{putLong(Double.doubleToLongBits(v));out.write(writeBuffer,0,8);incCount(8);}
    public final void writeBytes(String s) throws IOException{
        int len=s.length();
        for(int i=0;i<len;i++)out.write((byte)s.charAt(i));
        incCount(len);
    }
    public final void writeChars(String s) throws IOException{
        int len=s.length();
        for(int i=0;i<len;i++){
            char c=s.charAt(i);
            writeBuffer[0]=(byte)(c>>>8);
            writeBuffer[1]=(byte)c;
            out.write(writeBuffer,0,2);
        }
        incCount(len*2);
    }
    public final void writeUTF(String str) throws IOException{
        int strlen=str.length();
        int utflen=0;
        for(int i=0;i<strlen;i++){
            char c=str.charAt(i);
            if(c>=0x0001 && c<=0x007F)utflen+=1;
            else if(c>0x07FF)utflen+=3;
            else utflen+=2;
        }
        if(utflen>65535 || utflen<strlen)throw new UTFDataFormatException(tooLongMsg(str,utflen));
        if(bytearr==null || bytearr.length<utflen+2)bytearr=new byte[utflen*2+2];
        bytearr[0]=(byte)(utflen>>>8);
        bytearr[1]=(byte)utflen;
        int count=2;
        for(int i=0;i<strlen;i++){
            char c=str.charAt(i);
            if(c>=0x0001 && c<=0x007F){
                bytearr[count++]=(byte)c;
            }else if(c>0x07FF){
                bytearr[count++]=(byte)(0xE0|((c>>12)&0x0F));
                bytearr[count++]=(byte)(0x80|((c>>6)&0x3F));
                bytearr[count++]=(byte)(0x80|(c&0x3F));
            }else{
                bytearr[count++]=(byte)(0xC0|((c>>6)&0x1F));
                bytearr[count++]=(byte)(0x80|(c&0x3F));
            }
        }
        write(bytearr,0,utflen+2);
    }
    public final int size(){return written;}
    private static String tooLongMsg(String s,int bits32){
        long actualLength=(long)s.length()+Integer.toUnsignedLong(bits32-s.length());
        return "encoded string ("+s.substring(0,8)+"..."+s.substring(s.length()-8)+") too long: "+actualLength+" bytes";
    }
}
