package java.io;

/** Big-endian data reads over a source; multi-byte reads go through an 8-byte buffer, matching java.io.DataInputStream. */
public class DataInputStream extends FilterInputStream implements DataInput {
    private final byte[] readBuffer=new byte[8];
    private byte[] bytearr=new byte[0];
    private char[] chararr=new char[0];
    public DataInputStream(InputStream in){super(in);}
    public final int read(byte[] b) throws IOException{return in.read(b,0,b.length);}
    public final int read(byte[] b,int off,int len) throws IOException{return in.read(b,off,len);}
    public final void readFully(byte[] b) throws IOException{readFully(b,0,b.length);}
    public final void readFully(byte[] b,int off,int len) throws IOException{
        checkFromIndexSize(off,len,b.length);
        int n=0;
        while(n<len){
            int count=in.read(b,off+n,len-n);
            if(count<0)throw new EOFException();
            n+=count;
        }
    }
    public final int skipBytes(int n) throws IOException{
        int total=0,cur;
        while(total<n && (cur=(int)in.skip(n-total))>0)total+=cur;
        return total;
    }
    public final boolean readBoolean() throws IOException{return readUnsignedByte()!=0;}
    public final byte readByte() throws IOException{return (byte)readUnsignedByte();}
    public final int readUnsignedByte() throws IOException{
        int ch=in.read();
        if(ch<0)throw new EOFException();
        return ch;
    }
    public final short readShort() throws IOException{
        readFully(readBuffer,0,2);
        return (short)(((readBuffer[0]&0xff)<<8)|(readBuffer[1]&0xff));
    }
    public final int readUnsignedShort() throws IOException{
        readFully(readBuffer,0,2);
        return ((readBuffer[0]&0xff)<<8)|(readBuffer[1]&0xff);
    }
    public final char readChar() throws IOException{
        readFully(readBuffer,0,2);
        return (char)(((readBuffer[0]&0xff)<<8)|(readBuffer[1]&0xff));
    }
    public final int readInt() throws IOException{
        readFully(readBuffer,0,4);
        return ((readBuffer[0]&0xff)<<24)|((readBuffer[1]&0xff)<<16)|((readBuffer[2]&0xff)<<8)|(readBuffer[3]&0xff);
    }
    public final long readLong() throws IOException{
        readFully(readBuffer,0,8);
        int hi=((readBuffer[0]&0xff)<<24)|((readBuffer[1]&0xff)<<16)|((readBuffer[2]&0xff)<<8)|(readBuffer[3]&0xff);
        int lo=((readBuffer[4]&0xff)<<24)|((readBuffer[5]&0xff)<<16)|((readBuffer[6]&0xff)<<8)|(readBuffer[7]&0xff);
        return ((long)hi<<32)+(lo&0xFFFFFFFFL);
    }
    public final float readFloat() throws IOException{return Float.intBitsToFloat(readInt());}
    public final double readDouble() throws IOException{return Double.longBitsToDouble(readLong());}
    public final String readUTF() throws IOException{return readUTF(this);}
    public static final String readUTF(DataInput in) throws IOException{
        int utflen=in.readUnsignedShort();
        byte[] bytearr;
        char[] chararr;
        if(in instanceof DataInputStream){
            DataInputStream dis=(DataInputStream)in;
            if(dis.bytearr.length<utflen){
                dis.bytearr=new byte[utflen*2];
                dis.chararr=new char[utflen*2];
            }
            bytearr=dis.bytearr;
            chararr=dis.chararr;
        }else{
            bytearr=new byte[utflen];
            chararr=new char[utflen];
        }
        in.readFully(bytearr,0,utflen);
        int count=0;
        int chararr_count=0;
        int c;
        while(count<utflen){
            c=bytearr[count]&0xff;
            if(c>127)break;
            count++;
            chararr[chararr_count++]=(char)c;
        }
        byte char2;
        byte char3;
        while(count<utflen){
            c=bytearr[count]&0xff;
            int k=c>>4;
            if(k<=7){
                count++;
                chararr[chararr_count++]=(char)c;
            }else if(k==12||k==13){
                count+=2;
                if(count>utflen)throw new UTFDataFormatException("malformed input: partial character at end");
                char2=bytearr[count-1];
                if((char2&0xC0)!=0x80)throw new UTFDataFormatException("malformed input around byte "+count);
                chararr[chararr_count++]=(char)(((c&0x1F)<<6)|(char2&0x3F));
            }else if(k==14){
                count+=3;
                if(count>utflen)throw new UTFDataFormatException("malformed input: partial character at end");
                char2=bytearr[count-2];
                char3=bytearr[count-1];
                if((char2&0xC0)!=0x80||(char3&0xC0)!=0x80)throw new UTFDataFormatException("malformed input around byte "+(count-1));
                chararr[chararr_count++]=(char)(((c&0x0F)<<12)|((char2&0x3F)<<6)|(char3&0x3F));
            }else{
                throw new UTFDataFormatException("malformed input around byte "+count);
            }
        }
        return new String(chararr,0,chararr_count);
    }
}
