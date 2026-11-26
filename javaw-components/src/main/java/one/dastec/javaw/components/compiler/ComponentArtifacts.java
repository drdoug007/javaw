package one.dastec.javaw.components.compiler;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.*;
import one.dastec.javaw.spi.SourceExtension;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M6 build artifacts (docs/web-components.md, "Generated artifacts and TypeScript interop"):
 * {@code custom-elements.json}, {@code <module>.components.d.ts} and the {@code <module>.components.js}
 * ES module. They describe what the runtime really defines: the observed attributes and JS properties
 * of each tag, the events its class declares ({@code dispatch(new X(...))} and {@code @Fires}), and the
 * slots and CSS parts written literally in its templates.
 *
 * <p>Source facts (Javadoc, literal defaults, slots, parts) are collected before the templates and fields
 * are rewritten; attributes and properties come from the lowered definitions.
 */
final class ComponentArtifacts {
    static final String MANIFEST_SUFFIX = ".custom-elements.json";

    /**
     * Event names the DOM lib already declares on {@code GlobalEventHandlersEventMap}/{@code ElementEventMap}
     * (and so on every element). A global {@code HTMLElementEventMap} entry for one of them would silently
     * retype the native event on every element, so those names are typed only on the component's own
     * {@code <Class>EventMap}.
     */
    static final Set<String> NATIVE_EVENTS = Set.of(
            "abort", "animationcancel", "animationend", "animationiteration", "animationstart", "auxclick",
            "beforeinput", "beforematch", "beforetoggle", "blur", "cancel", "canplay", "canplaythrough", "change",
            "click", "close", "compositionend", "compositionstart", "compositionupdate", "contextlost",
            "contextmenu", "contextrestored", "copy", "cuechange", "cut", "dblclick", "drag", "dragend",
            "dragenter", "dragleave", "dragover", "dragstart", "drop", "durationchange", "emptied", "ended",
            "error", "focus", "focusin", "focusout", "formdata", "fullscreenchange", "fullscreenerror",
            "gotpointercapture", "input", "invalid", "keydown", "keypress", "keyup", "load", "loadeddata",
            "loadedmetadata", "loadstart", "lostpointercapture", "mousedown", "mouseenter", "mouseleave",
            "mousemove", "mouseout", "mouseover", "mouseup", "paste", "pause", "play", "playing",
            "pointercancel", "pointerdown", "pointerenter", "pointerleave", "pointermove", "pointerout",
            "pointerover", "pointerup", "progress", "ratechange", "reset", "resize", "scroll", "scrollend",
            "securitypolicyviolation", "seeked", "seeking", "select", "selectionchange", "selectstart",
            "slotchange", "stalled", "submit", "suspend", "timeupdate", "toggle", "touchcancel", "touchend",
            "touchmove", "touchstart", "transitioncancel", "transitionend", "transitionrun", "transitionstart",
            "volumechange", "waiting", "webkitanimationend", "webkitanimationiteration", "webkitanimationstart",
            "webkittransitionend", "wheel");

    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern SLOT = Pattern.compile("<slot(?=[\\s/>])([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("(?:^|\\s)name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern PART = Pattern.compile("(?<=[\\s\"'])part\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))", Pattern.CASE_INSENSITIVE);

    private record FieldDoc(String defaultValue, String description) {}

    private static final class Info {
        final String tag, className;
        String description;
        final Map<String, FieldDoc> fields = new HashMap<>();
        final Set<String> slots = new LinkedHashSet<>(), parts = new LinkedHashSet<>();
        Map<String, ComponentEvents.Event> events = Map.of();
        List<String> observed = List.of();
        List<ReactiveFields.Field> reactive = List.of();

        Info(String tag, String className) { this.tag = tag; this.className = className; }
    }

    private final ComponentEvents events;
    private final Map<String, List<String>> enums = new HashMap<>();
    private final Map<String, Info> byTag = new LinkedHashMap<>();

    ComponentArtifacts(com.github.javaparser.ast.CompilationUnit cu, ComponentEvents events) {
        this.events = events;
        for (EnumDeclaration e : cu.findAll(EnumDeclaration.class)) {
            List<String> names = new ArrayList<>();
            for (EnumConstantDeclaration c : e.getEntries()) names.add(c.getNameAsString().toLowerCase(Locale.ROOT).replace('_', '-'));
            enums.putIfAbsent(e.getNameAsString(), names);
        }
    }

    /** Source facts of {@code component} and the component classes it extends, before any rewrite. */
    void scan(String tag, ClassOrInterfaceDeclaration component, Map<String, ClassOrInterfaceDeclaration> componentsByName) {
        Info info = new Info(tag, component.getNameAsString());
        info.description = javadoc(component);
        for (FieldDeclaration field : component.getFields()) {
            if (field.getVariables().size() != 1) continue;
            VariableDeclarator v = field.getVariable(0);
            info.fields.put(v.getNameAsString(), new FieldDoc(v.getInitializer().map(ComponentArtifacts::literal).orElse(null), javadoc(field)));
        }
        Set<String> seen = new HashSet<>();
        for (ClassOrInterfaceDeclaration c = component; c != null && seen.add(c.getNameAsString());
             c = c.getExtendedTypes().isEmpty() ? null : componentsByName.get(c.getExtendedTypes().get(0).getNameAsString()))
            for (MethodCallExpr call : c.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals("html") || call.getArguments().size() != 1) continue;
                if (call.getArgument(0) instanceof LiteralStringValueExpr text
                        && (text instanceof StringLiteralExpr || text instanceof TextBlockLiteralExpr))
                    markup(text instanceof TextBlockLiteralExpr block ? block.asString() : ((StringLiteralExpr) text).asString(), info);
            }
        byTag.put(tag, info);
    }

    void declaredEvents(String tag, Map<String, ComponentEvents.Event> declared) {
        byTag.get(tag).events = new LinkedHashMap<>(declared);
    }

    void lowered(String tag, List<String> observedAttributes, List<ReactiveFields.Field> fields) {
        Info info = byTag.get(tag);
        info.observed = observedAttributes;
        info.reactive = fields;
    }

    boolean isEmpty() { return byTag.isEmpty(); }

    void emit(SourceExtension.Output output) {
        String module = output.moduleName();
        output.artifact(module + ".components.js", module(module));
        output.artifact(module + ".components.d.ts", declarations(module));
        output.artifact(module + MANIFEST_SUFFIX, manifest(module));
    }

    /** Literal {@code <slot>} names ("" for the default slot) and {@code part} names; bound values are skipped. */
    private static void markup(String html, Info info) {
        String text = COMMENT.matcher(html).replaceAll("");
        Matcher slot = SLOT.matcher(text);
        while (slot.find()) {
            Matcher name = NAME.matcher(slot.group(1));
            String value = name.find() ? group(name) : "";
            if (!value.contains("${")) info.slots.add(value);
        }
        Matcher part = PART.matcher(text);
        while (part.find()) {
            String value = group(part);
            if (value.contains("${")) continue;
            for (String p : value.trim().split("\\s+")) if (!p.isEmpty()) info.parts.add(p);
        }
    }

    private static String group(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) if (m.group(i) != null) return m.group(i);
        return "";
    }

    private static String javadoc(BodyDeclaration<?> node) {
        Optional<JavadocComment> doc = node instanceof com.github.javaparser.ast.nodeTypes.NodeWithJavadoc<?> n ? n.getJavadocComment() : Optional.empty();
        return doc.map(d -> d.parse().getDescription().toText().trim().replaceAll("\\s+", " ")).filter(s -> !s.isEmpty()).orElse(null);
    }

    /** The JS text of a literal initializer, or null. */
    private static String literal(Expression e) {
        if (e instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.MINUS && (u.getExpression() instanceof IntegerLiteralExpr || u.getExpression() instanceof DoubleLiteralExpr)) {
            String inner = literal(u.getExpression());
            return inner == null ? null : "-" + inner;
        }
        if (e instanceof StringLiteralExpr s) return ComponentExtension.quoteJson(s.asString());
        if (e instanceof BooleanLiteralExpr b) return String.valueOf(b.getValue());
        if (e instanceof IntegerLiteralExpr i) { try { return String.valueOf(i.asNumber()); } catch (RuntimeException ex) { return null; } }
        if (e instanceof DoubleLiteralExpr d) {
            double value = d.asDouble();
            return value == Math.rint(value) && !Double.isInfinite(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
        if (e instanceof NullLiteralExpr) return "null";
        return null;
    }

    // ---- TypeScript types -------------------------------------------------------------------------------

    private static String propertyType(ReactiveFields.Field f) {
        return switch (f.kind()) {
            case "string" -> "string | null";
            case "boolean" -> "boolean";
            default -> "number";
        };
    }

    private static String attributeType(ReactiveFields.Field f) {
        if (f == null) return "string";
        return switch (f.kind()) { case "boolean" -> "boolean"; case "string" -> "string"; default -> "number"; };
    }

    static String detailName(ComponentEvents.Event e) { return e.simpleName() + "Detail"; }

    private String detailType(String javaType) {
        String simple = javaType.contains(".") ? javaType.substring(javaType.lastIndexOf('.') + 1) : javaType;
        switch (javaType) {
            case "boolean": return "boolean";
            case "byte": case "short": case "int": case "float": case "double": return "number";
            case "long": return "bigint";
            case "char": return "string";
            default: break;
        }
        switch (simple) {
            case "Boolean": return "boolean | null";
            case "Byte": case "Short": case "Integer": case "Float": case "Double": return "number | null";
            case "Long": return "bigint | null";
            case "Character": case "String": return "string | null";
            case "Element": return "Element | null";
            default: break;
        }
        List<String> constants = enums.get(simple);
        if (constants != null) {
            StringBuilder union = new StringBuilder();
            for (String c : constants) union.append(ComponentExtension.quoteJson(c)).append(" | ");
            return union.append("null").toString();
        }
        ComponentEvents.Event nested = events.find(javaType);
        return nested != null ? detailName(nested) + " | null" : "unknown";
    }

    private static String eventType(ComponentEvents.Event e) { return "CustomEvent<" + detailName(e) + ">"; }

    // ---- custom-elements.json -----------------------------------------------------------------------------

    private String manifest(String module) {
        String path = module + ".components.js";
        List<Object> declarations = new ArrayList<>(), exports = new ArrayList<>();
        for (Info info : byTag.values()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("kind", "class");
            d.put("name", info.className);
            if (info.description != null) d.put("description", info.description);
            d.put("tagName", info.tag);
            d.put("customElement", true);
            Map<String, ReactiveFields.Field> byAttribute = new HashMap<>();
            List<Object> members = new ArrayList<>();
            for (ReactiveFields.Field f : info.reactive) {
                if (!f.property()) continue;
                byAttribute.put(f.attribute(), f);
                FieldDoc doc = info.fields.getOrDefault(f.name(), new FieldDoc(null, null));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("kind", "field");
                m.put("name", f.name());
                m.put("privacy", "public");
                m.put("type", Map.of("text", propertyType(f)));
                if (doc.description() != null) m.put("description", doc.description());
                if (doc.defaultValue() != null) m.put("default", doc.defaultValue());
                m.put("attribute", f.attribute());
                if (f.reflect()) m.put("reflects", true);
                members.add(m);
            }
            List<Object> attributes = new ArrayList<>();
            for (String name : info.observed) {
                ReactiveFields.Field f = byAttribute.get(name);
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("name", name);
                a.put("type", Map.of("text", attributeType(f)));
                if (f != null) {
                    FieldDoc doc = info.fields.getOrDefault(f.name(), new FieldDoc(null, null));
                    if (doc.description() != null) a.put("description", doc.description());
                    if (doc.defaultValue() != null) a.put("default", doc.defaultValue());
                    a.put("fieldName", f.name());
                }
                attributes.add(a);
            }
            if (!attributes.isEmpty()) d.put("attributes", attributes);
            if (!members.isEmpty()) d.put("members", members);
            List<Object> eventList = new ArrayList<>();
            for (ComponentEvents.Event e : info.events.values()) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("name", e.name());
                ev.put("type", Map.of("text", eventType(e)));
                String doc = javadoc(e.declaration());
                if (doc != null) ev.put("description", doc);
                eventList.add(ev);
            }
            if (!eventList.isEmpty()) d.put("events", eventList);
            if (!info.slots.isEmpty()) {
                List<Object> slots = new ArrayList<>();
                for (String s : info.slots) slots.add(Map.of("name", s));
                d.put("slots", slots);
            }
            if (!info.parts.isEmpty()) {
                List<Object> parts = new ArrayList<>();
                for (String p : info.parts) parts.add(Map.of("name", p));
                d.put("cssParts", parts);
            }
            d.put("superclass", Map.of("name", "HTMLElement"));
            declarations.add(d);
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("kind", "custom-element-definition");
            x.put("name", info.tag);
            x.put("declaration", Map.of("name", info.className, "module", path));
            exports.add(x);
        }
        Map<String, Object> mod = new LinkedHashMap<>();
        mod.put("kind", "javascript-module");
        mod.put("path", path);
        mod.put("declarations", declarations);
        mod.put("exports", exports);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", "2.1.0");
        root.put("readme", "");
        root.put("modules", List.of(mod));
        StringBuilder out = new StringBuilder();
        json(root, out, "");
        return out.append('\n').toString();
    }

    @SuppressWarnings("unchecked")
    private static void json(Object value, StringBuilder out, String indent) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) { out.append("{}"); return; }
            // Map.of has no order; sort those small maps for stable output.
            Map<String, Object> ordered = map instanceof LinkedHashMap<?, ?> ? (Map<String, Object>) map : new TreeMap<>((Map<String, Object>) map);
            out.append("{\n");
            int i = 0;
            for (var e : ordered.entrySet()) {
                out.append(indent).append("  ").append(ComponentExtension.quoteJson(e.getKey())).append(": ");
                json(e.getValue(), out, indent + "  ");
                out.append(++i < ordered.size() ? ",\n" : "\n");
            }
            out.append(indent).append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) { out.append("[]"); return; }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                out.append(indent).append("  ");
                json(list.get(i), out, indent + "  ");
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            out.append(indent).append(']');
        } else if (value instanceof Boolean b) out.append(b);
        else out.append(ComponentExtension.quoteJson(String.valueOf(value)));
    }

    // ---- <module>.components.d.ts -------------------------------------------------------------------------

    private String declarations(String module) {
        StringBuilder out = new StringBuilder();
        out.append("// Generated by javaw-components for ").append(module).append(".wasm. Do not edit.\n");
        out.append("// Element types and event details of the custom elements defined by ").append(module).append(".components.js.\n\n");
        Set<ComponentEvents.Event> used = new LinkedHashSet<>();
        for (Info info : byTag.values()) used.addAll(info.events.values());
        // Details reference other event records, so declare every event record of the module.
        for (ComponentEvents.Event e : events.events()) {
            String doc = javadoc(e.declaration());
            if (doc != null) out.append("/** ").append(doc.replace("*/", "*\\/")).append(" */\n");
            out.append("export interface ").append(detailName(e)).append(" {\n");
            for (Parameter p : e.declaration().getParameters())
                out.append("  readonly ").append(p.getNameAsString()).append(": ").append(detailType(p.getTypeAsString())).append(";\n");
            out.append("}\n\n");
        }
        Map<String, Set<String>> global = new LinkedHashMap<>();
        for (Info info : byTag.values()) {
            String map = info.className + "EventMap";
            out.append("export interface ").append(map).append(" extends HTMLElementEventMap {\n");
            for (ComponentEvents.Event e : info.events.values()) {
                out.append("  ").append(ComponentExtension.quoteJson(e.name())).append(": ").append(eventType(e)).append(";\n");
                if (!NATIVE_EVENTS.contains(e.name()))
                    global.computeIfAbsent(e.name(), k -> new LinkedHashSet<>()).add(eventType(e));
            }
            out.append("}\n\n");
            if (info.description != null) out.append("/** ").append(info.description.replace("*/", "*\\/")).append(" */\n");
            out.append("export interface ").append(info.className).append(" extends HTMLElement {\n");
            for (ReactiveFields.Field f : info.reactive) {
                if (!f.property()) continue;
                FieldDoc doc = info.fields.get(f.name());
                if (doc != null && doc.description() != null) out.append("  /** ").append(doc.description().replace("*/", "*\\/")).append(" */\n");
                out.append("  ").append(f.name()).append(": ").append(propertyType(f)).append(";\n");
            }
            out.append("  /** Resolves after the pending update completes: true unless the update limit dropped it; rejects if it threw. */\n");
            out.append("  readonly updateComplete: Promise<boolean>;\n");
            String self = info.className;
            out.append("  addEventListener<K extends keyof ").append(map).append(">(type: K, listener: (this: ").append(self)
                    .append(", ev: ").append(map).append("[K]) => any, options?: boolean | AddEventListenerOptions): void;\n");
            out.append("  addEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions): void;\n");
            out.append("  removeEventListener<K extends keyof ").append(map).append(">(type: K, listener: (this: ").append(self)
                    .append(", ev: ").append(map).append("[K]) => any, options?: boolean | EventListenerOptions): void;\n");
            out.append("  removeEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions): void;\n");
            out.append("}\n\n");
        }
        out.append("/** Tag names this module defines. */\n");
        out.append("export declare const tagNames: readonly (").append(String.join(" | ", byTag.keySet().stream().map(ComponentExtension::quoteJson).toList())).append(")[];\n");
        out.append("/** The URL of ").append(module).append(".wasm next to this module. */\n");
        out.append("export declare const wasmUrl: URL;\n\n");
        out.append("export interface LoadOptions {\n")
           .append("  /** The module to instantiate: a URL, a Response, bytes or a compiled WebAssembly.Module. Defaults to wasmUrl. */\n")
           .append("  source?: string | URL | Response | ArrayBuffer | Uint8Array | WebAssembly.Module;\n")
           .append("  /** Extra imports merged into the javaw import object. */\n")
           .append("  imports?: Record<string, Record<string, unknown>>;\n")
           .append("  /** The document whose window's customElements registry receives the tags. */\n")
           .append("  document?: Document;\n")
           .append("}\n\n");
        out.append("/** The result of instantiateJavaw from javaw-runtime.js. */\n");
        out.append("export interface JavawInstance {\n  instance: WebAssembly.Instance;\n  module: WebAssembly.Module;\n  exports: WebAssembly.Exports;\n  runtime: unknown;\n  classes: unknown;\n}\n\n");
        out.append("/** Instantiates the module once and defines every tag; later calls return the same promise. A failed load is not cached: the next call retries. */\n");
        out.append("export declare function load(options?: LoadOptions): Promise<JavawInstance>;\n");
        out.append("/** Uses an instance already created with instantiateJavaw (which defined the tags) instead of loading another. */\n");
        out.append("export declare function attach(instance: JavawInstance): Promise<JavawInstance>;\n");
        out.append("/** Resolves when every tag of this module is defined. */\n");
        out.append("export declare function whenDefined(): Promise<void>;\n\n");
        out.append("declare global {\n  interface HTMLElementTagNameMap {\n");
        for (Info info : byTag.values())
            out.append("    ").append(ComponentExtension.quoteJson(info.tag)).append(": ").append(info.className).append(";\n");
        out.append("  }\n");
        if (!global.isEmpty()) {
            out.append("  interface HTMLElementEventMap {\n");
            for (var e : global.entrySet())
                out.append("    ").append(ComponentExtension.quoteJson(e.getKey())).append(": ").append(String.join(" | ", e.getValue())).append(";\n");
            out.append("  }\n");
        }
        out.append("}\n");
        return out.toString();
    }

    // ---- <module>.components.js ---------------------------------------------------------------------------

    private String module(String module) {
        StringBuilder tags = new StringBuilder();
        for (String tag : byTag.keySet()) tags.append(tags.length() > 0 ? ", " : "").append(ComponentExtension.quoteJson(tag));
        return """
            // Generated by javaw-components for %1$s.wasm. Do not edit.
            // Loads javaw-runtime.js (copied next to this file), instantiates %1$s.wasm and defines its custom elements.
            import './javaw-runtime.js';

            export const tagNames = Object.freeze([%2$s]);
            export const wasmUrl = new URL('./%1$s.wasm', import.meta.url);

            let loading = null;
            let registry = null;

            function registryOf(doc) {
                const view = doc && doc.defaultView;
                return (view && view.customElements) || (typeof customElements !== 'undefined' ? customElements : null);
            }

            async function start(options) {
                const javaw = globalThis.Javaw;
                if (!javaw || typeof javaw.instantiateJavaw !== 'function') throw new Error('javaw-runtime.js did not load');
                const doc = options.document || (typeof document !== 'undefined' ? document : null);
                registry = registryOf(doc);
                let source = options.source === undefined ? wasmUrl : options.source;
                if (typeof source === 'string' || source instanceof URL) source = await fetch(source);
                return javaw.instantiateJavaw(source, options.imports || {}, doc);
            }

            /** Instantiates the module once and defines every tag; later calls return the same promise. A failed load is not cached: the next call retries. */
            export function load(options = {}) {
                if (!loading) loading = start(options).catch(e => { loading = null; throw e; });
                return loading;
            }

            /** Uses an instance already created with instantiateJavaw (which defined the tags) instead of loading another. */
            export function attach(instance) {
                if (!instance || !instance.runtime) throw new TypeError('attach expects the result of instantiateJavaw');
                if (!loading) {
                    registry = registryOf(instance.runtime.doc);
                    loading = Promise.resolve(instance);
                }
                return loading;
            }

            /** Resolves when every tag of this module is defined. */
            export async function whenDefined() {
                const r = registry || registryOf(typeof document !== 'undefined' ? document : null);
                if (!r) throw new Error('no customElements registry');
                await Promise.all(tagNames.map(tag => r.whenDefined(tag)));
            }
            """.formatted(module, tags);
    }
}
