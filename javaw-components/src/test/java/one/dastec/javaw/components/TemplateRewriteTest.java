package one.dastec.javaw.components;

import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M3 rewrite on the JVM: the rewritten source is written under target/generated-sources and compiled
 * with javac against the public API, so the Parts overloads accept exactly what javaw-core accepts.
 */
class TemplateRewriteTest {
    static final String SOURCE = """
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.Event;

        @CustomElement("x-rw")
        public class Rw extends WebComponent {
            enum Tone { LOW, HIGH }
            private int n;
            private Integer boxed = 3;
            private Tone tone = Tone.LOW;
            private String s = "s";
            @Override protected Template render() {
                return html(\"""
                    <p class="a ${s} ${tone} b" data-n=${n} ?hidden=${n > 2} .title=${s}
                       @click=${() -> n++} @input=${e -> s = e.type()} @focus=${this::poke}>
                      ${s} ${n} ${'c'} ${2.5} ${boxed} ${tone} ${null} ${n > 0 ? html("<b>${n}</b>") : null}
                    </p>
                    \""");
            }
            private void poke() { requestUpdate(); }
        }
        """;

    @Test
    void rewrittenSourceCompilesWithJavac() throws Exception {
        Map<String, String> sections = new LinkedHashMap<>();
        String rewritten = ComponentExtensionAccess.rewriteUserTypes(SOURCE, sections);
        Path dir = Path.of("target/generated-sources/javaw-components/rewrite-test");
        Files.createDirectories(dir);
        Path file = dir.resolve("Rw.java");
        Files.writeString(file, rewritten);

        assertTrue(rewritten.contains("Template.of(1, Parts.attr(s), Parts.attr(tone), Parts.attr(n), Parts.bool(n > 2), Parts.prop(s), "
                + "Parts.onMouse(() -> n++), Parts.on(e -> s = e.type()), Parts.onFocus(this::poke), Parts.child(s), Parts.child(n), "
                + "Parts.child('c'), Parts.child(2.5), Parts.child(boxed), Parts.child(tone), Parts.childNothing(), "
                + "Parts.child(n > 0 ? Template.of(2, Parts.child(n)) : null))"), rewritten);
        assertFalse(rewritten.contains("html("), rewritten);
        String templates = sections.get("javaw_wc_templates");
        assertNotNull(templates, sections.toString());
        assertTrue(templates.contains("\"html\":\"<b><!--wc$0--></b>\""), templates);
        assertTrue(templates.contains("{\"kind\":\"attr\",\"name\":\"class\",\"bindings\":2}"), templates);
        assertTrue(templates.contains("{\"kind\":\"event\",\"name\":\"click\",\"bindings\":1}"), templates);
        assertTrue(templates.contains("wc$0=\\\"a \\u0001wc\\u0001 \\u0001wc\\u0001 b\\\""), templates);

        var javac = ToolProvider.getSystemJavaCompiler();
        String classpath = Path.of("target/classes").toAbsolutePath() + java.io.File.pathSeparator
                + Path.of("../javaw-core/target/classes").toAbsolutePath();
        Path out = Files.createDirectories(Path.of("target/generated-sources/javaw-components/rewrite-test-classes"));
        var errors = new java.io.ByteArrayOutputStream();
        int status = javac.run(null, null, errors, "-proc:none", "-cp", classpath, "-d", out.toString(), file.toString());
        assertEquals(0, status, errors + "\n" + rewritten);
    }

    @Test
    void inheritedAndQualifiedHtmlCallsAreRewritten() {
        Map<String, String> sections = new LinkedHashMap<>();
        String rewritten = ComponentExtensionAccess.rewriteUserTypes("""
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            abstract class Base extends WebComponent {
                protected Template frame(Template inner) { return html("<div>${inner}</div>"); }
            }
            @CustomElement("x-sub")
            class Sub extends Base {
                private boolean flag;
                @Override protected Template render() {
                    return frame(html(\"""\s\s
                        <p>${flag ? WebComponent.html("<b>x</b>") : null}</p>
                        \"""));
                }
            }
            """, sections);
        assertFalse(rewritten.contains("html("), rewritten);
        String templates = sections.get("javaw_wc_templates");
        assertTrue(templates.contains("{\"id\":2,\"html\":\"<p><!--wc$0--></p>\\u000a\""), "no blank first line: " + templates);
        assertTrue(templates.contains("\"html\":\"<b>x</b>\""), templates);
    }

    @Test
    void wildcardImportsAndOwnHtmlMethods() {
        Map<String, String> sections = new LinkedHashMap<>();
        String rewritten = ComponentExtensionAccess.rewriteUserTypes("""
            import static one.dastec.javaw.components.Templates.*;
            import one.dastec.javaw.components.Template;
            final class Helper {
                static Template cell(int n) { return html("<td>${n}</td>"); }
            }
            final class Own {
                static String html(String s) { return s.toUpperCase(); }
                static String shout() { return html("<b>${not a binding}</b>"); }
            }
            """, sections);
        assertTrue(rewritten.contains("return Template.of(1, Parts.child(n));"), rewritten);
        assertTrue(rewritten.contains("return html(\"<b>${not a binding}</b>\");"), "a class's own html method is left alone: " + rewritten);
        assertTrue(sections.get("javaw_wc_templates").contains("<td><!--wc$0--></td>"), sections.toString());
    }

    @Test
    void sourceMapsPointIntoTheUsersFileOnly() {
        String source = """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-map")
            class MapView extends WebComponent {
                @Property private int count;
                @Override protected Template render() {
                    return html(\"""
                        <p>${count}</p>
                        <button @click=${() -> count++}>+</button>
                        \""");
                }
            }
            """;
        var result = one.dastec.javaw.JavacToWasm.compile("App.java", source, true);
        var map = one.dastec.javaw.sourcemap.SourceMap.parse(result.getSourceMapJson());
        long lines = source.lines().count();
        java.util.Set<Integer> mapped = new java.util.TreeSet<>();
        for (var entry : map.getMappings()) {
            assertTrue(entry.getOriginalLine() < lines, "mapping beyond App.java: line " + (entry.getOriginalLine() + 1)
                    + " of " + lines + " (injected runtime source must not map onto the user's file)");
            mapped.add(entry.getOriginalLine() + 1);
        }
        assertTrue(mapped.contains(11), "the lambda written in the template binding maps to its own line: " + mapped);
    }
}
