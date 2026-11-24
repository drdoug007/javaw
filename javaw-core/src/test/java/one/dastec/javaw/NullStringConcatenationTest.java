package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/** JLS 15.18.1: null String operands of + and += convert to "null" (values checked against the JVM). */
class NullStringConcatenationTest {
    @Test
    void nullStringOperandsRenderAsNull() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            class Holder {
                public String text;
                public String built;
                public boolean flag = true;
                public char letter = 'z';
                public Holder(String start) { built = "start=" + start; }
            }
            class App {
                public static String none;
                static String nothing() { return null; }
                public static int test() {
                    String n = null;
                    if (!("a" + n).equals("anull")) return -1;
                    if (!(n + "b").equals("nullb")) return -2;
                    if (!(n + n).equals("nullnull")) return -3;
                    if (!("x" + nothing() + "y").equals("xnully")) return -4;
                    Holder h = new Holder(null);
                    if (!h.built.equals("start=null")) return -6;
                    String acc = null;
                    acc += "!";
                    if (!acc.equals("null!")) return -7;
                    String tail = "t";
                    tail += n;
                    if (!tail.equals("tnull")) return -8;
                    none += "1";
                    if (!none.equals("null1")) return -9;
                    String empty = "";
                    if (!(empty + n).equals("null")) return -10;
                    if (!("v=" + (n == null ? null : "set")).equals("v=null")) return -11;
                    if (!("a" + null).equals("anull")) return -12;
                    if (!(null + "b").equals("nullb")) return -13;
                    if (!("c" + (Object) null).equals("cnull")) return -14;
                    Holder flags = new Holder("f");
                    if (!("b=" + flags.flag + "," + flags.letter + "," + (1 > 2)).equals("b=true,z,false")) return -16;
                    String plus = "p";
                    plus += null;
                    if (!plus.equals("pnull")) return -15;
                    return 1;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
}
