package javaw.runtime;

/** SHA-256 (FIPS 180), not SHA-224. Constants from the JDK's sun.security.provider.SHA2.SHA256. */
public final class Sha256 extends BlockDigest {
    private static final int[] ROUND_CONSTS = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    private static final int[] INITIAL_HASHES = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    private int[] h;
    private int[] w;

    public Sha256(String algorithm) {
        super(algorithm, "SHA-256", 32, 64, 8, false);
        this.h = new int[8];
        this.w = new int[64];
        resetState();
    }

    protected void compress(byte[] block,int offset) {
        int[] w = this.w;
        for (int t = 0; t < 16; t++) {
            int i = offset + 4 * t;
            w[t] = (block[i] & 0xff) << 24 | (block[i + 1] & 0xff) << 16 | (block[i + 2] & 0xff) << 8 | (block[i + 3] & 0xff);
        }
        for (int t = 16; t < 64; t++) {
            int x = w[t - 15];
            int y = w[t - 2];
            int s0 = Integer.rotateRight(x,7) ^ Integer.rotateRight(x,18) ^ (x >>> 3);
            int s1 = Integer.rotateRight(y,17) ^ Integer.rotateRight(y,19) ^ (y >>> 10);
            w[t] = s0 + s1 + w[t - 7] + w[t - 16];
        }
        int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
        for (int t = 0; t < 64; t++) {
            int sigma1 = Integer.rotateRight(e,6) ^ Integer.rotateRight(e,11) ^ Integer.rotateRight(e,25);
            int ch = g ^ (e & (f ^ g));
            int t1 = hh + sigma1 + ch + ROUND_CONSTS[t] + w[t];
            int sigma0 = Integer.rotateRight(a,2) ^ Integer.rotateRight(a,13) ^ Integer.rotateRight(a,22);
            int maj = (a & b) ^ ((a ^ b) & c);
            int t2 = sigma0 + maj;
            hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
    }

    protected void output(byte[] out,int offset) {
        for (int i = 0; i < 8; i++) {
            int v = h[i];
            out[offset] = (byte) (v >>> 24);
            out[offset + 1] = (byte) (v >>> 16);
            out[offset + 2] = (byte) (v >>> 8);
            out[offset + 3] = (byte) v;
            offset += 4;
        }
    }

    protected void resetState() {
        System.arraycopy(INITIAL_HASHES,0,h,0,8);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Sha256 copy = (Sha256) super.clone();
        copy.h = h.clone();
        copy.w = w.clone();
        return copy;
    }
}
