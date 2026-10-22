package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DomHelper {

    public static Set<String> collectDomImports(CompilationUnit cu) {
        Set<String> imports = new LinkedHashSet<>();
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String mName = call.getNameAsString();
            if (isTerminalMethod(mName, call)) {
                addTerminalImports(mName, imports);
            } else if (isDomStaticMethod(mName) || isDomInstanceMethod(mName)) {
                if (mName.equals("drawImage") && call.getArguments().size() > 5) {
                    imports.add("drawImageFull");
                } else if (isWebGLMethod(mName, call)) {
                    imports.add("webgl_" + mName);
                } else {
                    imports.add(mName);
                }
            } else if (mName.equals("add") || mName.equals("remove") || mName.equals("contains") || mName.equals("toggle")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.contains("classList") || scopeStr.contains("ClassList")) {
                        if (mName.equals("add")) imports.add("addClass");
                        else if (mName.equals("remove")) imports.add("removeClass");
                        else if (mName.equals("contains")) imports.add("hasClass");
                        else if (mName.equals("toggle")) imports.add("toggleClass");
                    }
                }
            } else if (isJsStaticMethod(mName, call) || isJsInstanceMethod(mName)) {
                addJsImports(mName, call, imports);
            } else if (isThreadMethod(mName, call)) {
                addThreadImports(mName, imports);
            }
        }
        return imports;
    }

    private static boolean isTerminalMethod(String mName, MethodCallExpr call) {
        if (!call.getScope().isPresent()) return false;
        String scope = call.getScope().get().toString();
        return scope.equals("WASITerminal") || scope.endsWith(".WASITerminal") ||
               scope.equals("Terminal") || scope.endsWith(".Terminal");
    }

    private static void addTerminalImports(String mName, Set<String> imports) {
        if (mName.equals("clear")) {
            imports.add("dom_terminal_clear");
        } else if (mName.equals("getColumns")) {
            imports.add("dom_terminal_get_columns");
        } else if (mName.equals("getRows")) {
            imports.add("dom_terminal_get_rows");
        }
    }

    private static boolean isThreadMethod(String mName, MethodCallExpr call) {
        if (!call.getScope().isPresent()) return false;
        String scope = call.getScope().get().toString();
        return scope.equals("WasmThread") || scope.endsWith(".WasmThread");
    }

    private static void addThreadImports(String mName, Set<String> imports) {
        if (mName.equals("spawn")) {
            imports.add("thread_spawn");
        } else if (mName.equals("sleep")) {
            imports.add("thread_sleep");
        } else if (mName.equals("currentThreadId")) {
            imports.add("thread_current_id");
        } else if (mName.equals("hardwareConcurrency")) {
            imports.add("thread_hardware_concurrency");
        }
    }

    private static void addJsImports(String mName, MethodCallExpr call, Set<String> imports) {
        if (mName.equals("global") || mName.equals("getWindow")) {
            imports.add("js_global");
        } else if (mName.equals("eval")) {
            imports.add("js_eval");
        } else if (mName.equals("createObject")) {
            imports.add("js_createObject");
        } else if (mName.equals("createArray")) {
            imports.add("js_createArray");
        } else if (mName.equals("undefined")) {
            imports.add("js_undefined");
        } else if (mName.equals("nullValue")) {
            imports.add("js_null");
        } else if (mName.equals("getInstance")) {
            if (call != null && call.getScope().isPresent() && call.getScope().get().toString().contains("Null")) {
                imports.add("js_null");
            } else {
                imports.add("js_undefined");
            }
        } else if (mName.equals("forSymbol")) {
            imports.add("js_symbol_for");
        } else if (mName.equals("typeError")) {
            imports.add("js_type_error_of");
        } else if (mName.equals("rangeError")) {
            imports.add("js_range_error_of");
        } else if (mName.equals("syntaxError")) {
            imports.add("js_syntax_error_of");
        } else if (mName.equals("of")) {
            String scope = (call != null && call.getScope().isPresent()) ? call.getScope().get().toString() : "";
            if (scope.contains("Number")) {
                imports.add("js_number_of_double");
                imports.add("js_number_of_int");
            } else if (scope.contains("Boolean")) {
                imports.add("js_boolean_of");
            } else if (scope.contains("String")) {
                imports.add("js_string_of");
            } else if (scope.contains("BigInt")) {
                imports.add("js_bigint_of_i64");
                imports.add("js_bigint_of_str");
            } else if (scope.contains("Symbol")) {
                imports.add("js_symbol_of");
            } else if (scope.contains("Error")) {
                imports.add("js_error_of");
            } else {
                imports.add("js_number_of_int");
                imports.add("js_number_of_double");
                imports.add("js_boolean_of");
                imports.add("js_string_of");
                imports.add("js_bigint_of_i64");
            }
        } else if (mName.equals("getTypeof")) {
            imports.add("js_typeof");
        } else if (mName.equals("isNumber")) {
            imports.add("js_isNumber");
        } else if (mName.equals("isBoolean")) {
            imports.add("js_isBoolean");
        } else if (mName.equals("isString")) {
            imports.add("js_isString");
        } else if (mName.equals("isBigInt")) {
            imports.add("js_isBigInt");
        } else if (mName.equals("isSymbol")) {
            imports.add("js_isSymbol");
        } else if (mName.equals("isUndefined")) {
            imports.add("js_isUndefined");
        } else if (mName.equals("isNull")) {
            imports.add("js_isNull");
        } else if (mName.equals("isObject")) {
            imports.add("js_isObject");
        } else if (mName.equals("isFunction")) {
            imports.add("js_isFunction");
        } else if (mName.equals("isError")) {
            imports.add("js_isError");
        } else if (mName.equals("instanceOf")) {
            imports.add("js_instanceof");
        } else if (mName.equals("strictEquals")) {
            imports.add("js_strictEquals");
        } else if (mName.equals("isNaN")) {
            imports.add("js_isNaN");
        } else if (mName.equals("isFinite")) {
            imports.add("js_isFinite");
        } else if (mName.equals("isInteger")) {
            imports.add("js_isInteger");
        } else if (mName.equals("doubleValue")) {
            imports.add("js_asDouble");
        } else if (mName.equals("intValue")) {
            imports.add("js_asInt");
        } else if (mName.equals("booleanValue") || mName.equals("asBooleanPrimitive")) {
            imports.add("js_asBoolean");
        } else if (mName.equals("stringValue")) {
            imports.add("js_asString");
        } else if (mName.equals("getDescription")) {
            imports.add("js_symbolDescription");
        } else if (mName.equals("getMessage")) {
            imports.add("js_errorMessage");
        } else if (mName.equals("getName")) {
            imports.add("js_errorName");
        } else if (mName.equals("getStack")) {
            imports.add("js_errorStack");
        } else if (mName.equals("get")) {
            imports.add("js_get");
        } else if (mName.equals("getString")) {
            imports.add("js_getString");
        } else if (mName.equals("getInt")) {
            imports.add("js_getInt");
        } else if (mName.equals("getDouble")) {
            imports.add("js_getDouble");
        } else if (mName.equals("getBoolean")) {
            imports.add("js_getBoolean");
        } else if (mName.equals("set")) {
            imports.add("js_setString");
            imports.add("js_setInt");
            imports.add("js_setDouble");
            imports.add("js_setBoolean");
            imports.add("js_setObject");
        } else if (mName.equals("call")) {
            imports.add("js_call0");
            imports.add("js_call1_str");
            imports.add("js_call1_int");
            imports.add("js_call1_double");
            imports.add("js_call1_bool");
            imports.add("js_call1_obj");
            imports.add("js_call2_str_str");
            imports.add("js_call2_str_int");
            imports.add("js_call2_str_obj");
            imports.add("js_call2_obj_obj");
        } else if (mName.equals("callString")) {
            imports.add("js_callString0");
            imports.add("js_callString1_str");
        } else if (mName.equals("callInt")) {
            imports.add("js_callInt0");
            imports.add("js_callInt1_int");
        } else if (mName.equals("callDouble")) {
            imports.add("js_callDouble0");
        } else if (mName.equals("callBoolean")) {
            imports.add("js_callBoolean0");
        } else if (mName.equals("asString")) {
            imports.add("js_asString");
        } else if (mName.equals("asInt")) {
            imports.add("js_asInt");
        } else if (mName.equals("asDouble")) {
            imports.add("js_asDouble");
        } else if (mName.equals("asBoolean")) {
            imports.add("js_asBoolean");
        } else if (mName.equals("isArray")) {
            imports.add("js_isArray");
        } else if (mName.equals("length")) {
            imports.add("js_length");
        } else if (mName.equals("getIndex")) {
            imports.add("js_getIndex");
        } else if (mName.equals("setIndex")) {
            imports.add("js_setIndexStr");
            imports.add("js_setIndexInt");
            imports.add("js_setIndexDouble");
            imports.add("js_setIndexBoolean");
            imports.add("js_setIndexObj");
        } else if (mName.equals("push")) {
            imports.add("js_pushStr");
            imports.add("js_pushInt");
            imports.add("js_pushDouble");
            imports.add("js_pushBoolean");
            imports.add("js_pushObj");
        } else if (mName.equals("getAsString")) {
            imports.add("js_resource_get_string");
        } else if (mName.equals("hasResource")) {
            imports.add("js_resource_has");
        } else if (mName.equals("getLength")) {
            imports.add("js_resource_get_length");
        } else if (mName.equals("injectStyle")) {
            imports.add("js_resource_inject_style");
        } else if (mName.equals("injectStyleSheet")) {
            imports.add("js_resource_inject_stylesheet");
        } else if (mName.equals("injectScript")) {
            imports.add("js_resource_inject_script");
        } else if (mName.equals("injectScriptTag")) {
            imports.add("js_resource_inject_script_tag");
        } else if (mName.equals("listResources")) {
            imports.add("js_resource_list");
        } else if (mName.equals("mountAll")) {
            imports.add("js_resource_mount_all");
        } else if (mName.equals("getColumns")) {
            imports.add("dom_terminal_get_columns");
        } else if (mName.equals("getRows")) {
            imports.add("dom_terminal_get_rows");
        } else if (mName.equals("clear") && (call != null && call.getScope().map(s -> s.toString().contains("Terminal")).orElse(false))) {
            imports.add("dom_terminal_clear");
        }
    }

    public static boolean isJsStaticMethod(String mName, MethodCallExpr call) {
        if (mName.equals("global") || mName.equals("getWindow") || mName.equals("eval") ||
                mName.equals("createObject") || mName.equals("createArray") ||
                mName.equals("undefined") || mName.equals("nullValue")) {
            return true;
        }
        if (mName.equals("getAsString") || mName.equals("hasResource") || mName.equals("getLength") ||
                mName.equals("injectStyle") || mName.equals("injectStyleSheet") ||
                mName.equals("injectScript") || mName.equals("injectScriptTag") ||
                mName.equals("listResources") || mName.equals("mountAll")) {
            return true;
        }
        if (mName.equals("of") || mName.equals("forSymbol") || mName.equals("typeError") ||
                mName.equals("rangeError") || mName.equals("syntaxError") || mName.equals("getInstance") ||
                mName.equals("fromHandle")) {
            if (call != null && call.getScope().isPresent()) {
                String scope = call.getScope().get().toString();
                return isJsValueClass(scope);
            }
        }
        return false;
    }

    public static boolean isJsStaticMethod(String mName) {
        return isJsStaticMethod(mName, null);
    }

    public static boolean isJsValueClass(String s) {
        if (s == null) return false;
        return s.equals("JSValue") || s.endsWith(".JSValue") ||
                s.equals("JSNumber") || s.endsWith(".JSNumber") ||
                s.equals("JSBoolean") || s.endsWith(".JSBoolean") ||
                s.equals("JSString") || s.endsWith(".JSString") ||
                s.equals("JSBigInt") || s.endsWith(".JSBigInt") ||
                s.equals("JSSymbol") || s.endsWith(".JSSymbol") ||
                s.equals("JSUndefined") || s.endsWith(".JSUndefined") ||
                s.equals("JSError") || s.endsWith(".JSError") ||
                s.equals("JSNull") || s.endsWith(".JSNull") ||
                s.equals("JSObject") || s.endsWith(".JSObject");
    }

    public static boolean isJsInstanceMethod(String mName) {
        return mName.equals("get") || mName.equals("getString") || mName.equals("getInt") ||
                mName.equals("getDouble") || mName.equals("getBoolean") || mName.equals("set") ||
                mName.equals("call") || mName.equals("callString") || mName.equals("callInt") ||
                mName.equals("callDouble") || mName.equals("callBoolean") ||
                mName.equals("asString") || mName.equals("asInt") || mName.equals("asDouble") ||
                mName.equals("asBoolean") || mName.equals("asBooleanPrimitive") ||
                mName.equals("asNumber") || mName.equals("asStringValue") || mName.equals("asBigInt") ||
                mName.equals("asSymbol") || mName.equals("asObject") || mName.equals("asError") ||
                mName.equals("isArray") || mName.equals("length") ||
                mName.equals("getIndex") || mName.equals("setIndex") || mName.equals("push") ||
                mName.equals("getTypeof") || mName.equals("isNumber") || mName.equals("isBoolean") ||
                mName.equals("isString") || mName.equals("isBigInt") || mName.equals("isSymbol") ||
                mName.equals("isUndefined") || mName.equals("isNull") || mName.equals("isObject") ||
                mName.equals("isFunction") || mName.equals("isError") || mName.equals("instanceOf") ||
                mName.equals("strictEquals") || mName.equals("isNaN") || mName.equals("isFinite") ||
                mName.equals("isInteger") || mName.equals("doubleValue") || mName.equals("intValue") ||
                mName.equals("longValue") || mName.equals("booleanValue") || mName.equals("stringValue") ||
                mName.equals("getDescription") || mName.equals("getMessage") || mName.equals("getName") ||
                mName.equals("getStack");
    }

    public static boolean isWebGLMethod(String mName, MethodCallExpr call) {
        if (mName.equals("clearColor") || mName.equals("clear") || mName.equals("viewport") ||
                mName.equals("enable") || mName.equals("disable")) {
            if (call.getScope().isPresent()) {
                String scopeStr = call.getScope().get().toString();
                return scopeStr.contains("gl") || scopeStr.contains("GL") || scopeStr.contains("WebGL") || scopeStr.contains("webgl");
            }
        }
        return false;
    }

    public static boolean isDomStaticMethod(String mName) {
        return mName.equals("getElementById") ||
                mName.equals("createElement") ||
                mName.equals("getBody") ||
                mName.equals("querySelector") ||
                mName.equals("querySelectorAll") ||
                mName.equals("getCanvasContext2D") ||
                mName.equals("setTimeout") ||
                mName.equals("clearTimeout") ||
                mName.equals("setInterval") ||
                mName.equals("clearInterval") ||
                mName.equals("requestAnimationFrame") ||
                mName.equals("cancelAnimationFrame") ||
                mName.equals("fetch") ||
                mName.equals("alert") ||
                mName.equals("confirm") ||
                mName.equals("prompt");
    }

    public static boolean isDomInstanceMethod(String mName) {
        return mName.equals("setInnerText") ||
                mName.equals("getInnerText") ||
                mName.equals("setAttribute") ||
                mName.equals("getAttribute") ||
                mName.equals("appendChild") ||
                mName.equals("removeChild") ||
                mName.equals("remove") ||
                mName.equals("getParent") ||
                mName.equals("getValue") ||
                mName.equals("setValue") ||
                mName.equals("isChecked") ||
                mName.equals("setChecked") ||
                mName.equals("setStyle") ||
                mName.equals("getStyle") ||
                mName.equals("addClass") ||
                mName.equals("removeClass") ||
                mName.equals("hasClass") ||
                mName.equals("toggleClass") ||
                mName.equals("getClassList") ||
                mName.equals("getContext2D") ||
                mName.equals("getContext") ||
                mName.equals("getWebGLContext") ||
                mName.equals("getWidth") ||
                mName.equals("setWidth") ||
                mName.equals("getHeight") ||
                mName.equals("setHeight") ||
                mName.equals("setFillStyle") ||
                mName.equals("getFillStyle") ||
                mName.equals("setStrokeStyle") ||
                mName.equals("getStrokeStyle") ||
                mName.equals("setLineWidth") ||
                mName.equals("getLineWidth") ||
                mName.equals("setFont") ||
                mName.equals("getFont") ||
                mName.equals("setTextAlign") ||
                mName.equals("getTextAlign") ||
                mName.equals("fillRect") ||
                mName.equals("strokeRect") ||
                mName.equals("clearRect") ||
                mName.equals("beginPath") ||
                mName.equals("closePath") ||
                mName.equals("moveTo") ||
                mName.equals("lineTo") ||
                mName.equals("arc") ||
                mName.equals("rect") ||
                mName.equals("ellipse") ||
                mName.equals("fill") ||
                mName.equals("stroke") ||
                mName.equals("fillText") ||
                mName.equals("strokeText") ||
                mName.equals("drawImage") ||
                mName.equals("drawImageFull") ||
                mName.equals("save") ||
                mName.equals("restore") ||
                mName.equals("translate") ||
                mName.equals("rotate") ||
                mName.equals("scale") ||
                mName.equals("resetTransform") ||
                mName.equals("setGlobalAlpha") ||
                mName.equals("getGlobalAlpha") ||
                mName.equals("clearColor") ||
                mName.equals("clear") ||
                mName.equals("viewport") ||
                mName.equals("enable") ||
                mName.equals("disable") ||
                mName.equals("addEventListener");
    }

    public static void emitDomImports(Set<String> domImports, StringBuilder watOutput) {
        if (domImports.contains("getElementById")) {
            watOutput.append("  (import \"dom\" \"getElementById\" (func $dom_getElementById (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("createElement")) {
            watOutput.append("  (import \"dom\" \"createElement\" (func $dom_createElement (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getBody")) {
            watOutput.append("  (import \"dom\" \"getBody\" (func $dom_getBody (result i32)))\n");
        }
        if (domImports.contains("querySelector")) {
            watOutput.append("  (import \"dom\" \"querySelector\" (func $dom_querySelector (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("querySelectorAll")) {
            watOutput.append("  (import \"dom\" \"querySelectorAll\" (func $dom_querySelectorAll (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getCanvasContext2D")) {
            watOutput.append("  (import \"dom\" \"getCanvasContext2D\" (func $dom_getCanvasContext2D (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getContext2D")) {
            watOutput.append("  (import \"dom\" \"getContext2D\" (func $dom_getContext2D (param i32) (result i32)))\n");
        }
        if (domImports.contains("getContext")) {
            watOutput.append("  (import \"dom\" \"getContext\" (func $dom_getContext (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getWebGLContext")) {
            watOutput.append("  (import \"dom\" \"getWebGLContext\" (func $dom_getWebGLContext (param i32) (result i32)))\n");
        }
        if (domImports.contains("getWidth")) {
            watOutput.append("  (import \"dom\" \"getWidth\" (func $dom_getWidth (param i32) (result i32)))\n");
        }
        if (domImports.contains("setWidth")) {
            watOutput.append("  (import \"dom\" \"setWidth\" (func $dom_setWidth (param i32 i32)))\n");
        }
        if (domImports.contains("getHeight")) {
            watOutput.append("  (import \"dom\" \"getHeight\" (func $dom_getHeight (param i32) (result i32)))\n");
        }
        if (domImports.contains("setHeight")) {
            watOutput.append("  (import \"dom\" \"setHeight\" (func $dom_setHeight (param i32 i32)))\n");
        }
        if (domImports.contains("setInnerText")) {
            watOutput.append("  (import \"dom\" \"setInnerText\" (func $dom_setInnerText (param i32 i32 i32)))\n");
        }
        if (domImports.contains("getInnerText")) {
            watOutput.append("  (import \"dom\" \"getInnerText\" (func $dom_getInnerText (param i32) (result i32)))\n");
        }
        if (domImports.contains("setAttribute")) {
            watOutput.append("  (import \"dom\" \"setAttribute\" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("getAttribute")) {
            watOutput.append("  (import \"dom\" \"getAttribute\" (func $dom_getAttribute (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("appendChild")) {
            watOutput.append("  (import \"dom\" \"appendChild\" (func $dom_appendChild (param i32 i32)))\n");
        }
        if (domImports.contains("removeChild")) {
            watOutput.append("  (import \"dom\" \"removeChild\" (func $dom_removeChild (param i32 i32)))\n");
        }
        if (domImports.contains("remove")) {
            watOutput.append("  (import \"dom\" \"remove\" (func $dom_remove (param i32)))\n");
        }
        if (domImports.contains("getParent")) {
            watOutput.append("  (import \"dom\" \"getParent\" (func $dom_getParent (param i32) (result i32)))\n");
        }
        if (domImports.contains("getValue")) {
            watOutput.append("  (import \"dom\" \"getValue\" (func $dom_getValue (param i32) (result i32)))\n");
        }
        if (domImports.contains("setValue")) {
            watOutput.append("  (import \"dom\" \"setValue\" (func $dom_setValue (param i32 i32 i32)))\n");
        }
        if (domImports.contains("isChecked")) {
            watOutput.append("  (import \"dom\" \"isChecked\" (func $dom_isChecked (param i32) (result i32)))\n");
        }
        if (domImports.contains("setChecked")) {
            watOutput.append("  (import \"dom\" \"setChecked\" (func $dom_setChecked (param i32 i32)))\n");
        }
        if (domImports.contains("setStyle")) {
            watOutput.append("  (import \"dom\" \"setStyle\" (func $dom_setStyle (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("getStyle")) {
            watOutput.append("  (import \"dom\" \"getStyle\" (func $dom_getStyle (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("addClass")) {
            watOutput.append("  (import \"dom\" \"addClass\" (func $dom_addClass (param i32 i32 i32)))\n");
        }
        if (domImports.contains("removeClass")) {
            watOutput.append("  (import \"dom\" \"removeClass\" (func $dom_removeClass (param i32 i32 i32)))\n");
        }
        if (domImports.contains("hasClass")) {
            watOutput.append("  (import \"dom\" \"hasClass\" (func $dom_hasClass (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("toggleClass")) {
            watOutput.append("  (import \"dom\" \"toggleClass\" (func $dom_toggleClass (param i32 i32 i32)))\n");
        }
        if (domImports.contains("setFillStyle")) {
            watOutput.append("  (import \"dom\" \"setFillStyle\" (func $dom_setFillStyle (param i32 i32 i32)))\n");
        }
        if (domImports.contains("getFillStyle")) {
            watOutput.append("  (import \"dom\" \"getFillStyle\" (func $dom_getFillStyle (param i32) (result i32)))\n");
        }
        if (domImports.contains("setStrokeStyle")) {
            watOutput.append("  (import \"dom\" \"setStrokeStyle\" (func $dom_setStrokeStyle (param i32 i32 i32)))\n");
        }
        if (domImports.contains("getStrokeStyle")) {
            watOutput.append("  (import \"dom\" \"getStrokeStyle\" (func $dom_getStrokeStyle (param i32) (result i32)))\n");
        }
        if (domImports.contains("setLineWidth")) {
            watOutput.append("  (import \"dom\" \"setLineWidth\" (func $dom_setLineWidth (param i32 f64)))\n");
        }
        if (domImports.contains("getLineWidth")) {
            watOutput.append("  (import \"dom\" \"getLineWidth\" (func $dom_getLineWidth (param i32) (result f64)))\n");
        }
        if (domImports.contains("setFont")) {
            watOutput.append("  (import \"dom\" \"setFont\" (func $dom_setFont (param i32 i32 i32)))\n");
        }
        if (domImports.contains("getFont")) {
            watOutput.append("  (import \"dom\" \"getFont\" (func $dom_getFont (param i32) (result i32)))\n");
        }
        if (domImports.contains("setTextAlign")) {
            watOutput.append("  (import \"dom\" \"setTextAlign\" (func $dom_setTextAlign (param i32 i32 i32)))\n");
        }
        if (domImports.contains("getTextAlign")) {
            watOutput.append("  (import \"dom\" \"getTextAlign\" (func $dom_getTextAlign (param i32) (result i32)))\n");
        }
        if (domImports.contains("fillRect")) {
            watOutput.append("  (import \"dom\" \"fillRect\" (func $dom_fillRect (param i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("strokeRect")) {
            watOutput.append("  (import \"dom\" \"strokeRect\" (func $dom_strokeRect (param i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("clearRect")) {
            watOutput.append("  (import \"dom\" \"clearRect\" (func $dom_clearRect (param i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("beginPath")) {
            watOutput.append("  (import \"dom\" \"beginPath\" (func $dom_beginPath (param i32)))\n");
        }
        if (domImports.contains("closePath")) {
            watOutput.append("  (import \"dom\" \"closePath\" (func $dom_closePath (param i32)))\n");
        }
        if (domImports.contains("moveTo")) {
            watOutput.append("  (import \"dom\" \"moveTo\" (func $dom_moveTo (param i32 f64 f64)))\n");
        }
        if (domImports.contains("lineTo")) {
            watOutput.append("  (import \"dom\" \"lineTo\" (func $dom_lineTo (param i32 f64 f64)))\n");
        }
        if (domImports.contains("arc")) {
            watOutput.append("  (import \"dom\" \"arc\" (func $dom_arc (param i32 f64 f64 f64 f64 f64 i32)))\n");
        }
        if (domImports.contains("rect")) {
            watOutput.append("  (import \"dom\" \"rect\" (func $dom_rect (param i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("ellipse")) {
            watOutput.append("  (import \"dom\" \"ellipse\" (func $dom_ellipse (param i32 f64 f64 f64 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("fill")) {
            watOutput.append("  (import \"dom\" \"fill\" (func $dom_fill (param i32)))\n");
        }
        if (domImports.contains("stroke")) {
            watOutput.append("  (import \"dom\" \"stroke\" (func $dom_stroke (param i32)))\n");
        }
        if (domImports.contains("fillText")) {
            watOutput.append("  (import \"dom\" \"fillText\" (func $dom_fillText (param i32 i32 i32 f64 f64)))\n");
        }
        if (domImports.contains("strokeText")) {
            watOutput.append("  (import \"dom\" \"strokeText\" (func $dom_strokeText (param i32 i32 i32 f64 f64)))\n");
        }
        if (domImports.contains("drawImage")) {
            watOutput.append("  (import \"dom\" \"drawImage\" (func $dom_drawImage (param i32 i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("drawImageFull")) {
            watOutput.append("  (import \"dom\" \"drawImageFull\" (func $dom_drawImageFull (param i32 i32 f64 f64 f64 f64 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("save")) {
            watOutput.append("  (import \"dom\" \"save\" (func $dom_save (param i32)))\n");
        }
        if (domImports.contains("restore")) {
            watOutput.append("  (import \"dom\" \"restore\" (func $dom_restore (param i32)))\n");
        }
        if (domImports.contains("translate")) {
            watOutput.append("  (import \"dom\" \"translate\" (func $dom_translate (param i32 f64 f64)))\n");
        }
        if (domImports.contains("rotate")) {
            watOutput.append("  (import \"dom\" \"rotate\" (func $dom_rotate (param i32 f64)))\n");
        }
        if (domImports.contains("scale")) {
            watOutput.append("  (import \"dom\" \"scale\" (func $dom_scale (param i32 f64 f64)))\n");
        }
        if (domImports.contains("resetTransform")) {
            watOutput.append("  (import \"dom\" \"resetTransform\" (func $dom_resetTransform (param i32)))\n");
        }
        if (domImports.contains("setGlobalAlpha")) {
            watOutput.append("  (import \"dom\" \"setGlobalAlpha\" (func $dom_setGlobalAlpha (param i32 f64)))\n");
        }
        if (domImports.contains("getGlobalAlpha")) {
            watOutput.append("  (import \"dom\" \"getGlobalAlpha\" (func $dom_getGlobalAlpha (param i32) (result f64)))\n");
        }
        if (domImports.contains("webgl_clearColor")) {
            watOutput.append("  (import \"dom\" \"webgl_clearColor\" (func $dom_webgl_clearColor (param i32 f64 f64 f64 f64)))\n");
        }
        if (domImports.contains("webgl_clear")) {
            watOutput.append("  (import \"dom\" \"webgl_clear\" (func $dom_webgl_clear (param i32 i32)))\n");
        }
        if (domImports.contains("webgl_viewport")) {
            watOutput.append("  (import \"dom\" \"webgl_viewport\" (func $dom_webgl_viewport (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("webgl_enable")) {
            watOutput.append("  (import \"dom\" \"webgl_enable\" (func $dom_webgl_enable (param i32 i32)))\n");
        }
        if (domImports.contains("webgl_disable")) {
            watOutput.append("  (import \"dom\" \"webgl_disable\" (func $dom_webgl_disable (param i32 i32)))\n");
        }
        if (domImports.contains("addEventListener")) {
            watOutput.append("  (import \"dom\" \"addEventListener\" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("setTimeout")) {
            watOutput.append("  (import \"dom\" \"setTimeout\" (func $dom_setTimeout (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("clearTimeout")) {
            watOutput.append("  (import \"dom\" \"clearTimeout\" (func $dom_clearTimeout (param i32)))\n");
        }
        if (domImports.contains("setInterval")) {
            watOutput.append("  (import \"dom\" \"setInterval\" (func $dom_setInterval (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("clearInterval")) {
            watOutput.append("  (import \"dom\" \"clearInterval\" (func $dom_clearInterval (param i32)))\n");
        }
        if (domImports.contains("requestAnimationFrame")) {
            watOutput.append("  (import \"dom\" \"requestAnimationFrame\" (func $dom_requestAnimationFrame (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("cancelAnimationFrame")) {
            watOutput.append("  (import \"dom\" \"cancelAnimationFrame\" (func $dom_cancelAnimationFrame (param i32)))\n");
        }
        if (domImports.contains("fetch")) {
            watOutput.append("  (import \"dom\" \"fetch\" (func $dom_fetch (param i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("alert")) {
            watOutput.append("  (import \"dom\" \"alert\" (func $dom_alert (param i32 i32)))\n");
        }
        if (domImports.contains("confirm")) {
            watOutput.append("  (import \"dom\" \"confirm\" (func $dom_confirm (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("prompt")) {
            watOutput.append("  (import \"dom\" \"prompt\" (func $dom_prompt (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_global")) {
            watOutput.append("  (import \"dom\" \"js_global\" (func $dom_js_global (result i32)))\n");
        }
        if (domImports.contains("js_eval")) {
            watOutput.append("  (import \"dom\" \"js_eval\" (func $dom_js_eval (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_createObject")) {
            watOutput.append("  (import \"dom\" \"js_createObject\" (func $dom_js_createObject (result i32)))\n");
        }
        if (domImports.contains("js_createArray")) {
            watOutput.append("  (import \"dom\" \"js_createArray\" (func $dom_js_createArray (result i32)))\n");
        }
        if (domImports.contains("js_get")) {
            watOutput.append("  (import \"dom\" \"js_get\" (func $dom_js_get (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_getString")) {
            watOutput.append("  (import \"dom\" \"js_getString\" (func $dom_js_getString (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_getInt")) {
            watOutput.append("  (import \"dom\" \"js_getInt\" (func $dom_js_getInt (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_getDouble")) {
            watOutput.append("  (import \"dom\" \"js_getDouble\" (func $dom_js_getDouble (param i32 i32 i32) (result f64)))\n");
        }
        if (domImports.contains("js_getBoolean")) {
            watOutput.append("  (import \"dom\" \"js_getBoolean\" (func $dom_js_getBoolean (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_setString")) {
            watOutput.append("  (import \"dom\" \"js_setString\" (func $dom_js_setString (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setInt")) {
            watOutput.append("  (import \"dom\" \"js_setInt\" (func $dom_js_setInt (param i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setDouble")) {
            watOutput.append("  (import \"dom\" \"js_setDouble\" (func $dom_js_setDouble (param i32 i32 i32 f64)))\n");
        }
        if (domImports.contains("js_setBoolean")) {
            watOutput.append("  (import \"dom\" \"js_setBoolean\" (func $dom_js_setBoolean (param i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setObject")) {
            watOutput.append("  (import \"dom\" \"js_setObject\" (func $dom_js_setObject (param i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("js_call0")) {
            watOutput.append("  (import \"dom\" \"js_call0\" (func $dom_js_call0 (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call1_str")) {
            watOutput.append("  (import \"dom\" \"js_call1_str\" (func $dom_js_call1_str (param i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call1_int")) {
            watOutput.append("  (import \"dom\" \"js_call1_int\" (func $dom_js_call1_int (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call1_double")) {
            watOutput.append("  (import \"dom\" \"js_call1_double\" (func $dom_js_call1_double (param i32 i32 i32 f64) (result i32)))\n");
        }
        if (domImports.contains("js_call1_bool")) {
            watOutput.append("  (import \"dom\" \"js_call1_bool\" (func $dom_js_call1_bool (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call1_obj")) {
            watOutput.append("  (import \"dom\" \"js_call1_obj\" (func $dom_js_call1_obj (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call2_str_str")) {
            watOutput.append("  (import \"dom\" \"js_call2_str_str\" (func $dom_js_call2_str_str (param i32 i32 i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call2_str_int")) {
            watOutput.append("  (import \"dom\" \"js_call2_str_int\" (func $dom_js_call2_str_int (param i32 i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call2_str_obj")) {
            watOutput.append("  (import \"dom\" \"js_call2_str_obj\" (func $dom_js_call2_str_obj (param i32 i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_call2_obj_obj")) {
            watOutput.append("  (import \"dom\" \"js_call2_obj_obj\" (func $dom_js_call2_obj_obj (param i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_callString0")) {
            watOutput.append("  (import \"dom\" \"js_callString0\" (func $dom_js_callString0 (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_callString1_str")) {
            watOutput.append("  (import \"dom\" \"js_callString1_str\" (func $dom_js_callString1_str (param i32 i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_callInt0")) {
            watOutput.append("  (import \"dom\" \"js_callInt0\" (func $dom_js_callInt0 (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_callInt1_int")) {
            watOutput.append("  (import \"dom\" \"js_callInt1_int\" (func $dom_js_callInt1_int (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_callDouble0")) {
            watOutput.append("  (import \"dom\" \"js_callDouble0\" (func $dom_js_callDouble0 (param i32 i32 i32) (result f64)))\n");
        }
        if (domImports.contains("js_callBoolean0")) {
            watOutput.append("  (import \"dom\" \"js_callBoolean0\" (func $dom_js_callBoolean0 (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_asString")) {
            watOutput.append("  (import \"dom\" \"js_asString\" (func $dom_js_asString (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_asInt")) {
            watOutput.append("  (import \"dom\" \"js_asInt\" (func $dom_js_asInt (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_asDouble")) {
            watOutput.append("  (import \"dom\" \"js_asDouble\" (func $dom_js_asDouble (param i32) (result f64)))\n");
        }
        if (domImports.contains("js_asBoolean")) {
            watOutput.append("  (import \"dom\" \"js_asBoolean\" (func $dom_js_asBoolean (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isArray")) {
            watOutput.append("  (import \"dom\" \"js_isArray\" (func $dom_js_isArray (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_length")) {
            watOutput.append("  (import \"dom\" \"js_length\" (func $dom_js_length (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_getIndex")) {
            watOutput.append("  (import \"dom\" \"js_getIndex\" (func $dom_js_getIndex (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_setIndexObj")) {
            watOutput.append("  (import \"dom\" \"js_setIndexObj\" (func $dom_js_setIndexObj (param i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setIndexStr")) {
            watOutput.append("  (import \"dom\" \"js_setIndexStr\" (func $dom_js_setIndexStr (param i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setIndexInt")) {
            watOutput.append("  (import \"dom\" \"js_setIndexInt\" (func $dom_js_setIndexInt (param i32 i32 i32)))\n");
        }
        if (domImports.contains("js_setIndexDouble")) {
            watOutput.append("  (import \"dom\" \"js_setIndexDouble\" (func $dom_js_setIndexDouble (param i32 i32 f64)))\n");
        }
        if (domImports.contains("js_setIndexBoolean")) {
            watOutput.append("  (import \"dom\" \"js_setIndexBoolean\" (func $dom_js_setIndexBoolean (param i32 i32 i32)))\n");
        }
        if (domImports.contains("js_pushObj")) {
            watOutput.append("  (import \"dom\" \"js_pushObj\" (func $dom_js_pushObj (param i32 i32)))\n");
        }
        if (domImports.contains("js_pushStr")) {
            watOutput.append("  (import \"dom\" \"js_pushStr\" (func $dom_js_pushStr (param i32 i32 i32)))\n");
        }
        if (domImports.contains("js_pushInt")) {
            watOutput.append("  (import \"dom\" \"js_pushInt\" (func $dom_js_pushInt (param i32 i32)))\n");
        }
        if (domImports.contains("js_pushDouble")) {
            watOutput.append("  (import \"dom\" \"js_pushDouble\" (func $dom_js_pushDouble (param i32 f64)))\n");
        }
        if (domImports.contains("js_pushBoolean")) {
            watOutput.append("  (import \"dom\" \"js_pushBoolean\" (func $dom_js_pushBoolean (param i32 i32)))\n");
        }
        if (domImports.contains("js_number_of_int")) {
            watOutput.append("  (import \"dom\" \"js_number_of_int\" (func $dom_js_number_of_int (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_number_of_double")) {
            watOutput.append("  (import \"dom\" \"js_number_of_double\" (func $dom_js_number_of_double (param f64) (result i32)))\n");
        }
        if (domImports.contains("js_boolean_of")) {
            watOutput.append("  (import \"dom\" \"js_boolean_of\" (func $dom_js_boolean_of (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_string_of")) {
            watOutput.append("  (import \"dom\" \"js_string_of\" (func $dom_js_string_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_bigint_of_i64")) {
            watOutput.append("  (import \"dom\" \"js_bigint_of_i64\" (func $dom_js_bigint_of_i64 (param i64) (result i32)))\n");
        }
        if (domImports.contains("js_bigint_of_str")) {
            watOutput.append("  (import \"dom\" \"js_bigint_of_str\" (func $dom_js_bigint_of_str (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_symbol_of")) {
            watOutput.append("  (import \"dom\" \"js_symbol_of\" (func $dom_js_symbol_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_symbol_for")) {
            watOutput.append("  (import \"dom\" \"js_symbol_for\" (func $dom_js_symbol_for (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_undefined")) {
            watOutput.append("  (import \"dom\" \"js_undefined\" (func $dom_js_undefined (result i32)))\n");
        }
        if (domImports.contains("js_null")) {
            watOutput.append("  (import \"dom\" \"js_null\" (func $dom_js_null (result i32)))\n");
        }
        if (domImports.contains("js_error_of")) {
            watOutput.append("  (import \"dom\" \"js_error_of\" (func $dom_js_error_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_type_error_of")) {
            watOutput.append("  (import \"dom\" \"js_type_error_of\" (func $dom_js_type_error_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_range_error_of")) {
            watOutput.append("  (import \"dom\" \"js_range_error_of\" (func $dom_js_range_error_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_syntax_error_of")) {
            watOutput.append("  (import \"dom\" \"js_syntax_error_of\" (func $dom_js_syntax_error_of (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_typeof")) {
            watOutput.append("  (import \"dom\" \"js_typeof\" (func $dom_js_typeof (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isNumber")) {
            watOutput.append("  (import \"dom\" \"js_isNumber\" (func $dom_js_isNumber (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isBoolean")) {
            watOutput.append("  (import \"dom\" \"js_isBoolean\" (func $dom_js_isBoolean (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isString")) {
            watOutput.append("  (import \"dom\" \"js_isString\" (func $dom_js_isString (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isBigInt")) {
            watOutput.append("  (import \"dom\" \"js_isBigInt\" (func $dom_js_isBigInt (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isSymbol")) {
            watOutput.append("  (import \"dom\" \"js_isSymbol\" (func $dom_js_isSymbol (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isUndefined")) {
            watOutput.append("  (import \"dom\" \"js_isUndefined\" (func $dom_js_isUndefined (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isNull")) {
            watOutput.append("  (import \"dom\" \"js_isNull\" (func $dom_js_isNull (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isObject")) {
            watOutput.append("  (import \"dom\" \"js_isObject\" (func $dom_js_isObject (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isFunction")) {
            watOutput.append("  (import \"dom\" \"js_isFunction\" (func $dom_js_isFunction (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isError")) {
            watOutput.append("  (import \"dom\" \"js_isError\" (func $dom_js_isError (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_instanceof")) {
            watOutput.append("  (import \"dom\" \"js_instanceof\" (func $dom_js_instanceof (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_strictEquals")) {
            watOutput.append("  (import \"dom\" \"js_strictEquals\" (func $dom_js_strictEquals (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_isNaN")) {
            watOutput.append("  (import \"dom\" \"js_isNaN\" (func $dom_js_isNaN (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isFinite")) {
            watOutput.append("  (import \"dom\" \"js_isFinite\" (func $dom_js_isFinite (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_isInteger")) {
            watOutput.append("  (import \"dom\" \"js_isInteger\" (func $dom_js_isInteger (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_symbolDescription")) {
            watOutput.append("  (import \"dom\" \"js_symbolDescription\" (func $dom_js_symbolDescription (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_errorMessage")) {
            watOutput.append("  (import \"dom\" \"js_errorMessage\" (func $dom_js_errorMessage (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_errorName")) {
            watOutput.append("  (import \"dom\" \"js_errorName\" (func $dom_js_errorName (param i32) (result i32)))\n");
        }
        if (domImports.contains("js_errorStack")) {
            watOutput.append("  (import \"dom\" \"js_errorStack\" (func $dom_js_errorStack (param i32) (result i32)))\n");
        }
        if (domImports.contains("thread_spawn")) {
            watOutput.append("  (import \"dom\" \"thread_spawn\" (func $dom_thread_spawn (param i32 i32 i32) (result i32)))\n");
        }
        if (domImports.contains("thread_sleep")) {
            watOutput.append("  (import \"dom\" \"thread_sleep\" (func $dom_thread_sleep (param i32)))\n");
        }
        if (domImports.contains("thread_current_id")) {
            watOutput.append("  (import \"dom\" \"thread_current_id\" (func $dom_thread_current_id (result i32)))\n");
        }
        if (domImports.contains("thread_hardware_concurrency")) {
            watOutput.append("  (import \"dom\" \"thread_hardware_concurrency\" (func $dom_thread_hardware_concurrency (result i32)))\n");
        }
        if (domImports.contains("js_resource_get_string")) {
            watOutput.append("  (import \"dom\" \"js_resource_get_string\" (func $dom_js_resource_get_string (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_resource_has")) {
            watOutput.append("  (import \"dom\" \"js_resource_has\" (func $dom_js_resource_has (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_resource_get_length")) {
            watOutput.append("  (import \"dom\" \"js_resource_get_length\" (func $dom_js_resource_get_length (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("js_resource_inject_style")) {
            watOutput.append("  (import \"dom\" \"js_resource_inject_style\" (func $dom_js_resource_inject_style (param i32 i32)))\n");
        }
        if (domImports.contains("js_resource_inject_stylesheet")) {
            watOutput.append("  (import \"dom\" \"js_resource_inject_stylesheet\" (func $dom_js_resource_inject_stylesheet (param i32 i32)))\n");
        }
        if (domImports.contains("js_resource_inject_script")) {
            watOutput.append("  (import \"dom\" \"js_resource_inject_script\" (func $dom_js_resource_inject_script (param i32 i32)))\n");
        }
        if (domImports.contains("js_resource_inject_script_tag")) {
            watOutput.append("  (import \"dom\" \"js_resource_inject_script_tag\" (func $dom_js_resource_inject_script_tag (param i32 i32)))\n");
        }
        if (domImports.contains("js_resource_list")) {
            watOutput.append("  (import \"dom\" \"js_resource_list\" (func $dom_js_resource_list (result i32)))\n");
        }
        if (domImports.contains("js_resource_mount_all")) {
            watOutput.append("  (import \"dom\" \"js_resource_mount_all\" (func $dom_js_resource_mount_all))\n");
        }
        if (domImports.contains("dom_terminal_clear")) {
            watOutput.append("  (import \"dom\" \"terminal_clear\" (func $dom_terminal_clear))\n");
        }
        if (domImports.contains("dom_terminal_get_columns")) {
            watOutput.append("  (import \"dom\" \"terminal_get_columns\" (func $dom_terminal_get_columns (result i32)))\n");
        }
        if (domImports.contains("dom_terminal_get_rows")) {
            watOutput.append("  (import \"dom\" \"terminal_get_rows\" (func $dom_terminal_get_rows (result i32)))\n");
        }
    }
}
