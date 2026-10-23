package com.example.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumericConformanceTest {
    @Test void executesMutationsCastsAndPromotions() throws Exception {
        byte[] wasm = JavacToWasm.compile("NumericApp.java", """
            class NumericCell { public long wide; public double decimal; public byte small; }
            class NumericApp {
                public static long longMutation() { long x = 9007199254740993L; long old = x++; long next = --x; return old + next; }
                public static double floatMutation() { float x = 1.25f; float old = x++; return old + --x; }
                public static double fieldMutation() { var c = new NumericCell(); c.decimal = 1.5; double old = c.decimal++; return old + c.decimal; }
                public static long wideField() { var c = new NumericCell(); c.wide = 9007199254740993L; return ++c.wide; }
                public static int arrayMutation() { int[] a = new int[]{3, 8}; int i = 0; int old = a[i++]++; return old + a[0] + i; }
                public static int narrowMutation() { byte x = 127; byte old = x++; char c = 65535; ++c; short s = -32768; --s; return x + old + c + s; }
                public static int casts() { return (byte)255 + (short)65535 + (char)-1; }
                public static int saturating(double x) { return (int)x; }
                public static long saturatingLong(double x) { return (long)x; }
                public static double unaryWidth() { int x = -2147483648; return -x; }
                public static float roundedFloat() { var x = 16777216f + 1f; return x; }
                public static double ternaryWidth(boolean flag) { return (flag ? 2 : 3.5) + 0.25; }
                public static int compound() { int x = 3; x += 2.75; byte b = 127; b += 2; return x + b; }
                public static int div(int x, int y) { return x / y; }
                public static long longDiv(long x, long y) { return x / y; }
                public static int zeroDivision() { try { return 1 / 0; } catch (ArithmeticException error) { return 7; } }
                public static double rem(double x, double y) { return x % y; }
                public static float remFloat(float x, float y) { return x % y; }
                public static long charPromotion() { return 1L + 'a'; }
                public static double hexPromotion() { return 0xffffffff; }
                public static boolean floatComparison() { return 16777216f + 1f == 16777216f; }
                public static int kind(byte x) { return 1; }
                public static int kind(int x) { return 2; }
                public static int constantNarrow(boolean flag) { byte x = 1; final int delta = 2; var y = flag ? x : ((delta << 2) + 3); return kind(y); }
                public static int notNarrow(boolean flag) { byte x = 1; int delta = 2; var y = flag ? x : delta + 3; return kind(y); }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.compound(), -122);
            assert.equal(e.div(-2147483648, -1), -2147483648);
            assert.equal(e.longDiv(-9223372036854775808n, -1n), -9223372036854775808n);
            assert.equal(e.zeroDivision(), 7);
            assert.equal(e.charPromotion(), 98n);
            assert.equal(e.hexPromotion(), -1);
            assert.equal(e.floatComparison(), 1);
            for (const x of [0, -0, 1, -1, 5.5, -5.5, Number.MIN_VALUE, Number.MAX_VALUE, NaN, Infinity, -Infinity]) {
              for (const y of [0, -0, 2.25, -2.25, Number.MIN_VALUE, Number.MAX_VALUE, NaN, Infinity]) {
                assert.ok(Object.is(e.rem(x, y), x % y), `${x} % ${y}`);
                assert.ok(Object.is(e.remFloat(x, y), Math.fround(Math.fround(x) % Math.fround(y))));
              }
            }
            assert.equal(e.longMutation(), 18014398509481986n);
            assert.equal(e.floatMutation(), 2.5);
            assert.equal(e.fieldMutation(), 4);
            assert.equal(e.wideField(), 9007199254740994n);
            assert.equal(e.arrayMutation(), 8);
            assert.equal(e.narrowMutation(), 32766);
            assert.equal(e.casts(), 65533);
            assert.equal(e.saturating(NaN), 0);
            assert.equal(e.saturating(Infinity), 2147483647);
            assert.equal(e.saturating(-Infinity), -2147483648);
            assert.equal(e.saturatingLong(Infinity), 9223372036854775807n);
            assert.equal(e.unaryWidth(), -2147483648);
            assert.equal(e.roundedFloat(), 16777216);
            assert.equal(e.ternaryWidth(0), 3.75);
            assert.equal(e.ternaryWidth(1), 2.25);
            assert.equal(e.constantNarrow(0), 1);
            assert.equal(e.notNarrow(0), 2);
            """);
    }
}
