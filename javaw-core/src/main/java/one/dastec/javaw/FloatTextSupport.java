package one.dastec.javaw;

import com.github.javaparser.ast.CompilationUnit;

/**
 * {@code Double.toString} and {@code Float.toString} for source-compiled code, as Java source compiled
 * into every module and called from the runtime's {@code $__str_from_f64} and {@code $__str_from_f32}.
 *
 * <p>The digits follow the Java 19+ specification: among the decimals that round to the value (round
 * to nearest, ties to even), take those of minimal length m, or of length 1 or 2 when m is 1; pick the
 * one closest to the value, and on a tie the one whose last digit is even. They are generated exactly
 * with a small fixed-size bignum (free-format digit generation in the style of Steele and White):
 * the value and the half-gaps to its neighbours are scaled to integers, and after each digit the
 * remainder is compared with the gaps. Values in [10^-3, 10^7) print in plain notation, others as
 * {@code d.dddE±n}.
 *
 * <p>The runtime passes the raw IEEE bits, so the code needs no library calls.
 */
final class FloatTextSupport {
    private FloatTextSupport() {}

    static final String CLASS = "__JavawFloatText";

    static final String SOURCE = """
        final class __JavawFloatText {
            // Bignums are 48 little-endian 32-bit words; no static fields, so modules get no extra static initialization.

            public static String __javaw_f64_text(long bits) {
                boolean negative = bits < 0;
                int biased = (int) ((bits >>> 52) & 0x7FF);
                long fraction = bits & 0xFFFFFFFFFFFFFL;
                if (biased == 0x7FF) return fraction != 0 ? "NaN" : negative ? "-Infinity" : "Infinity";
                if (biased == 0 && fraction == 0) return negative ? "-0.0" : "0.0";
                if (biased == 0) return __javaw_float_text(negative, fraction, -1074, false);
                return __javaw_float_text(negative, fraction | (1L << 52), biased - 1075, fraction == 0 && biased > 1);
            }

            public static String __javaw_f32_text(int bits) {
                boolean negative = bits < 0;
                int biased = (bits >>> 23) & 0xFF;
                long fraction = bits & 0x7FFFFF;
                if (biased == 0xFF) return fraction != 0 ? "NaN" : negative ? "-Infinity" : "Infinity";
                if (biased == 0 && fraction == 0) return negative ? "-0.0" : "0.0";
                if (biased == 0) return __javaw_float_text(negative, fraction, -149, false);
                return __javaw_float_text(negative, fraction | (1L << 23), biased - 150, fraction == 0 && biased > 1);
            }

            /** The text of f * 2^e; halfLow when the gap to the next smaller value is half the upper gap. */
            private static String __javaw_float_text(boolean negative, long f, int e, boolean halfLow) {
                // In units of 2^(e-2): value 4f, half-gaps 2 above and 2 (or 1) below; d is the denominator.
                int[] n = new int[48];
                int[] d = new int[48];
                int[] high = new int[48];
                int[] low = new int[48];
                int[] t = new int[48];
                __jft_set(n, f * 4);
                __jft_set(high, 2);
                __jft_set(low, halfLow ? 1 : 2);
                __jft_set(d, 1);
                if (e >= 2) { __jft_shiftLeft(n, e - 2); __jft_shiftLeft(high, e - 2); __jft_shiftLeft(low, e - 2); }
                else __jft_shiftLeft(d, 2 - e);
                // k estimates floor(log10(value)); the loops below correct it.
                int log2 = e;
                long top = f;
                while (top > 1) { top = top >>> 1; log2++; }
                int k = (log2 * 78913) >> 18;
                if (k >= 0) {
                    for (int i = 0; i < k; i++) __jft_multiply(d, 10);
                } else {
                    for (int i = 0; i < -k; i++) { __jft_multiply(n, 10); __jft_multiply(high, 10); __jft_multiply(low, 10); }
                }
                __jft_copy(t, d);
                __jft_multiply(t, 10);
                while (__jft_compare(n, t) >= 0) { __jft_multiply(d, 10); __jft_multiply(t, 10); k++; }
                while (__jft_compare(n, d) < 0) { __jft_multiply(n, 10); __jft_multiply(high, 10); __jft_multiply(low, 10); k--; }

                boolean inclusive = (f & 1) == 0;
                long q = 0;
                int length = 0;
                while (true) {
                    int digit = 0;
                    while (__jft_compare(n, d) >= 0) { __jft_subtract(n, d); digit++; }
                    q = q * 10 + digit;
                    length++;
                    int c = __jft_compare(n, low);
                    boolean downOk = c < 0 || (inclusive && c == 0);
                    __jft_add(t, n, high);
                    c = __jft_compare(t, d);
                    boolean upOk = c > 0 || (inclusive && c == 0);
                    // Length 1 is decided among the decimals of length 1 or 2, so go on to the second digit.
                    if ((downOk || upOk) && length >= 2) {
                        boolean up = upOk;
                        if (downOk && upOk) {
                            __jft_add(t, n, n);
                            c = __jft_compare(t, d);
                            up = c > 0 || (c == 0 && (q & 1) != 0);
                        }
                        if (up) q++;
                        break;
                    }
                    __jft_multiply(n, 10);
                    __jft_multiply(high, 10);
                    __jft_multiply(low, 10);
                }
                // Rounding up may carry into a new leading digit (99 -> 100).
                long limit = 1;
                for (int i = 0; i < length; i++) limit = limit * 10;
                if (q >= limit) { q = q / 10; k++; }
                while (q % 10 == 0 && q >= 10) q = q / 10;
                char[] digits = new char[20];
                int count = 0;
                long rest = q;
                while (rest > 0) { digits[count] = (char) ('0' + (int) (rest % 10)); rest = rest / 10; count++; }
                // digits holds the decimal digits in reverse order; k is the exponent of the first one.
                StringBuilder out = new StringBuilder();
                if (negative) out.append('-');
                if (k >= -3 && k < 7) {
                    if (k >= 0) {
                        for (int i = 0; i <= k; i++) out.append(i < count ? digits[count - 1 - i] : '0');
                        out.append('.');
                        if (count > k + 1) for (int i = k + 1; i < count; i++) out.append(digits[count - 1 - i]);
                        else out.append('0');
                    } else {
                        out.append("0.");
                        for (int i = 0; i < -k - 1; i++) out.append('0');
                        for (int i = 0; i < count; i++) out.append(digits[count - 1 - i]);
                    }
                } else {
                    out.append(digits[count - 1]);
                    out.append('.');
                    if (count > 1) for (int i = 1; i < count; i++) out.append(digits[count - 1 - i]);
                    else out.append('0');
                    out.append('E');
                    out.append(k);
                }
                return out.toString();
            }

            private static void __jft_set(int[] a, long value) {
                for (int i = 0; i < 48; i++) a[i] = 0;
                a[0] = (int) value;
                a[1] = (int) (value >>> 32);
            }

            private static void __jft_copy(int[] to, int[] from) {
                for (int i = 0; i < 48; i++) to[i] = from[i];
            }

            private static void __jft_shiftLeft(int[] a, int bits) {
                int words = bits >>> 5;
                int r = bits & 31;
                for (int i = 48 - 1; i >= 0; i--) {
                    long hi = i - words >= 0 ? (a[i - words] & 0xFFFFFFFFL) : 0;
                    long lo = r != 0 && i - words - 1 >= 0 ? (a[i - words - 1] & 0xFFFFFFFFL) : 0;
                    a[i] = r == 0 ? (int) hi : (int) ((hi << r) | (lo >>> (32 - r)));
                }
            }

            private static void __jft_multiply(int[] a, int m) {
                long carry = 0;
                for (int i = 0; i < 48; i++) {
                    long p = (a[i] & 0xFFFFFFFFL) * m + carry;
                    a[i] = (int) p;
                    carry = p >>> 32;
                }
            }

            private static int __jft_compare(int[] a, int[] b) {
                for (int i = 48 - 1; i >= 0; i--) {
                    long x = a[i] & 0xFFFFFFFFL;
                    long y = b[i] & 0xFFFFFFFFL;
                    if (x != y) return x < y ? -1 : 1;
                }
                return 0;
            }

            private static void __jft_subtract(int[] a, int[] b) {
                long borrow = 0;
                for (int i = 0; i < 48; i++) {
                    long x = (a[i] & 0xFFFFFFFFL) - (b[i] & 0xFFFFFFFFL) - borrow;
                    a[i] = (int) x;
                    borrow = x < 0 ? 1 : 0;
                }
            }

            private static void __jft_add(int[] to, int[] a, int[] b) {
                long carry = 0;
                for (int i = 0; i < 48; i++) {
                    long s = (a[i] & 0xFFFFFFFFL) + (b[i] & 0xFFFFFFFFL) + carry;
                    to[i] = (int) s;
                    carry = s >>> 32;
                }
            }
        }
        """;

    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu, "java.lang", SOURCE);
    }
}
