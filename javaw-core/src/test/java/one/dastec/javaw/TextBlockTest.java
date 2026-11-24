package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/** Text blocks outside component templates, against the JVM's values of the same blocks. */
class TextBlockTest {
    // The same text blocks as in the program below, evaluated by javac and the JVM.
    static final String PLAIN = """
        p {
          margin: 0;
        }
        """;
    static final String ESCAPES = """
        a\tb "quoted" \"""triple\""" \\2022 \
        joined\s
          kept  A
        end""";

    @Test
    void textBlocksMatchTheJvm() throws Exception {
        String program = """
            class App {
                static final String PLAIN = \"""
                    p {
                      margin: 0;
                    }
                    \""";
                public static String plain() { return PLAIN; }
                public static String escapes() {
                    String s = \"""
                        a\\tb "quoted" \\\"""triple\\\""" \\\\2022 \\
                        joined\\s
                          kept  A
                        end\""";
                    return s;
                }
                public static int test() {
                    int bad = 0;
                    if (!plain().equals(%s)) bad += 1;
                    if (!escapes().equals(%s)) bad += 2;
                    if (escapes().length() != %d) bad += 4;
                    if (\"""
                        x
                        \""".length() + plain().length() != %d) bad += 8;
                    return bad == 0 ? 1 : -bad;
                }
            }
            """.formatted(java(PLAIN), java(ESCAPES), ESCAPES.length(), "x\n".length() + PLAIN.length());
        byte[] wasm = JavacToWasm.compile("App.java", program, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }

    private static String java(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
