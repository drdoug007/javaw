package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavacToWasmTest {

    @Test
    public void testGenerateWatFromSource() {
        String javaSource = "package com.example.javaw;\n" +
                "public class TestClass {\n" +
                "    public void sayHello() {}\n" +
                "    public int addNumbers(int a, int b) { return a + b; }\n" +
                "    public double getPi() { return 3.14159; }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(module"));
        assertTrue(wat.contains("(func $sayHello (export \"sayHello\")"));
        assertTrue(wat.contains("(func $addNumbers (export \"addNumbers\") (param $a i32) (param $b i32) (result i32)"));
        assertTrue(wat.contains("local.get $a"));
        assertTrue(wat.contains("local.get $b"));
        assertTrue(wat.contains("i32.add"));
        assertTrue(wat.contains("(func $getPi (export \"getPi\") (result f64)"));
        assertTrue(wat.contains("f64.const 3.14159"));
    }
    @Test
    public void testGenerateWatWithMemoryAndStringLiterals() {
        String javaSource = "package com.example.javaw;\n" +
                "public class HelloApp {\n" +
                "    public void run() {\n" +
                "        String s = \"Hello, WebAssembly!\";\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(memory (export \"memory\")"));
        assertTrue(wat.contains("(data (i32.const 4096) \"Hello, WebAssembly!\\00\")"));
        assertTrue(wat.contains("i32.const 4096"));
    }

    @Test
    public void testGenerateWatWithForEachLoopAndArrays() {
        String javaSource = "package com.example.javaw;\n" +
                "public class ForEachApp {\n" +
                "    public int sum(int[] numbers) {\n" +
                "        int total = 0;\n" +
                "        for (int n : numbers) {\n" +
                "            if (n == 3) continue;\n" +
                "            if (n > 8) break;\n" +
                "            total += n;\n" +
                "        }\n" +
                "        return total;\n" +
                "    }\n" +
                "    public double sumDoubles(double[] vals) {\n" +
                "        double total = 0.0;\n" +
                "        for (double v : vals) {\n" +
                "            total += v;\n" +
                "        }\n" +
                "        return total;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $sum (export \"sum\") (param $numbers i32) (result i32)"));
        assertTrue(wat.contains("(local $__arr_ptr_1 i32)"));
        assertTrue(wat.contains("(local $__arr_len_1 i32)"));
        assertTrue(wat.contains("(local $__arr_idx_1 i32)"));
        assertTrue(wat.contains("block $break_foreach_"));
        assertTrue(wat.contains("loop $loop_foreach_"));
        assertTrue(wat.contains("block $continue_foreach_"));
        assertTrue(wat.contains("i32.load offset=4"));
        assertTrue(wat.contains("(func $sumDoubles (export \"sumDoubles\") (param $vals i32) (result f64)"));
        assertTrue(wat.contains("f64.load offset=8"));
    }

    @Test
    public void testGenerateWatWithDomOperations() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "public class DomApp {\n" +
                "    public void start() {\n" +
                "        Element root = DOM.getElementById(\"app\");\n" +
                "        Element h1 = DOM.createElement(\"h1\");\n" +
                "        h1.setInnerText(\"Hello DOM\");\n" +
                "        h1.setAttribute(\"class\", \"title\");\n" +
                "        root.appendChild(h1);\n" +
                "        h1.addEventListener(\"click\", \"handleClick\");\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"getElementById\" (func $dom_getElementById (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"createElement\" (func $dom_createElement (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setInnerText\" (func $dom_setInnerText (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setAttribute\" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"appendChild\" (func $dom_appendChild (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"addEventListener\" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))"));

        assertTrue(wat.contains("call $dom_getElementById"));
        assertTrue(wat.contains("call $dom_createElement"));
        assertTrue(wat.contains("call $dom_setInnerText"));
        assertTrue(wat.contains("call $dom_setAttribute"));
        assertTrue(wat.contains("call $dom_appendChild"));
        assertTrue(wat.contains("call $dom_addEventListener"));
    }

    @Test
    public void testGenerateWatWithExtendedDomOperations() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "public class ExtendedDomApp {\n" +
                "    public void run() {\n" +
                "        Element root = DOM.querySelector(\"#app\");\n" +
                "        Element[] items = DOM.querySelectorAll(\".item\");\n" +
                "        String txt = root.getInnerText();\n" +
                "        String val = root.getValue();\n" +
                "        root.setValue(\"Hello\");\n" +
                "        boolean checked = root.isChecked();\n" +
                "        root.setChecked(true);\n" +
                "        root.setStyle(\"color\", \"red\");\n" +
                "        String color = root.getStyle(\"color\");\n" +
                "        root.addClass(\"active\");\n" +
                "        root.removeClass(\"hidden\");\n" +
                "        boolean has = root.hasClass(\"active\");\n" +
                "        root.toggleClass(\"highlight\");\n" +
                "        Element parent = root.getParent();\n" +
                "        root.remove();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"querySelector\" (func $dom_querySelector (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"querySelectorAll\" (func $dom_querySelectorAll (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getInnerText\" (func $dom_getInnerText (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getValue\" (func $dom_getValue (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setValue\" (func $dom_setValue (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"isChecked\" (func $dom_isChecked (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setChecked\" (func $dom_setChecked (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setStyle\" (func $dom_setStyle (param i32 i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getStyle\" (func $dom_getStyle (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"addClass\" (func $dom_addClass (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"removeClass\" (func $dom_removeClass (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"hasClass\" (func $dom_hasClass (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"toggleClass\" (func $dom_toggleClass (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getParent\" (func $dom_getParent (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"remove\" (func $dom_remove (param i32)))"));

        assertTrue(wat.contains("call $dom_querySelector"));
        assertTrue(wat.contains("call $dom_querySelectorAll"));
        assertTrue(wat.contains("call $dom_getInnerText"));
        assertTrue(wat.contains("call $dom_getValue"));
        assertTrue(wat.contains("call $dom_setValue"));
        assertTrue(wat.contains("call $dom_isChecked"));
        assertTrue(wat.contains("call $dom_setChecked"));
        assertTrue(wat.contains("call $dom_setStyle"));
        assertTrue(wat.contains("call $dom_getStyle"));
        assertTrue(wat.contains("call $dom_addClass"));
        assertTrue(wat.contains("call $dom_removeClass"));
        assertTrue(wat.contains("call $dom_hasClass"));
        assertTrue(wat.contains("call $dom_toggleClass"));
        assertTrue(wat.contains("call $dom_getParent"));
        assertTrue(wat.contains("call $dom_remove"));
    }

    @Test
    public void testGenerateWatWithForLoopAndBreakContinue() {
        String javaSource = "package com.example.javaw;\n" +
                "public class LoopApp {\n" +
                "    public int sumTo(int n) {\n" +
                "        int sum = 0;\n" +
                "        for (int i = 1; i <= n; i++) {\n" +
                "            if (i == 5) continue;\n" +
                "            if (i > 10) break;\n" +
                "            sum += i;\n" +
                "        }\n" +
                "        return sum;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $sumTo (export \"sumTo\") (param $n i32) (result i32)"));
        assertTrue(wat.contains("(local $sum i32)"));
        assertTrue(wat.contains("(local $i i32)"));
        assertTrue(wat.contains("block $break_for_"));
        assertTrue(wat.contains("loop $loop_for_"));
        assertTrue(wat.contains("block $continue_for_"));
        assertTrue(wat.contains("br $continue_for_"));
        assertTrue(wat.contains("br $break_for_"));
        assertTrue(wat.contains("i32.add"));
    }

    @Test
    public void testGenerateWatWithSwitchStatement() {
        String javaSource = "package com.example.javaw;\n" +
                "public class SwitchApp {\n" +
                "    public int runSwitch(int code) {\n" +
                "        int res = 0;\n" +
                "        switch (code) {\n" +
                "            case 1:\n" +
                "                res = 100;\n" +
                "                break;\n" +
                "            case 2:\n" +
                "                res = 200;\n" +
                "                break;\n" +
                "            default:\n" +
                "                res = -1;\n" +
                "                break;\n" +
                "        }\n" +
                "        return res;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $runSwitch (export \"runSwitch\") (param $code i32) (result i32)"));
        assertTrue(wat.contains("(local $__switch_val_1 i32)"));
        assertTrue(wat.contains("block $break_switch_"));
        assertTrue(wat.contains("block $case_"));
        assertTrue(wat.contains("i32.eq"));
        assertTrue(wat.contains("br_if $case_"));
    }

    @Test
    public void testGenerateWatWithLabeledBreakAndContinue() {
        String javaSource = "package com.example.javaw;\n" +
                "public class LabeledApp {\n" +
                "    public int searchMatrix() {\n" +
                "        int count = 0;\n" +
                "        outer: for (int i = 0; i < 5; i++) {\n" +
                "            for (int j = 0; j < 5; j++) {\n" +
                "                if (i == 2 && j == 1) break outer;\n" +
                "                if (j == 3) continue outer;\n" +
                "                count++;\n" +
                "            }\n" +
                "        }\n" +
                "        return count;\n" +
                "    }\n" +
                "    public int testBlock(int flag) {\n" +
                "        int x = 10;\n" +
                "        myBlock: {\n" +
                "            if (flag == 1) {\n" +
                "                x += 5;\n" +
                "                break myBlock;\n" +
                "            }\n" +
                "            x += 20;\n" +
                "        }\n" +
                "        return x;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $searchMatrix (export \"searchMatrix\") (result i32)"));
        assertTrue(wat.contains("block $break_for_"));
        assertTrue(wat.contains("block $continue_for_"));
        assertTrue(wat.contains("br $break_for_1"));
        assertTrue(wat.contains("br $continue_for_1"));
        assertTrue(wat.contains("(func $testBlock (export \"testBlock\") (param $flag i32) (result i32)"));
        assertTrue(wat.contains("block $break_label_"));
        assertTrue(wat.contains("br $break_label_"));
    }

    @Test
    public void testGenerateWatWithStringsAndStringBuilder() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.util.StringBuilder;\n" +
                "public class StringApp {\n" +
                "    public String buildMessage(String name, int age) {\n" +
                "        String prefix = \"Hello, \";\n" +
                "        String greeting = prefix + name + \"!\";\n" +
                "        StringBuilder sb = new StringBuilder(\"Details: \");\n" +
                "        sb.append(greeting);\n" +
                "        sb.append(\" Age: \");\n" +
                "        sb.append(age);\n" +
                "        return sb.toString();\n" +
                "    }\n" +
                "    public int testOps(String s) {\n" +
                "        if (s.equals(\"test\")) {\n" +
                "            return s.length() + s.charAt(0) + s.indexOf(\"es\");\n" +
                "        }\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("$__str_alloc"));
        assertTrue(wat.contains("$__str_concat"));
        assertTrue(wat.contains("$__str_length"));
        assertTrue(wat.contains("$__str_equals"));
        assertTrue(wat.contains("$__str_charAt"));
        assertTrue(wat.contains("$__str_indexOf"));
        assertTrue(wat.contains("$__sb_new"));
        assertTrue(wat.contains("$__sb_append_str"));
        assertTrue(wat.contains("$__sb_append_i32"));
        assertTrue(wat.contains("$__sb_toString"));
    }

    @Test
    public void testGenerateWatWithObjectOrientedAndPolymorphism() {
        String javaSource = "package com.example.javaw;\n" +
                "public class OopApp {\n" +
                "    public int testOop() {\n" +
                "        Shape s = new Circle(10, 5);\n" +
                "        return s.area();\n" +
                "    }\n" +
                "}\n" +
                "class Shape {\n" +
                "    public int color;\n" +
                "    public Shape(int color) {\n" +
                "        this.color = color;\n" +
                "    }\n" +
                "    public int area() {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n" +
                "class Circle extends Shape {\n" +
                "    public int radius;\n" +
                "    public Circle(int color, int radius) {\n" +
                "        super(color);\n" +
                "        this.radius = radius;\n" +
                "    }\n" +
                "    public int area() {\n" +
                "        return 3 * radius * radius;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(table (export \"__vtable\")"));
        assertTrue(wat.contains("call_indirect"));
        assertTrue(wat.contains("$__heap_alloc"));
        assertTrue(wat.contains("(func $Circle__init"));
        assertTrue(wat.contains("(func $Shape__init"));
        assertTrue(wat.contains("(func $Circle_area"));
        assertTrue(wat.contains("(func $Shape_area"));
        assertTrue(wat.contains("(func $testOop (export \"testOop\") (result i32)"));
    }

    @Test
    public void testGenerateWatWithCanvasAndWebGLGraphics() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "import com.example.javaw.dom.CanvasRenderingContext2D;\n" +
                "import com.example.javaw.dom.WebGLRenderingContext;\n" +
                "public class GraphicsApp {\n" +
                "    public void drawScene() {\n" +
                "        Element canvas = DOM.getElementById(\"myCanvas\");\n" +
                "        canvas.setWidth(800);\n" +
                "        canvas.setHeight(600);\n" +
                "        CanvasRenderingContext2D ctx = canvas.getContext2D();\n" +
                "        ctx.setFillStyle(\"#ff0000\");\n" +
                "        ctx.fillRect(10, 20, 100, 50);\n" +
                "        ctx.beginPath();\n" +
                "        ctx.arc(50, 50, 25, 0, 6.28);\n" +
                "        ctx.fill();\n" +
                "        ctx.strokeText(\"Canvas Java\", 150, 80);\n" +
                "        ctx.save();\n" +
                "        ctx.translate(100, 100);\n" +
                "        ctx.rotate(0.5);\n" +
                "        ctx.restore();\n" +
                "    }\n" +
                "    public void initGl() {\n" +
                "        Element glCanvas = DOM.getElementById(\"glCanvas\");\n" +
                "        WebGLRenderingContext gl = glCanvas.getWebGLContext();\n" +
                "        gl.clearColor(0.1, 0.2, 0.3, 1.0);\n" +
                "        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);\n" +
                "        gl.viewport(0, 0, 800, 600);\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"setWidth\" (func $dom_setWidth (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setHeight\" (func $dom_setHeight (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getContext2D\" (func $dom_getContext2D (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setFillStyle\" (func $dom_setFillStyle (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"fillRect\" (func $dom_fillRect (param i32 f64 f64 f64 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"beginPath\" (func $dom_beginPath (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"arc\" (func $dom_arc (param i32 f64 f64 f64 f64 f64 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"fill\" (func $dom_fill (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"strokeText\" (func $dom_strokeText (param i32 i32 i32 f64 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"save\" (func $dom_save (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"translate\" (func $dom_translate (param i32 f64 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"rotate\" (func $dom_rotate (param i32 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"restore\" (func $dom_restore (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"getWebGLContext\" (func $dom_getWebGLContext (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"webgl_clearColor\" (func $dom_webgl_clearColor (param i32 f64 f64 f64 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"webgl_clear\" (func $dom_webgl_clear (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"webgl_viewport\" (func $dom_webgl_viewport (param i32 i32 i32 i32 i32)))"));
    }

    @Test
    public void testGenerateWatWithAsyncAndWindowApis() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Window;\n" +
                "public class AsyncApp {\n" +
                "    public void scheduleTasks() {\n" +
                "        int tId = Window.setTimeout(\"onTimeout\", 1000);\n" +
                "        int iId = Window.setInterval(\"onTick\", 500);\n" +
                "        Window.clearTimeout(tId);\n" +
                "        Window.clearInterval(iId);\n" +
                "        int fId = DOM.requestAnimationFrame(\"onFrame\");\n" +
                "        DOM.cancelAnimationFrame(fId);\n" +
                "        DOM.fetch(\"/api/data\", \"onDataReceived\");\n" +
                "        Window.alert(\"Notification\");\n" +
                "        boolean ok = Window.confirm(\"Are you sure?\");\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"setTimeout\" (func $dom_setTimeout (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"setInterval\" (func $dom_setInterval (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"clearTimeout\" (func $dom_clearTimeout (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"clearInterval\" (func $dom_clearInterval (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"requestAnimationFrame\" (func $dom_requestAnimationFrame (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"cancelAnimationFrame\" (func $dom_cancelAnimationFrame (param i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"fetch\" (func $dom_fetch (param i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"alert\" (func $dom_alert (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"confirm\" (func $dom_confirm (param i32 i32) (result i32)))"));
    }

    @Test
    public void testGenerateWatWithJSObjectDynamicInterop() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.JSObject;\n" +
                "public class JsInteropApp {\n" +
                "    public void runInterop() {\n" +
                "        JSObject global = JSObject.global();\n" +
                "        JSObject custom = JSObject.eval(\"({ x: 10, name: 'test' })\");\n" +
                "        JSObject obj = JSObject.createObject();\n" +
                "        JSObject arr = JSObject.createArray();\n" +
                "        obj.set(\"title\", \"Hello\");\n" +
                "        obj.set(\"count\", 42);\n" +
                "        obj.set(\"ratio\", 3.14);\n" +
                "        obj.set(\"active\", true);\n" +
                "        obj.set(\"nested\", custom);\n" +
                "        String title = obj.getString(\"title\");\n" +
                "        int count = obj.getInt(\"count\");\n" +
                "        double ratio = obj.getDouble(\"ratio\");\n" +
                "        boolean active = obj.getBoolean(\"active\");\n" +
                "        JSObject nested = obj.get(\"nested\");\n" +
                "        JSObject res = obj.call(\"toString\");\n" +
                "        String resStr = obj.callString(\"toString\");\n" +
                "        int len = arr.length();\n" +
                "        arr.push(\"item1\");\n" +
                "        arr.push(100);\n" +
                "        arr.push(true);\n" +
                "        JSObject first = arr.getIndex(0);\n" +
                "        arr.setIndex(0, \"updated\");\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"js_global\" (func $dom_js_global (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_eval\" (func $dom_js_eval (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_createObject\" (func $dom_js_createObject (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_createArray\" (func $dom_js_createArray (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setString\" (func $dom_js_setString (param i32 i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setInt\" (func $dom_js_setInt (param i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setDouble\" (func $dom_js_setDouble (param i32 i32 i32 f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setBoolean\" (func $dom_js_setBoolean (param i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setObject\" (func $dom_js_setObject (param i32 i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_getString\" (func $dom_js_getString (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_getInt\" (func $dom_js_getInt (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_getDouble\" (func $dom_js_getDouble (param i32 i32 i32) (result f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_getBoolean\" (func $dom_js_getBoolean (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_get\" (func $dom_js_get (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_call0\" (func $dom_js_call0 (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_callString0\" (func $dom_js_callString0 (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_length\" (func $dom_js_length (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_pushStr\" (func $dom_js_pushStr (param i32 i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_pushInt\" (func $dom_js_pushInt (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_pushBoolean\" (func $dom_js_pushBoolean (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_getIndex\" (func $dom_js_getIndex (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_setIndexStr\" (func $dom_js_setIndexStr (param i32 i32 i32 i32)))"));
    }

    @Test
    public void testGenerateWatWithWasiAndSystemOut() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.wasi.WASI;\n" +
                "public class WasiApp {\n" +
                "    public void runWasi() {\n" +
                "        System.out.println(\"Hello WASI!\");\n" +
                "        System.out.print(\"Count: \");\n" +
                "        System.out.println(42);\n" +
                "        System.out.println(3.14);\n" +
                "        System.out.println(true);\n" +
                "        System.err.println(\"Warning message\");\n" +
                "        WASI.println(\"Via WASI facade\");\n" +
                "        WASI.eprintln(\"Error via WASI\");\n" +
                "        WASI.exit(0);\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"wasi_snapshot_preview1\" \"fd_write\" (func $__wasi_fd_write (param i32 i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"wasi_snapshot_preview1\" \"proc_exit\" (func $__wasi_proc_exit (param i32)))"));
        assertTrue(wat.contains("call $__wasi_print_str"));
        assertTrue(wat.contains("call $__wasi_print_int"));
        assertTrue(wat.contains("call $__wasi_print_double"));
        assertTrue(wat.contains("call $__wasi_print_bool"));
        assertTrue(wat.contains("call $__wasi_proc_exit"));
    }

    @Test
    public void testGenerateWatWithAtomicIntegerAndAtomics() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.concurrent.AtomicInteger;\n" +
                "import com.example.javaw.concurrent.Atomics;\n" +
                "import com.example.javaw.concurrent.WasmThread;\n" +
                "public class ThreadApp {\n" +
                "    public int runAtomics() {\n" +
                "        AtomicInteger counter = new AtomicInteger(10);\n" +
                "        counter.incrementAndGet();\n" +
                "        counter.compareAndSet(11, 42);\n" +
                "        int[] arr = new int[]{1, 2, 3};\n" +
                "        Atomics.add(arr, 0, 5);\n" +
                "        Atomics.compareExchange(arr, 1, 2, 99);\n" +
                "        WasmThread.spawn(\"workerHandler\", 1);\n" +
                "        WasmThread.sleep(10);\n" +
                "        return counter.get();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(memory (export \"memory\") 2 20 shared)"));
        assertTrue(wat.contains("i32.atomic.load"));
        assertTrue(wat.contains("i32.atomic.rmw.add"));
        assertTrue(wat.contains("i32.atomic.rmw.cmpxchg"));
        assertTrue(wat.contains("(import \"dom\" \"thread_spawn\" (func $dom_thread_spawn (param i32 i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"thread_sleep\" (func $dom_thread_sleep (param i32)))"));
        assertTrue(wat.contains("call $dom_thread_spawn"));
        assertTrue(wat.contains("call $dom_thread_sleep"));
    }

    @Test
    public void testMultiFileCompilation() {
        String src1 = "package com.example.javaw;\n" +
                "public class ServiceA {\n" +
                "    public int calculate(int x) { return x * 2; }\n" +
                "}\n";
        String src2 = "package com.example.javaw;\n" +
                "public class ServiceB {\n" +
                "    public int compute(int y) { return y + 10; }\n" +
                "}\n";

        CompilationUnit cu1 = StaticJavaParser.parse(src1);
        CompilationUnit cu2 = StaticJavaParser.parse(src2);

        String wat = JavacToWasm.generateWat(java.util.List.of(cu1, cu2));
        assertTrue(wat.contains("(func $calculate"));
        assertTrue(wat.contains("(func $compute"));
    }

    @Test
    public void testGenerateWatWithExceptionsAndTryCatch() {
        String javaSource = "package com.example.javaw;\n" +
                "public class ExceptionApp {\n" +
                "    public int testTryCatch(int val) {\n" +
                "        try {\n" +
                "            if (val == 0) {\n" +
                "                throw new RuntimeException(\"Zero not allowed\");\n" +
                "            }\n" +
                "            return 100 / val;\n" +
                "        } catch (RuntimeException e) {\n" +
                "            return -1;\n" +
                "        } finally {\n" +
                "            val = val + 1;\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(tag $Exception (export \"Exception\") (param i32))"));
        assertTrue(wat.contains("throw $Exception"));
        assertTrue(wat.contains("(catch $Exception"));
        assertTrue(wat.contains("(catch_all"));
        assertTrue(wat.contains("(rethrow 0)"));
    }
}
