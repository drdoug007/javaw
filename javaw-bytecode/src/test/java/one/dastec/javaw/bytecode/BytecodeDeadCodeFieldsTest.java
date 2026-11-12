package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** {@link BytecodeDeadCode#fields} jumps between structural characters; it must split text exactly like the per-character loop it replaced. */
class BytecodeDeadCodeFieldsTest {
    private static final Pattern NAME = Pattern.compile("\\(func (\\$[^\\s()]+)");
    private static final String[] TOKENS = {"(", ")", "(", ")", "\"", "\\", ";", ";;", "(;", "\n", " ", "x", "func", "(func $f", "(func $",
            "(global $g", "\\\"", "\\\\", "é", "\t", "(export \"a)\" (func $f))", ";; c)\n", "(data \"(;\")"};

    /** The loop {@code BytecodeDeadCode.fields} used before, kept verbatim as the reference. */
    private static List<BytecodeDeadCode.Field> reference(String wat, int position) {
        List<BytecodeDeadCode.Field> result = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = position; i < wat.length(); i++) {
            char c = wat.charAt(i);
            if (c == '"') {
                for (i++; i < wat.length() && wat.charAt(i) != '"'; i++) if (wat.charAt(i) == '\\') i++;
            } else if (c == ';' && i + 1 < wat.length() && wat.charAt(i + 1) == ';') {
                while (i < wat.length() && wat.charAt(i) != '\n') i++;
            } else if (c == '(' && i + 1 < wat.length() && wat.charAt(i + 1) == ';') {
                return null;
            } else if (c == '(') {
                if (depth++ == 0) start = i;
            } else if (c == ')') {
                if (depth == 0) return result;
                if (--depth == 0) {
                    int end = i + 1;
                    if (end < wat.length() && wat.charAt(end) == '\n') end++;
                    Matcher name = NAME.matcher(wat).region(start, end);
                    result.add(new BytecodeDeadCode.Field(start, end, name.lookingAt() ? name.group(1) : null));
                }
            }
        }
        return null;
    }

    private static void check(String wat) {
        for (int position = 0; position <= wat.length(); position++)
            assertEquals(reference(wat, position), BytecodeDeadCode.fields(wat, position), wat + " from " + position);
    }

    @Test void splitsRandomTextExactlyLikeThePerCharacterLoop() {
        Random random = new Random(20261112);
        for (int round = 0; round < 50_000; round++) {
            StringBuilder text = new StringBuilder(random.nextBoolean() ? "(module\n" : "");
            for (int i = random.nextInt(30); i > 0; i--) text.append(TOKENS[random.nextInt(TOKENS.length)]);
            if (random.nextBoolean()) text.append(")");
            check(text.toString());
        }
    }

    @Test void handlesEscapesCommentsAndUnterminatedText() {
        for (String wat : List.of("(module\n(func $a)\n(func $b))", "(module\n(func $a)(func $b)\n)", "(module (data \"\\\"\") (func $f))",
                "(module (data \"a\\\\\") (func $f))", "(module (data \"a\\", "(module (data \"a", "(module ;; x\n(func $f))",
                "(module ;; never ends", "(module (; block ;) (func $f))", "(module ; (func $f))", "(module (func $f (export \"(;\")))",
                "(module\n(func $f\n(local $x i32)\n)\n)", "", ")", "(module\n", "(module\n(global $g (mut i32) (i32.const 0))\n)\n"))
            check(wat);
    }
}
