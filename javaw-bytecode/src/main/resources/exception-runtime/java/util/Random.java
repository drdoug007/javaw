package java.util;

/**
 * Implementation of Random for the javaw bytecode compiler, for callers that pass a seed.
 * The 48-bit linear congruential generator and its constants are the JDK's, so seeded sequences
 * are bit-for-bit the JDK's. The no-argument constructor (time and entropy) is not linked.
 */
public class Random implements java.io.Serializable {
    private long seed;
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;
    private static final double DOUBLE_UNIT = 0x1.0p-53;
    private static final float FLOAT_UNIT = 0x1.0p-24f;
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    public Random(long seed) {
        if (getClass() == Random.class)
            this.seed = initialScramble(seed);
        else
            setSeed(seed);
    }

    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    public void setSeed(long seed) {
        this.seed = initialScramble(seed);
        haveNextNextGaussian = false;
    }

    protected int next(int bits) {
        seed = (seed * multiplier + addend) & mask;
        return (int) (seed >>> (48 - bits));
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len; )
            for (int rnd = nextInt(), n = Math.min(len - i, 4); n-- > 0; rnd >>= 8)
                bytes[i++] = (byte) rnd;
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException("bound must be positive");
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)
            r = (int) ((bound * (long) r) >> 31);
        else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
            }
        }
        return r;
    }

    public int nextInt(int origin, int bound) {
        if (origin >= bound)
            throw new IllegalArgumentException("bound must be greater than origin");
        int r = nextInt();
        int n = bound - origin, m = n - 1;
        if ((n & m) == 0)
            r = (r & m) + origin;
        else if (n > 0) {
            for (int u = r >>> 1; u + m - (r = u % n) < 0; u = nextInt() >>> 1) {
            }
            r += origin;
        } else {
            while (r < origin || r >= bound)
                r = nextInt();
        }
        return r;
    }

    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) * FLOAT_UNIT;
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * DOUBLE_UNIT;
    }

    public double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }
}
