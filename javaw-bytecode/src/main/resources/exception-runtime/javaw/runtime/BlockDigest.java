package javaw.runtime;

import java.security.DigestException;
import java.security.MessageDigest;

/** A block-based digest engine with the standard 0x80/zero/length-bit padding. */
public abstract class BlockDigest extends MessageDigest implements Cloneable {
    private final String canonicalName;
    private final int digestLength;
    private final int blockSize;
    private final int lengthBytes;
    private final boolean littleEndian;
    private byte[] buffer;
    private int used;
    private long count;

    protected BlockDigest(String algorithm,String canonicalName,int digestLength,int blockSize,int lengthBytes,boolean littleEndian) {
        super(algorithm);
        this.canonicalName = canonicalName;
        this.digestLength = digestLength;
        this.blockSize = blockSize;
        this.lengthBytes = lengthBytes;
        this.littleEndian = littleEndian;
        this.buffer = new byte[blockSize];
    }

    protected abstract void compress(byte[] block,int offset);
    protected abstract void output(byte[] out,int offset);
    protected abstract void resetState();

    protected final int engineGetDigestLength(){return digestLength;}

    protected final void engineUpdate(byte input) {
        buffer[used++] = input;
        count++;
        if (used == blockSize) {
            compress(buffer,0);
            used = 0;
        }
    }

    protected final void engineUpdate(byte[] input,int offset,int len) {
        if (len == 0) {
            return;
        }
        if (offset < 0 || len < 0 || offset > input.length - len) {
            throw new ArrayIndexOutOfBoundsException("Range [" + offset + ", " + offset + " + " + len
                    + ") out of bounds for length " + input.length);
        }
        count += len;
        if (used > 0) {
            int n = Math.min(len,blockSize - used);
            System.arraycopy(input,offset,buffer,used,n);
            used += n;
            offset += n;
            len -= n;
            if (used == blockSize) {
                compress(buffer,0);
                used = 0;
            }
        }
        while (len >= blockSize) {
            compress(input,offset);
            offset += blockSize;
            len -= blockSize;
        }
        if (len > 0) {
            System.arraycopy(input,offset,buffer,0,len);
            used = len;
        }
    }

    protected final byte[] engineDigest() {
        byte[] out = new byte[digestLength];
        buffer[used++] = (byte) 0x80;
        if (used > blockSize - lengthBytes) {
            zeroBlock(used);
            compress(buffer,0);
            zeroBlock(0);
        } else {
            zeroBlock(used);
        }
        // Only the low 8 bytes of the length field can be non-zero; zeroBlock already cleared the rest
        // (a long shift by 64 or more would wrap and repeat the count in SHA-512's high 8 bytes).
        long bits = count * 8;
        if (littleEndian) {
            for (int i = 0; i < 8; i++) {
                buffer[blockSize - lengthBytes + i] = (byte) (bits >>> (8 * i));
            }
        } else {
            for (int i = 0; i < 8; i++) {
                buffer[blockSize - 1 - i] = (byte) (bits >>> (8 * i));
            }
        }
        compress(buffer,0);
        output(out,0);
        resetState();
        used = 0;
        count = 0;
        return out;
    }

    protected final void engineReset() {
        resetState();
        used = 0;
        count = 0;
    }

    protected final int engineDigest(byte[] buf,int offset,int len) throws DigestException {
        if (len < digestLength) {
            throw new DigestException("Length must be at least " + digestLength + " for " + canonicalName + "digests");
        }
        // Check the range before hashing, so a failed call keeps the pending input.
        if (offset < 0 || len < 0 || offset > buf.length - len) {
            throw new DigestException("Buffer too short to store digest");
        }
        byte[] result = engineDigest();
        System.arraycopy(result,0,buf,offset,digestLength);
        return digestLength;
    }

    private void zeroBlock(int from) {
        for (int i = from; i < blockSize; i++) {
            buffer[i] = 0;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        BlockDigest copy = (BlockDigest) super.clone();
        copy.buffer = buffer.clone();
        return copy;
    }
}
