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
}
