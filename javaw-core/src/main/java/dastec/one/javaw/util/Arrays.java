package dastec.one.javaw.util;

/**
 * High-performance array utilities lowered to WebAssembly bulk memory operations (memory.copy, memory.fill).
 */
public class Arrays {

    public static void fill(int[] a, int val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(double[] a, double val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(double[] a, int fromIndex, int toIndex, double val) {
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(boolean[] a, boolean val) {
        fill(a, 0, a.length, val ? 1 : 0);
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
        int v = val ? 1 : 0;
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, int val) {
        boolean b = val != 0;
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = b;
        }
    }

    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        int len = Math.min(original.length, newLength);
        System.arraycopy(original, 0, copy, 0, len);
        return copy;
    }

    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        int len = Math.min(original.length, newLength);
        System.arraycopy(original, 0, copy, 0, len);
        return copy;
    }

    public static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) newLength = 0;
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0, newLength);
        return copy;
    }

    public static double[] copyOfRange(double[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) newLength = 0;
        double[] copy = new double[newLength];
        System.arraycopy(original, from, copy, 0, newLength);
        return copy;
    }

    public static boolean equals(int[] a, int[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) return false;
        }
        return true;
    }

    public static boolean equals(double[] a, double[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) return false;
        }
        return true;
    }
}
