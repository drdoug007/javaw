package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** M6: custom-elements.json, <module>.components.d.ts and the <module>.components.js ES module. */
class ArtifactsTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.*;
        import one.dastec.javaw.dom.Element;

        enum Tone { LOW, VERY_HIGH }

        /** A list of things to do. */
        @CustomElement(value = "todo-list", observedAttributes = {"data-mode"})
        class TodoList extends WebComponent {
            /** Fired when the number of open items changes. */
            public record RemainingChanged(int remaining, String label, Tone tone, long total, Integer missing, Element where, Inner inner)
                implements ComponentEvent {}
            public record Inner(boolean ok, char key) implements ComponentEvent {}
            /** The heading text. */
            @Property(reflect = true) private String heading = "Todos";
            @Property(attribute = "max") private int maxItems = 10;
            @Property private boolean compact;
            @Property private double ratio = -1.5;
            @State private String draft = "";

            @Override protected Template render() {
                return html(\"""
                    <!-- <slot name="commented"></slot> -->
                    <header part="header title"><slot name="title"></slot></header>
                    <slot></slot>
                    <slot name=${draft}></slot>
                    <button part=${draft} @click=${this::changed}>${heading}</button>
                    <footer part='footer'></footer>
                    \""");
            }
            private void changed() { dispatch(new RemainingChanged(1, "x", Tone.LOW, 2L, null, host(), new Inner(true, 'k'))); }
        }

        @CustomElement("todo-badge")
        @Fires(TodoList.Inner.class)
        class Badge extends WebComponent {
            @Property private String text;
        }
        """;

    @Test
    void manifestDescribesTagsAttributesPropertiesEventsSlotsAndParts() throws Exception {
        Map<String, String> artifacts = JavacToWasm.compile("App.java", PROGRAM, false).getArtifacts();
        assertEquals(List.of("App.components.js", "App.components.d.ts", "custom-elements.json"), List.copyOf(artifacts.keySet()));
        node(Map.of("custom-elements.json", artifacts.get("custom-elements.json")), """
            const m = JSON.parse(fs.readFileSync('custom-elements.json', 'utf8'));
            assert.equal(m.schemaVersion, '2.1.0');
            assert.equal(m.modules.length, 1);
            const mod = m.modules[0];
            assert.equal(mod.kind, 'javascript-module');
            assert.equal(mod.path, 'App.components.js');
            assert.deepEqual(mod.exports, [
              { kind: 'custom-element-definition', name: 'todo-list', declaration: { module: 'App.components.js', name: 'TodoList' } },
              { kind: 'custom-element-definition', name: 'todo-badge', declaration: { module: 'App.components.js', name: 'Badge' } },
            ]);
            const [todo, badge] = mod.declarations;
            assert.equal(todo.kind, 'class');
            assert.equal(todo.name, 'TodoList');
            assert.equal(todo.tagName, 'todo-list');
            assert.equal(todo.customElement, true);
            assert.equal(todo.description, 'A list of things to do.');
            assert.deepEqual(todo.superclass, { name: 'HTMLElement' });
            assert.deepEqual(todo.attributes, [
              { name: 'data-mode', type: { text: 'string' } },
              { name: 'heading', type: { text: 'string' }, description: 'The heading text.', default: '"Todos"', fieldName: 'heading' },
              { name: 'max', type: { text: 'number' }, default: '10', fieldName: 'maxItems' },
              { name: 'compact', type: { text: 'boolean' }, fieldName: 'compact' },
              { name: 'ratio', type: { text: 'number' }, default: '-1.5', fieldName: 'ratio' },
            ]);
            assert.deepEqual(todo.members, [
              { kind: 'field', name: 'heading', privacy: 'public', type: { text: 'string | null' }, description: 'The heading text.', default: '"Todos"', attribute: 'heading', reflects: true },
              { kind: 'field', name: 'maxItems', privacy: 'public', type: { text: 'number' }, default: '10', attribute: 'max' },
              { kind: 'field', name: 'compact', privacy: 'public', type: { text: 'boolean' }, attribute: 'compact' },
              { kind: 'field', name: 'ratio', privacy: 'public', type: { text: 'number' }, default: '-1.5', attribute: 'ratio' },
            ], '@State fields are not part of the element API');
            assert.deepEqual(todo.events, [
              { name: 'remaining-changed', type: { text: 'CustomEvent<RemainingChangedDetail>' }, description: 'Fired when the number of open items changes.' },
            ]);
            assert.deepEqual(todo.slots, [{ name: 'title' }, { name: '' }], 'literal slots only; commented and bound names are skipped');
            assert.deepEqual(todo.cssParts, [{ name: 'header' }, { name: 'title' }, { name: 'footer' }]);
            assert.equal(badge.tagName, 'todo-badge');
            assert.deepEqual(badge.events, [{ name: 'inner', type: { text: 'CustomEvent<InnerDetail>' } }], '@Fires');
            assert.equal(badge.slots, undefined);
            assert.deepEqual(badge.members.map(x => x.type.text), ['string | null']);
            """);
    }

    @Test
    void declarationsTypeElementsAndEvents() {
        String dts = JavacToWasm.compile("App.java", PROGRAM, false).getArtifacts().get("App.components.d.ts");
        assertTrue(dts.contains("""
            export interface RemainingChangedDetail {
              readonly remaining: number;
              readonly label: string | null;
              readonly tone: "low" | "very-high" | null;
              readonly total: bigint;
              readonly missing: number | null;
              readonly where: Element | null;
              readonly inner: InnerDetail | null;
            }
            """), dts);
        assertTrue(dts.contains("""
            export interface InnerDetail {
              readonly ok: boolean;
              readonly key: string;
            }
            """), dts);
        assertTrue(dts.contains("""
            export interface TodoListEventMap extends HTMLElementEventMap {
              "remaining-changed": CustomEvent<RemainingChangedDetail>;
            }

            /** A list of things to do. */
            export interface TodoList extends HTMLElement {
              /** The heading text. */
              heading: string | null;
              maxItems: number;
              compact: boolean;
              ratio: number;
              addEventListener<K extends keyof TodoListEventMap>(type: K, listener: (this: TodoList, ev: TodoListEventMap[K]) => any, options?: boolean | AddEventListenerOptions): void;
            """), dts);
        assertFalse(dts.contains("draft"), "@State fields are not declared");
        assertTrue(dts.contains("""
            declare global {
              interface HTMLElementTagNameMap {
                "todo-list": TodoList;
                "todo-badge": Badge;
              }
              interface HTMLElementEventMap {
                "remaining-changed": CustomEvent<RemainingChangedDetail>;
                "inner": CustomEvent<InnerDetail>;
              }
            }
            """), dts);
        assertTrue(dts.contains("export declare const tagNames: readonly (\"todo-list\" | \"todo-badge\")[];"), dts);
        assertTrue(dts.contains("export declare function load(options?: LoadOptions): Promise<JavawInstance>;"), dts);
    }

    @Test
    void programsWithoutComponentsHaveNoArtifacts() {
        assertTrue(JavacToWasm.compile("App.java", "class App { public static int f() { return 1; } }", false).getArtifacts().isEmpty());
    }

    @Test
    void esModuleLoadsTheWasmAndDefinesEveryTag() throws Exception {
        var result = JavacToWasm.compile("App.java", PROGRAM, false);
        assertNotNull(result.getWasm());
        Path dir = Files.createDirectories(Path.of("target/m6-module").toAbsolutePath());
        Files.write(dir.resolve("App.wasm"), result.getWasm());
        node(dir, Map.of("App.components.js", result.getArtifacts().get("App.components.js"),
                "javaw-runtime.js", Files.readString(Path.of("../javaw-core/src/main/resources/javaw-runtime.js"))), """
            const { Window } = await import(pathToFileURL(happy).href);
            const window = new Window();
            const document = window.document;
            const mod = await import(pathToFileURL(path.resolve('App.components.js')).href);
            assert.deepEqual(mod.tagNames, ['todo-list', 'todo-badge']);
            assert.equal(mod.wasmUrl.href, pathToFileURL(path.resolve('App.wasm')).href);
            const first = mod.load({ source: fs.readFileSync('App.wasm'), document });
            assert.equal(mod.load(), first, 'load runs once');
            const loaded = await first;
            assert.equal(typeof loaded.exports.memory, 'object');
            assert.equal(mod.attach(loaded), first, 'attach after load keeps the first instance');
            await mod.whenDefined();
            assert.ok(window.customElements.get('todo-list'));
            assert.ok(window.customElements.get('todo-badge'));
            document.body.innerHTML = '<todo-list heading="Mine"></todo-list>';
            const el = document.querySelector('todo-list');
            assert.equal(el.heading, 'Mine');
            await new Promise(resolve => setTimeout(resolve, 0));
            assert.equal(el.shadowRoot.querySelector('button').textContent, 'Mine');
            await window.happyDOM.abort();
            """);
    }

    @Test
    void compileFilesWritesArtifactsNextToTheWasm() throws Exception {
        Path dir = Path.of("target/m6-files").toAbsolutePath();
        Path src = Files.createDirectories(dir.resolve("src")).resolve("App.java");
        Files.writeString(src, PROGRAM);
        Path out = dir.resolve("out");
        assertTrue(JavacToWasm.compileFiles(List.of(src), out.resolve("wat/shop.wat"), out.resolve("shop.wasm"), null, null, "-O0", false, true));
        assertTrue(Files.isRegularFile(out.resolve("shop.wasm")));
        String manifest = Files.readString(out.resolve("custom-elements.json"));
        assertTrue(manifest.contains("\"path\": \"shop.components.js\""), manifest);
        assertTrue(Files.readString(out.resolve("shop.components.js")).contains("new URL('./shop.wasm', import.meta.url)"));
        assertTrue(Files.readString(out.resolve("shop.components.d.ts")).contains("\"todo-list\": TodoList;"));
        assertFalse(Files.exists(out.resolve("wat/custom-elements.json")));
    }

    /**
     * Type-checks a consumer against the .d.ts with a local TypeScript compiler. Opt-in: set the system
     * property {@code javaw.tsc} to a {@code typescript/lib/tsc.js}; the offline suite has no TypeScript.
     */
    @Test
    void typeScriptConsumerTypeChecks() throws Exception {
        String tsc = System.getProperty("javaw.tsc");
        Assumptions.assumeTrue(tsc != null && Files.isRegularFile(Path.of(tsc)), "set -Djavaw.tsc=<typescript/lib/tsc.js> to type-check the .d.ts");
        Map<String, String> artifacts = JavacToWasm.compile("App.java", PROGRAM, false).getArtifacts();
        Path dir = Files.createDirectories(Path.of("target/m6-tsc").toAbsolutePath());
        for (var a : artifacts.entrySet()) Files.writeString(dir.resolve(a.getKey()), a.getValue());
        Files.writeString(dir.resolve("consumer.ts"), """
            import { load, tagNames, type TodoList, type RemainingChangedDetail } from './App.components.js';

            async function main(): Promise<void> {
              await load();
              const list = document.createElement('todo-list');
              const typed: TodoList = list;
              typed.heading = 'Shopping';
              typed.heading = null;
              typed.maxItems = 3;
              typed.compact = true;
              // @ts-expect-error maxItems is a number
              typed.maxItems = '3';
              // @ts-expect-error compact is a boolean
              typed.compact = 'yes';
              // @ts-expect-error @State fields are not element properties
              typed.draft = '';
              list.addEventListener('remaining-changed', e => {
                const d: RemainingChangedDetail = e.detail;
                const n: number = d.remaining;
                const big: bigint = d.total;
                const tone: 'low' | 'very-high' | null = d.tone;
                if (d.inner) { const k: string = d.inner.key; console.log(k); }
                // @ts-expect-error remaining is a number
                const s: string = e.detail.remaining;
                console.log(n, big, tone, s);
              });
              document.querySelector('todo-badge')?.addEventListener('inner', e => console.log(e.detail.ok));
              document.body.addEventListener('remaining-changed', e => console.log(e.detail.label));
              const tag: 'todo-list' | 'todo-badge' = tagNames[0];
              console.log(tag);
            }
            main();
            """);
        Process p = new ProcessBuilder("node", tsc, "--noEmit", "--strict", "--target", "es2022", "--module", "es2022",
                "--moduleResolution", "bundler", "--lib", "es2022,dom", "consumer.ts", "App.components.d.ts")
                .directory(dir.toFile()).redirectErrorStream(true).start();
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "tsc timed out");
        assertEquals(0, p.exitValue(), new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static void node(Map<String, String> files, String body) throws Exception {
        node(Files.createDirectories(Path.of("target/m6-node").toAbsolutePath()), files, body);
    }

    /** Writes {@code files} into {@code dir} and runs {@code body} as an ES module there. */
    private static void node(Path dir, Map<String, String> files, String body) throws Exception {
        for (var f : files.entrySet()) Files.writeString(dir.resolve(f.getKey()), f.getValue());
        Path happy = Path.of("../test-vendor/node/node_modules/happy-dom/lib/index.js").toAbsolutePath().normalize();
        Path script = dir.resolve("check.mjs");
        Files.writeString(script, """
            import assert from 'node:assert/strict';
            import fs from 'node:fs';
            import path from 'node:path';
            import { pathToFileURL } from 'node:url';
            const happy = process.argv[2];
            try {
            """ + body + """
            } catch (error) { console.error(error); process.exitCode = 1; }
            """);
        Process p = new ProcessBuilder("node", script.toString(), happy.toString()).directory(dir.toFile()).redirectErrorStream(true).start();
        assertTrue(p.waitFor(90, TimeUnit.SECONDS), "Node timed out");
        assertEquals(0, p.exitValue(), new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }
}
