package java.io;

/** Delegating filter over the protected `out` stream; every method is overridable and close() remembers that it closed. */
public class FilterOutputStream extends OutputStream {
    protected OutputStream out;
    private boolean closed;
    public FilterOutputStream(OutputStream out){this.out=out;}
    public void write(int b) throws IOException{out.write(b);}
    public void write(byte[] b) throws IOException{write(b,0,b.length);}
    public void write(byte[] b,int off,int len) throws IOException{
        int length=b.length;
        if((off|len)<0 || len>length-off)
            throw new IndexOutOfBoundsException("Range ["+off+", "+off+" + "+len+") out of bounds for length "+length);
        for(int i=0;i<len;i++)write(b[off+i]);
    }
    public void flush() throws IOException{out.flush();}
    public void close() throws IOException{
        if(closed)return;
        closed=true;
        Throwable flushException=null;
        try{
            flush();
        }catch(Throwable e){
            flushException=e;
            throw e;
        }finally{
            if(flushException==null){
                out.close();
            }else{
                try{
                    out.close();
                }catch(Throwable closeException){
                    if(flushException!=closeException)closeException.addSuppressed(flushException);
                    throw closeException;
                }
            }
        }
    }
}
