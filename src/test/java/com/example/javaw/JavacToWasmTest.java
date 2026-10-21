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

        assertTrue(wat.contains("(memory (export \"memory\") 1)"));
        assertTrue(wat.contains("(data (i32.const 4096) \"Hello, WebAssembly!\\00\")"));
        assertTrue(wat.contains("i32.const 4096"));
        assertTrue(wat.contains("i32.const 19")); // length of "Hello, WebAssembly!"
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
}
