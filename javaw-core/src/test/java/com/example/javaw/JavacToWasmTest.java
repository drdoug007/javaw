package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavacToWasmTest {
    static {
        StaticJavaParser.getConfiguration().setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17);
    }

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

    @Test
    public void testGenerateWatWithRecordAndMultiValueReturns() {
        String javaSource = "package com.example.javaw;\n" +
                "record Point(int x, int y) {}\n" +
                "public class RecordApp {\n" +
                "    public Point createPoint(int x, int y) {\n" +
                "        return new Point(x, y);\n" +
                "    }\n" +
                "    public int sumPoint(Point p) {\n" +
                "        return p.x() + p.y();\n" +
                "    }\n" +
                "    public int testUnpack() {\n" +
                "        Point pt = createPoint(10, 20);\n" +
                "        return pt.x() + pt.y();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $createPoint (export \"createPoint\") (param $x i32) (param $y i32) (result i32 i32)"));
        assertTrue(wat.contains("(func $sumPoint (export \"sumPoint\") (param $p$x i32) (param $p$y i32) (result i32)"));
        assertTrue(wat.contains("(local $pt$x i32)"));
        assertTrue(wat.contains("(local $pt$y i32)"));
    }

    @Test
    public void testRecordMethodsAndComponentAccess() {
        String javaSource = "package com.example.javaw;\n" +
                "record DivResult(int quotient, int remainder) {\n" +
                "    public static DivResult divide(int a, int b) {\n" +
                "        return new DivResult(a / b, a % b);\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $DivResult_divide (export \"DivResult_divide\") (param $a i32) (param $b i32) (result i32 i32)"));
    }

    @Test
    public void testBulkMemoryOperationsSystemArrayCopyAndArrays() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.util.Arrays;\n" +
                "public class BulkMemoryApp {\n" +
                "    public void copyData(int[] src, int[] dest, int len) {\n" +
                "        System.arraycopy(src, 0, dest, 0, len);\n" +
                "    }\n" +
                "    public void fillData(int[] arr) {\n" +
                "        Arrays.fill(arr, 42);\n" +
                "        Arrays.fill(arr, 0);\n" +
                "    }\n" +
                "    public int[] copyArray(int[] arr, int newLen) {\n" +
                "        return Arrays.copyOf(arr, newLen);\n" +
                "    }\n" +
                "    public boolean compareArrays(int[] a, int[] b) {\n" +
                "        return Arrays.equals(a, b);\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("memory.copy"));
        assertTrue(wat.contains("call $__array_fill_i32"));
        assertTrue(wat.contains("call $__array_copyOf_i32"));
        assertTrue(wat.contains("call $__array_equals_i32"));
        assertTrue(wat.contains("(func $__array_fill_i32"));
        assertTrue(wat.contains("(func $__array_copyOf_i32"));
        assertTrue(wat.contains("(func $__array_copyOfRange_i32"));
        assertTrue(wat.contains("(func $__array_equals_i32"));
    }

    @Test
    public void testExtendedWasiAndVfsWatGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.wasi.WASI;\n" +
                "import com.example.javaw.wasi.WASIFile;\n" +
                "public class WasiVfsApp {\n" +
                "    public void testFiles() {\n" +
                "        WASI.writeString(\"/test.txt\", \"Hello VFS\");\n" +
                "        String content = WASI.readString(\"/test.txt\");\n" +
                "        boolean exists = WASI.fileExists(\"/test.txt\");\n" +
                "        int len = WASI.fileLength(\"/test.txt\");\n" +
                "        WASI.deleteFile(\"/test.txt\");\n" +
                "        WASI.createDirectory(\"/data\");\n" +
                "        double time = WASI.currentTimeMillis();\n" +
                "        int rand = WASI.randomInt();\n" +
                "        \n" +
                "        WASIFile file = new WASIFile(\"/file.txt\");\n" +
                "        file.writeString(\"Content\");\n" +
                "        String read = file.readString();\n" +
                "        boolean fileExists = file.exists();\n" +
                "        int fileLen = file.length();\n" +
                "        file.delete();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("call $__wasi_write_file_str"));
        assertTrue(wat.contains("call $__wasi_read_file_str"));
        assertTrue(wat.contains("call $__wasi_file_exists"));
        assertTrue(wat.contains("call $__wasi_file_length"));
        assertTrue(wat.contains("call $__wasi_file_delete"));
        assertTrue(wat.contains("call $__wasi_create_dir"));
        assertTrue(wat.contains("call $__wasi_clock_time_ms"));
        assertTrue(wat.contains("call $__wasi_random_int"));
        assertTrue(wat.contains("(func $__wasi_write_file_str"));
        assertTrue(wat.contains("(func $__wasi_read_file_str"));
        assertTrue(wat.contains("(func $__wasi_file_exists"));
        assertTrue(wat.contains("(func $__wasi_file_length"));
        assertTrue(wat.contains("(func $__wasi_file_delete"));
        assertTrue(wat.contains("(func $__wasi_create_dir"));
        assertTrue(wat.contains("(func $__wasi_clock_time_ms"));
        assertTrue(wat.contains("(func $__wasi_random_int"));
    }

    @Test
    public void testInMemoryCompilerApi() {
        String javaSource = "package com.example.javaw;\n" +
                "public class DynamicApp {\n" +
                "    public int multiply(int a, int b) { return a * b; }\n" +
                "}\n";

        JavacToWasm.CompilationResult result = JavacToWasm.compile(javaSource);
        assertTrue(result.getWat().contains("(func $multiply"));
        assertTrue(result.isWasmGenerated());
        assertTrue(result.getWasm().length > 0);
    }

    @Test
    public void testSimdVectorWatGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.simd.Float32x4;\n" +
                "import com.example.javaw.simd.Int32x4;\n" +
                "public class SimdApp {\n" +
                "    public Float32x4 vectorMath(Float32x4 a, Float32x4 b) {\n" +
                "        Float32x4 sum = a.add(b);\n" +
                "        Float32x4 mul = sum.mul(Float32x4.splat(2.0f));\n" +
                "        Float32x4 sqrt = mul.sqrt();\n" +
                "        return sqrt;\n" +
                "    }\n" +
                "    public Int32x4 intVectorOps(int[] arr) {\n" +
                "        Int32x4 v = Int32x4.load(arr, 0);\n" +
                "        Int32x4 shifted = v.shiftLeft(2);\n" +
                "        shifted.store(arr, 0);\n" +
                "        return shifted;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $vectorMath (export \"vectorMath\") (param $a v128) (param $b v128) (result v128)"));
        assertTrue(wat.contains("f32x4.add"));
        assertTrue(wat.contains("f32x4.splat"));
        assertTrue(wat.contains("f32x4.mul"));
        assertTrue(wat.contains("f32x4.sqrt"));
        assertTrue(wat.contains("(func $intVectorOps (export \"intVectorOps\") (param $arr i32) (result v128)"));
        assertTrue(wat.contains("v128.load"));
        assertTrue(wat.contains("i32x4.shl"));
        assertTrue(wat.contains("v128.store"));
    }

    @Test
    public void testMultiMemorySegregationWatGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.memory.Memory;\n" +
                "public class MultiMemoryApp {\n" +
                "    public void runMemoryOperations() {\n" +
                "        Memory heap = Memory.HEAP;\n" +
                "        heap.storeI32(0, 42);\n" +
                "        int val = heap.loadI32(0);\n" +
                "        heap.storeF64(8, 3.14159);\n" +
                "        double pi = heap.loadF64(8);\n" +
                "        int size = heap.size();\n" +
                "        heap.grow(1);\n" +
                "        heap.copy(100, Memory.STRINGS, 0, 16);\n" +
                "        heap.fill(200, 0, 32);\n" +
                "        String hello = \"Hello segregated memory\";\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(memory $strings 2)"));
        assertTrue(wat.contains("(export \"memory\" (memory $strings))"));
        assertTrue(wat.contains("(export \"strings\" (memory $strings))"));
        assertTrue(wat.contains("(memory $heap 2)"));
        assertTrue(wat.contains("(export \"heap\" (memory $heap))"));
        assertTrue(wat.contains("(memory $stack 2)"));
        assertTrue(wat.contains("(export \"stack\" (memory $stack))"));
        assertTrue(wat.contains("(data (memory $strings) (i32.const"));
        assertTrue(wat.contains("i32.store $heap"));
        assertTrue(wat.contains("i32.load $heap"));
        assertTrue(wat.contains("f64.store $heap"));
        assertTrue(wat.contains("f64.load $heap"));
        assertTrue(wat.contains("memory.size $heap"));
        assertTrue(wat.contains("memory.grow $heap"));
        assertTrue(wat.contains("memory.copy $heap $strings"));
        assertTrue(wat.contains("memory.fill $heap"));
    }

    @Test
    public void testBase64VLQEncodingAndDecoding() {
        int[] testValues = {0, 1, -1, 2, -2, 15, -15, 16, -16, 31, -31, 32, -32, 127, -127, 1000, -1000, 65536, -65536};
        for (int val : testValues) {
            String encoded = com.example.javaw.sourcemap.Base64VLQ.encode(val);
            java.util.List<Integer> decoded = com.example.javaw.sourcemap.Base64VLQ.decode(encoded);
            org.junit.jupiter.api.Assertions.assertEquals(1, decoded.size(), "Decoded size mismatch for: " + val);
            org.junit.jupiter.api.Assertions.assertEquals(val, (int) decoded.get(0), "Decoded value mismatch for: " + val);
        }
    }

    @Test
    public void testSourceMapGeneratorAndParser() {
        com.example.javaw.sourcemap.SourceMapGenerator generator = new com.example.javaw.sourcemap.SourceMapGenerator("TestClass.wat");
        generator.addSource("TestClass.java", "public class TestClass { ... }");
        generator.addName("addNumbers");

        // genLine=0, genCol=0 -> srcIdx=0, origLine=0, origCol=0
        generator.addMapping(0, 0, "TestClass.java", 0, 0, "addNumbers");
        // genLine=1, genCol=2 -> srcIdx=0, origLine=1, origCol=4
        generator.addMapping(1, 2, "TestClass.java", 1, 4, null);
        // genLine=2, genCol=4 -> srcIdx=0, origLine=2, origCol=8
        generator.addMapping(2, 4, "TestClass.java", 2, 8, null);

        String json = generator.generateJson();
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"version\": 3"));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"file\": \"TestClass.wat\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"TestClass.java\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"mappings\":"));

        com.example.javaw.sourcemap.SourceMap parsed = com.example.javaw.sourcemap.SourceMap.parse(json);
        org.junit.jupiter.api.Assertions.assertEquals(3, parsed.getVersion());
        org.junit.jupiter.api.Assertions.assertEquals("TestClass.wat", parsed.getFile());
        org.junit.jupiter.api.Assertions.assertEquals(1, parsed.getSources().size());
        org.junit.jupiter.api.Assertions.assertEquals("TestClass.java", parsed.getSources().get(0));

        com.example.javaw.sourcemap.MappingEntry m0 = parsed.findMapping(0, 0);
        org.junit.jupiter.api.Assertions.assertNotNull(m0);
        org.junit.jupiter.api.Assertions.assertEquals(0, m0.getOriginalLine());
        org.junit.jupiter.api.Assertions.assertEquals(0, m0.getOriginalColumn());
        org.junit.jupiter.api.Assertions.assertEquals("addNumbers", m0.getName());

        com.example.javaw.sourcemap.MappingEntry m1 = parsed.findMapping(1, 2);
        org.junit.jupiter.api.Assertions.assertNotNull(m1);
        org.junit.jupiter.api.Assertions.assertEquals(1, m1.getOriginalLine());
        org.junit.jupiter.api.Assertions.assertEquals(4, m1.getOriginalColumn());

        com.example.javaw.sourcemap.MappingEntry m2 = parsed.findMapping(2, 4);
        org.junit.jupiter.api.Assertions.assertNotNull(m2);
        org.junit.jupiter.api.Assertions.assertEquals(2, m2.getOriginalLine());
        org.junit.jupiter.api.Assertions.assertEquals(8, m2.getOriginalColumn());
    }

    @Test
    public void testSourceMapWasmEmbeddingAndExtraction() {
        byte[] dummyWasm = new byte[]{0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
        String mapUrl = "App.wasm.map";

        byte[] embedded = com.example.javaw.sourcemap.SourceMapGenerator.embedSourceMapInWasm(dummyWasm, mapUrl);
        org.junit.jupiter.api.Assertions.assertTrue(embedded.length > dummyWasm.length);

        String extracted = com.example.javaw.sourcemap.SourceMapGenerator.extractSourceMapUrlFromWasm(embedded);
        org.junit.jupiter.api.Assertions.assertEquals(mapUrl, extracted);
    }

    @Test
    public void testCompilationResultWithSourceMap() {
        String javaSource = "package com.example.javaw;\n" +
                "public class DebuggableApp {\n" +
                "    public int calculate(int x, int y) {\n" +
                "        int sum = x + y;\n" +
                "        return sum * 2;\n" +
                "    }\n" +
                "}\n";

        JavacToWasm.CompilationResult result = JavacToWasm.compile("DebuggableApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getSourceMapGenerator());
        org.junit.jupiter.api.Assertions.assertNotNull(result.getSourceMapJson());
        org.junit.jupiter.api.Assertions.assertTrue(result.getSourceMapJson().contains("\"mappings\""));
        org.junit.jupiter.api.Assertions.assertTrue(result.getSourceMapDataUri().startsWith("data:application/json;charset=utf-8;base64,"));
        org.junit.jupiter.api.Assertions.assertTrue(result.getWat().contains("(@custom \"sourceMappingURL\""));

        com.example.javaw.sourcemap.SourceMap map = result.getSourceMap();
        org.junit.jupiter.api.Assertions.assertNotNull(map);
        org.junit.jupiter.api.Assertions.assertTrue(map.getMappings().size() >= 3);
        org.junit.jupiter.api.Assertions.assertEquals("DebuggableApp.java", map.getSources().get(0));
    }

    @Test
    public void testJsInlineAnnotationWATGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.js.JS;\n" +
                "public class JsBridgeApp {\n" +
                "    @JS(\"return Math.hypot(a, b);\")\n" +
                "    public static native double hypot(double a, double b);\n" +
                "\n" +
                "    @JS(\"console.log(message);\")\n" +
                "    public static native void log(String message);\n" +
                "\n" +
                "    @JS(\"return 'Hello ' + name;\")\n" +
                "    public static native String greet(String name);\n" +
                "\n" +
                "    @JS(\"return window.innerWidth > 500;\")\n" +
                "    public static native boolean isWide();\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(@custom \"javaw_js_snippets\""));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_JsBridgeApp_hypot\" (func $dom_js_inline_JsBridgeApp_hypot (param f64 f64) (result f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_JsBridgeApp_log\" (func $dom_js_inline_JsBridgeApp_log (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_JsBridgeApp_greet\" (func $dom_js_inline_JsBridgeApp_greet (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_JsBridgeApp_isWide\" (func $dom_js_inline_JsBridgeApp_isWide (result i32)))"));
        assertTrue(wat.contains("(func $hypot (export \"hypot\") (param $a f64) (param $b f64) (result f64)"));
        assertTrue(wat.contains("call $dom_js_inline_JsBridgeApp_hypot"));
        assertTrue(wat.contains("call $dom_js_inline_JsBridgeApp_log"));
        assertTrue(wat.contains("call $dom_js_inline_JsBridgeApp_greet"));
        assertTrue(wat.contains("call $dom_js_inline_JsBridgeApp_isWide"));

        JavacToWasm.CompilationResult result = JavacToWasm.compile("JsBridgeApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsSnippets());
        org.junit.jupiter.api.Assertions.assertEquals(4, result.getJsSnippets().size());
        org.junit.jupiter.api.Assertions.assertTrue(result.getJsSnippetsJson().contains("Math.hypot"));
    }

    @Test
    public void testJsCoerceAnnotationAndSnippetModels() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.js.JS;\n" +
                "public class CoerceApp {\n" +
                "    @JS(\"return arr.reduce((a, b) => a + b, 0);\")\n" +
                "    public static native int sumArray(@JS.Coerce int[] arr);\n" +
                "\n" +
                "    @JS(\"return arr.map(x => x * 2);\")\n" +
                "    @JS.Coerce\n" +
                "    public static native int[] doubleArray(@JS.Coerce int[] arr);\n" +
                "\n" +
                "    @JS(\"return items.join(', ');\")\n" +
                "    public static native String joinStrings(@JS.Coerce String[] items);\n" +
                "\n" +
                "    @JS(\"return { ok: true, count: 42 };\")\n" +
                "    @JS.Coerce\n" +
                "    public static native String getJson();\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"js_inline_CoerceApp_sumArray\" (func $dom_js_inline_CoerceApp_sumArray (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CoerceApp_doubleArray\" (func $dom_js_inline_CoerceApp_doubleArray (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CoerceApp_joinStrings\" (func $dom_js_inline_CoerceApp_joinStrings (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CoerceApp_getJson\" (func $dom_js_inline_CoerceApp_getJson (result i32)))"));

        JavacToWasm.CompilationResult result = JavacToWasm.compile("CoerceApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsSnippets());
        var snippets = result.getJsSnippets();
        org.junit.jupiter.api.Assertions.assertEquals(4, snippets.size());

        var sumModel = snippets.get("js_inline_CoerceApp_sumArray");
        org.junit.jupiter.api.Assertions.assertNotNull(sumModel);
        org.junit.jupiter.api.Assertions.assertTrue(sumModel.getParameters().get(0).isCoerced());

        var doubleModel = snippets.get("js_inline_CoerceApp_doubleArray");
        org.junit.jupiter.api.Assertions.assertNotNull(doubleModel);
        org.junit.jupiter.api.Assertions.assertTrue(doubleModel.isCoerced());
        org.junit.jupiter.api.Assertions.assertTrue(doubleModel.getParameters().get(0).isCoerced());

        String json = result.getJsSnippetsJson();
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"isCoerced\":true"));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"coerced\":true"));
    }

    @Test
    public void testJsValueHierarchyWatGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.*;\n" +
                "public class JsValueApp {\n" +
                "    public static int checkValues() {\n" +
                "        JSValue n = JSValue.of(123);\n" +
                "        JSNumber num = JSNumber.of(45.67);\n" +
                "        JSString str = JSString.of(\"hello\");\n" +
                "        JSError err = JSError.typeError(\"Bad type\");\n" +
                "        boolean isNum = n.isNumber();\n" +
                "        String t = str.getTypeof();\n" +
                "        boolean inst = err.instanceOf(\"TypeError\");\n" +
                "        return 1;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"js_number_of_int\" (func $dom_js_number_of_int (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_number_of_double\" (func $dom_js_number_of_double (param f64) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_string_of\" (func $dom_js_string_of (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_type_error_of\" (func $dom_js_type_error_of (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_isNumber\" (func $dom_js_isNumber (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_typeof\" (func $dom_js_typeof (param i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_instanceof\" (func $dom_js_instanceof (param i32 i32 i32) (result i32)))"));
    }

    @Test
    public void testJsImportAnnotationAndFacadeGeneration() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.js.JS;\n" +
                "@JS.Import(\"Math\")\n" +
                "class CustomMath {\n" +
                "    public static native double hypot(double a, double b);\n" +
                "    public static native double PI();\n" +
                "}\n" +
                "@JS.Import(\"Date\")\n" +
                "class CustomDate {\n" +
                "    public static native CustomDate create(double ms);\n" +
                "    public native double getTime();\n" +
                "}\n" +
                "public class JsImportApp {\n" +
                "    public static double testMath() {\n" +
                "        return CustomMath.hypot(3.0, 4.0);\n" +
                "    }\n" +
                "    public static double testDate() {\n" +
                "        CustomDate d = CustomDate.create(1000.0);\n" +
                "        return d.getTime();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"dom\" \"js_inline_CustomMath_hypot\" (func $dom_js_inline_CustomMath_hypot (param f64 f64) (result f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CustomMath_PI\" (func $dom_js_inline_CustomMath_PI (result f64)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CustomDate_create\" (func $dom_js_inline_CustomDate_create (param f64) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_inline_CustomDate_getTime\" (func $dom_js_inline_CustomDate_getTime (param i32) (result f64)))"));
        assertTrue(wat.contains("(func $CustomDate_getTime (export \"CustomDate_getTime\") (param $this i32) (result f64)"));

        JavacToWasm.CompilationResult result = JavacToWasm.compile("JsImportApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsSnippets());
        var snippets = result.getJsSnippets();
        org.junit.jupiter.api.Assertions.assertTrue(snippets.containsKey("js_inline_CustomMath_hypot"));
        org.junit.jupiter.api.Assertions.assertEquals("return Math.hypot(a, b);", snippets.get("js_inline_CustomMath_hypot").getJsCode());
        org.junit.jupiter.api.Assertions.assertEquals("return Math.PI;", snippets.get("js_inline_CustomMath_PI").getJsCode());
        org.junit.jupiter.api.Assertions.assertEquals("return new Date(ms);", snippets.get("js_inline_CustomDate_create").getJsCode());
        org.junit.jupiter.api.Assertions.assertEquals("return this.getTime();", snippets.get("js_inline_CustomDate_getTime").getJsCode());
    }

    @Test
    public void testJsExportAnnotationAndMetadata() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.js.JS;\n" +
                "@JS.Export(\"Calculator\")\n" +
                "public class ExportedCalculator {\n" +
                "    private int base;\n" +
                "    public ExportedCalculator(int base) {\n" +
                "        this.base = base;\n" +
                "    }\n" +
                "    public int add(int x) {\n" +
                "        return this.base + x;\n" +
                "    }\n" +
                "    public static int multiply(int a, int b) {\n" +
                "        return a * b;\n" +
                "    }\n" +
                "    public String format(String prefix) {\n" +
                "        return prefix + \": \" + this.base;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(@custom \"javaw_js_exports\""));
        assertTrue(wat.contains("(func $ExportedCalculator__init (export \"ExportedCalculator__init\") (param $this i32) (param $base i32)"));
        assertTrue(wat.contains("(func $ExportedCalculator_add (export \"ExportedCalculator_add\") (param $this i32) (param $x i32) (result i32)"));
        assertTrue(wat.contains("(func $multiply (export \"multiply\") (param $a i32) (param $b i32) (result i32)"));

        JavacToWasm.CompilationResult result = JavacToWasm.compile("ExportedCalculator.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsExports());
        var exports = result.getJsExports();
        org.junit.jupiter.api.Assertions.assertTrue(exports.containsKey("Calculator"));
        var calcExport = exports.get("Calculator");
        org.junit.jupiter.api.Assertions.assertEquals("ExportedCalculator", calcExport.getClassName());
        org.junit.jupiter.api.Assertions.assertEquals("Calculator", calcExport.getExportName());
        org.junit.jupiter.api.Assertions.assertEquals(1, calcExport.getConstructors().size());
        org.junit.jupiter.api.Assertions.assertEquals(3, calcExport.getMethods().size());
    }

    @Test
    public void testJsCodeAndResourceBundling() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.js.JS;\n" +
                "import com.example.javaw.js.JSResource;\n" +
                "@JS.Code(\"function bundledHelper(x) { return x * 100; }\")\n" +
                "@JS.Resource(value = \"h1 { color: blue; }\", path = \"styles.css\")\n" +
                "public class BundledApp {\n" +
                "    @JS.Resource(value = \"{\\\"version\\\": 1}\", path = \"app.json\")\n" +
                "    public static String CONFIG;\n" +
                "    public static int testResources() {\n" +
                "        String css = JSResource.getAsString(\"styles.css\");\n" +
                "        boolean hasApp = JSResource.hasResource(\"app.json\");\n" +
                "        JSResource.injectStyle(css);\n" +
                "        JSResource.injectScript(\"console.log('Injected');\");\n" +
                "        String[] list = JSResource.listResources();\n" +
                "        JSResource.mountAll();\n" +
                "        return hasApp ? 1 : 0;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(@custom \"javaw_js_code\""));
        assertTrue(wat.contains("(@custom \"javaw_js_resources\""));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_get_string\" (func $dom_js_resource_get_string (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_has\" (func $dom_js_resource_has (param i32 i32) (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_inject_style\" (func $dom_js_resource_inject_style (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_inject_script\" (func $dom_js_resource_inject_script (param i32 i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_list\" (func $dom_js_resource_list (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"js_resource_mount_all\" (func $dom_js_resource_mount_all))"));

        JavacToWasm.CompilationResult result = JavacToWasm.compile("BundledApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsCodeBlocks());
        org.junit.jupiter.api.Assertions.assertEquals(1, result.getJsCodeBlocks().size());
        org.junit.jupiter.api.Assertions.assertEquals("function bundledHelper(x) { return x * 100; }", result.getJsCodeBlocks().get(0));

        org.junit.jupiter.api.Assertions.assertNotNull(result.getJsResources());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getJsResources().size());
        org.junit.jupiter.api.Assertions.assertEquals("h1 { color: blue; }", result.getJsResources().get("styles.css"));
        org.junit.jupiter.api.Assertions.assertEquals("{\\\"version\\\": 1}", result.getJsResources().get("app.json"));
    }

    @Test
    public void testWasiTerminalAndRepl() {
        String javaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.wasi.WASITerminal;\n" +
                "public class TerminalApp {\n" +
                "    public static String runRepl() {\n" +
                "        WASITerminal.setColor(WASITerminal.BRIGHT_CYAN);\n" +
                "        WASITerminal.println(\"=== javaw WASI Shell ===\");\n" +
                "        WASITerminal.resetColor();\n" +
                "        String input = WASITerminal.readLine(\"prompt> \");\n" +
                "        int ch = WASITerminal.readChar();\n" +
                "        int cols = WASITerminal.getColumns();\n" +
                "        int rows = WASITerminal.getRows();\n" +
                "        WASITerminal.clear();\n" +
                "        return input;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(import \"wasi_snapshot_preview1\" \"fd_write\""));
        assertTrue(wat.contains("(import \"wasi_snapshot_preview1\" \"fd_read\""));
        assertTrue(wat.contains("(import \"dom\" \"terminal_clear\" (func $dom_terminal_clear))"));
        assertTrue(wat.contains("(import \"dom\" \"terminal_get_columns\" (func $dom_terminal_get_columns (result i32)))"));
        assertTrue(wat.contains("(import \"dom\" \"terminal_get_rows\" (func $dom_terminal_get_rows (result i32)))"));
        assertTrue(wat.contains("call $__wasi_read_line"));
        assertTrue(wat.contains("call $__wasi_read_char"));
    }

    @Test
    public void testCompileAllExampleApplications() throws Exception {
        java.nio.file.Path examplesDir = java.nio.file.Files.exists(java.nio.file.Paths.get("examples"))
                ? java.nio.file.Paths.get("examples")
                : java.nio.file.Paths.get("../examples");
        org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(examplesDir), "examples directory must exist");

        String[] exampleFiles = new String[]{
                "todo/TodoApp.java",
                "canvas/GameOfLife.java",
                "simd/SimdParticles.java",
                "jsinterop/JsInteropShowcase.java",
                "terminal/WasiShellDemo.java",
                "records/RecordMathEngine.java",
                "threads/WorkerAtomicsDemo.java"
        };

        for (String fileStr : exampleFiles) {
            java.nio.file.Path p = examplesDir.resolve(fileStr);
            org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(p), "Example file missing: " + fileStr);
            String content = java.nio.file.Files.readString(p);
            CompilationUnit cu = StaticJavaParser.parse(content);
            String wat = JavacToWasm.generateWat(cu);
            org.junit.jupiter.api.Assertions.assertNotNull(wat);
            org.junit.jupiter.api.Assertions.assertTrue(wat.contains("(module"), "WAT must contain module declaration for " + fileStr);

            JavacToWasm.CompilationResult res = JavacToWasm.compile(p.getFileName().toString(), content, true);
            org.junit.jupiter.api.Assertions.assertNotNull(res);
            org.junit.jupiter.api.Assertions.assertNotNull(res.getWat());
            org.junit.jupiter.api.Assertions.assertTrue(res.getWat().length() > 0);
        }
    }

    @Test
    public void testStandardMathAndPrimitiveWrappers() {
        String javaSource = "package com.example.javaw;\n" +
                "public class StandardApp {\n" +
                "    public static double runMath(double x, double y) {\n" +
                "        double f = Math.floor(x);\n" +
                "        double c = Math.ceil(x);\n" +
                "        double s = Math.sqrt(y);\n" +
                "        double ab = Math.abs(x);\n" +
                "        double sin = Math.sin(x);\n" +
                "        double pi = Math.PI;\n" +
                "        return f + c + s + ab + sin + pi;\n" +
                "    }\n" +
                "    public static int runWrappers(String s, int val) {\n" +
                "        int parsed = Integer.parseInt(s);\n" +
                "        String hex = Integer.toHexString(val);\n" +
                "        int popcnt = Integer.bitCount(val);\n" +
                "        int clz = Integer.numberOfLeadingZeros(val);\n" +
                "        int max = Integer.MAX_VALUE;\n" +
                "        long lmax = Long.MAX_VALUE;\n" +
                "        double dmax = Double.MAX_VALUE;\n" +
                "        boolean b = Boolean.parseBoolean(\"true\");\n" +
                "        boolean isD = Character.isDigit('9');\n" +
                "        return parsed + popcnt + clz;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("f64.floor"));
        assertTrue(wat.contains("f64.ceil"));
        assertTrue(wat.contains("f64.sqrt"));
        assertTrue(wat.contains("f64.abs"));
        assertTrue(wat.contains("(import \"dom\" \"math_sin\" (func $dom_math_sin (param f64) (result f64)))"));
        assertTrue(wat.contains("call $dom_math_sin"));
        assertTrue(wat.contains("f64.const 3.141592653589793"));
        assertTrue(wat.contains("call $__str_parse_i32"));
        assertTrue(wat.contains("call $__str_to_hex_i32"));
        assertTrue(wat.contains("i32.popcnt"));
        assertTrue(wat.contains("i32.clz"));
        assertTrue(wat.contains("i32.const 2147483647"));
        assertTrue(wat.contains("i64.const 9223372036854775807"));
        assertTrue(wat.contains("f64.const 1.7976931348623157e+308"));
        assertTrue(wat.contains("call $__str_parse_bool"));
        assertTrue(wat.contains("call $__char_is_digit"));

        JavacToWasm.CompilationResult res = JavacToWasm.compile("StandardApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(res);
        org.junit.jupiter.api.Assertions.assertNotNull(res.getWat());
    }

    @Test
    public void testRootObjectVirtualContracts() {
        String javaSource = "package com.example.javaw;\n" +
                "public class ObjectApp {\n" +
                "    public static String testToString(Item item) {\n" +
                "        return item.toString();\n" +
                "    }\n" +
                "    public static boolean testEquals(Item a, Item b) {\n" +
                "        return a.equals(b);\n" +
                "    }\n" +
                "    public static int testHashCode(Item item) {\n" +
                "        return item.hashCode();\n" +
                "    }\n" +
                "    public static Item testClone(Item item) {\n" +
                "        return (Item) item.clone();\n" +
                "    }\n" +
                "}\n" +
                "class Item {\n" +
                "    public int id;\n" +
                "    public Item(int id) {\n" +
                "        this.id = id;\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $Object_toString"));
        assertTrue(wat.contains("(func $Object_equals"));
        assertTrue(wat.contains("(func $Object_hashCode"));
        assertTrue(wat.contains("(func $Object_clone"));
        assertTrue(wat.contains("call_indirect (type $t_sig_"));
        assertTrue(wat.contains("call $Object_clone"));

        JavacToWasm.CompilationResult res = JavacToWasm.compile("ObjectApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(res);
        org.junit.jupiter.api.Assertions.assertNotNull(res.getWat());
    }

    @Test
    public void testJavaEnumLanguageConstructs() {
        String javaSource = "package com.example.javaw;\n" +
                "public class EnumApp {\n" +
                "    public static int runEnum(Status status) {\n" +
                "        String n = status.name();\n" +
                "        int ord = status.ordinal();\n" +
                "        int cmp = status.compareTo(Status.PENDING);\n" +
                "        Status[] all = Status.values();\n" +
                "        Status completed = Status.valueOf(\"COMPLETED\");\n" +
                "        int res = switch (status) {\n" +
                "            case PENDING -> 10;\n" +
                "            case ACTIVE -> 20;\n" +
                "            case COMPLETED -> 30;\n" +
                "        };\n" +
                "        return ord + cmp + all.length + res;\n" +
                "    }\n" +
                "}\n" +
                "enum Status {\n" +
                "    PENDING,\n" +
                "    ACTIVE,\n" +
                "    COMPLETED\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = JavacToWasm.generateWat(cu);

        assertTrue(wat.contains("(func $Status_name"));
        assertTrue(wat.contains("(func $Status_ordinal"));
        assertTrue(wat.contains("(func $Status_compareTo"));
        assertTrue(wat.contains("(func $Status_values"));
        assertTrue(wat.contains("(func $Status_valueOf"));
        assertTrue(wat.contains("(func $__init_enums"));
        assertTrue(wat.contains("(start $__init_enums)"));

        JavacToWasm.CompilationResult res = JavacToWasm.compile("EnumApp.java", javaSource, true);
        org.junit.jupiter.api.Assertions.assertNotNull(res);
        org.junit.jupiter.api.Assertions.assertNotNull(res.getWat());
    }
}
