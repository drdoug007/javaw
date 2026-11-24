package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/** Stores into arrays reached through another object's field use the element's width (values match the JVM). */
class ArrayFieldStoreTest {
    @Test
    void storesThroughFieldArraysUseElementWidth() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            class Slots {
                double[] numbers = new double[3];
                long[] wide = new long[2];
                boolean[] flags = new boolean[2];
                int[] counts = new int[2];
            }
            class App {
                public static int test() {
                    Slots s = new Slots();
                    Slots t = s;
                    t.numbers[1] = 2.5;
                    t.numbers[2] = t.numbers[1] * 2;
                    t.numbers[0] += 0.25;
                    if (s.numbers[0] != 0.25 || s.numbers[1] != 2.5 || s.numbers[2] != 5.0) return -1;
                    t.wide[1] = 1L << 40;
                    if (s.wide[1] != 1099511627776L || s.wide[0] != 0L) return -2;
                    t.flags[1] = true;
                    t.counts[0] = 7;
                    if (s.flags[0] || !s.flags[1] || s.counts[0] != 7 || s.counts[1] != 0) return -3;
                    return 1;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
}
