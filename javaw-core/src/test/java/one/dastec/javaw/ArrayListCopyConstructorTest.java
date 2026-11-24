package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@code new ArrayList<>(collection)} had no copy constructor in the source compiler, and a diamond whose target is a
 * generic supertype ({@code List<String> x = new ArrayList<>()}) inferred {@code ArrayList<Object>}; compared with the JVM.
 */
class ArrayListCopyConstructorTest {
    static String text() {
        List<String> base = new ArrayList<>();
        base.add("a"); base.add("b");
        List<String> copy = new ArrayList<>(base);
        copy.add("c");
        base.set(0, "z");
        HashSet<Integer> set = new HashSet<>();
        set.add(5);
        List<Integer> fromSet = new ArrayList<>(set);
        List<String> sized = new ArrayList<>(3);
        boolean npe = false;
        try { new ArrayList<String>((List<String>) null); } catch (NullPointerException e) { npe = true; }
        return copy.size() + ":" + copy.get(0) + copy.get(1) + copy.get(2) + ":" + base.get(0) + ":" + fromSet.get(0) + ":" + sized.size() + ":" + npe;
    }

    @Test
    void copyConstructorMatchesTheJvm() throws Exception {
        String program = """
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            class App {
                static String text() {
                    List<String> base = new ArrayList<>();
                    base.add("a"); base.add("b");
                    List<String> copy = new ArrayList<>(base);
                    copy.add("c");
                    base.set(0, "z");
                    HashSet<Integer> set = new HashSet<>();
                    set.add(5);
                    List<Integer> fromSet = new ArrayList<>(set);
                    List<String> sized = new ArrayList<>(3);
                    boolean npe = false;
                    try { new ArrayList<String>((List<String>) null); } catch (NullPointerException e) { npe = true; }
                    return copy.size() + ":" + copy.get(0) + copy.get(1) + copy.get(2) + ":" + base.get(0) + ":" + fromSet.get(0) + ":" + sized.size() + ":" + npe;
                }
                public static int test() { return text().equals("%s") ? 1 : 0; }
            }
            """.formatted(text());
        byte[] wasm = JavacToWasm.compile("App.java", program, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
}
