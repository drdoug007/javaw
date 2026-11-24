package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Field initializers and initializer blocks (JLS 8.3.2, 8.6, 8.7, 12.4.2, 12.5). The program returns 1 on the
 * JVM (javac 21); every check below was confirmed there.
 */
class FieldInitializerTest {
    static final String PROGRAM = """
import java.util.*;
import java.util.function.*;

class Base {
    public int base = 10;
    public int seenBySuper;
    public Base() { seenBySuper = probe(); }
    public int probe() { return -1; }
}
class Child extends Base {
    public int child = base + 5;
    public int[] grid = {1, 2, 3};
    public List<String> names = new ArrayList<String>();
    public int x = 5;
    public int y = x + 1;
    public static int built;
    public String trace = "";
    { trace = trace + "block;"; names.add("a"); }
    public Supplier<Integer> twice = () -> child * 2;
    public Child() { this(0); trace = trace + "noarg;"; }
    public Child(int x) { trace = trace + "ctor" + x + ";"; built++; }
    public int probe() { return child; }
}
class Plain { public int n = 3; public String s = "x"; }
enum Level { LOW, HIGH; public int weight = 7; }
class Later { public static int y = 41; }
class Early { public static int x = Later.y + 1; }
class Statics {
    public static final int CONSTANT = 9;
    public static int b = Statics.a + 1;
    public static int a = 1;
    public static String label = "init:" + CONSTANT;
    public static List<Integer> list = new ArrayList<Integer>();
    public static Function<Integer, Integer> inc = v -> v + 1;
    public static int fromMethod = compute();
    public static int order;
    static { list.add(4); order = list.size() * 10 + fromMethod; }
    static int compute() { return 6; }
}
public class Program {
    public static int test() {
        Child c = new Child();
        if (c.child != 15) return -1;
        if (c.grid[2] != 3 || c.grid.length != 3) return -2;
        if (c.names.size() != 1 || !c.names.get(0).equals("a")) return -3;
        if (c.y != 6) return -4;
        if (!c.trace.equals("block;ctor0;noarg;")) return -5;
        if (Child.built != 1) return -6;
        if (c.twice.get() != 30) return -7;
        if (c.seenBySuper != 0) return -8;
        if (c.base != 10) return -9;
        Plain p = new Plain();
        if (p.n != 3 || !p.s.equals("x")) return -10;
        if (Level.HIGH.weight != 7) return -11;
        if (Early.x != 42) return -12;
        if (Statics.b != 1 || Statics.a != 1) return -13;
        if (!Statics.label.equals("init:9")) return -14;
        if (Statics.inc.apply(4) != 5) return -15;
        if (Statics.order != 16) return -16;
        if (Statics.list.get(0) != 4) return -17;
        return 1;
    }
}
""";

    @Test
    void initializersRunInJavaOrder() throws Exception {
        OverloadResolutionTest.execute(JavacToWasm.compile("Program.java", PROGRAM, false).getWasm(), "assert.equal(e.test(), 1);");
    }

    @Test
    void staticInitializationOutsideClassesIsDiagnosed() {
        var record = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java",
                "record R(int v) { static String NAME = \"r\"; }", false));
        assertTrue(record.getMessage().contains("Static initialization in records"), record.getMessage());
        var iface = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java",
                "interface Names { String NAME = \"n\"; }", false));
        assertTrue(iface.getMessage().contains("Static initialization in interfaces"), iface.getMessage());
    }
}
