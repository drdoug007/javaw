package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/** String.indexOf(str, fromIndex) and indexOf(char, fromIndex), against JVM results. */
class StringIndexOfTest {
    @Test
    void indexOfHonoursTheStartIndex() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            class App {
                public static int test() {
                    String s = "a,b,,cd,";
                    int bad = 0;
                    if (s.indexOf(",", 0) != 1) bad += 1;
                    if (s.indexOf(",", 2) != 3) bad += 2;
                    if (s.indexOf(",", 4) != 4) bad += 4;
                    if (s.indexOf(",", 8) != -1) bad += 8;
                    if (s.indexOf(",", -3) != 1) bad += 16;
                    if (s.indexOf("cd", 5) != 5) bad += 32;
                    if (s.indexOf("cd", 6) != -1) bad += 64;
                    if (s.indexOf(',', 5) != 7) bad += 128;
                    if (s.indexOf("", 3) != 3) bad += 256;
                    if (s.indexOf("", 99) != 8) bad += 512;
                    // Nested string operations in the operands.
                    String t = "zab", u = "xyz";
                    if ("xxabcab".indexOf(t.substring(1), 3) != 5) bad += 2048;
                    if ("abcb".indexOf("b", u.substring(1).length()) != 3) bad += 4096;
                    if ("abab".indexOf("b", "qa".indexOf("a", 1)) != 1) bad += 8192;
                    int count = 0, at = 0;
                    at = s.indexOf(",", at);
                    while (at >= 0) { count++; at = s.indexOf(",", at + 1); }
                    if (count != 4) bad += 1024;
                    return bad == 0 ? 1 : -bad;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
}
