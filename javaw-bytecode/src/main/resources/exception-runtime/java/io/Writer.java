package java.io;

/** Character sink base class. The lock is retained for subclasses; the single-threaded contract does not enter monitors. */
public abstract class Writer implements Appendable,Closeable,Flushable {
    private char[] writeBuffer;
    protected Object lock;
    protected Writer() {lock=this;}
    protected Writer(Object lock) {if(lock==null)throw new NullPointerException();this.lock=lock;}
    public void write(int ch) throws IOException {
        if(writeBuffer==null)writeBuffer=new char[1024];
        writeBuffer[0]=(char)ch;write(writeBuffer,0,1);
    }
    public void write(char[] chars) throws IOException {write(chars,0,chars.length);}
    public abstract void write(char[] chars,int offset,int length) throws IOException;
    public void write(String text) throws IOException {write(text,0,text.length());}
    public void write(String text,int offset,int length) throws IOException {
        char[] buffer;
        if(length<=1024) {if(writeBuffer==null)writeBuffer=new char[1024];buffer=writeBuffer;}
        else buffer=new char[length];
        text.getChars(offset,offset+length,buffer,0);write(buffer,0,length);
    }
    public Writer append(CharSequence text) throws IOException {write(String.valueOf(text));return this;}
    public Writer append(CharSequence text,int start,int end) throws IOException {
        if(text==null)text="null";return append(text.subSequence(start,end));
    }
    public Writer append(char ch) throws IOException {write(ch);return this;}
    public abstract void flush() throws IOException;
    public abstract void close() throws IOException;
}
