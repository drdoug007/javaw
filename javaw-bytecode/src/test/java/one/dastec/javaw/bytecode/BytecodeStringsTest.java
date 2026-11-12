package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BytecodeStrings#runtimeWat} leaves each text constant's characters out until after pruning, and
 * {@link BytecodeStrings#charactersWat} adds them from the function name: one {@code memory.init} from a passive data
 * segment of the value's UTF-16LE units. These checks decode every segment back to its value.
 */
class BytecodeStringsTest {
    private static final List<String> VALUES = List.of("", "a", "_", "__", "0", "f", "/ by zero", "java.lang.String",
            "éü", "Ā࿿", "က￿", "😀", "\u0000\u0001", "a_b", "Main$1", "\"", "\\", "say \"hi\"\\n");

    /** One constant as {@link BytecodeStrings#runtimeWat} writes it: no characters yet. */
    private static String constant(String value) {
        String function = BytecodeStrings.symbol(value), global = function + "_cached";
        StringBuilder wat = new StringBuilder("(global ").append(global).append(" (mut i32) (i32.const 0))\n");
        wat.append("(func ").append(function).append(" (result i32) (local $chars i32) (local $text i32)\n");
        wat.append("global.get ").append(global).append(" if global.get ").append(global).append(" return end\n");
        wat.append("i32.const ").append(value.length()).append(" i32.const -5 i32.const 2 call $__new_primitive_array local.set $chars\n");
        wat.append("i32.const 24 call $__heap_alloc local.tee $text i32.eqz if call $__throw_out_of_memory unreachable end\n");
        wat.append("local.get $text call $intern local.set $text\nlocal.get $text global.set ").append(global).append(" local.get $text)\n");
        return wat.toString();
    }

    /** The bytes of a WAT string literal body: raw characters and {@code \\hh} escapes, as utf16 writes them. */
    private static byte[] unescape(String literal) {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '\\') { bytes.write(Integer.parseInt(literal, i + 1, i + 3, 16)); i += 2; }
            else bytes.write(c);
        }
        return bytes.toByteArray();
    }

    @Test void copiesEveryConstantsCharactersFromItsOwnDataSegment() {
        StringBuilder lean = new StringBuilder("(module\n(func $other (result i32) i32.const 7)\n");
        for (String value : VALUES) lean.append(constant(value));
        lean.append(")\n");
        String wat = BytecodeStrings.charactersWat(lean.toString());
        assertTrue(wat.endsWith(")\n") && wat.startsWith(lean.substring(0, lean.indexOf("(global"))), wat);
        int segment = 0;
        for (String value : VALUES) {
            String function = "(func " + BytecodeStrings.symbol(value) + " ";
            int at = wat.indexOf(function), end = wat.indexOf("\n(", at + 1);
            String body = wat.substring(at, end);
            if (value.isEmpty()) { assertFalse(body.contains("memory.init"), body); continue; }
            String name = "$__chars_" + segment++;
            assertTrue(body.contains("local.get $chars i32.const 8 i32.add i32.const 0 i32.const " + value.length() * 2 + " memory.init " + name + "\n"), body);
            int data = wat.indexOf("(data " + name + " \""), open = data + ("(data " + name + " \"").length(), close = wat.indexOf("\")\n", open);
            assertTrue(data > 0, name);
            assertArrayEquals(value.getBytes(java.nio.charset.StandardCharsets.UTF_16LE), unescape(wat.substring(open, close)), value);
        }
        assertFalse(wat.contains("$__chars_" + segment), "one segment per non-empty constant");
        assertFalse(wat.contains("i32.store16"));
    }

    @Test void literalsCannotSpellModuleText() {
        // These UTF-16LE units spell "(global $__text_7a_cached (mut i32) " byte for byte; the collector's root scan
        // would otherwise match it inside a data segment.
        String text = "(global $__text_7a_cached (mut i32) ";
        String crafted = new String((text.length() % 2 == 0 ? text : text + " ").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                java.nio.charset.StandardCharsets.UTF_16LE);
        assertTrue(new String(crafted.getBytes(java.nio.charset.StandardCharsets.UTF_16LE), java.nio.charset.StandardCharsets.ISO_8859_1).startsWith(text));
        for (String value : List.of(crafted, "(data (i32.const 8) ", ";; comment", "a)b$c")) {
            String literal = BytecodeStrings.utf16(value);
            for (char c : "()$;\"".toCharArray()) assertTrue(literal.indexOf(c) < 0, literal);
            assertArrayEquals(value.getBytes(java.nio.charset.StandardCharsets.UTF_16LE), unescape(literal));
        }
    }

    @Test void leavesModulesWithoutTextConstantsUnchanged() {
        String wat = "(module\n(func $__textual_helper_is_not_called_here (result i32) i32.const 1)\n)\n".replace("(func $__text", "(func $__TEXT");
        assertSame(wat, BytecodeStrings.charactersWat(wat));
    }

    @Test void rejectsTextFunctionsWithoutTheGeneratedHeader() {
        assertThrows(IllegalStateException.class, () -> BytecodeStrings.charactersWat("(module\n(func $__text_zz (result i32) i32.const 0)\n)\n"));
        String foreign = constant("a").replace("(local $chars i32) ", "");
        assertThrows(IllegalStateException.class, () -> BytecodeStrings.charactersWat("(module\n" + foreign + ")\n"));
    }
}
