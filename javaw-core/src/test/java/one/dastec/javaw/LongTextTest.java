package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/**
 * {@code String.valueOf(long)} and {@code StringBuilder.append(long)} used the 32-bit conversion (invalid
 * WAT for a long argument), against the JVM's text of the same values.
 */
class LongTextTest {
    static final class P {
        private final long id;
        P(long id) { this.id = id; }
        public long id() { return id; }
    }

    static String text(long a) {
        return String.valueOf(9007199254740993L) + "|" + String.valueOf(new P(a).id()) + "|"
                + new StringBuilder().append(a).append(',').append(-a).append(',').append(Long.MIN_VALUE).toString();
    }

    @Test
    void longTextMatchesTheJvm() throws Exception {
        String program = """
            class P {
                private final long id;
                P(long id) { this.id = id; }
                public long id() { return this.id; }
            }
            class App {
                static String text(long a) {
                    return String.valueOf(9007199254740993L) + "|" + String.valueOf(new P(a).id()) + "|"
                            + new StringBuilder().append(a).append(',').append(-a).append(',').append(Long.MIN_VALUE).toString();
                }
                public static int test() {
                    int bad = 0;
                    if (!text(%dL).equals("%s")) bad += 1;
                    if (!text(%dL).equals("%s")) bad += 2;
                    if (!text(0L).equals("%s")) bad += 4;
                    return bad == 0 ? 1 : -bad;
                }
            }
            """.formatted(4294967296123L, text(4294967296123L), -7L, text(-7L), text(0L));
        byte[] wasm = JavacToWasm.compile("App.java", program, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
}
