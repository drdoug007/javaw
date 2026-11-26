package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** LinkedHashMap and LinkedHashSet in the source compiler: insertion order matches javac on the JVM. */
class LinkedHashMapSupportTest {
    static final String PROGRAM = """
        import java.util.*;
        import java.util.function.*;

        class App {
            static String keys(Map<String, Integer> map) {
                StringBuilder out = new StringBuilder();
                for (String k : map.keySet()) out.append(k).append('=').append(map.get(k)).append(',');
                return out.toString();
            }
            public static String trace() {
                StringBuilder out = new StringBuilder();
                Map<String, Integer> m = new LinkedHashMap<String, Integer>();
                String[] names = {"zeta", "alpha", "mid", "beta", "omega", "gamma", "delta", "kappa"};
                for (int i = 0; i < names.length; i++) m.put(names[i], i);
                out.append(keys(m)).append('|');
                m.put("alpha", 100);                    // re-put keeps the position
                m.remove("mid");
                m.put("mid", 7);                        // re-insert goes last
                out.append(keys(m)).append('|');
                for (int i = 0; i < 60; i++) m.put("k" + i, i);   // growth keeps the order
                for (int i = 0; i < 60; i += 2) m.remove("k" + i);
                Iterator<String> it = m.keySet().iterator();
                while (it.hasNext()) { String k = it.next(); if (k.startsWith("k") && k.length() == 2) it.remove(); }
                out.append(keys(m)).append('|');
                StringBuilder values = new StringBuilder();
                for (Integer v : m.values()) values.append(v).append(' ');
                out.append(values).append('|');
                StringBuilder entries = new StringBuilder();
                for (Map.Entry<String, Integer> e : m.entrySet()) { if (e.getKey().equals("beta")) e.setValue(-1); entries.append(e.getKey()).append(':').append(e.getValue()).append(' '); }
                out.append(entries).append('|');
                BiConsumer<String, Integer> each = (k, v) -> out.append(k.charAt(0));
                m.forEach(each);
                out.append('|');
                Map<String, Integer> copy = new LinkedHashMap<String, Integer>(m);
                copy.putIfAbsent("first", 1); copy.computeIfAbsent("zeta", k -> 9); copy.merge("gamma", 10, (a, b) -> a + b);
                out.append(keys(copy)).append('|');
                m.clear(); m.put("again", 1); out.append(keys(m)).append('|');
                Set<Integer> set = new LinkedHashSet<Integer>();
                int[] order = {42, 7, 99, 7, 13, 42, 1000, -5};
                for (int v : order) set.add(v);
                set.remove(99);
                StringBuilder s = new StringBuilder();
                for (Integer v : set) s.append(v).append(' ');
                out.append(s).append('|');
                Set<Integer> setCopy = new LinkedHashSet<Integer>(set);
                setCopy.add(3);
                out.append(setCopy.size()).append(setCopy.contains(1000)).append('|');
                try { Iterator<String> bad = copy.keySet().iterator(); bad.next(); copy.put("new", 1); bad.next(); out.append("no CME"); }
                catch (ConcurrentModificationException ex) { out.append("CME"); }
                return out.toString();
            }
        }
        """;

    @Test
    void insertionOrderMatchesTheJvm() throws Exception {
        Path dir = Files.createDirectories(Path.of("target/linked-hash-map-test"));
        Path file = dir.resolve("App.java");
        Files.writeString(file, PROGRAM);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", dir.toString(), file.toString()));
        String expected;
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})) {
            Method trace = loader.loadClass("App").getDeclaredMethod("trace");
            trace.setAccessible(true);
            expected = (String) trace.invoke(null);
        }
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        assertNotNull(wasm);
        String script = """
            const {instantiateJavaw} = require(process.argv[1]);
            instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)))
              .then(({instance: {exports: e}, runtime: r}) => process.stdout.write(r.readWasmStringFromPtr(e.trace())))
              .catch(error => { console.error(error); process.exitCode = 1; });
            """;
        Process node = new ProcessBuilder("node", "-e", script, Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(30, TimeUnit.SECONDS), "Node execution timed out");
            String actual = new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, node.exitValue(), actual + new String(node.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            assertEquals(expected, actual);
        } finally {
            if (node.isAlive()) node.destroyForcibly();
        }
    }
}
