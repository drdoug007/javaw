package javaw.runtime;

/** SHA-1 (FIPS 180), big-endian. Constants from the JDK's sun.security.provider.SHA. */
public final class Sha1 extends BlockDigest {
    private static final int[] INITIAL_HASHES = {
        0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0
    };

    private int[] h;
    private int[] w;

    public Sha1(String algorithm) {
        super(algorithm, "SHA-1", 20, 64, 8, false);
        this.h = new int[5];
        this.w = new int[80];
        resetState();
    }

    protected void compress(byte[] block,int offset) {
        int[] w = this.w;
        for (int t = 0; t < 16; t++) {
            int i = offset + 4 * t;
            w[t] = (block[i] & 0xff) << 24 | (block[i + 1] & 0xff) << 16 | (block[i + 2] & 0xff) << 8 | (block[i + 3] & 0xff);
        }
        for (int t = 16; t < 80; t++) {
            w[t] = Integer.rotateLeft(w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16], 1);
        }
        int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4];
        for (int t = 0; t < 80; t++) {
            int f;
            int k;
            if (t < 20) {
                f = (b & c) | (~b & d);
                k = 0x5a827999;
            } else if (t < 40) {
                f = b ^ c ^ d;
                k = 0x6ed9eba1;
            } else if (t < 60) {
                f = (b & c) | (b & d) | (c & d);
                k = 0x8f1bbcdc;
            } else {
                f = b ^ c ^ d;
                k = 0xca62c1d6;
            }
            int temp = Integer.rotateLeft(a,5) + f + e + k + w[t];
            e = d;
            d = c;
            c = Integer.rotateLeft(b,30);
            b = a;
            a = temp;
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e;
    }

    protected void output(byte[] out,int offset) {
        for (int i = 0; i < 5; i++) {
            int v = h[i];
            out[offset] = (byte) (v >>> 24);
            out[offset + 1] = (byte) (v >>> 16);
            out[offset + 2] = (byte) (v >>> 8);
            out[offset + 3] = (byte) v;
            offset += 4;
        }
    }

    protected void resetState() {
        System.arraycopy(INITIAL_HASHES,0,h,0,5);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Sha1 copy = (Sha1) super.clone();
        copy.h = h.clone();
        copy.w = w.clone();
        return copy;
    }
}
