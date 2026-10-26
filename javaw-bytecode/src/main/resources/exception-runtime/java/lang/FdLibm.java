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

// Javaw adaptation, 2026-10-26: selected algorithms; inline unavailable Double
// constants; route internal StrictMath calls through portable Math adapters.
// The original GPLv2 with Classpath exception continues to apply.
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
    static final class Log10 {
        private static final double ivln10    = 0x1.bcb7b1526e50ep-2;  // 4.34294481903251816668e-01

        private static final double log10_2hi = 0x1.34413509f6p-2;     // 3.01029995663611771306e-01;
        private static final double log10_2lo = 0x1.9fef311f12b36p-42; // 3.69423907715893078616e-13;

        private Log10() {
            throw new UnsupportedOperationException();
        }

        public static double compute(double x) {
            double y, z;
            int i, k;

            int hx = __HI(x); // high word of x
            int lx = __LO(x); // low word of x

            k=0;
            if (hx < 0x0010_0000) {                  /* x < 2**-1022  */
                if (((hx & EXP_SIGNIF_BITS) | lx) == 0) {
                    return -TWO54/0.0;               /* log(+-0)=-inf */
                }
                if (hx < 0) {
                    return (x - x)/0.0;              /* log(-#) = NaN */
                }
                k -= 54;
                x *= TWO54; /* subnormal number, scale up x */
                hx = __HI(x);
            }

            if (hx >= EXP_BITS) {
                return x + x;
            }

            k += (hx >> 20) - 1023;
            i  = (k  & SIGN_BIT) >>> 31; // unsigned shift
            hx = (hx & 0x000f_ffff) | ((0x3ff - i) << 20);
            y  = (double)(k + i);
            x = __HI(x, hx); // replace high word of x with hx
            z  = y * log10_2lo + ivln10 * Math.log(x);
            return  z + y * log10_2hi;
        }
    }
    static final class Pow {
        private Pow() {
            throw new UnsupportedOperationException();
        }

        public static double compute(final double x, final double y) {
            double z;
            double r, s, t, u, v, w;
            int i, j, k, n;

            // y == zero: x**0 = 1
            if (y == 0.0)
                return 1.0;

            // +/-NaN return x + y to propagate NaN significands
            if (Double.isNaN(x) || Double.isNaN(y))
                return x + y;

            final double y_abs = Math.abs(y);
            double x_abs   = Math.abs(x);
            // Special values of y
            if (y == 2.0) {
                return x * x;
            } else if (y == 0.5) {
                if (x >= -0x1.fffffffffffffp1023) // Handle x == -infinity later
                    return Math.sqrt(x + 0.0); // Add 0.0 to properly handle x == -0.0
            } else if (y_abs == 1.0) {        // y is  +/-1
                return (y == 1.0) ? x : 1.0 / x;
            } else if (y_abs == INFINITY) {       // y is +/-infinity
                if (x_abs == 1.0)
                    return  y - y;         // inf**+/-1 is NaN
                else if (x_abs > 1.0) // (|x| > 1)**+/-inf = inf, 0
                    return (y >= 0) ? y : 0.0;
                else                       // (|x| < 1)**-/+inf = inf, 0
                    return (y < 0) ? -y : 0.0;
            }

            final int hx = __HI(x);
            int ix = hx & EXP_SIGNIF_BITS;

            /*
             * When x < 0, determine if y is an odd integer:
             * y_is_int = 0       ... y is not an integer
             * y_is_int = 1       ... y is an odd int
             * y_is_int = 2       ... y is an even int
             */
            int y_is_int  = 0;
            if (hx < 0) {
                if (y_abs >= 0x1.0p53)   // |y| >= 2^53 = 9.007199254740992E15
                    y_is_int = 2; // y is an even integer since ulp(2^53) = 2.0
                else if (y_abs >= 1.0) { // |y| >= 1.0
                    long y_abs_as_long = (long) y_abs;
                    if ( ((double) y_abs_as_long) == y_abs) {
                        y_is_int = 2 -  (int)(y_abs_as_long & 0x1L);
                    }
                }
            }

            // Special value of x
            if (x_abs == 0.0 ||
                x_abs == INFINITY ||
                x_abs == 1.0) {
                z = x_abs;                 // x is +/-0, +/-inf, +/-1
                if (y < 0.0)
                    z = 1.0/z;     // z = (1/|x|)
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | y_is_int) == 0) {
                        z = (z-z)/(z-z); // (-1)**non-int is NaN
                    } else if (y_is_int == 1)
                        z = -1.0 * z;             // (x < 0)**odd = -(|x|**odd)
                }
                return z;
            }

            n = (hx >> 31) + 1;

            // (x < 0)**(non-int) is NaN
            if ((n | y_is_int) == 0)
                return (x - x)/(x - x);

            s = 1.0; // s (sign of result -ve**odd) = -1 else = 1
            if ( (n | (y_is_int - 1)) == 0)
                s = -1.0; // (-ve)**(odd int)

            double p_h, p_l, t1, t2;
            // |y| is huge
            if (y_abs > 0x1.00000_ffff_ffffp31) { // if |y| > ~2**31
                final double INV_LN2   =  0x1.7154_7652_b82fep0;   //  1.44269504088896338700e+00 = 1/ln2
                final double INV_LN2_H =  0x1.715476p0;            //  1.44269502162933349609e+00 = 24 bits of 1/ln2
                final double INV_LN2_L =  0x1.4ae0_bf85_ddf44p-26; //  1.92596299112661746887e-08 = 1/ln2 tail

                // Over/underflow if x is not close to one
                if (x_abs < 0x1.fffff_0000_0000p-1) // |x| < ~0.9999995231628418
                    return (y < 0.0) ? s * INFINITY : s * 0.0;
                if (x_abs > 0x1.00000_ffff_ffffp0)         // |x| > ~1.0
                    return (y > 0.0) ? s * INFINITY : s * 0.0;
                /*
                 * now |1-x| is tiny <= 2**-20, sufficient to compute
                 * log(x) by x - x^2/2 + x^3/3 - x^4/4
                 */
                t = x_abs - 1.0;        // t has 20 trailing zeros
                w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
                u = INV_LN2_H * t;      // INV_LN2_H has 21 sig. bits
                v =  t * INV_LN2_L - w * INV_LN2;
                t1 = u + v;
                t1 =__LO(t1, 0);
                t2 = v - (t1 - u);
            } else {
                final double CP      =  0x1.ec70_9dc3_a03fdp-1;  //  9.61796693925975554329e-01 = 2/(3ln2)
                final double CP_H    =  0x1.ec709ep-1;           //  9.61796700954437255859e-01 = (float)cp
                final double CP_L    = -0x1.e2fe_0145_b01f5p-28; // -7.02846165095275826516e-09 = tail of CP_H

                double z_h, z_l, ss, s2, s_h, s_l, t_h, t_l;
                n = 0;
                // Take care of subnormal numbers
                if (ix < 0x00100000) {
                    x_abs *= 0x1.0p53; // 2^53 = 9007199254740992.0
                    n -= 53;
                    ix = __HI(x_abs);
                }
                n  += ((ix) >> 20) - 0x3ff;
                j  = ix & 0x000fffff;
                // Determine interval
                ix = j | 0x3ff00000;          // Normalize ix
                if (j <= 0x3988E)
                    k = 0;         // |x| <sqrt(3/2)
                else if (j < 0xBB67A)
                    k = 1;         // |x| <sqrt(3)
                else {
                    k = 0;
                    n += 1;
                    ix -= 0x00100000;
                }
                x_abs = __HI(x_abs, ix);

                // Compute ss = s_h + s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5)

                final double BP[]    = {1.0,
                                       1.5};
                final double DP_H[]  = {0.0,
                                        0x1.2b80_34p-1};        // 5.84962487220764160156e-01
                final double DP_L[]  = {0.0,
                                        0x1.cfde_b43c_fd006p-27};// 1.35003920212974897128e-08

                // Poly coefs for (3/2)*(log(x)-2s-2/3*s**3
                final double L1      =  0x1.3333_3333_33303p-1;  //  5.99999999999994648725e-01
                final double L2      =  0x1.b6db_6db6_fabffp-2;  //  4.28571428578550184252e-01
                final double L3      =  0x1.5555_5518_f264dp-2;  //  3.33333329818377432918e-01
                final double L4      =  0x1.1746_0a91_d4101p-2;  //  2.72728123808534006489e-01
                final double L5      =  0x1.d864_a93c_9db65p-3;  //  2.30660745775561754067e-01
                final double L6      =  0x1.a7e2_84a4_54eefp-3;  //  2.06975017800338417784e-01
                u = x_abs - BP[k];               // BP[0]=1.0, BP[1]=1.5
                v = 1.0 / (x_abs + BP[k]);
                ss = u * v;
                s_h = ss;
                s_h = __LO(s_h, 0);
                // t_h=x_abs + BP[k] High
                t_h = 0.0;
                t_h = __HI(t_h, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18) );
                t_l = x_abs - (t_h - BP[k]);
                s_l = v * ((u - s_h * t_h) - s_h * t_l);
                // Compute log(x_abs)
                s2 = ss * ss;
                r = s2 * s2* (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
                r += s_l * (s_h + ss);
                s2  = s_h * s_h;
                t_h = 3.0 + s2 + r;
                t_h = __LO(t_h, 0);
                t_l = r - ((t_h - 3.0) - s2);
                // u+v = ss*(1+...)
                u = s_h * t_h;
                v = s_l * t_h + t_l * ss;
                // 2/(3log2)*(ss + ...)
                p_h = u + v;
                p_h = __LO(p_h, 0);
                p_l = v - (p_h - u);
                z_h = CP_H * p_h;             // CP_H + CP_L = 2/(3*log2)
                z_l = CP_L * p_h + p_l * CP + DP_L[k];
                // log2(x_abs) = (ss + ..)*2/(3*log2) = n + DP_H + z_h + z_l
                t = (double)n;
                t1 = (((z_h + z_l) + DP_H[k]) + t);
                t1 = __LO(t1, 0);
                t2 = z_l - (((t1 - t) - DP_H[k]) - z_h);
            }

            // Split up y into (y1 + y2) and compute (y1 + y2) * (t1 + t2)
            double y1  = y;
            y1 = __LO(y1, 0);
            p_l = (y - y1) * t1 + y * t2;
            p_h = y1 * t1;
            z = p_l + p_h;
            j = __HI(z);
            i = __LO(z);
            if (j >= 0x40900000) {                           // z >= 1024
                if (((j - 0x40900000) | i)!=0)               // if z > 1024
                    return s * INFINITY;                     // Overflow
                else {
                    final double OVT     =  8.0085662595372944372e-0017; // -(1024-log2(ovfl+.5ulp))
                    if (p_l + OVT > z - p_h)
                        return s * INFINITY;   // Overflow
                }
            } else if ((j & EXP_SIGNIF_BITS) >= 0x4090cc00 ) {        // z <= -1075
                if (((j - 0xc090cc00) | i)!=0)           // z < -1075
                    return s * 0.0;           // Underflow
                else {
                    if (p_l <= z - p_h)
                        return s * 0.0;      // Underflow
                }
            }
            /*
             * Compute 2**(p_h+p_l)
             */
            // Poly coefs for (3/2)*(log(x)-2s-2/3*s**3
            final double P1      =  0x1.5555_5555_5553ep-3;  //  1.66666666666666019037e-01
            final double P2      = -0x1.6c16_c16b_ebd93p-9;  // -2.77777777770155933842e-03
            final double P3      =  0x1.1566_aaf2_5de2cp-14; //  6.61375632143793436117e-05
            final double P4      = -0x1.bbd4_1c5d_26bf1p-20; // -1.65339022054652515390e-06
            final double P5      =  0x1.6376_972b_ea4d0p-25; //  4.13813679705723846039e-08
            final double LG2     =  0x1.62e4_2fef_a39efp-1;  //  6.93147180559945286227e-01
            final double LG2_H   =  0x1.62e43p-1;            //  6.93147182464599609375e-01
            final double LG2_L   = -0x1.05c6_10ca_86c39p-29; // -1.90465429995776804525e-09
            i = j & EXP_SIGNIF_BITS;
            k = (i >> 20) - 0x3ff;
            n = 0;
            if (i > 0x3fe00000) {              // if |z| > 0.5, set n = [z + 0.5]
                n = j + (0x00100000 >> (k + 1));
                k = ((n & EXP_SIGNIF_BITS) >> 20) - 0x3ff;     // new k for n
                t = 0.0;
                t = __HI(t, (n & ~(0x000fffff >> k)) );
                n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
                if (j < 0)
                    n = -n;
                p_h -= t;
            }
            t = p_l + p_h;
            t = __LO(t, 0);
            u = t * LG2_H;
            v = (p_l - (t - p_h)) * LG2 + t * LG2_L;
            z = u + v;
            w = v - (z - u);
            t  = z * z;
            t1  = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
            r  = (z * t1)/(t1 - 2.0) - (w + z * w);
            z  = 1.0 - (r - z);
            j  = __HI(z);
            j += (n << 20);
            if ((j >> 20) <= 0)
                z = Math.scalb(z, n); // subnormal output
            else {
                int z_hi = __HI(z);
                z_hi += (n << 20);
                z = __HI(z, z_hi);
            }
            return s * z;
        }
    }
    static final class Sin {
        private Sin() {throw new UnsupportedOperationException();}

        static double compute(double x) {
            double[] y = new double[2];
            double z = 0.0;
            int n, ix;

            // High word of x.
            ix = __HI(x);

            // |x| ~< pi/4
            ix &= EXP_SIGNIF_BITS;
            if (ix <= 0x3fe9_21fb) {
                return __kernel_sin(x, z, 0);
            } else if (ix >= EXP_BITS) {  // sin(Inf or NaN) is NaN
                return x - x;
            } else { // argument reduction needed
                n = RemPio2.__ieee754_rem_pio2(x, y);
                switch(n & 3) {
                case 0: return  Sin.__kernel_sin(y[0], y[1], 1);
                case 1: return  Cos.__kernel_cos(y[0], y[1]);
                case 2: return -Sin.__kernel_sin(y[0], y[1], 1);
                default:
                    return -Cos.__kernel_cos(y[0], y[1]);
                }
            }
        }

        /** __kernel_sin( x, y, iy)
         * kernel sin function on [-pi/4, pi/4], pi/4 ~ 0.7854
         * Input x is assumed to be bounded by ~pi/4 in magnitude.
         * Input y is the tail of x.
         * Input iy indicates whether y is 0. (if iy=0, y assume to be 0).
         *
         * Algorithm
         *      1. Since sin(-x) = -sin(x), we need only to consider positive x.
         *      2. if x < 2^-27 (hx<0x3e400000 0), return x with inexact if x!=0.
         *      3. sin(x) is approximated by a polynomial of degree 13 on
         *         [0,pi/4]
         *                               3            13
         *              sin(x) ~ x + S1*x + ... + S6*x
         *         where
         *
         *      |sin(x)         2     4     6     8     10     12  |     -58
         *      |----- - (1+S1*x +S2*x +S3*x +S4*x +S5*x  +S6*x   )| <= 2
         *      |  x                                               |
         *
         *      4. sin(x+y) = sin(x) + sin'(x')*y
         *                  ~ sin(x) + (1-x*x/2)*y
         *         For better accuracy, let
         *                   3      2      2      2      2
         *              r = x *(S2+x *(S3+x *(S4+x *(S5+x *S6))))
         *         then                   3    2
         *              sin(x) = x + (S1*x + (x *(r-y/2)+y))
         */
        private static final double
            S1  = -0x1.5555555555549p-3,  // -1.66666666666666324348e-01
            S2  =  0x1.111111110f8a6p-7,  //  8.33333333332248946124e-03
            S3  = -0x1.a01a019c161d5p-13, // -1.98412698298579493134e-04
            S4  =  0x1.71de357b1fe7dp-19, //  2.75573137070700676789e-06
            S5  = -0x1.ae5e68a2b9cebp-26, // -2.50507602534068634195e-08
            S6  =  0x1.5d93a5acfd57cp-33; //  1.58969099521155010221e-10

        static double __kernel_sin(double x, double y, int iy) {
            double z, r, v;
            int ix;
            ix = __HI(x) & EXP_SIGNIF_BITS;    // high word of x
            if (ix < 0x3e40_0000) {            // |x| < 2**-27
                if ((int)x == 0)               // generate inexact
                    return x;
            }
            z       =  x*x;
            v       =  z*x;
            r       =  S2 + z*(S3 + z*(S4 + z*(S5 + z*S6)));
            if (iy == 0) {
                return x + v*(S1 + z*r);
            } else {
                return x - ((z*(0.5*y - v*r) - y) - v*S1);
            }
        }
    }
    static final class Cos {
        private Cos() {throw new UnsupportedOperationException();}

        static double compute(double x) {
            double[] y = new double[2];
            double z = 0.0;
            int n, ix;

            // High word of x.
            ix = __HI(x);

            // |x| ~< pi/4
            ix &= EXP_SIGNIF_BITS;
            if (ix <= 0x3fe9_21fb) {
                return __kernel_cos(x, z);
            } else if (ix >= EXP_BITS) { // cos(Inf or NaN) is NaN
                return x - x;
            } else { // argument reduction needed
                n = RemPio2.__ieee754_rem_pio2(x,y);
                switch (n & 3) {
                case 0: return  Cos.__kernel_cos(y[0], y[1]);
                case 1: return -Sin.__kernel_sin(y[0], y[1],1);
                case 2: return -Cos.__kernel_cos(y[0], y[1]);
                default:
                    return  Sin.__kernel_sin(y[0], y[1], 1);
                }
            }
        }

        /**
         * __kernel_cos( x,  y )
         * kernel cos function on [-pi/4, pi/4], pi/4 ~ 0.785398164
         * Input x is assumed to be bounded by ~pi/4 in magnitude.
         * Input y is the tail of x.
         *
         * Algorithm
         *      1. Since cos(-x) = cos(x), we need only to consider positive x.
         *      2. if x < 2^-27 (hx < 0x3e4000000), return 1 with inexact if x != 0.
         *      3. cos(x) is approximated by a polynomial of degree 14 on
         *         [0,pi/4]
         *                                       4            14
         *              cos(x) ~ 1 - x*x/2 + C1*x + ... + C6*x
         *         where the remez error is
         *
         *      |              2     4     6     8     10    12     14 |     -58
         *      |cos(x)-(1-.5*x +C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  )| <= 2
         *      |                                                      |
         *
         *                     4     6     8     10    12     14
         *      4. let r = C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  , then
         *             cos(x) = 1 - x*x/2 + r
         *         since cos(x+y) ~ cos(x) - sin(x)*y
         *                        ~ cos(x) - x*y,
         *         a correction term is necessary in cos(x) and hence
         *              cos(x+y) = 1 - (x*x/2 - (r - x*y))
         *         For better accuracy when x > 0.3, let qx = |x|/4 with
         *         the last 32 bits mask off, and if x > 0.78125, let qx = 0.28125.
         *         Then
         *              cos(x+y) = (1-qx) - ((x*x/2-qx) - (r-x*y)).
         *         Note that 1-qx and (x*x/2-qx) is EXACT here, and the
         *         magnitude of the latter is at least a quarter of x*x/2,
         *         thus, reducing the rounding error in the subtraction.
         */
        private static final double
            C1  =  0x1.555555555554cp-5,  //  4.16666666666666019037e-02
            C2  = -0x1.6c16c16c15177p-10, // -1.38888888888741095749e-03
            C3  =  0x1.a01a019cb159p-16,  //  2.48015872894767294178e-05
            C4  = -0x1.27e4f809c52adp-22, // -2.75573143513906633035e-07
            C5  =  0x1.1ee9ebdb4b1c4p-29, //  2.08757232129817482790e-09
            C6  = -0x1.8fae9be8838d4p-37; // -1.13596475577881948265e-11

        static double __kernel_cos(double x, double y) {
            double a, hz, z, r, qx = 0.0;
            int ix;
            ix = __HI(x) & EXP_SIGNIF_BITS;       // ix = |x|'s high word
            if (ix < 0x3e40_0000) {           // if x < 2**27
                if (((int)x) == 0) {          // generate inexact
                    return 1.0;
                }
            }
            z  = x*x;
            r  = z*(C1 + z*(C2 + z*(C3 + z*(C4 + z*(C5 + z*C6)))));
            if (ix < 0x3FD3_3333) {                    // if |x| < 0.3
                return 1.0 - (0.5*z - (z*r - x*y));
            } else {
                if (ix > 0x3fe9_0000) {               // x > 0.78125
                    qx = 0.28125;
                } else {
                    qx = __HI_LO(ix - 0x0020_0000, 0);
                }
                hz = 0.5*z - qx;
                a  = 1.0 - qx;
                return a - (hz - (z*r - x*y));
            }
        }
    }
    static final class RemPio2 {
        /*
         * Table of constants for 2/pi, 396 Hex digits (476 decimal) of 2/pi
         */
        private static final int[] two_over_pi = {
            0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62,
            0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A,
            0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129,
            0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41,
            0x3991D6, 0x398353, 0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8,
            0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
            0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5,
            0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB, 0x5FB11F, 0x8D5D08,
            0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3,
            0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880,
            0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B,
        };

        private static final int[] npio2_hw = {
            0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C,
            0x4025FDBB, 0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C,
            0x40346B9C, 0x4035FDBB, 0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A,
            0x403DD85A, 0x403F6A7A, 0x40407E4C, 0x4041475C, 0x4042106C, 0x4042D97C,
            0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB, 0x4046C6CB, 0x40478FDB,
            0x404858EB, 0x404921FB,
        };

        /*
         * invpio2:  53 bits of 2/pi
         * pio2_1:   first  33 bit of pi/2
         * pio2_1t:  pi/2 - pio2_1
         * pio2_2:   second 33 bit of pi/2
         * pio2_2t:  pi/2 - (pio2_1+pio2_2)
         * pio2_3:   third  33 bit of pi/2
         * pio2_3t:  pi/2 - (pio2_1+pio2_2+pio2_3)
         */

        private static final double
            invpio2 =  0x1.45f306dc9c883p-1,   // 6.36619772367581382433e-01
            pio2_1  =  0x1.921fb544p0,         // 1.57079632673412561417e+00
            pio2_1t =  0x1.0b4611a626331p-34,  // 6.07710050650619224932e-11
            pio2_2  =  0x1.0b4611a6p-34,       // 6.07710050630396597660e-11
            pio2_2t =  0x1.3198a2e037073p-69,  // 2.02226624879595063154e-21
            pio2_3  =  0x1.3198a2ep-69,        // 2.02226624871116645580e-21
            pio2_3t =  0x1.b839a252049c1p-104; // 8.47842766036889956997e-32

        static int __ieee754_rem_pio2(double x, double[] y) {
            double z = 0.0, w, t, r, fn;
            double[] tx = new double[3];
            int e0, i, j, nx, n, ix, hx;

            hx = __HI(x);           // high word of x
            ix = hx & EXP_SIGNIF_BITS;
            if (ix <= 0x3fe9_21fb) {   // |x| ~<= pi/4 , no need for reduction
                y[0] = x;
                y[1] = 0;
                return 0;
            }
            if (ix < 0x4002_d97c) {  // |x| < 3pi/4, special case with n=+-1
                if (hx > 0) {
                    z = x - pio2_1;
                    if (ix != 0x3ff9_21fb) {    // 33+53 bit pi is good enough
                        y[0] = z - pio2_1t;
                        y[1] = (z - y[0]) - pio2_1t;
                    } else {                // near pi/2, use 33+33+53 bit pi
                        z -= pio2_2;
                        y[0] = z - pio2_2t;
                        y[1] = (z-y[0])-pio2_2t;
                    }
                    return 1;
                } else {    // negative x
                    z = x + pio2_1;
                    if (ix != 0x3ff_921fb) {    // 33+53 bit pi is good enough
                        y[0] = z + pio2_1t;
                        y[1] = (z - y[0]) + pio2_1t;
                    } else {                // near pi/2, use 33+33+53 bit pi
                        z += pio2_2;
                        y[0] = z + pio2_2t;
                        y[1] = (z - y[0]) + pio2_2t;
                    }
                    return -1;
                }
            }
            if (ix <= 0x4139_21fb) { // |x| ~<= 2^19*(pi/2), medium size
                t  = Math.abs(x);
                n  = (int) (t*invpio2 + 0.5);
                fn = (double)n;
                r  = t - fn*pio2_1;
                w  = fn*pio2_1t;    // 1st round good to 85 bit
                if (n < 32 && ix != npio2_hw[n - 1]) {
                    y[0] = r - w;     // quick check no cancellation
                } else {
                    j = ix >> 20;
                    y[0] = r - w;
                    i = j - (((__HI(y[0])) >> 20) & 0x7ff);
                    if (i > 16) {  // 2nd iteration needed, good to 118
                        t  = r;
                        w  = fn*pio2_2;
                        r  = t - w;
                        w  = fn*pio2_2t - ((t - r) - w);
                        y[0] = r - w;
                        i = j - (((__HI(y[0])) >> 20) & 0x7ff);
                        if (i > 49)  { // 3rd iteration need, 151 bits acc
                            t  = r; // will cover all possible cases
                            w  = fn*pio2_3;
                            r  = t - w;
                            w  = fn*pio2_3t - ((t - r) - w);
                            y[0] = r - w;
                        }
                    }
                }
                y[1] = (r - y[0]) - w;
                if (hx < 0) {
                    y[0] = -y[0];
                    y[1] = -y[1];
                    return -n;
                } else {
                    return n;
                }
            }
            /*
             * all other (large) arguments
             */
            if (ix >= EXP_BITS) {            // x is inf or NaN
                y[0] = y[1] = x - x;
                return 0;
            }
            // set z = scalbn(|x|, ilogb(x)-23)
            z = __LO(z, __LO(x));
            e0 = (ix >> 20) - 1046;        // e0 = ilogb(z) - 23;
            z = __HI(z, ix - (e0 << 20));
            for (i=0; i < 2; i++) {
                tx[i] = (double)((int)(z));
                z     = (z - tx[i])*TWO24;
            }
            tx[2] = z;
            nx = 3;
            while (tx[nx - 1] == 0.0) { // skip zero term
                nx--;
            }
            n = KernelRemPio2.__kernel_rem_pio2(tx, y, e0, nx, 2, two_over_pi);
            if (hx < 0) {
                y[0] = -y[0];
                y[1] = -y[1];
                return -n;
            }
            return n;
        }
    }
    static final class KernelRemPio2 {
        /*
         * Constants:
         * The hexadecimal values are the intended ones for the following
         * constants. The decimal values may be used, provided that the
         * compiler will convert from decimal to binary accurately enough
         * to produce the hexadecimal values shown.
         */

        private static final int init_jk[] = {2, 3, 4, 6}; // initial value for jk

        private static final double PIo2[] = {
            0x1.921fb4p0,    // 1.57079625129699707031e+00
            0x1.4442dp-24,   // 7.54978941586159635335e-08
            0x1.846988p-48,  // 5.39030252995776476554e-15
            0x1.8cc516p-72,  // 3.28200341580791294123e-22
            0x1.01b838p-96,  // 1.27065575308067607349e-29
            0x1.a25204p-120, // 1.22933308981111328932e-36
            0x1.382228p-145, // 2.73370053816464559624e-44
            0x1.9f31dp-169,  // 2.16741683877804819444e-51
        };

        static final double
            twon24  = 0x1.0p-24; // 5.96046447753906250000e-08

        static int __kernel_rem_pio2(double[] x, double[] y, int e0, int nx, int prec, final int[] ipio2) {
            int jz, jx, jv, jp, jk, carry, n, i, j, k, m, q0, ih;
            int[] iq = new int[20];
            double z,fw;
            double [] f = new double[20];
            double [] fq= new double[20];
            double [] q = new double[20];

            // initialize jk
            jk = init_jk[prec];
            jp = jk;

            // determine jx, jv, q0, note that 3 > q0
            jx = nx - 1;
            jv = (e0 - 3)/24;
            if (jv < 0) {
                jv = 0;
            }
            q0 =  e0 - 24*(jv + 1);

            // set up f[0] to f[jx+jk] where f[jx+jk] = ipio2[jv+jk]
            j = jv - jx;
            m = jx + jk;
            for (i = 0; i <= m; i++, j++) {
                f[i] = (j < 0) ? 0.0 : (double) ipio2[j];
            }

            // compute q[0],q[1],...q[jk]
            for (i=0; i <= jk; i++) {
                for(j = 0, fw = 0.0; j <= jx; j++) {
                    fw += x[j]*f[jx + i - j];
                }
                q[i] = fw;
            }

            jz = jk;
            while(true) {
                // distill q[] into iq[] reversingly
                for(i=0, j=jz, z=q[jz]; j > 0; i++, j--) {
                    fw    =  (double)((int)(twon24* z));
                    iq[i] =  (int)(z - TWO24*fw);
                    z     =  q[j - 1] + fw;
                }

                // compute n
                z  = Math.scalb(z, q0);              // actual value of z
                z -= 8.0*Math.floor(z*0.125);        // trim off integer >= 8
                n  = (int) z;
                z -= (double)n;
                ih = 0;
                if (q0 > 0) {      // need iq[jz - 1] to determine n
                    i  = (iq[jz - 1] >> (24 - q0));
                    n += i;
                    iq[jz - 1] -= i << (24 - q0);
                    ih = iq[jz - 1] >> (23 - q0);
                } else if (q0 == 0) {
                    ih = iq[jz-1]>>23;
                } else if (z >= 0.5) {
                    ih=2;
                }

                if (ih > 0) {      // q > 0.5
                    n += 1;
                    carry = 0;
                    for (i=0; i < jz; i++) {        // compute 1-q
                        j = iq[i];
                        if (carry == 0) {
                            if (j != 0) {
                                carry = 1;
                                iq[i] = 0x100_0000 - j;
                            }
                        } else {
                            iq[i] = 0xff_ffff - j;
                        }
                    }
                    if (q0 > 0) {          // rare case: chance is 1 in 12
                        switch(q0) {
                        case 1:
                            iq[jz-1] &= 0x7f_ffff;
                            break;
                        case 2:
                            iq[jz-1] &= 0x3f_ffff;
                            break;
                        }
                    }
                    if (ih == 2) {
                        z = 1.0 - z;
                        if (carry != 0) {
                            z -= Math.scalb(1.0, q0);
                        }
                    }
                }

                // check if recomputation is needed
                if (z == 0.0) {
                    j = 0;
                    for (i = jz - 1; i >= jk; i--) {
                        j |= iq[i];
                    }
                    if (j == 0) { // need recomputation
                        for(k=1; iq[jk - k] == 0; k++);   // k = no. of terms needed

                        for(i = jz + 1; i <= jz + k; i++) {   // add q[jz+1] to q[jz+k]
                            f[jx + i] = (double) ipio2[jv + i];
                            for (j=0, fw = 0.0; j <= jx; j++) {
                                fw += x[j]*f[jx + i - j];
                            }
                            q[i] = fw;
                        }
                        jz += k;
                        continue;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            // chop off zero terms
            if (z == 0.0) {
                jz -= 1;
                q0 -= 24;
                while (iq[jz] == 0) {
                    jz--;
                    q0-=24;
                }
            } else { // break z into 24-bit if necessary
                z = Math.scalb(z, -q0);
                if (z >= TWO24) {
                    fw = (double)((int)(twon24*z));
                    iq[jz] = (int)(z - TWO24*fw);
                    jz += 1;
                    q0 += 24;
                    iq[jz] = (int) fw;
                } else {
                    iq[jz] = (int) z;
                }
            }

            // convert integer "bit" chunk to floating-point value
            fw = Math.scalb(1.0, q0);
            for(i = jz; i>=0; i--) {
                q[i] = fw*(double)iq[i];
                fw *= twon24;
            }

            // compute PIo2[0,...,jp]*q[jz,...,0]
            for(i = jz; i>=0; i--) {
                for (fw = 0.0, k = 0; k <= jp && k <= jz-i; k++) {
                    fw += PIo2[k]*q[i + k];
                }
                fq[jz - i] = fw;
            }

            // compress fq[] into y[]
            switch(prec) {
            case 0:
                fw = 0.0;
                for (i = jz; i >=0; i--) {
                    fw += fq[i];
                }
                y[0] = (ih == 0)? fw: -fw;
                break;
            case 1:
            case 2:
                fw = 0.0;
                for (i = jz; i>=0; i--) {
                    fw += fq[i];
                }
                y[0] = (ih == 0) ? fw: -fw;
                fw = fq[0] - fw;
                for (i = 1; i <= jz; i++) {
                    fw += fq[i];
                }
                y[1] = (ih == 0)? fw: -fw;
                break;
            case 3:     // painful
                for (i = jz; i > 0; i--) {
                    fw      = fq[i - 1] + fq[i];
                    fq[i]  += fq[i - 1] - fw;
                    fq[i - 1] = fw;
                }
                for (i = jz; i>1; i--) {
                    fw      = fq[i - 1] + fq[i];
                    fq[i]  += fq[i - 1] - fw;
                    fq[i-1] = fw;
                }
                for (fw = 0.0, i = jz; i >= 2; i--) {
                    fw += fq[i];
                }
                if (ih == 0) {
                    y[0] =  fq[0];
                    y[1] =  fq[1];
                    y[2] =  fw;
                } else {
                    y[0] = -fq[0];
                    y[1] = -fq[1];
                    y[2] = -fw;
                }
            }
            return n & 7;
        }
    }
    static final class Tan {
        private Tan() {throw new UnsupportedOperationException();}

        static double compute(double x) {
            double[] y = new double[2];
            double z = 0.0;
            int n, ix;

            // High word of x.
            ix = __HI(x);

            // |x| ~< pi/4
            ix &= EXP_SIGNIF_BITS;
            if (ix <= 0x3fe9_21fb) {
                return __kernel_tan(x, z, 1);
            } else if (ix >= EXP_BITS) { // tan(Inf or NaN) is NaN
                return x - x;            // NaN
            } else {           // argument reduction needed
                n = RemPio2.__ieee754_rem_pio2(x, y);
                return __kernel_tan(y[0], y[1], 1 - ((n & 1) << 1)); // 1 -- n even; -1 -- n odd
            }
        }

        /** __kernel_tan( x, y, k )
         * kernel tan function on [-pi/4, pi/4], pi/4 ~ 0.7854
         * Input x is assumed to be bounded by ~pi/4 in magnitude.
         * Input y is the tail of x.
         * Input k indicates whether tan (if k=1) or
         * -1/tan (if k= -1) is returned.
         *
         * Algorithm
         *      1. Since tan(-x) = -tan(x), we need only to consider positive x.
         *      2. if x < 2^-28 (hx < 0x3e300000 0), return x with inexact if x != 0.
         *      3. tan(x) is approximated by a odd polynomial of degree 27 on
         *         [0, 0.67434]
         *                               3             27
         *              tan(x) ~ x + T1*x + ... + T13*x
         *         where
         *
         *              |tan(x)         2     4            26   |     -59.2
         *              |----- - (1+T1*x +T2*x +.... +T13*x    )| <= 2
         *              |  x                                    |
         *
         *         Note: tan(x+y) = tan(x) + tan'(x)*y
         *                        ~ tan(x) + (1+x*x)*y
         *         Therefore, for better accuracy in computing tan(x+y), let
         *                   3      2      2       2       2
         *              r = x *(T2+x *(T3+x *(...+x *(T12+x *T13))))
         *         then
         *                                  3    2
         *              tan(x+y) = x + (T1*x + (x *(r+y)+y))
         *
         *      4. For x in [0.67434,pi/4],  let y = pi/4 - x, then
         *              tan(x) = tan(pi/4-y) = (1-tan(y))/(1+tan(y))
         *                     = 1 - 2*(tan(y) - (tan(y)^2)/(1+tan(y)))
         */
        private static final double
            pio4  =  0x1.921fb54442d18p-1,  // 7.85398163397448278999e-01
            pio4lo=  0x1.1a62633145c07p-55, // 3.06161699786838301793e-17
            T[] = {
             0x1.5555555555563p-2,  //  3.33333333333334091986e-01
             0x1.111111110fe7ap-3,  //  1.33333333333201242699e-01
             0x1.ba1ba1bb341fep-5,  //  5.39682539762260521377e-02
             0x1.664f48406d637p-6,  //  2.18694882948595424599e-02
             0x1.226e3e96e8493p-7,  //  8.86323982359930005737e-03
             0x1.d6d22c9560328p-9,  //  3.59207910759131235356e-03
             0x1.7dbc8fee08315p-10, //  1.45620945432529025516e-03
             0x1.344d8f2f26501p-11, //  5.88041240820264096874e-04
             0x1.026f71a8d1068p-12, //  2.46463134818469906812e-04
             0x1.47e88a03792a6p-14, //  7.81794442939557092300e-05
             0x1.2b80f32f0a7e9p-14, //  7.14072491382608190305e-05
            -0x1.375cbdb605373p-16, // -1.85586374855275456654e-05
             0x1.b2a7074bf7ad4p-16, //  2.59073051863633712884e-05
        };

        static double __kernel_tan(double x, double y, int iy) {
            double z, r, v, w, s;
            int ix, hx;
            hx = __HI(x);   // high word of x
            ix = hx & EXP_SIGNIF_BITS;     // high word of |x|
            if (ix < 0x3e30_0000) {  // x < 2**-28
                if ((int)x == 0) {   // generate inexact
                    if (((ix | __LO(x)) | (iy + 1)) == 0) {
                        return 1.0 / Math.abs(x);
                    } else {
                        if (iy == 1) {
                            return x;
                        } else {    // compute -1 / (x+y) carefully
                            double a, t;

                            z = w = x + y;
                            z= __LO(z, 0);
                            v = y - (z - x);
                            t = a = -1.0 / w;
                            t = __LO(t, 0);
                            s = 1.0 + t * z;
                            return t + a * (s + t * v);
                        }
                    }
                }
            }
            if (ix >= 0x3FE5_9428) {  // |x| >= 0.6744
                if ( hx < 0) {
                    x = -x;
                    y = -y;
                }
                z = pio4 - x;
                w = pio4lo - y;
                x = z + w;
                y = 0.0;
            }
            z       =  x*x;
            w       =  z*z;
            /* Break x^5*(T[1]+x^2*T[2]+...) into
             *    x^5(T[1]+x^4*T[3]+...+x^20*T[11]) +
             *    x^5(x^2*(T[2]+x^4*T[4]+...+x^22*[T12]))
             */
            r =    T[1] + w*(T[3] + w*(T[5] + w*(T[7] + w*(T[9]  + w*T[11]))));
            v = z*(T[2] + w*(T[4] + w*(T[6] + w*(T[8] + w*(T[10] + w*T[12])))));
            s = z*x;
            r = y + z*(s*(r + v) + y);
            r += T[0]*s;
            w = x + r;
            if (ix >= 0x3FE5_9428) {
                v = (double)iy;
                return (double)(1-((hx >> 30) & 2))*(v - 2.0*(x - (w*w/(w + v) - r)));
            }
            if (iy == 1) {
                return w;
            } else { /* if were to allow error up to 2 ulp,
                        could simply return -1.0/(x + r) here */
                //  compute -1.0/(x + r) accurately
                double a,t;
                z = w;
                z = __LO(z, 0);
                v = r - (z - x);   // z + v = r + x
                t = a  = -1.0/w;    // a = -1.0/w
                t = __LO(t, 0);
                s = 1.0 + t*z;
                return t + a*(s + t*v);
            }
        }
    }
    static final class Asin {
        private Asin() {throw new UnsupportedOperationException();}

        private static final double
            pio2_hi = 0x1.921fb54442d18p0,   //  1.57079632679489655800e+00
            pio2_lo = 0x1.1a62633145c07p-54, //  6.12323399573676603587e-17
            pio4_hi = 0x1.921fb54442d18p-1,  //  7.85398163397448278999e-01
        // coefficient for R(x^2)
            pS0 =  0x1.5555555555555p-3,     //  1.66666666666666657415e-01
            pS1 = -0x1.4d61203eb6f7dp-2,     // -3.25565818622400915405e-01
            pS2 =  0x1.9c1550e884455p-3,     //  2.01212532134862925881e-01
            pS3 = -0x1.48228b5688f3bp-5,     // -4.00555345006794114027e-02
            pS4 =  0x1.9efe07501b288p-11,    //  7.91534994289814532176e-04
            pS5 =  0x1.23de10dfdf709p-15,    //  3.47933107596021167570e-05
            qS1 = -0x1.33a271c8a2d4bp1,      // -2.40339491173441421878e+00
            qS2 =  0x1.02ae59c598ac8p1,      //  2.02094576023350569471e+00
            qS3 = -0x1.6066c1b8d0159p-1,     // -6.88283971605453293030e-01
            qS4 =  0x1.3b8c5b12e9282p-4;     //  7.70381505559019352791e-02

        static double compute(double x) {
            double t = 0, w, p, q, c, r, s;
            int hx, ix;
            hx = __HI(x);
            ix = hx & EXP_SIGNIF_BITS;
            if (ix >= 0x3ff0_0000) {           // |x| >= 1
                if(((ix - 0x3ff0_0000) | __LO(x)) == 0) {
                    // asin(1) = +-pi/2 with inexact
                    return x*pio2_hi + x*pio2_lo;
                }
                return (x - x)/(x - x);         // asin(|x| > 1) is NaN
            } else if (ix < 0x3fe0_0000) {     // |x| < 0.5
                if (ix < 0x3e40_0000) {        // if |x| < 2**-27
                    if (HUGE + x > 1.0) {// return x with inexact if x != 0
                        return x;
                    }
                } else {
                    t = x*x;
                }
                p = t*(pS0 + t*(pS1 + t*(pS2 + t*(pS3 + t*(pS4 + t*pS5)))));
                q = 1.0 + t*(qS1 + t*(qS2 + t*(qS3 + t*qS4)));
                w = p/q;
                return x + x*w;
            }
            // 1 > |x| >= 0.5
            w = 1.0 - Math.abs(x);
            t = w*0.5;
            p = t*(pS0 + t*(pS1 + t*(pS2 + t*(pS3 + t*(pS4 + t*pS5)))));
            q = 1.0 + t*(qS1 + t*(qS2 + t*(qS3 + t*qS4)));
            s = Math.sqrt(t);
            if (ix >= 0x3FEF_3333) {    // if |x| > 0.975
                w = p/q;
                t = pio2_hi - (2.0*(s + s*w) - pio2_lo);
            } else {
                w  = s;
                w  = __LO(w, 0);
                c  = (t - w*w)/(s + w);
                r  = p/q;
                p  = 2.0*s*r - (pio2_lo - 2.0*c);
                q  = pio4_hi - 2.0*w;
                t  = pio4_hi - (p - q);
            }
            return (hx > 0) ? t : -t;
        }
    }
    static final class Acos {
        private Acos() {throw new UnsupportedOperationException();}

        private static final double
            pio2_hi =  0x1.921fb54442d18p0,   //  1.57079632679489655800e+00
            pio2_lo =  0x1.1a62633145c07p-54, //  6.12323399573676603587e-17
            pS0     =  0x1.5555555555555p-3,  //  1.66666666666666657415e-01
            pS1     = -0x1.4d61203eb6f7dp-2,  // -3.25565818622400915405e-01
            pS2     =  0x1.9c1550e884455p-3,  //  2.01212532134862925881e-01
            pS3     = -0x1.48228b5688f3bp-5,  // -4.00555345006794114027e-02
            pS4     =  0x1.9efe07501b288p-11, //  7.91534994289814532176e-04
            pS5     =  0x1.23de10dfdf709p-15, //  3.47933107596021167570e-05
            qS1     = -0x1.33a271c8a2d4bp1,   // -2.40339491173441421878e+00
            qS2     =  0x1.02ae59c598ac8p1,   //  2.02094576023350569471e+00
            qS3     = -0x1.6066c1b8d0159p-1,  // -6.88283971605453293030e-01
            qS4     =  0x1.3b8c5b12e9282p-4;  //  7.70381505559019352791e-02

        static double compute(double x) {
            double z, p, q, r, w, s, c, df;
            int hx, ix;
            hx = __HI(x);
            ix = hx & EXP_SIGNIF_BITS;
            if (ix >= 0x3ff0_0000) {    // |x| >= 1
                if (((ix - 0x3ff0_0000) | __LO(x)) == 0) {  // |x| == 1
                    if (hx > 0) {// acos(1) = 0
                        return 0.0;
                    }else {       // acos(-1)= pi
                        return Math.PI + 2.0*pio2_lo;
                    }
                }
                return (x - x)/(x - x);         // acos(|x| > 1) is NaN
            }
            if (ix < 0x3fe0_0000) {     // |x| < 0.5
                if (ix <= 0x3c60_0000) {  // if |x| < 2**-57
                    return pio2_hi + pio2_lo;
                }
                z = x*x;
                p = z*(pS0 + z*(pS1 + z*(pS2 + z*(pS3 + z*(pS4 + z*pS5)))));
                q = 1.0 + z*(qS1 + z*(qS2 + z*(qS3 + z*qS4)));
                r = p/q;
                return pio2_hi - (x - (pio2_lo - x*r));
            } else if (hx < 0) {             // x < -0.5
                z = (1.0 + x)*0.5;
                p = z*(pS0 + z*(pS1 + z*(pS2 + z*(pS3 + z*(pS4 + z*pS5)))));
                q = 1.0 + z*(qS1 + z*(qS2 + z*(qS3 + z*qS4)));
                s = Math.sqrt(z);
                r = p/q;
                w = r*s - pio2_lo;
                return Math.PI - 2.0*(s+w);
            } else {                        // x > 0.5
                z = (1.0 - x)*0.5;
                s = Math.sqrt(z);
                df = s;
                df = __LO(df, 0);
                c  = (z - df*df)/(s + df);
                p = z*(pS0 + z*(pS1 + z*(pS2 + z*(pS3 + z*(pS4 + z*pS5)))));
                q = 1.0 + z*(qS1 + z*(qS2 + z*(qS3 + z*qS4)));
                r = p/q;
                w = r*s + c;
                return 2.0*(df + w);
            }
        }
    }
    static final class Atan {
        private Atan() {throw new UnsupportedOperationException();}

        private static final double atanhi[] = {
            0x1.dac670561bb4fp-2,  // atan(0.5)hi 4.63647609000806093515e-01
            0x1.921fb54442d18p-1,  // atan(1.0)hi 7.85398163397448278999e-01
            0x1.f730bd281f69bp-1,  // atan(1.5)hi 9.82793723247329054082e-01
            0x1.921fb54442d18p0,   // atan(inf)hi 1.57079632679489655800e+00
        };

        private static final double atanlo[] = {
            0x1.a2b7f222f65e2p-56, // atan(0.5)lo 2.26987774529616870924e-17
            0x1.1a62633145c07p-55, // atan(1.0)lo 3.06161699786838301793e-17
            0x1.007887af0cbbdp-56, // atan(1.5)lo 1.39033110312309984516e-17
            0x1.1a62633145c07p-54, // atan(inf)lo 6.12323399573676603587e-17
        };

        private static final double aT[] = {
             0x1.555555555550dp-2, //  3.33333333333329318027e-01
            -0x1.999999998ebc4p-3, // -1.99999999998764832476e-01
             0x1.24924920083ffp-3, //  1.42857142725034663711e-01
            -0x1.c71c6fe231671p-4, // -1.11111104054623557880e-01
             0x1.745cdc54c206ep-4, //  9.09088713343650656196e-02
            -0x1.3b0f2af749a6dp-4, // -7.69187620504482999495e-02
             0x1.10d66a0d03d51p-4, //  6.66107313738753120669e-02
            -0x1.dde2d52defd9ap-5, // -5.83357013379057348645e-02
             0x1.97b4b24760debp-5, //  4.97687799461593236017e-02
            -0x1.2b4442c6a6c2fp-5, // -3.65315727442169155270e-02
             0x1.0ad3ae322da11p-6, //  1.62858201153657823623e-02
        };

        static double compute(double x) {
            double w, s1, s2, z;
            int ix, hx, id;

            hx = __HI(x);
            ix = hx & EXP_SIGNIF_BITS;
            if (ix >= 0x4410_0000) {    // if |x| >= 2^66
                if (ix > EXP_BITS ||
                    (ix == EXP_BITS && (__LO(x) != 0))) {
                    return x+x;             // NaN
                }
                if (hx > 0) {
                    return atanhi[3] + atanlo[3];
                } else {
                    return -atanhi[3] - atanlo[3];
                }
            } if (ix < 0x3fdc_0000) {        // |x| < 0.4375
                if (ix < 0x3e20_0000) {      // |x| < 2^-29
                    if (HUGE + x > 1.0) { // raise inexact
                        return x;
                    }
                }
                id = -1;
            } else {
                x = Math.abs(x);
                if (ix < 0x3ff3_0000) {         // |x| < 1.1875
                    if (ix < 0x3fe60000) {      // 7/16 <= |x| < 11/16
                        id = 0;
                        x = (2.0*x - 1.0)/(2.0 + x);
                    } else {                    // 11/16 <= |x| < 19/16
                        id = 1;
                        x  = (x - 1.0)/(x + 1.0);
                    }
                } else {
                    if (ix < 0x4003_8000) {      // |x| < 2.4375
                        id = 2;
                        x  = (x - 1.5)/(1.0 + 1.5*x);
                    } else {                    // 2.4375 <= |x| < 2^66
                        id = 3;
                        x  = -1.0/x;
                    }
                }
            }
            // end of argument reduction
            z = x*x;
            w = z*z;
            // break sum from i=0 to 10 aT[i]z**(i+1) into odd and even poly
            s1 = z*(aT[0] + w*(aT[2] + w*(aT[4] + w*(aT[6] + w*(aT[8] + w*aT[10])))));
            s2 = w*(aT[1] + w*(aT[3] + w*(aT[5] + w*(aT[7] + w*aT[9]))));
            if (id < 0) {
                return x - x*(s1 + s2);
            } else {
                z = atanhi[id] - ((x*(s1+s2) - atanlo[id]) - x);
                return (hx < 0) ? -z: z;
            }
        }
    }
    static final class Atan2 {
        private Atan2() {throw new UnsupportedOperationException();}

        private static final double
            tiny    = 1.0e-300,
            pi_o_4  = 0x1.921fb54442d18p-1,  // 7.8539816339744827900E-01
            pi_o_2  = 0x1.921fb54442d18p0,   // 1.5707963267948965580E+00
            pi_lo   = 0x1.1a62633145c07p-53; // 1.2246467991473531772E-16

        static double compute(double y, double x) {
            double z;
            int k, m, hx, hy, ix, iy;
            /*unsigned*/ int lx, ly;

            hx = __HI(x);
            ix = hx & EXP_SIGNIF_BITS;
            lx = __LO(x);
            hy = __HI(y);
            iy = hy & EXP_SIGNIF_BITS;
            ly = __LO(y);
            if (Double.isNaN(x) || Double.isNaN(y))
                return x + y;
            if (((hx - 0x3ff0_0000) | lx) == 0) // x = 1.0
                return Math.atan(y);
            m = ((hy >> 31) & 1)|((hx >> 30) & 2);  // 2*sign(x) + sign(y)

            // when y = 0
            if ((iy | ly) == 0) {
                switch(m) {
                case 0:
                case 1: return y;               // atan(+/-0, +anything)  = +/-0
                case 2: return  Math.PI + tiny; // atan(+0,   -anything)  =  pi
                case 3: return -Math.PI - tiny; // atan(-0,   -anything)  = -pi
                }
            }
            // when x = 0
            if ((ix | lx) == 0) {
                return (hy < 0)?  -pi_o_2 - tiny : pi_o_2 + tiny;
            }

            // when x is INF
            if (ix == EXP_BITS) {
                if (iy == EXP_BITS) {
                    switch(m) {
                    case 0: return  pi_o_4 + tiny;      // atan(+INF, +INF)
                    case 1: return -pi_o_4 - tiny;      // atan(-INF, +INF)
                    case 2: return  3.0*pi_o_4 + tiny;  // atan(+INF, -INF)
                    case 3: return -3.0*pi_o_4 - tiny;  // atan(-INF, -INF)
                    }
                } else {
                    switch(m) {
                    case 0: return  0.0;                // atan(+..., +INF)
                    case 1: return -0.0;                // atan(-..., +INF)
                    case 2: return  Math.PI + tiny;     // atan(+..., -INF)
                    case 3: return -Math.PI - tiny;     // atan(-..., -INF)
                    }
                }
            }
            // when y is INF
            if (iy == EXP_BITS) {
                return (hy < 0)? -pi_o_2 - tiny : pi_o_2 + tiny;
            }

            // compute y/x
            k = (iy - ix) >> 20;
            if (k > 60) {   // |y/x| >  2**60
                z = pi_o_2+0.5*pi_lo;
            } else if (hx < 0 && k < -60) { // |y|/x < -2**60
                z = 0.0;
            } else { // safe to do y/x
                z = Math.atan(Math.abs(y/x));
            }
            switch (m) {
            case 0:  return  z;                     // atan(+, +)
            case 1:  return -z;                     // atan(-, +)
            case 2:  return  Math.PI - (z - pi_lo); // atan(+, -)
            default: return (z - pi_lo) - Math.PI;  // atan(-, -), case 3
            }
        }
    }
    static final class Expm1 {
        private static final double huge        =  1.0e+300;
        private static final double tiny        =  1.0e-300;
        private static final double o_threshold =  0x1.62e42fefa39efp9;   //  7.09782712893383973096e+02
        private static final double ln2_hi      =  0x1.62e42feep-1;       //  6.93147180369123816490e-01
        private static final double ln2_lo      =  0x1.a39ef35793c76p-33; //  1.90821492927058770002e-10
        private static final double invln2      =  0x1.71547652b82fep0;   //  1.44269504088896338700e+00
        // scaled coefficients related to expm1
        private static final double Q1          = -0x1.11111111110f4p-5;  // -3.33333333333331316428e-02
        private static final double Q2          =  0x1.a01a019fe5585p-10; //  1.58730158725481460165e-03
        private static final double Q3          = -0x1.4ce199eaadbb7p-14; // -7.93650757867487942473e-05
        private static final double Q4          =  0x1.0cfca86e65239p-18; //  4.00821782732936239552e-06
        private static final double Q5          = -0x1.afdb76e09c32dp-23; // -2.01099218183624371326e-07

        static double compute(double x) {
            double y, hi, lo, c=0, t, e, hxs, hfx, r1;
            int k, xsb;
            /*unsigned*/ int hx;

            hx  = __HI(x);  // high word of x
            xsb = hx & SIGN_BIT;                 // sign bit of x
            y = Math.abs(x);
            hx &= EXP_SIGNIF_BITS;               // high word of |x|

            // filter out huge and non-finite argument
            if (hx >= 0x4043_687A) {                  // if |x| >= 56*ln2
                if (hx >= 0x4086_2E42) {              // if |x| >= 709.78...
                    if (hx >= 0x7ff_00000) {
                        if (((hx & 0xf_ffff) | __LO(x)) != 0) {
                            return x + x;     // NaN
                        } else {
                            return (xsb == 0)? x : -1.0; // exp(+-inf)={inf,-1}
                        }
                    }
                    if (x > o_threshold) {
                        return huge*huge; // overflow
                    }
                }
                if (xsb != 0) { // x < -56*ln2, return -1.0 with inexact
                    if (x + tiny < 0.0) {         // raise inexact
                        return tiny - 1.0;        // return -1
                    }
                }
            }

            // argument reduction
            if (hx > 0x3fd6_2e42) {         // if  |x| > 0.5 ln2
                if (hx < 0x3FF0_A2B2) {     // and |x| < 1.5 ln2
                    if (xsb == 0) {
                        hi = x - ln2_hi;
                        lo = ln2_lo;
                        k =  1;
                    } else {
                        hi = x + ln2_hi;
                        lo = -ln2_lo;
                        k = -1;
                    }
                } else {
                    k  = (int)(invln2*x + ((xsb == 0) ? 0.5 : -0.5));
                    t  = k;
                    hi = x - t*ln2_hi;      // t*ln2_hi is exact here
                    lo = t*ln2_lo;
                }
                x  = hi - lo;
                c  = (hi - x) - lo;
            } else if (hx < 0x3c90_0000) {  // when |x| < 2**-54, return x
                t = huge + x; // return x with inexact flags when x != 0
                return x - (t - (huge + x));
            } else {
                k = 0;
            }

            // x is now in primary range
            hfx = 0.5*x;
            hxs = x*hfx;
            r1 = 1.0 + hxs*(Q1 + hxs*(Q2 + hxs*(Q3 + hxs*(Q4 + hxs*Q5))));
            t  = 3.0 - r1*hfx;
            e  = hxs *((r1 - t)/(6.0 - x*t));
            if (k == 0) {
                return x - (x*e - hxs);          // c is 0
            } else {
                e  = (x*(e - c) - c);
                e -= hxs;
                if (k == -1) {
                    return 0.5*(x - e) - 0.5;
                }
                if (k == 1) {
                    if (x < -0.25) {
                        return -2.0*(e - (x + 0.5));
                    } else {
                        return 1.0 + 2.0*(x - e);
                    }
                }
                if (k <= -2 || k > 56) {   // suffice to return exp(x) - 1
                    y = 1.0 - (e - x);
                    y = __HI(y, __HI(y) + (k << 20));     // add k to y's exponent
                    return y - 1.0;
                }
                t = 1.0;
                if (k < 20) {
                    t = __HI(t, 0x3ff0_0000 - (0x2_00000 >> k));  // t = 1-2^-k
                    y = t - ( e - x);
                    y = __HI(y, __HI(y) + (k << 20));     // add k to y's exponent
                } else {
                    t = __HI(t, ((0x3ff - k) << 20));     // 2^-k
                    y = x - (e + t);
                    y += 1.0;
                    y = __HI(y, __HI(y) + (k << 20));     // add k to y's exponent
                }
            }
            return y;
        }
    }
}
