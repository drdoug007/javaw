package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Enhanced for over a method call whose arguments include char or floating-point literals: the loop must
 * use the call's Iterable result (it was lowered as an array loop), compared with the JVM.
 */
class ForEachCallArgumentTest {
    static final String SOURCE = """
        import java.util.ArrayList;
        import java.util.List;
        class App {
            static List<String> split(String csv, char separator) {
                List<String> out = new ArrayList<String>();
                String current = "";
                for (int i = 0; i < csv.length(); i++) {
                    char c = csv.charAt(i);
                    if (c == separator) { out.add(current); current = ""; } else current = current + c;
                }
                out.add(current);
                return out;
            }
            static List<Integer> scaled(double factor, float bias) {
                List<Integer> out = new ArrayList<Integer>();
                for (int i = 1; i <= 3; i++) out.add((int) (i * factor + bias));
                return out;
            }
            public static int test() {
                int total = 0;
                for (String part : split("ab|c|def", '|')) total = total * 10 + part.length();
                for (Integer n : scaled(2.5, 0.5f)) total = total * 10 + n;
                return total;
            }
        }
        """;

    static int jvm() {
        int total = 0;
        for (String part : "ab|c|def".split("\\|")) total = total * 10 + part.length();
        List<Integer> scaled = new ArrayList<>();
        for (int i = 1; i <= 3; i++) scaled.add((int) (i * 2.5 + 0.5f));
        for (Integer n : scaled) total = total * 10 + n;
        return total;
    }

    @Test
    void loopsOverTheCallResult() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", SOURCE, false).getWasm();
        assertEquals(213358, jvm());
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), " + jvm() + ");");
    }
}
