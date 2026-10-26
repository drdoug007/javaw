/*
 * Copyright (c) 1998, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

/**
 * Port of the "Freely Distributable Math Library", version 5.3, from
 * C to Java.
 *
 * <p>The C version of fdlibm relied on the idiom of pointer aliasing
 * a 64-bit double floating-point value as a two-element array of
 * 32-bit integers and reading and writing the two halves of the
 * double independently. This coding pattern was problematic to C
 * optimizers and not directly expressible in Java. Therefore, rather
 * than a memory level overlay, if portions of a double need to be
 * operated on as integer values, the standard library methods for
 * bitwise floating-point to integer conversion,
 * Double.longBitsToDouble and Double.doubleToRawLongBits, are directly
 * or indirectly used.
 *
 * <p>The C version of fdlibm also took some pains to signal the
 * correct IEEE 754 exceptional conditions divide by zero, invalid,
 * overflow and underflow. For example, overflow would be signaled by
 * {@code huge * huge} where {@code huge} was a large constant that
 * would overflow when squared. Since IEEE floating-point exceptional
 * handling is not supported natively in the JVM, such coding patterns
 * have been omitted from this port. For example, rather than {@code
 * return huge * huge}, this port will use {@code return INFINITY}.
 *
 * <p>Various comparison and arithmetic operations in fdlibm could be
 * done either based on the integer view of a value or directly on the
 * floating-point representation. Which idiom is faster may depend on
 * platform specific factors. However, for code clarity if no other
 * reason, this port will favor expressing the semantics of those
 * operations in terms of floating-point operations when convenient to
 * do so.
 */
final class FdLibm {
    // Constants used by multiple algorithms
    private static final double INFINITY = (1.0/0.0);
    private static final double TWO24    = 0x1.0p24; // 1.67772160000000000000e+07
    private static final double TWO54    = 0x1.0p54; // 1.80143985094819840000e+16
    private static final double HUGE     = 1.0e+300;

    /*
     * Constants for bit-wise manipulation of IEEE 754 double
     * values. These constants are for the high-order 32-bits of a
     * 64-bit double value: 1 sign bit as the most significant bit,
     * followed by 11 exponent bits, and then the remaining bits as
     * the significand.
     */
    private static final int SIGN_BIT        = 0x8000_0000;
    private static final int EXP_BITS        = 0x7ff0_0000;
    private static final int EXP_SIGNIF_BITS = 0x7fff_ffff;

    private FdLibm() {
        throw new UnsupportedOperationException("No FdLibm instances for you.");
    }

    /**
     * Return the low-order 32 bits of the double argument as an int.
     */
    private static int __LO(double x) {
        long transducer = Double.doubleToRawLongBits(x);
        return (int)transducer;
    }

    /**
     * Return a double with its low-order bits of the second argument
     * and the high-order bits of the first argument..
     */
    private static double __LO(double x, int low) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0xFFFF_FFFF_0000_0000L) |
                                       (low    & 0x0000_0000_FFFF_FFFFL));
    }

    /**
     * Return the high-order 32 bits of the double argument as an int.
     */
    private static int __HI(double x) {
        long transducer = Double.doubleToRawLongBits(x);
        return (int)(transducer >> 32);
    }

    /**
     * Return a double with its high-order bits of the second argument
     * and the low-order bits of the first argument..
     */
    private static double __HI(double x, int high) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0x0000_0000_FFFF_FFFFL) |
                                       ( ((long)high)) << 32 );
    }

    /**
     * Return a double with its high-order bits of the first argument
     * and the low-order bits of the second argument..
     */
    private static double __HI_LO(int high, int low) {
        return Double.longBitsToDouble(((long)high << 32) |
                                       (low & 0xffff_ffffL));
    }

    static final class Exp {
        private Exp() {throw new UnsupportedOperationException();}

        private static final double[] half = {0.5, -0.5,};
        private static final double huge    = 1.0e+300;
        private static final double twom1000=     0x1.0p-1000;             //  9.33263618503218878990e-302 = 2^-1000
        private static final double o_threshold=  0x1.62e42fefa39efp9;     //  7.09782712893383973096e+02
        private static final double u_threshold= -0x1.74910d52d3051p9;     // -7.45133219101941108420e+02;
        private static final double[] ln2HI   ={  0x1.62e42feep-1,         //  6.93147180369123816490e-01
                                                 -0x1.62e42feep-1};        // -6.93147180369123816490e-01
        private static final double[] ln2LO   ={  0x1.a39ef35793c76p-33,   //  1.90821492927058770002e-10
                                                 -0x1.a39ef35793c76p-33};  // -1.90821492927058770002e-10
        private static final double invln2 =      0x1.71547652b82fep0;     //  1.44269504088896338700e+00

        private static final double P1   =  0x1.555555555553ep-3;  //  1.66666666666666019037e-01
        private static final double P2   = -0x1.6c16c16bebd93p-9;  // -2.77777777770155933842e-03
        private static final double P3   =  0x1.1566aaf25de2cp-14; //  6.61375632143793436117e-05
        private static final double P4   = -0x1.bbd41c5d26bf1p-20; // -1.65339022054652515390e-06
        private static final double P5   =  0x1.6376972bea4d0p-25; //  4.13813679705723846039e-08

        public static double compute(double x) {
            double y;
            double hi = 0.0;
            double lo = 0.0;
            double c;
            double t;
            int k = 0;
            int xsb;
            /*unsigned*/ int hx;

            hx  = __HI(x);  /* high word of x */
            xsb = (hx >> 31) & 1;               /* sign bit of x */
            hx &= EXP_SIGNIF_BITS;              /* high word of |x| */

            /* filter out non-finite argument */
            if (hx >= 0x40862E42) {                  /* if |x| >= 709.78... */
                if (hx >= 0x7ff00000) {
                    if (((hx & 0xfffff) | __LO(x)) != 0)
                        return x + x;                /* NaN */
                    else
                        return (xsb == 0) ? x : 0.0;    /* exp(+-inf) = {inf, 0} */
                }
                if (x > o_threshold)
                    return huge * huge; /* overflow */
                if (x < u_threshold) // unsigned compare needed here?
                    return twom1000 * twom1000; /* underflow */
            }

            /* argument reduction */
            if (hx > 0x3fd62e42) {           /* if  |x| > 0.5 ln2 */
                if(hx < 0x3FF0A2B2) {       /* and |x| < 1.5 ln2 */
                    hi = x - ln2HI[xsb];
                    lo=ln2LO[xsb];
                    k = 1 - xsb - xsb;
                } else {
                    k  = (int)(invln2 * x + half[xsb]);
                    t  = k;
                    hi = x - t*ln2HI[0];    /* t*ln2HI is exact here */
                    lo = t*ln2LO[0];
                }
                x  = hi - lo;
            } else if (hx < 0x3e300000)  {     /* when |x|<2**-28 */
                if (huge + x > 1.0)
                    return 1.0 + x; /* trigger inexact */
            } else {
                k = 0;
            }

            /* x is now in primary range */
            t  = x * x;
            c  = x - t*(P1 + t*(P2 + t*(P3 + t*(P4 + t*P5))));
            if (k == 0)
                return 1.0 - ((x*c)/(c - 2.0) - x);
            else
                y = 1.0 - ((lo - (x*c)/(2.0 - c)) - hi);

            if(k >= -1021) {
                y = __HI(y, __HI(y) + (k << 20)); /* add k to y's exponent */
                return y;
            } else {
                y = __HI(y, __HI(y) + ((k + 1000) << 20)); /* add k to y's exponent */
                return y * twom1000;
            }
        }
    }
    static final class Log {
        private Log() {throw new UnsupportedOperationException();}

        private static final double
            ln2_hi = 0x1.62e42feep-1,       // 6.93147180369123816490e-01
            ln2_lo = 0x1.a39ef35793c76p-33, // 1.90821492927058770002e-10

            Lg1    = 0x1.5555555555593p-1,  // 6.666666666666735130e-01
            Lg2    = 0x1.999999997fa04p-2,  // 3.999999999940941908e-01
            Lg3    = 0x1.2492494229359p-2,  // 2.857142874366239149e-01
            Lg4    = 0x1.c71c51d8e78afp-3,  // 2.222219843214978396e-01
            Lg5    = 0x1.7466496cb03dep-3,  // 1.818357216161805012e-01
            Lg6    = 0x1.39a09d078c69fp-3,  // 1.531383769920937332e-01
            Lg7    = 0x1.2f112df3e5244p-3;  // 1.479819860511658591e-01

        static double compute(double x) {
            double hfsq, f, s, z, R, w, t1, t2, dk;
            int k, hx, i, j;
            /*unsigned*/ int lx;

            hx = __HI(x);           // high word of x
            lx = __LO(x);           // low  word of x

            k=0;
            if (hx < 0x0010_0000) {                  // x < 2**-1022
                if (((hx & EXP_SIGNIF_BITS) | lx) == 0) { // log(+-0) = -inf
                    return -TWO54/0.0;
                }
                if (hx < 0) {                        // log(-#) = NaN
                    return (x - x)/0.0;
                }
                k -= 54;
                x *= TWO54;    // subnormal number, scale up x
                hx = __HI(x);  // high word of x
            }
            if (hx >= EXP_BITS) {
                return x + x;
            }
            k += (hx >> 20) - 1023;
            hx &= 0x000f_ffff;
            i = (hx + 0x9_5f64) & 0x10_0000;
            x =__HI(x, hx | (i ^ 0x3ff0_0000));  // normalize x or x/2
            k += (i >> 20);
            f = x - 1.0;
            if ((0x000f_ffff & (2 + hx)) < 3) {// |f| < 2**-20
                if (f == 0.0) {
                    if (k == 0) {
                        return 0.0;
                    } else {
                        dk = (double)k;
                        return dk*ln2_hi + dk*ln2_lo;
                    }
                }
                R = f*f*(0.5 - 0.33333333333333333*f);
                if (k == 0) {
                    return f - R;
                } else {
                    dk = (double)k;
                    return dk*ln2_hi - ((R - dk*ln2_lo) - f);
                }
            }
            s = f/(2.0 + f);
            dk = (double)k;
            z = s*s;
            i = hx - 0x6_147a;
            w = z*z;
            j = 0x6b851 - hx;
            t1= w*(Lg2 + w*(Lg4 + w*Lg6));
            t2= z*(Lg1 + w*(Lg3 + w*(Lg5 + w*Lg7)));
            i |= j;
            R = t2 + t1;
            if (i > 0) {
                hfsq = 0.5*f*f;
                if (k == 0) {
                    return f-(hfsq - s*(hfsq + R));
                } else {
                    return dk*ln2_hi - ((hfsq - (s*(hfsq + R) + dk*ln2_lo)) - f);
                }
            } else {
                if (k == 0) {
                    return f - s*(f - R);
                } else {
                    return dk*ln2_hi - ((s*(f - R) - dk*ln2_lo) - f);
                }
            }
        }
    }
}
