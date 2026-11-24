package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.StringJoiner;

/** Double/Float text in Wasm on every formatting path, against JDK 21 output. */
class FloatTextWasmTest {
    @Test
    void everyPathFormatsLikeTheJvm() throws Exception {
        SplittableRandom random = new SplittableRandom(7);
        StringJoiner doubles = new StringJoiner(", "), doubleText = new StringJoiner(", ");
        StringJoiner floats = new StringJoiner(", "), floatText = new StringJoiner(", ");
        java.util.List<Long> dbits = new java.util.ArrayList<>();
        for (double v : FloatTextSupportTest.DOUBLE_EDGES) dbits.add(Double.doubleToRawLongBits(v));
        for (int i = 0; i < 200; i++) dbits.add(random.nextLong());
        for (int i = 0; i < 100; i++) dbits.add(Double.doubleToRawLongBits(random.nextInt(1000) / 8.0));
        for (long b : dbits) { doubles.add("Double.longBitsToDouble(" + b + "L)"); doubleText.add(quote(Double.toString(Double.longBitsToDouble(b)))); }
        java.util.List<Integer> fbits = new java.util.ArrayList<>();
        for (float v : FloatTextSupportTest.FLOAT_EDGES) fbits.add(Float.floatToRawIntBits(v));
        for (int i = 0; i < 100; i++) fbits.add(random.nextInt());
        for (int b : fbits) { floats.add("Float.intBitsToFloat(" + b + ")"); floatText.add(quote(Float.toString(Float.intBitsToFloat(b)))); }
        byte[] wasm = JavacToWasm.compile("App.java", """
            class App {
                /** 0 when every path matches; otherwise a code naming the path and index. */
                public static int test() {
                    double[] d = {%s};
                    String[] dt = {%s};
                    float[] f = {%s};
                    String[] ft = {%s};
                    for (int i = 0; i < d.length; i++) {
                        double v = d[i];
                        if (!("" + v).equals(dt[i])) return 100000 + i;
                        if (!String.valueOf(v).equals(dt[i])) return 200000 + i;
                        if (!Double.toString(v).equals(dt[i])) return 300000 + i;
                        StringBuilder sb = new StringBuilder();
                        sb.append(v);
                        if (!sb.toString().equals(dt[i])) return 400000 + i;
                        Double boxed = v;
                        if (!("" + boxed).equals(dt[i])) return 500000 + i;
                    }
                    for (int i = 0; i < f.length; i++) {
                        float v = f[i];
                        if (!("" + v).equals(ft[i])) return 600000 + i;
                        if (!String.valueOf(v).equals(ft[i])) return 700000 + i;
                        if (!Float.toString(v).equals(ft[i])) return 800000 + i;
                        StringBuilder sb = new StringBuilder("x");
                        sb.append(v);
                        sb.insert(0, v);
                        if (!sb.toString().equals(ft[i] + "x" + ft[i])) return 900000 + i;
                        Float boxed = v;
                        if (!("" + boxed).equals(ft[i])) return 1000000 + i;
                    }
                    // StringBuilder mutators used as statements drop the returned builder.
                    StringBuilder r = new StringBuilder("abc");
                    r.reverse();
                    r.deleteCharAt(0);
                    r.insert(0, "x");
                    r.delete(0, 1);
                    r.replace(0, 1, "z");
                    if (!r.toString().equals("za")) return -1;
                    return 0;
                }
            }
            """.formatted(doubles, doubleText, floats, floatText), false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 0);");
    }

    private static String quote(String s) {
        return '"' + s + '"';
    }
}
