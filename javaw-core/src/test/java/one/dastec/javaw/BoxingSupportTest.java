package one.dastec.javaw;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BoxingSupportTest {
 @Test void executesNullableWrappersAndLooseInvocationConversions() throws Exception {
  byte[] wasm = JavacToWasm.compile("BoxingApp.java", """
    class BoxCell { public Integer value = 8; public Long wide; }
    class BoxingApp {
      static Integer initial = 42;
      public static int initialized() { var cell = new BoxCell(); return initial + cell.value; }
      public static Integer boxed(int x) { return x; }
      public static long unboxed(Long x) { return x; }
      public static int pick(long x) { return 1; }
      public static int pick(Integer x) { return 2; }
      public static int loose(Integer x) { return x + 1; }
      public static int run() { Integer x = 2; Integer y = 3; var cell = new BoxCell(); cell.value = x; return cell.value + y + loose(4); }
      public static long wide() { Long x = 9007199254740993L; var cell = new BoxCell(); cell.wide = x; return cell.wide + 1L; }
      public static double floats() { Float x = 1.25f; Double y = 2.5; return x + y; }
      public static boolean logical() { Boolean x = true; return x && !Boolean.FALSE; }
      public static boolean cached() { Integer x = 127; Integer y = 127; Long a = 12L; Long b = 12L; return x == y && a == b; }
      public static boolean uncached() { Integer x = new Integer(1); Integer y = new Integer(1); return x == y; }
      public static int nullUnbox() { Integer x = null; try { int y = x; return y; } catch (NullPointerException e) { return 7; } }
      public static int ordering() { return pick(1); }
      public static int virtualNumber() { Number x = 4; return x.intValue(); }
      public static int arrays() { Integer[] a = new Integer[]{1, 2}; a[0] = 3; return a[0] + a[1]; }
      public static long mutation() { Long x = 9007199254740993L; Long old = x++; return old + x; }
      public static boolean values() { Integer x = new Integer(1000); Integer y = 1000; return x.equals(y) && !x.equals(null) && x.hashCode() == y.hashCode(); }
      public static boolean truths() { Boolean x = Boolean.TRUE; return x; }
      public static long textLong() { var x = Long.valueOf("123456"); return x + 1L; }
      public static double textFloating() { var x = Double.valueOf("2.5"); var y = Float.valueOf("1.25"); return x + y; }
      public static int textInteger() { var x = Integer.valueOf("ff", 16); var y = Short.valueOf("2"); var z = Byte.valueOf("3"); return x + y + z; }
      public static boolean textBoolean() { var x = Boolean.valueOf("true"); return x; }
      public static int accessors() { Integer x = Integer.valueOf(123); return x.intValue(); }
    }
    """, false).getWasm();
  OverloadResolutionTest.execute(wasm, """
    assert.equal(e.run(), 10);
    assert.equal(e.initialized(), 50);
    assert.equal(e.arrays(), 5);
    assert.equal(e.mutation(), 18014398509481987n);
    assert.equal(e.values(), 1);
    assert.equal(e.truths(), 1);
    assert.equal(e.wide(), 9007199254740994n);
    assert.equal(e.floats(), 3.75);
    assert.equal(e.logical(), 1);
    assert.equal(e.cached(), 1);
    assert.equal(e.uncached(), 0);
    assert.equal(e.nullUnbox(), 7);
    assert.equal(e.ordering(), 1);
    assert.equal(e.virtualNumber(), 4);
    assert.equal(e.accessors(), 123);
    assert.equal(e.textLong(), 123457n);
    assert.equal(e.textFloating(), 3.75);
    assert.equal(e.textInteger(), 260);
    assert.equal(e.textBoolean(), 1);
    """);
 }
}
