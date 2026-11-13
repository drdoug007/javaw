package java.security;

public abstract class MessageDigestSpi {
    public MessageDigestSpi(){}

    protected int engineGetDigestLength(){return 0;}

    protected abstract void engineUpdate(byte input);
    protected abstract void engineUpdate(byte[] input,int offset,int len);
    protected abstract byte[] engineDigest();
    protected abstract void engineReset();

    protected int engineDigest(byte[] buf,int offset,int len) throws DigestException {
        byte[] b = engineDigest();
        if (len < b.length) {
            throw new DigestException("partial digests not returned");
        }
        if (buf.length - offset < b.length) {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        }
        System.arraycopy(b,0,buf,offset,b.length);
        return b.length;
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
