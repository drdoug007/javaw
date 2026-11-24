package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The injected Double/Float.toString source, compiled with javac and compared with the JDK on edge
 * cases and random bit patterns (Wasm execution is checked separately in FloatTextWasmTest).
 */
class FloatTextSupportTest {
    static final double[] DOUBLE_EDGES = {0.0, -0.0, 1.0, -1.0, 12.5, 0.5, 0.05, 0.1, 0.2, 0.3, 1e7, 9999999.0, 9999999.999999999,
            1e-3, 0.001, 9.999999999999998E-4, 1e-4, 123456.789, 2.0 / 3, 1e23, 2e23, 8.41e21, 5e-324, 1e-323, Double.MIN_VALUE,
            Double.MIN_NORMAL, Double.MAX_VALUE, Math.nextDown(Double.MIN_NORMAL), Math.PI, Math.E, 100.0, 1e21, 1e22,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 4.35, 2.82879384806159E17, 1.18575755E-316,
            2.2250738585072014E-308, 9.007199254740993E15, 9.007199254740992E15, 1.0E-5, 0.1 + 0.2, 1.7976931348623157E308};
    static final float[] FLOAT_EDGES = {0f, -0f, 1f, 0.1f, 1.1f, 12.5f, 1e7f, 9999999f, 1e-3f, 1e-4f, Float.MIN_VALUE, Float.MIN_NORMAL,
            Float.MAX_VALUE, 3.4e38f, 1.4e-45f, 3.1415927f, 16777216f, 16777217f, 2.0f / 3, 8.0e-3f, 1.0e10f, Float.NaN,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.3f, 100f};

    @Test
    void matchesTheJdkOnEdgesAndRandomBits() throws Exception {
        Path dir = Files.createDirectories(Path.of("target/float-text-test"));
        Path file = dir.resolve(FloatTextSupport.CLASS + ".java");
        Files.writeString(file, FloatTextSupport.SOURCE);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", dir.toString(), file.toString()));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})) {
            Class<?> type = loader.loadClass(FloatTextSupport.CLASS);
            Method f64 = type.getDeclaredMethod("__javaw_f64_text", long.class);
            Method f32 = type.getDeclaredMethod("__javaw_f32_text", int.class);
            f64.setAccessible(true);
            f32.setAccessible(true);
            for (double v : DOUBLE_EDGES) checkDouble(f64, Double.doubleToRawLongBits(v));
            for (float v : FLOAT_EDGES) checkFloat(f32, Float.floatToRawIntBits(v));
            SplittableRandom random = new SplittableRandom(20261121);
            for (int i = 0; i < 1_000_000; i++) checkDouble(f64, random.nextLong());
            // Short decimals, where ties and the length-1-or-2 rule matter most.
            for (int i = 0; i < 300_000; i++) checkDouble(f64, Double.doubleToRawLongBits(random.nextInt(100_000) * Math.pow(10, random.nextInt(-330, 310))));
            for (int i = 0; i < 1_000_000; i++) checkFloat(f32, random.nextInt());
            for (int bits = 0; bits < 200_000; bits++) checkFloat(f32, bits); // subnormals
        }
    }

    private static void checkDouble(Method f64, long bits) throws Exception {
        assertEquals(Double.toString(Double.longBitsToDouble(bits)), f64.invoke(null, bits), () -> "bits " + Long.toHexString(bits));
    }

    private static void checkFloat(Method f32, int bits) throws Exception {
        assertEquals(Float.toString(Float.intBitsToFloat(bits)), f32.invoke(null, bits), () -> "bits " + Integer.toHexString(bits));
    }
}
