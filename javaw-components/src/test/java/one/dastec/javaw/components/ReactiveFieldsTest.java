package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** M2: @Property/@State fields, the batched update lifecycle, attributes, reflection and JS properties. */
class ReactiveFieldsTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.Changes;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Property;
        import one.dastec.javaw.components.State;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.Element;

        @CustomElement("x-reactive")
        class Reactive extends WebComponent {
            @Property(reflect = true) private int count;
            @Property private String label = "hi";
            @Property(attribute = "is-open", reflect = true) private boolean open;
            @Property private double ratio;
            @State private int renders;
            private String trace = "";
            private Element out;
            @Override protected void connected() { host().addEventListener("bump", () -> bump()); }
            @Override protected void willUpdate(Changes changes) { trace = trace + "will:" + changes.size() + ";"; }
            @Override protected void update(Changes changes) {
                renders++;
                if (out == null) { out = createElement("span"); renderRoot().appendChild(out); }
                out.setInnerText(label + ":" + count + ":" + open);
                trace = trace + "update" + (changes.has("count") ? "+count" : "") + ";";
                host().setAttribute("data-trace", trace);
                host().setAttribute("data-renders", "" + renders);
            }
            @Override protected void firstUpdated(Changes changes) { trace = trace + "first;"; }
            @Override protected void updated(Changes changes) {
                trace = trace + "updated;";
                if (count == 100) count = 101;
            }
            private void bump() { count++; count += 2; }
        }

        @CustomElement("x-runaway")
        class Runaway extends WebComponent {
            @Property private int n;
            @Override protected void updated(Changes changes) { if (n >= 500 && n < 1000) n = n + 1; }
        }
        """;

    @Test
    void reactiveFieldsDriveBatchedUpdates() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, """
            document.body.innerHTML = '<x-reactive id="early" label="attr"></x-reactive>';
            document.getElementById('early').label = 'pre';
            """, """
            const tick = async () => { await settle(); await settle(); };
            const text = el => el.shadowRoot.querySelector('span').textContent;
            await tick();
            const early = document.getElementById('early');
            assert.equal(early.label, 'pre', 'a property set before upgrade is replayed and wins over the initial attribute');
            assert.ok(text(early).startsWith('pre:'), text(early));

            const el = document.createElement('x-reactive');
            el.setAttribute('count', '3');
            document.body.appendChild(el);
            await tick();
            assert.equal(text(el), 'hi:3:false', 'attribute conversion and initial values');
            assert.equal(el.getAttribute('data-trace'), 'will:5;update+count;', 'the first update lists every reactive field');

            el.count = 5; el.count = 6; el.label = 'x';
            assert.equal(text(el), 'hi:3:false', 'updates are asynchronous');
            await tick();
            assert.equal(text(el), 'x:6:false');
            assert.ok(el.getAttribute('data-trace').endsWith('first;updated;will:2;update+count;'), el.getAttribute('data-trace'));
            assert.equal(el.getAttribute('count'), '6', 'reflect writes the attribute');
            assert.equal(el.getAttribute('data-renders'), '2', 'one update per batch; writes during update() schedule nothing');

            el.open = true; await tick();
            assert.equal(el.getAttribute('is-open'), '', 'boolean reflection');
            el.open = false; await tick();
            assert.equal(el.hasAttribute('is-open'), false);
            el.setAttribute('is-open', ''); await tick();
            assert.equal(el.open, true, 'attribute to property');

            el.ratio = 1.5;
            assert.equal(el.ratio, 1.5);
            assert.throws(() => { el.count = 'x'; }, TypeError);
            assert.throws(() => { el.label = 3; }, TypeError);
            el.label = null; await tick();
            assert.equal(el.label, null);

            el.dispatchEvent(new window.Event('bump')); await tick();
            assert.equal(el.count, 9, 'count++ then count += 2 in Java');
            el.count = 100; await tick();
            assert.equal(el.count, 101, 'a write in updated() starts another cycle');
            assert.equal(text(el), 'null:101:true');
            assert.equal(errors.length, 0, String(errors[0]));

            el.count = 3e9; assert.equal(el.count, 2147483647, 'int narrowing saturates as in Java');
            el.count = -Infinity; assert.equal(el.count, -2147483648);
            el.count = NaN; assert.equal(el.count, 0);
            el.count = 2.9; assert.equal(el.count, 2);
            await tick();
            assert.equal(errors.length, 0, 'after saturation: ' + String(errors[0]));

            const run = document.createElement('x-runaway');
            document.body.appendChild(run);
            await tick();
            run.n = 500; await tick();
            assert.equal(errors.length, 1, 'a runaway update loop is stopped and reported');
            assert.match(String(errors[0]), /more than 100 updates/);
            assert.equal(run.n, 600, 'the 100th update in the flush was the last');
            errors.length = 0;
            run.n = 900; await tick();
            assert.ok(run.n > 900, 'the component updates again after the limit: ' + run.n);
            errors.length = 0;
            assert.equal(errors.length, 0, String(errors[0]));
            """);
    }

    @Test
    void reactiveFieldRulesAreDiagnosed() {
        String imports = """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.State;
            import one.dastec.javaw.components.WebComponent;
            """;
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @Property int n; }", "must be a private, non-static, non-final");
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @Property private java.util.List<String> n; }", "use @State for other types");
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @State private int n; void f(A o) { o.n = 1; } }", "can only be written as");
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @Property private int a; @Property(attribute = \"a\") private int b; }",
                "use the attribute \"a\"");
        // The core compiler rejects inner and anonymous classes first; check the extension's own diagnostic directly.
        var nested = assertThrows(IllegalArgumentException.class, () -> ComponentExtensionAccess.rewrite(imports
                + "@CustomElement(\"x-a\") class A extends WebComponent { @State private int n; void f() { new Object() { void g() { n = 1; } }; } }"));
        assertTrue(nested.getMessage().contains("nested class"), nested.getMessage());
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @State private int n; void f() { A o = this; o.n = 1; } }", "can only be written as");
        String rewritten = ComponentExtensionAccess.rewrite(imports + """
            @CustomElement("x-a") class A extends WebComponent {
                static class Point { int n; }
                static class Counter { int n; void inc() { n = n + 1; this.n++; } }
                @State private int n = 1;
                Runnable later = () -> n = 7;
                A() { n = 2; Runnable r = () -> n = 8; }
                void h(Point p) { p.n = 9; new Object() { int n; void k() { this.n = 10; n = 11; } }; }
                void nested() { n = (n = 12) + 1; }
                void f(int n) { n = 3; }
                void g() { n = 4; this.n += 5; n--; }
            }
            """);
        assertTrue(rewritten.contains("this.n = 2") || rewritten.contains("n = 2;"), "constructor writes are initialization: " + rewritten);
        assertTrue(rewritten.contains("n = 3;"), "a parameter shadows the field: " + rewritten);
        assertTrue(rewritten.contains("A.__set$n(this, 4)"), rewritten);
        assertTrue(rewritten.contains("A.__set$n(this, (int) (this.n + (5)))"), rewritten);
        assertTrue(rewritten.contains("A.__post$n(this, (int) (this.n - 1))"), rewritten);
        assertTrue(rewritten.contains("A.__set$n(this, 7)") && rewritten.contains("A.__set$n(this, 8)"), "lambdas in initializers run later: " + rewritten);
        assertTrue(rewritten.contains("n = n + 1;") && rewritten.contains("this.n++;"), "a nested class's own field: " + rewritten);
        assertTrue(rewritten.contains("p.n = 9;"), "an unrelated receiver: " + rewritten);
        assertTrue(rewritten.contains("this.n = 10;") && rewritten.contains("n = 11;"), "an anonymous class's own field: " + rewritten);
        assertTrue(rewritten.contains("A.__set$n(this, (A.__set$n(this, 12)) + 1)"), "nested writes: " + rewritten);
    }

    private static void reject(String source, String fragment) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", source, false));
        assertTrue(error.getMessage().contains(fragment), error.getMessage());
    }
}
