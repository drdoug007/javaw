package one.dastec.javaw.components.compiler;

/**
 * Java source of the template renderer, injected into modules that use templates.
 *
 * <p>Each rendered template is a {@code __WcInstance} in Wasm holding the previous value of every
 * binding. A render compares the new values with those and calls the host only for bindings that
 * changed, so an update in which nothing changed makes no host calls. The DOM side (cloning, marker
 * lookup, committing) is in {@code javaw-runtime.js} ({@code wc*} methods); instances are identified
 * there by an int id.
 */
final class TemplateRuntime {
    private TemplateRuntime() {}

    // Part kinds; keep in sync with __WcRenderer.commitPart.
    static final String TYPES = """
        final class Template {
            int __id;
            TemplatePart[] __parts;
            Template(int id, TemplatePart[] parts) { __id = id; __parts = parts; }
            public static Template of(int id, TemplatePart... parts) { return new Template(id, parts); }
        }
        final class TemplatePart {
            int __kind;
            String __text;
            boolean __flag;
            double __number;
            Template __template;
            int __key;
            Object __value;
            __JavawDomRunnable __run;
            EventListener<Event> __handler;
            EventListener<MouseEvent> __mouse;
            EventListener<PointerEvent> __pointer;
            EventListener<KeyboardEvent> __keyboard;
            EventListener<FocusEvent> __focus;
            EventListener<InputEvent> __input;
            EventListener<SubmitEvent> __submit;
            TemplatePart(int kind) { __kind = kind; }
        }
        final class Parts {
            private static TemplatePart text(int kind, String value) { TemplatePart p = new TemplatePart(kind); p.__text = value; return p; }
            public static TemplatePart child(String value) { return text(0, value); }
            public static TemplatePart child(int value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(long value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(char value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(float value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(double value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(boolean value) { return text(0, String.valueOf(value)); }
            public static TemplatePart child(Template value) {
                if (value == null) return text(0, null);
                TemplatePart p = new TemplatePart(1);
                p.__template = value;
                return p;
            }
            public static TemplatePart child(Object value) {
                if (value == null) return text(0, null);
                if (value instanceof Template template) return child(template);
                if (value instanceof Iterable<?>) throw new UnsupportedOperationException("Iterable children arrive in milestone M4");
                return text(0, String.valueOf(value));
            }
            public static TemplatePart child(Iterable<?> value) {
                throw new UnsupportedOperationException("Iterable children arrive in milestone M4");
            }
            public static TemplatePart childNothing() { return text(0, null); }
            public static TemplatePart attr(String value) { return text(2, value); }
            public static TemplatePart attr(int value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(long value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(char value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(float value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(double value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(boolean value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(Object value) { return text(2, value == null ? null : String.valueOf(value)); }
            public static TemplatePart attrNothing() { return text(2, null); }
            public static TemplatePart bool(boolean value) { TemplatePart p = new TemplatePart(3); p.__flag = value; return p; }
            public static TemplatePart prop(String value) { return text(4, value); }
            public static TemplatePart prop(boolean value) { TemplatePart p = new TemplatePart(5); p.__flag = value; return p; }
            public static TemplatePart prop(int value) { TemplatePart p = new TemplatePart(6); p.__number = value; return p; }
            public static TemplatePart prop(char value) { return text(4, String.valueOf(value)); }
            public static TemplatePart prop(double value) { TemplatePart p = new TemplatePart(6); p.__number = value; return p; }
            public static TemplatePart propNothing() { return text(4, null); }
            public static TemplatePart on(__JavawDomRunnable listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 7);
                p.__run = listener;
                return p;
            }
            public static TemplatePart on(EventListener<Event> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 8);
                p.__handler = listener;
                return p;
            }
            public static TemplatePart eventNothing() { return new TemplatePart(9); }
            public static TemplatePart __componentProp(int key, Object value) {
                TemplatePart p = new TemplatePart(16);
                p.__key = key;
                p.__value = value;
                return p;
            }
            public static TemplatePart onMouse(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onMouse(EventListener<MouseEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 10);
                p.__mouse = listener;
                return p;
            }
            public static TemplatePart onPointer(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onPointer(EventListener<PointerEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 11);
                p.__pointer = listener;
                return p;
            }
            public static TemplatePart onKeyboard(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onKeyboard(EventListener<KeyboardEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 12);
                p.__keyboard = listener;
                return p;
            }
            public static TemplatePart onFocus(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onFocus(EventListener<FocusEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 13);
                p.__focus = listener;
                return p;
            }
            public static TemplatePart onInput(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onInput(EventListener<InputEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 14);
                p.__input = listener;
                return p;
            }
            public static TemplatePart onSubmit(__JavawDomRunnable listener) { return on(listener); }
            public static TemplatePart onSubmit(EventListener<SubmitEvent> listener) {
                TemplatePart p = new TemplatePart(listener == null ? 9 : 15);
                p.__submit = listener;
                return p;
            }
        }
        final class __WcInstance {
            int id;
            int template;
            int[] kinds;
            int[] keys;
            boolean[] set;
            String[] texts;
            boolean[] flags;
            double[] numbers;
            Object[] objects;
            __WcInstance(int id, int template, int count) {
                this.id = id;
                this.template = template;
                kinds = new int[count];
                keys = new int[count];
                set = new boolean[count];
                texts = new String[count];
                flags = new boolean[count];
                numbers = new double[count];
                objects = new Object[count];
            }
        }
        final class __WcRenderer {
            static void renderRoot(WebComponent c, Template t) {
                if (t == null && c.__rootTemplate == null) return;
                if (c.__rootTemplate == null) c.__rootTemplate = new __WcInstance(__wc_rootRange(c.host()), 0, 1);
                commitPart(c, c.__rootTemplate, 0, Parts.child(t));
            }
            static void commit(WebComponent c, __WcInstance inst, Template t) {
                for (int i = 0; i < t.__parts.length; i++) commitPart(c, inst, i, t.__parts[i]);
            }
            static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }
            static boolean sameValue(Object a, Object b) { return a == null ? b == null : a.equals(b); }
            static void commitPart(WebComponent c, __WcInstance inst, int i, TemplatePart p) {
                int kind = p.__kind;
                boolean first = !inst.set[i];
                if (kind == 0) {
                    if (!first && inst.kinds[i] == 0 && same(inst.texts[i], p.__text)) return;
                    boolean hadContent = !first && (inst.kinds[i] == 1 || inst.texts[i] != null);
                    dropChild(c, inst, i);
                    inst.kinds[i] = 0;
                    inst.texts[i] = p.__text;
                    inst.set[i] = true;
                    if (p.__text == null && !hadContent) return;
                    __wc_childText(inst.id, i, p.__text == null ? "" : p.__text, p.__text != null);
                } else if (kind == 1) {
                    Template t = p.__template;
                    if (!first && inst.kinds[i] == 1) {
                        __WcInstance current = (__WcInstance) inst.objects[i];
                        if (current.template == t.__id) { commit(c, current, t); return; }
                    }
                    dropChild(c, inst, i);
                    __WcInstance child = new __WcInstance(__wc_createInstance(c.host(), t.__id), t.__id, t.__parts.length);
                    c.__templates.put(Integer.valueOf(child.id), child);
                    commit(c, child, t);
                    inst.kinds[i] = 1;
                    inst.objects[i] = child;
                    inst.set[i] = true;
                    __wc_childTemplate(inst.id, i, child.id);
                } else if (kind == 2 || kind == 4) {
                    if (!first && inst.kinds[i] == kind && same(inst.texts[i], p.__text)) return;
                    inst.kinds[i] = kind;
                    inst.texts[i] = p.__text;
                    inst.set[i] = true;
                    String value = p.__text == null ? "" : p.__text;
                    if (kind == 2) __wc_attr(inst.id, i, value, p.__text != null);
                    else __wc_propString(inst.id, i, value, p.__text != null);
                } else if (kind == 3 || kind == 5) {
                    if (!first && inst.kinds[i] == kind && inst.flags[i] == p.__flag) return;
                    inst.kinds[i] = kind;
                    inst.flags[i] = p.__flag;
                    inst.set[i] = true;
                    if (kind == 3) __wc_bool(inst.id, i, p.__flag);
                    else __wc_propBool(inst.id, i, p.__flag);
                } else if (kind == 6) {
                    if (!first && inst.kinds[i] == 6 && !WebComponent.__wc_doubleChanged(inst.numbers[i], p.__number)) return;
                    inst.kinds[i] = 6;
                    inst.numbers[i] = p.__number;
                    inst.set[i] = true;
                    __wc_propNumber(inst.id, i, p.__number);
                } else if (kind == 16) {
                    // .name on a Java component of this module: set its field through the generated setter, unmarshalled.
                    if (!first && inst.kinds[i] == 16 && inst.keys[i] == p.__key && sameValue(inst.objects[i], p.__value)) return;
                    WebComponent target = __wc_peer(inst.id, i);
                    if (target == null) throw new IllegalStateException("property binding target is not an upgraded component of this module");
                    __JavawComponentFactories.__wc_assignProperty(target, p.__key, p.__value);
                    // Recorded only once set, so a failed assignment is retried by the next render.
                    inst.kinds[i] = 16;
                    inst.keys[i] = p.__key;
                    inst.objects[i] = p.__value;
                    inst.set[i] = true;
                } else {
                    // Events: the JS listener is stable and calls the current Java listener; only adding or
                    // removing it crosses to the host.
                    boolean had = inst.set[i] && inst.kinds[i] != 9;
                    boolean has = kind != 9;
                    inst.kinds[i] = kind;
                    inst.objects[i] = p;
                    inst.set[i] = true;
                    if (had != has) __wc_event(inst.id, i, has);
                }
            }
            static void dropChild(WebComponent c, __WcInstance inst, int i) {
                if (inst.set[i] && inst.kinds[i] == 1 && inst.objects[i] != null) {
                    release(c, (__WcInstance) inst.objects[i]);
                    inst.objects[i] = null;
                }
            }
            static void release(WebComponent c, __WcInstance inst) {
                for (int i = 0; i < inst.kinds.length; i++) dropChild(c, inst, i);
                c.__templates.remove(Integer.valueOf(inst.id));
                __wc_release(inst.id);
            }
            public static void __wc_dispatch(WebComponent c, int id, int index, JSObject event) {
                __WcInstance inst = c.__templates.get(Integer.valueOf(id));
                if (inst == null) return;
                int kind = inst.kinds[index];
                if (kind < 7 || kind == 9) return;
                TemplatePart part = (TemplatePart) inst.objects[index];
                if (kind == 7) part.__run.run();
                else if (kind == 8) part.__handler.handleEvent(new Event(event));
                else if (kind == 10) part.__mouse.handleEvent(new MouseEvent(event));
                else if (kind == 11) part.__pointer.handleEvent(new PointerEvent(event));
                else if (kind == 12) part.__keyboard.handleEvent(new KeyboardEvent(event));
                else if (kind == 13) part.__focus.handleEvent(new FocusEvent(event));
                else if (kind == 14) part.__input.handleEvent(new InputEvent(event));
                else if (kind == 15) part.__submit.handleEvent(new SubmitEvent(event));
            }
            @JS("return runtime.wcRootRange(host);")
            public static native int __wc_rootRange(Element host);
            @JS("return runtime.wcCreateInstance(host, template);")
            public static native int __wc_createInstance(Element host, int template);
            @JS("runtime.wcChildText(id, index, text, present);")
            public static native void __wc_childText(int id, int index, String text, boolean present);
            @JS("runtime.wcChildTemplate(id, index, child);")
            public static native void __wc_childTemplate(int id, int index, int child);
            @JS("runtime.wcAttr(id, index, value, present);")
            public static native void __wc_attr(int id, int index, String value, boolean present);
            @JS("runtime.wcBool(id, index, on);")
            public static native void __wc_bool(int id, int index, boolean on);
            @JS("runtime.wcProp(id, index, present ? value : null);")
            public static native void __wc_propString(int id, int index, String value, boolean present);
            @JS("runtime.wcProp(id, index, value);")
            public static native void __wc_propBool(int id, int index, boolean value);
            @JS("runtime.wcProp(id, index, value);")
            public static native void __wc_propNumber(int id, int index, double value);
            @JS("runtime.wcEvent(id, index, on);")
            public static native void __wc_event(int id, int index, boolean on);
            @JS("return runtime.wcPeer(id, index);")
            public static native WebComponent __wc_peer(int id, int index);
            @JS("runtime.wcRelease(id);")
            public static native void __wc_release(int id);
        }
        """;

    /** Members added to {@code WebComponent} when the module uses templates. */
    static final String COMPONENT_MEMBERS = """
            HashMap<Integer, __WcInstance> __templates = new HashMap<Integer, __WcInstance>();
            __WcInstance __rootTemplate;
            protected Template render() { return null; }
            protected void update(Changes changes) { __WcRenderer.renderRoot(this, render()); }
        """;
}
