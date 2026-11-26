package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** StringBuilder.append(Object) follows String.valueOf: boxes, builders, char[] and null append their text. */
class StringBuilderAppendObjectTest {
    @Test
    void referenceArgumentsAppendTheirText() throws Exception {
        String[][] cases = {
            {"Integer x = 5; return new StringBuilder().append(x).toString();", "5"},
            {"Map<String,Integer> m = new HashMap<String,Integer>(); m.put(\"a\", 5); return new StringBuilder().append(m.get(\"a\")).toString();", "5"},
            {"Object o = Integer.valueOf(5); return new StringBuilder().append(o).toString();", "5"},
            {"StringBuilder a = new StringBuilder(\"q\"); return new StringBuilder().append(a).append(a).toString();", "qq"},
            {"Integer n = null; Object o = null; return new StringBuilder().append(n).append('/').append(o).toString();", "null/null"},
            {"char[] cs = {'h','i'}; return new StringBuilder().append(cs).append(7).append((short) 3).append(-2L).toString();", "hi73-2"},
            {"List<Integer> l = new ArrayList<Integer>(); l.add(5); l.add(-6); StringBuilder b = new StringBuilder(); for (Integer v : l) b.append(v).append(' '); return b.toString();", "5 -6 "},
            {"Map<String,Integer> m = new HashMap<String,Integer>(); m.put(\"a\", 5); StringBuilder b = new StringBuilder(); for (Map.Entry<String,Integer> e : m.entrySet()) b.append(e.getKey()).append(e.getValue()).append(\"/\" + e.getValue()); return b.toString();", "a5/5"},
            {"StringBuilder other = null; return new StringBuilder(\"x\").append(other).toString();", "xnull"},
            {"Object o = \"s\"; int n = 3; return new StringBuilder().append(o instanceof String).append(n > 2).append(!(n == 3)).append(\"/\" + (o instanceof Integer)).toString();", "truetruefalse/false"},
            {"Double d = 1.5; Boolean t = true; Character c = 'z'; return new StringBuilder().append(d).append(t).append(c).toString();", "1.5truez"},
        };
        for (String[] c : cases) {
            byte[] wasm = JavacToWasm.compile("App.java", "import java.util.*;\nclass App { public static String t() { " + c[0] + " } }", false).getWasm();
            assertNotNull(wasm, c[0]);
            String script = "const {instantiateJavaw} = require(process.argv[1]); instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)))"
                    + ".then(({instance:{exports:e},runtime:r}) => process.stdout.write(r.readWasmStringFromPtr(e.t()))).catch(err => { console.error(err); process.exitCode = 1; });";
            Process node = new ProcessBuilder("node", "-e", script, Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).redirectErrorStream(true).start();
            try {
                try (var in = node.getOutputStream()) { in.write(wasm); }
                assertTrue(node.waitFor(30, TimeUnit.SECONDS));
                assertEquals(c[1], new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8), c[0]);
            } finally {
                if (node.isAlive()) node.destroyForcibly();
            }
        }
    }
}
