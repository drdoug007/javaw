package java.io;

/** Character source over a fixed String; the mark can be reset and a negative skip moves back to the start. */
public class StringReader extends Reader {
    private int length;
    private String str;
    private int next;
    private int mark;
    public StringReader(String s) {length=s.length();str=s;}
    private void ensureOpen() throws IOException {if(str==null)throw new IOException("Stream closed");}
    public int read() throws IOException {
        ensureOpen();if(next>=length)return -1;return str.charAt(next++);
    }
    public int read(char[] cbuf,int off,int len) throws IOException {
        ensureOpen();checkFromIndexSize(off,len,cbuf.length);
        if(len==0)return 0;
        if(next>=length)return -1;
        int n=Math.min(length-next,len);
        str.getChars(next,next+n,cbuf,off);next+=n;return n;
    }
    public long skip(long n) throws IOException {
        ensureOpen();
        if(next>=length)return 0;
        long r=Math.min(length-next,n);
        r=Math.max(-next,r);
        next+=(int)r;return r;
    }
    public boolean ready() throws IOException {ensureOpen();return true;}
    public boolean markSupported() {return true;}
    public void mark(int readAheadLimit) throws IOException {
        if(readAheadLimit<0)throw new IllegalArgumentException("Read-ahead limit < 0");
        ensureOpen();mark=next;
    }
    public void reset() throws IOException {ensureOpen();next=mark;}
    public void close() {str=null;}
}
