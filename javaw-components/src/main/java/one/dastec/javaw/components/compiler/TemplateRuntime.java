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
        interface KeyFunction<T> { public Object key(T item); }
        interface ItemTemplate<T> { public Template render(T item); }
        interface TemplateSupplier { public Template get(); }
        final class Repeat {
            ArrayList<Object> __keys = new ArrayList<Object>();
            ArrayList<TemplatePart> __items = new ArrayList<TemplatePart>();
            Repeat() {}
        }
        final class ClassMap {
            Map<String, Boolean> __map;
            ClassMap(Map<String, Boolean> map) { __map = map; }
        }
        final class StyleMap {
            Map<String, String> __map;
            StyleMap(Map<String, String> map) { __map = map; }
        }
        final class Ref<T extends Element> {
            Element __element;
            public Ref() {}
            public T value() { return (T) __element; }
        }
        final class RefDirective {
            Ref __ref;
            RefDirective(Ref ref) { __ref = ref; }
        }
        final class __WcClassState {
            HashSet<String> applied = new HashSet<String>();
            __WcClassState() {}
        }
        final class __WcStyleState {
            HashMap<String, String> values = new HashMap<String, String>();
            __WcStyleState() {}
        }
        final class __WcRefState {
            Ref ref;
            Element element;
            __WcRefState() {}
        }
        final class __WcList {
            Object[] keys = new Object[0];
            __WcInstance[] items = new __WcInstance[0];
            ArrayList<__WcInstance> pending = new ArrayList<__WcInstance>();
            __WcList() {}
        }
        final class __WcUniqueKey {
            __WcUniqueKey() {}
        }
        final class __WcNullKey {
            __WcNullKey() {}
            public boolean equals(Object other) { return other instanceof __WcNullKey; }
            public int hashCode() { return 0; }
        }
        final class Templates {
            public static <T> Repeat repeat(Iterable<T> items, KeyFunction<T> key, ItemTemplate<T> template) { return WebComponent.__wc_repeat(items, key, template); }
            public static Template when(boolean condition, TemplateSupplier trueCase) { return condition ? trueCase.get() : null; }
            public static Template when(boolean condition, TemplateSupplier trueCase, TemplateSupplier falseCase) { return condition ? trueCase.get() : falseCase.get(); }
            public static Template nothing() { return null; }
            public static Css css(String text) { return new Css(text); }
            public static ClassMap classMap(Map<String, Boolean> classes) { return WebComponent.__wc_classMap(classes); }
            public static StyleMap styleMap(Map<String, String> styles) { return WebComponent.__wc_styleMap(styles); }
            public static <T extends Element> RefDirective ref(Ref<T> ref) { return new RefDirective(ref); }
        }
        final class TemplatePart {
            int __kind;
            String __text;
            boolean __flag;
            double __number;
            Template __template;
            Repeat __repeat;
            int __key;
            // A component property value, or the ClassMap, StyleMap or Ref of kinds 18 to 20 (no extra fields:
            // a part's size counts in every rendered template).
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
                if (value instanceof Repeat repeat) return child(repeat);
                if (value instanceof Iterable<?> iterable) return child(iterable);
                if (value instanceof ClassMap || value instanceof StyleMap || value instanceof RefDirective)
                    throw new IllegalArgumentException("classMap, styleMap and ref cannot be bound as child content");
                return text(0, String.valueOf(value));
            }
            public static TemplatePart child(Iterable<?> value) {
                if (value == null) return text(0, null);
                // A plain list: items are matched by index.
                Repeat list = new Repeat();
                int index = 0;
                for (Object item : value) {
                    list.__keys.add(Integer.valueOf(index));
                    list.__items.add(child(item));
                    index++;
                }
                return child(list);
            }
            public static TemplatePart child(Repeat value) {
                if (value == null) return text(0, null);
                TemplatePart p = new TemplatePart(17);
                p.__repeat = value;
                return p;
            }
            public static TemplatePart childNothing() { return text(0, null); }
            public static TemplatePart attr(String value) { return text(2, value); }
            public static TemplatePart attr(int value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(long value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(char value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(float value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(double value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(boolean value) { return text(2, String.valueOf(value)); }
            public static TemplatePart attr(Object value) {
                if (value instanceof ClassMap) return attr((ClassMap) value);
                if (value instanceof StyleMap) return attr((StyleMap) value);
                if (value instanceof RefDirective) throw new IllegalArgumentException("ref can only be bound in element position");
                return text(2, value == null ? null : String.valueOf(value));
            }
            public static TemplatePart attrNothing() { return text(2, null); }
            public static TemplatePart attr(ClassMap value) {
                if (value == null) return text(2, null);
                TemplatePart p = new TemplatePart(18);
                p.__value = value;
                return p;
            }
            public static TemplatePart attr(StyleMap value) {
                if (value == null) return text(2, null);
                TemplatePart p = new TemplatePart(19);
                p.__value = value;
                return p;
            }
            public static TemplatePart element(RefDirective value) {
                TemplatePart p = new TemplatePart(20);
                if (value != null) p.__value = value.__ref;
                return p;
            }
            public static TemplatePart elementNothing() { return new TemplatePart(20); }
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
                    boolean hadContent = !first && (inst.kinds[i] == 1 || inst.kinds[i] == 17 || inst.texts[i] != null);
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
                } else if (kind == 17) {
                    __WcList list = !first && inst.kinds[i] == 17 ? (__WcList) inst.objects[i] : null;
                    if (list == null) {
                        boolean hadContent = !first && (inst.kinds[i] == 1 || inst.texts[i] != null);
                        dropChild(c, inst, i);
                        if (hadContent) __wc_childText(inst.id, i, "", false);
                        list = new __WcList();
                        inst.kinds[i] = 17;
                        inst.objects[i] = list;
                        inst.texts[i] = null;
                        inst.set[i] = true;
                    }
                    reconcile(c, inst.id, i, list, p.__repeat);
                } else if (kind == 18) {
                    commitClassMap(inst, i, ((ClassMap) p.__value).__map, first || inst.kinds[i] != 18 ? null : (__WcClassState) inst.objects[i]);
                } else if (kind == 19) {
                    commitStyleMap(inst, i, ((StyleMap) p.__value).__map, first || inst.kinds[i] != 19 ? null : (__WcStyleState) inst.objects[i]);
                } else if (kind == 20) {
                    // ref, as lit-html's: a new Ref clears the old one; the element is assigned again if the Ref was changed elsewhere.
                    __WcRefState state = first || inst.kinds[i] != 20 ? new __WcRefState() : (__WcRefState) inst.objects[i];
                    Ref ref = (Ref) p.__value;
                    boolean changed = ref != state.ref;
                    if (changed && state.ref != null) state.ref.__element = null;
                    if (ref != null && (changed || ref.__element != state.element)) {
                        if (state.element == null) state.element = __wc_element(inst.id, i);
                        ref.__element = state.element;
                    }
                    state.ref = ref;
                    inst.kinds[i] = 20;
                    inst.objects[i] = state;
                    inst.set[i] = true;
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
            static boolean truthy(Boolean value) { return value != null && value.booleanValue(); }

            /**
             * classMap, as lit-html's: the first commit sets the attribute (static text plus the true keys), later
             * commits add and remove single classes, so classes added by other code stay. The host skips the
             * attribute's static classes.
             */
            static void commitClassMap(__WcInstance inst, int i, Map<String, Boolean> classes, __WcClassState state) {
                if (state == null) {
                    state = new __WcClassState();
                    String names = "";
                    for (Map.Entry<String, Boolean> entry : classes.entrySet()) {
                        if (!truthy(entry.getValue())) continue;
                        names = names.isEmpty() ? entry.getKey() : names + " " + entry.getKey();
                        state.applied.add(entry.getKey());
                    }
                    __wc_classMap(inst.id, i, " " + names + " ");
                    inst.kinds[i] = 18;
                    inst.objects[i] = state;
                    inst.set[i] = true;
                    return;
                }
                ArrayList<String> stale = new ArrayList<String>();
                for (String name : state.applied) if (!classes.containsKey(name)) stale.add(name);
                for (int k = 0; k < stale.size(); k++) {
                    state.applied.remove(stale.get(k));
                    __wc_classToggle(inst.id, i, stale.get(k), false);
                }
                for (Map.Entry<String, Boolean> entry : classes.entrySet()) {
                    boolean on = truthy(entry.getValue());
                    if (on == state.applied.contains(entry.getKey())) continue;
                    if (on) state.applied.add(entry.getKey()); else state.applied.remove(entry.getKey());
                    __wc_classToggle(inst.id, i, entry.getKey(), on);
                }
            }

            /**
             * styleMap, as lit-html's: the first commit sets the attribute, later commits set or remove single
             * properties. Unchanged values make no host call.
             */
            static void commitStyleMap(__WcInstance inst, int i, Map<String, String> styles, __WcStyleState state) {
                if (state == null) {
                    state = new __WcStyleState();
                    String text = "";
                    for (Map.Entry<String, String> entry : styles.entrySet()) {
                        state.values.put(entry.getKey(), entry.getValue());
                        if (entry.getValue() != null) text = text + styleName(entry.getKey()) + ":" + entry.getValue() + ";";
                    }
                    __wc_styleMap(inst.id, i, text);
                    inst.kinds[i] = 19;
                    inst.objects[i] = state;
                    inst.set[i] = true;
                    return;
                }
                ArrayList<String> stale = new ArrayList<String>();
                for (String name : state.values.keySet()) if (styles.get(name) == null) stale.add(name);
                for (int k = 0; k < stale.size(); k++) {
                    // As lit-html, a key of the previous render is cleared even if its value was null.
                    state.values.remove(stale.get(k));
                    __wc_styleRemove(inst.id, i, stale.get(k));
                }
                for (Map.Entry<String, String> entry : styles.entrySet()) {
                    String value = entry.getValue();
                    if (value == null) continue;
                    boolean known = state.values.containsKey(entry.getKey());
                    String old = state.values.put(entry.getKey(), value);
                    if (!known || !value.equals(old)) __wc_styleSet(inst.id, i, entry.getKey(), value);
                }
            }

            /** A camelCase style name as a CSS property name, as lit-html converts it ({@code webkitFoo} to {@code -webkit-foo}). */
            static String styleName(String key) {
                if (key.indexOf("-") >= 0) return key;
                String out = "";
                if (key.startsWith("webkit") || key.startsWith("moz") || key.startsWith("ms") || key.startsWith("o")) {
                    int length = key.startsWith("webkit") ? 6 : key.startsWith("moz") ? 3 : key.startsWith("ms") ? 2 : 1;
                    if (key.length() > length && key.charAt(length) >= 'A' && key.charAt(length) <= 'Z') out = "-";
                }
                for (int k = 0; k < key.length(); k++) {
                    char ch = key.charAt(k);
                    if (ch >= 'A' && ch <= 'Z') out = out + "-" + (char) (ch + 32);
                    else out = out + ch;
                }
                return out;
            }

            static void dropChild(WebComponent c, __WcInstance inst, int i) {
                if (inst.set[i] && inst.kinds[i] == 1 && inst.objects[i] != null) {
                    release(c, (__WcInstance) inst.objects[i]);
                    inst.objects[i] = null;
                } else if (inst.set[i] && inst.kinds[i] == 17 && inst.objects[i] != null) {
                    // The range is cleared by the caller; release the items' instances.
                    __WcList list = (__WcList) inst.objects[i];
                    for (int k = 0; k < list.items.length; k++) release(c, list.items[k]);
                    inst.objects[i] = null;
                }
            }

            /**
             * Keyed list update, ported from lit-html's repeat directive: a two-ended scan matches head to head,
             * tail to tail, old head to new tail and old tail to new head; the rest uses key maps built on
             * demand. Matched items keep their DOM and are moved; others are inserted or removed.
             */
            static void reconcile(WebComponent c, int listId, int binding, __WcList list, Repeat next) {
                try {
                    reconcileItems(c, listId, binding, list, next);
                } catch (RuntimeException failure) {
                    // An item failed part way: drop the whole list (its DOM and every item), so the next render rebuilds it.
                    for (int k = 0; k < list.items.length; k++) if (list.items[k] != null) release(c, list.items[k]);
                    for (int k = 0; k < list.pending.size(); k++) release(c, list.pending.get(k));
                    list.pending.clear();
                    list.keys = new Object[0];
                    list.items = new __WcInstance[0];
                    __wc_childText(listId, binding, "", false);
                    throw failure;
                }
                list.pending.clear();
            }

            static void reconcileItems(WebComponent c, int listId, int binding, __WcList list, Repeat next) {
                int n = next.__keys.size();
                Object[] newKeys = new Object[n];
                HashMap<Object, Integer> seen = new HashMap<Object, Integer>();
                for (int k = 0; k < n; k++) {
                    Object key = next.__keys.get(k);
                    if (key == null) key = new __WcNullKey();
                    if (seen.containsKey(key)) {
                        __wc_reportDuplicate(String.valueOf(next.__keys.get(k)));
                        key = new __WcUniqueKey();
                    }
                    seen.put(key, Integer.valueOf(k));
                    newKeys[k] = key;
                }
                Object[] oldKeys = list.keys;
                __WcInstance[] oldParts = new __WcInstance[list.items.length];
                for (int k = 0; k < oldParts.length; k++) oldParts[k] = list.items[k];
                __WcInstance[] newParts = new __WcInstance[n];
                int oldHead = 0, oldTail = oldParts.length - 1, newHead = 0, newTail = n - 1;
                HashMap<Object, Integer> newIndex = null;
                HashMap<Object, Integer> oldIndex = null;
                while (oldHead <= oldTail && newHead <= newTail) {
                    if (oldParts[oldHead] == null) {
                        oldHead++;
                    } else if (oldParts[oldTail] == null) {
                        oldTail--;
                    } else if (oldKeys[oldHead].equals(newKeys[newHead])) {
                        newParts[newHead] = item(c, oldParts[oldHead], next.__items.get(newHead));
                        oldHead++;
                        newHead++;
                    } else if (oldKeys[oldTail].equals(newKeys[newTail])) {
                        newParts[newTail] = item(c, oldParts[oldTail], next.__items.get(newTail));
                        oldTail--;
                        newTail--;
                    } else if (oldKeys[oldHead].equals(newKeys[newTail])) {
                        newParts[newTail] = item(c, oldParts[oldHead], next.__items.get(newTail));
                        __wc_listMove(listId, binding, oldParts[oldHead].id, newTail + 1 < n ? newParts[newTail + 1].id : 0);
                        oldHead++;
                        newTail--;
                    } else if (oldKeys[oldTail].equals(newKeys[newHead])) {
                        newParts[newHead] = item(c, oldParts[oldTail], next.__items.get(newHead));
                        __wc_listMove(listId, binding, oldParts[oldTail].id, oldParts[oldHead].id);
                        oldTail--;
                        newHead++;
                    } else {
                        if (newIndex == null) {
                            newIndex = new HashMap<Object, Integer>();
                            for (int k = newHead; k <= newTail; k++) newIndex.put(newKeys[k], Integer.valueOf(k));
                            oldIndex = new HashMap<Object, Integer>();
                            for (int k = oldHead; k <= oldTail; k++) oldIndex.put(oldKeys[k], Integer.valueOf(k));
                        }
                        if (!newIndex.containsKey(oldKeys[oldHead])) {
                            remove(c, listId, binding, oldParts[oldHead]);
                            oldHead++;
                        } else if (!newIndex.containsKey(oldKeys[oldTail])) {
                            remove(c, listId, binding, oldParts[oldTail]);
                            oldTail--;
                        } else {
                            Integer found = oldIndex.get(newKeys[newHead]);
                            __WcInstance oldPart = found == null ? null : oldParts[found.intValue()];
                            if (oldPart == null) {
                                __WcInstance fresh = created(list, __wc_listInsert(listId, binding, oldParts[oldHead].id));
                                newParts[newHead] = item(c, fresh, next.__items.get(newHead));
                            } else {
                                newParts[newHead] = item(c, oldPart, next.__items.get(newHead));
                                __wc_listMove(listId, binding, oldPart.id, oldParts[oldHead].id);
                                oldParts[found.intValue()] = null;
                            }
                            newHead++;
                        }
                    }
                }
                while (newHead <= newTail) {
                    __WcInstance fresh = created(list, __wc_listInsert(listId, binding, newTail + 1 < n ? newParts[newTail + 1].id : 0));
                    newParts[newHead] = item(c, fresh, next.__items.get(newHead));
                    newHead++;
                }
                while (oldHead <= oldTail) {
                    __WcInstance oldPart = oldParts[oldHead];
                    oldHead++;
                    if (oldPart != null) remove(c, listId, binding, oldPart);
                }
                list.keys = newKeys;
                list.items = newParts;
            }

            /** A new item, remembered until the list update completes so a failure can release it. */
            static __WcInstance created(__WcList list, int id) {
                __WcInstance item = new __WcInstance(id, 0, 1);
                list.pending.add(item);
                return item;
            }

            /** Brings an item's range up to date with its value; an item is a one-binding instance. */
            static __WcInstance item(WebComponent c, __WcInstance item, TemplatePart value) {
                commitPart(c, item, 0, value);
                return item;
            }

            static void remove(WebComponent c, int listId, int binding, __WcInstance item) {
                __wc_listRemove(item.id);
                release(c, item);
            }
            static void release(WebComponent c, __WcInstance inst) {
                for (int i = 0; i < inst.kinds.length; i++) {
                    if (inst.set[i] && inst.kinds[i] == 20) {
                        // A removed ref binding clears its Ref unless the Ref was given another element since.
                        __WcRefState state = (__WcRefState) inst.objects[i];
                        if (state.ref != null && state.ref.__element == state.element) state.ref.__element = null;
                    }
                    dropChild(c, inst, i);
                }
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
            @JS("return runtime.wcListInsert(id, index, before);")
            public static native int __wc_listInsert(int id, int index, int before);
            @JS("runtime.wcListMove(id, index, item, before);")
            public static native void __wc_listMove(int id, int index, int item, int before);
            @JS("runtime.wcListRemove(item);")
            public static native void __wc_listRemove(int item);
            @JS("runtime.reportCallbackError(new Error('repeat: duplicate key ' + key + '; the later item gets its own DOM'), 'repeat');")
            public static native void __wc_reportDuplicate(String key);
            @JS("runtime.wcClassMap(id, index, text);")
            public static native void __wc_classMap(int id, int index, String text);
            @JS("runtime.wcClassToggle(id, index, name, on);")
            public static native void __wc_classToggle(int id, int index, String name, boolean on);
            @JS("runtime.wcStyleMap(id, index, text);")
            public static native void __wc_styleMap(int id, int index, String text);
            @JS("runtime.wcStyleSet(id, index, name, value);")
            public static native void __wc_styleSet(int id, int index, String name, String value);
            @JS("runtime.wcStyleRemove(id, index, name);")
            public static native void __wc_styleRemove(int id, int index, String name);
            @JS("return runtime.wcElement(id, index);")
            public static native Element __wc_element(int id, int index);
            @JS("runtime.wcRelease(id);")
            public static native void __wc_release(int id);
        }
        """;

    /** Members added to {@code WebComponent} when the module uses templates. */
    static final String COMPONENT_MEMBERS = """
            HashMap<Integer, __WcInstance> __templates = new HashMap<Integer, __WcInstance>();
            __WcInstance __rootTemplate;
            protected Template render() { return null; }
            protected static <T> Repeat repeat(Iterable<T> items, KeyFunction<T> key, ItemTemplate<T> template) { return __wc_repeat(items, key, template); }
            public static <T> Repeat __wc_repeat(Iterable<T> items, KeyFunction<T> key, ItemTemplate<T> template) {
                Repeat list = new Repeat();
                for (T item : items) {
                    list.__keys.add(key.key(item));
                    list.__items.add(Parts.child(template.render(item)));
                }
                return list;
            }
            protected static Template when(boolean condition, TemplateSupplier trueCase) { return condition ? trueCase.get() : null; }
            protected static Template when(boolean condition, TemplateSupplier trueCase, TemplateSupplier falseCase) { return condition ? trueCase.get() : falseCase.get(); }
            protected static Template nothing() { return null; }
            protected static ClassMap classMap(Map<String, Boolean> classes) { return __wc_classMap(classes); }
            protected static StyleMap styleMap(Map<String, String> styles) { return __wc_styleMap(styles); }
            protected static <T extends Element> RefDirective ref(Ref<T> ref) { return new RefDirective(ref); }
            public static ClassMap __wc_classMap(Map<String, Boolean> classes) {
                if (classes == null) throw new NullPointerException("classMap: classes is null");
                return new ClassMap(classes);
            }
            public static StyleMap __wc_styleMap(Map<String, String> styles) {
                if (styles == null) throw new NullPointerException("styleMap: styles is null");
                return new StyleMap(styles);
            }
            protected void update(Changes changes) { __WcRenderer.renderRoot(this, render()); }
        """;
}
