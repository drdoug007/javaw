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
    void scalarLikePropertyTypes() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.Changes;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;

            enum Priority { LOW, VERY_HIGH }

            @CustomElement(value = "x-kinds", observedAttributes = {"data-bump"})
            class Kinds extends WebComponent {
                @Property(reflect = true) private byte small;
                @Property private short medium;
                @Property private char letter = 'a';
                @Property private long big;
                @Property(reflect = true) private float ratio;
                @Property(reflect = true) private Integer count;
                @Property private Long total;
                @Property private Character initial;
                @Property(reflect = true) private Priority level = Priority.LOW;
                @Property(reflect = true) private Boolean flag;
                @Override protected void update(Changes changes) {
                    host().setAttribute("data-text", small + "," + medium + "," + letter + "," + big + "," + ratio + "," + count + ","
                            + total + "," + initial + "," + level + "," + flag);
                }
                @Override protected void attributeChanged(String name, String oldValue, String newValue) {
                    if (name.equals("data-bump")) { big++; letter = 'z'; level = Priority.LOW; count = null; }
                }
            }

            @CustomElement("x-kinds-host")
            class KindsHost extends WebComponent {
                @Override protected Template render() {
                    return html("<x-kinds .small=${(byte) 5} .level=${Priority.VERY_HIGH} .total=${Long.valueOf(9L)}></x-kinds>");
                }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            document.body.innerHTML = '<x-kinds small="12" medium="-3" ratio="1.5" count="7" level="very-high" flag></x-kinds>';
            const el = document.querySelector('x-kinds');
            await el.updateComplete;
            assert.equal(el.getAttribute('data-text'), '12,-3,a,0,1.5,7,null,null,VERY_HIGH,true', 'attributes convert into every kind');
            assert.deepEqual([el.small, el.medium, el.letter, el.big, el.ratio, el.count, el.total, el.initial, el.level, el.flag],
                [12, -3, 'a', 0n, 1.5, 7, null, null, 'very-high', true], 'JS reads give numbers, strings, bigint, null and kebab names');

            el.small = 300; el.medium = 40000.9; el.ratio = 0.1; el.big = 2n ** 63n - 1n; el.total = 42; el.initial = 'Q'; el.count = null;
            el.level = 'low'; el.flag = null;
            await el.updateComplete;
            assert.equal(el.small, 44, 'byte narrows like a Java cast');
            assert.equal(el.medium, -25536, 'short narrows like a Java cast');
            assert.equal(el.ratio, Math.fround(0.1), 'float rounds like a Java cast');
            assert.equal(el.big, 9223372036854775807n);
            assert.equal(el.total, 42n, 'a safe integer is accepted for Long');
            assert.equal(el.initial, 'Q');
            assert.equal(el.count, null);
            assert.equal(el.getAttribute('small'), '44', 'byte reflects');
            assert.equal(el.getAttribute('ratio'), '0.1', 'float reflects with Java text');
            assert.equal(el.hasAttribute('count'), false, 'a null box removes its attribute');
            assert.equal(el.getAttribute('level'), 'low', 'an enum reflects its kebab name');
            assert.equal(el.hasAttribute('flag'), false, 'a null Boolean removes its attribute');

            for (const [name, value] of [['small', '1'], ['letter', 'ab'], ['letter', 5], ['big', 1.5], ['big', 2n ** 64n], ['big', 2 ** 60],
                                         ['level', 'VERY_HIGH'], ['level', 3], ['count', 'x'], ['letter', null], ['small', null]])
                assert.throws(() => { el[name] = value; }, TypeError, name + ' = ' + String(value));
            assert.match((() => { try { el.level = 'nope'; } catch (e) { return e.message; } })(), /x-kinds.level must be one of "low", "very-high" or null/);

            el.removeAttribute('small'); el.removeAttribute('level');
            await el.updateComplete;
            assert.equal(el.small, 0, 'removing a primitive attribute gives its default');
            assert.equal(el.level, null, 'removing an enum attribute gives null');
            el.small = 9; el.level = 'very-high';
            await el.updateComplete;
            el.setAttribute('small', '300');
            el.setAttribute('level', 'medium');
            await el.updateComplete;
            assert.equal(el.small, 9, 'an out-of-range byte attribute leaves the field unchanged');
            assert.equal(el.level, 'very-high', 'an unknown enum attribute leaves the field unchanged');
            assert.match(errors[1].message, /No enum constant Priority.MEDIUM/);
            assert.equal(errors.length, 2, 'both conversion failures are reported: ' + errors.map(e => e.message).join(' / '));

            el.setAttribute('data-bump', '');
            await el.updateComplete;
            assert.equal(el.big, -9223372036854775808n, 'long overflow wraps in Java (a Java ++ write)');
            assert.equal(el.letter, 'z');
            assert.equal(el.level, 'low');
            assert.equal(el.getAttribute('level'), 'low');

            const host = document.createElement('x-kinds-host');
            document.body.appendChild(host);
            await host.updateComplete;
            const inner = host.shadowRoot.querySelector('x-kinds');
            await inner.updateComplete;
            assert.deepEqual([inner.small, inner.level, inner.total], [5, 'very-high', 9n], 'typed .prop bindings set byte, enum and Long fields');
            """);
        var noAttribute = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-a") class A extends WebComponent { @Property(attribute = "n") private long n; }
            """, false));
        assertTrue(noAttribute.getMessage().contains("has no attribute converter"), noAttribute.getMessage());
        var noReflect = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-a") class A extends WebComponent { @Property(reflect = true) private char c; }
            """, false));
        assertTrue(noReflect.getMessage().contains("has no attribute to reflect to"), noReflect.getMessage());
    }

    @Test
    void propertiesInheritedFromComponentSuperclasses() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.Changes;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.State;
            import one.dastec.javaw.components.WebComponent;

            abstract class Labeled extends WebComponent {
                @Property(reflect = true) private String label = "none";
                @State private int renders;
                protected void relabel(String text) { label = text; }
                protected String label() { return label; }
                @Override protected void update(Changes changes) {
                    renders++;
                    host().setAttribute("data-text", describe() + (changes.has("label") ? "*" : ""));
                }
                protected abstract String describe();
            }

            @CustomElement("x-chip")
            class Chip extends Labeled {
                @Property private int size = 1;
                @Override protected String describe() { return label() + ":" + size; }
                @Override protected void connected() { if (size == 9) relabel("nine"); }
            }

            @CustomElement("x-big-chip")
            class BigChip extends Chip {
                @Property private boolean loud;
                @Override protected String describe() { return super.describe() + (loud ? "!" : ""); }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            // Parsed after definition: happy-dom skips initial attribute reactions on upgrade.
            document.body.innerHTML = '<x-big-chip label="hi" size="3" loud></x-big-chip>';
            const tick = async () => { await settle(); await settle(); };
            await tick();
            const big = document.querySelector('x-big-chip');
            assert.deepEqual([...big.constructor.observedAttributes].sort(), ['label', 'loud', 'size'],
                'attributes of base, component superclass and own fields are observed');
            assert.equal(big.getAttribute('data-text'), 'hi:3!*', 'initial attributes reach inherited fields');
            assert.equal(big.label, 'hi', 'JS accessor for a field of an abstract base');
            assert.equal(big.size, 3, 'JS accessor for a field of a component superclass');
            big.label = 'yo'; await big.updateComplete;
            assert.equal(big.getAttribute('label'), 'yo', 'an inherited property reflects');
            assert.equal(big.getAttribute('data-text'), 'yo:3!*');
            big.size = 5; await big.updateComplete;
            assert.equal(big.getAttribute('data-text'), 'yo:5!', 'only the changed inherited property is in Changes');
            big.setAttribute('size', '7'); await big.updateComplete;
            assert.equal(big.size, 7, 'attribute changes convert into inherited fields');
            assert.throws(() => { big.label = 5; }, TypeError, 'inherited accessors check types');

            const chip = document.createElement('x-chip');
            chip.size = 9;
            document.body.appendChild(chip);
            await chip.updateComplete;
            assert.equal(chip.getAttribute('data-text'), 'nine:9*', 'a write in an inherited method schedules an update');
            assert.equal(chip.getAttribute('label'), 'nine');
            assert.equal('loud' in chip, false, 'a superclass component has no subclass properties');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
        var clash = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.WebComponent;
            abstract class Base extends WebComponent { @Property private int size; }
            @CustomElement("x-a") class A extends Base { @Property(attribute = "size") private int other; }
            """, false));
        assertTrue(clash.getMessage().contains("both use the attribute \"size\""), clash.getMessage());
        var sameName = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.State;
            import one.dastec.javaw.components.WebComponent;
            abstract class Base extends WebComponent { @State private int count; }
            @CustomElement("x-a") class A extends Base { @State private int count; }
            """, false));
        assertTrue(sameName.getMessage().contains("'count' is declared in both A and Base"), sameName.getMessage());
    }

    @Test
    void updateCompleteSettlesAfterTheUpdate() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.Changes;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.WebComponent;

            @CustomElement("x-steps")
            class Steps extends WebComponent {
                @Property private int n;
                @Property private int echo;
                @Property private boolean boom;
                @Property private boolean spin;
                @Override protected void willUpdate(Changes changes) {
                    if (boom) throw new IllegalStateException("boom");
                }
                @Override protected void update(Changes changes) { host().setAttribute("data-n", "" + n); }
                @Override protected void updated(Changes changes) {
                    if (echo != n) echo = n;      // a second update in the same flush
                    if (spin) n++;                // never settles: hits the 100-update limit
                }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const el = document.createElement('x-steps');
            const before = el.updateComplete;
            document.body.appendChild(el);
            assert.equal(await before, true, 'the first update completes after connection');
            assert.equal(await el.updateComplete, true, 'nothing pending resolves at once');

            el.n = 4;
            assert.equal(el.getAttribute('data-n'), '0', 'the update is batched');
            const done = el.updateComplete;
            assert.ok(done instanceof window.Promise || typeof done.then === 'function');
            assert.equal(await done, true);
            assert.equal(el.getAttribute('data-n'), '4', 'the DOM is updated when the promise resolves');
            assert.equal(el.echo, 4, 'the chained update from updated() is complete too');

            el.boom = true;
            await assert.rejects(el.updateComplete, /Java exception in update: boom/, 'an update that throws rejects with the Java message');
            el.boom = false;
            assert.equal(await el.updateComplete, true, 'a later update resolves again');

            el.spin = true;
            assert.equal(await el.updateComplete, false, 'an update dropped by the 100-update limit resolves false');
            el.spin = false;
            await el.updateComplete;

            el.remove();
            el.n = 99;
            let settled = false;
            const waiting = el.updateComplete.then(v => { settled = true; return v; });
            for (let i = 0; i < 5; i++) await settle();
            assert.equal(settled, false, 'a disconnected component keeps its update pending');
            document.body.appendChild(el);
            assert.equal(await waiting, true, 'reconnecting completes it');
            assert.equal(el.getAttribute('data-n'), '99');
            const orphan = document.createElement('x-steps');
            document.body.appendChild(orphan);
            await orphan.updateComplete;
            orphan.n = 1;
            const orphanDone = orphan.updateComplete;
            orphan.remove();
            for (let i = 0; i < 3; i++) await settle();
            const refs = r.wcWaiting ? [...r.wcWaiting] : [];
            assert.ok(refs.every(ref => ref instanceof WeakRef), 'pending waiters hold their elements weakly');
            void orphanDone;
            assert.equal(errors.length, 2, 'the thrown update and the limit are reported');
            assert.match(errors[0].message, /boom/, 'the reported error carries the Java message');
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
        reject(imports + "@CustomElement(\"x-a\") class A extends WebComponent { @Property private int updateComplete; }", "reserved for the element's update promise");
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
