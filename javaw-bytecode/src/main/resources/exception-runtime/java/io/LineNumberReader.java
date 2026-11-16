package java.io;

/** A BufferedReader that tracks the current line number, and can be used to ignore linefeed characters following carriage returns. */
public class LineNumberReader extends BufferedReader {
    private static final int NONE=0,CHAR=1,EOL=2,EOF=3;
    private int prevChar=NONE;
    private int lineNumber=0;
    private int markedLineNumber;
    private boolean skipLF;
    private boolean markedSkipLF;
    private static final int maxSkipBufferSize=8192;
    private char[] skipBuffer=null;
    public LineNumberReader(Reader in){super(in);}
    public LineNumberReader(Reader in,int sz){super(in,sz);}
    public void setLineNumber(int lineNumber){this.lineNumber=lineNumber;}
    public int getLineNumber(){return lineNumber;}
    public int read() throws IOException {
        int c=super.read();
        if(skipLF){if(c=='\n')c=super.read();skipLF=false;}
        if(c=='\r'||c=='\n'){if(c=='\r')skipLF=true;lineNumber++;prevChar=EOL;return '\n';}
        if(c==-1){if(prevChar==CHAR)lineNumber++;prevChar=EOF;return -1;}
        prevChar=CHAR;
        return c;
    }
    public int read(char[] cbuf,int off,int len) throws IOException {
        int n=super.read(cbuf,off,len);
        if(n==-1){if(prevChar==CHAR)lineNumber++;prevChar=EOF;return -1;}
        for(int i=off;i<off+n;i++){
            int c=cbuf[i];
            if(skipLF){skipLF=false;if(c=='\n')continue;}
            if(c=='\r'){skipLF=true;lineNumber++;}else if(c=='\n')lineNumber++;
        }
        if(n>0){char last=cbuf[off+n-1];prevChar=(last=='\r'||last=='\n')?EOL:CHAR;}
        return n;
    }
    public String readLine() throws IOException {
        boolean[] term=new boolean[1];
        String l=super.readLine(skipLF,term);
        skipLF=false;
        if(l!=null){lineNumber++;prevChar=term[0]?EOL:EOF;}
        else{if(prevChar==CHAR)lineNumber++;prevChar=EOF;}
        return l;
    }
    public long skip(long n) throws IOException {
        if(n<0)throw new IllegalArgumentException("skip() value is negative");
        int nn=(int)Math.min(n,maxSkipBufferSize);
        if((skipBuffer==null)||(skipBuffer.length<nn))skipBuffer=new char[nn];
        long r=n;
        while(r>0){int nc=read(skipBuffer,0,(int)Math.min(r,nn));if(nc==-1)break;r-=nc;}
        if(n-r>0)prevChar=NONE;
        return n-r;
    }
    public void mark(int readAheadLimit) throws IOException {
        if(skipLF)readAheadLimit++;
        super.mark(readAheadLimit);
        markedLineNumber=lineNumber;markedSkipLF=skipLF;
    }
    public void reset() throws IOException {
        super.reset();
        lineNumber=markedLineNumber;skipLF=markedSkipLF;
    }
}
