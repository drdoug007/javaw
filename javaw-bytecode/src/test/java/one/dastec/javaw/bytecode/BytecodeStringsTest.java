package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BytecodeStrings#runtimeWat} leaves each text constant's character stores out until after pruning, and
 * {@link BytecodeStrings#charactersWat} restores them from the function name. These checks rebuild constants in the
 * form the generator wrote inline before, then require the restored text to match it exactly.
 */
class BytecodeStringsTest {
    private static final List<String> VALUES = List.of("", "a", "_", "__", "0", "f", "/ by zero", "java.lang.String",
            "éü", "Ā࿿", "က￿", "😀", "\u0000\u0001", "a_b", "Main$1");

    /** One constant as the generator wrote it inline, with its stores; {@code stores} false leaves them out. */
    private static String constant(String value, boolean stores) {
        String function = BytecodeStrings.symbol(value), global = function + "_cached";
        StringBuilder wat = new StringBuilder("(global ").append(global).append(" (mut i32) (i32.const 0))\n");
        wat.append("(func ").append(function).append(" (result i32) (local $chars i32) (local $text i32)\n");
        wat.append("global.get ").append(global).append(" if global.get ").append(global).append(" return end\n");
        wat.append("i32.const ").append(value.length()).append(" i32.const -5 i32.const 2 call $__new_primitive_array local.set $chars\n");
        if (stores) for (int i = 0; i < value.length(); i++)
            wat.append("local.get $chars i32.const ").append(value.charAt(i) + 0).append(" i32.store16 offset=").append(8 + i * 2).append('\n');
        wat.append("i32.const 24 call $__heap_alloc local.tee $text i32.eqz if call $__throw_out_of_memory unreachable end\n");
        wat.append("local.get $text call $intern local.set $text\nlocal.get $text global.set ").append(global).append(" local.get $text)\n");
        return wat.toString();
    }

    @Test void restoresEveryConstantsCharacterStoresExactly() {
        StringBuilder lean = new StringBuilder("(module\n(func $other (result i32) i32.const 7)\n"), full = new StringBuilder(lean);
        for (String value : VALUES) { lean.append(constant(value, false)); full.append(constant(value, true)); }
        lean.append(")\n"); full.append(")\n");
        assertEquals(full.toString(), BytecodeStrings.charactersWat(lean.toString()));
    }

    @Test void leavesModulesWithoutTextConstantsUnchanged() {
        String wat = "(module\n(func $__textual_helper_is_not_called_here (result i32) i32.const 1)\n)\n".replace("(func $__text", "(func $__TEXT");
        assertSame(wat, BytecodeStrings.charactersWat(wat));
    }

    @Test void rejectsTextFunctionsWithoutTheGeneratedHeader() {
        assertThrows(IllegalStateException.class, () -> BytecodeStrings.charactersWat("(module\n(func $__text_zz (result i32) i32.const 0)\n)\n"));
        String foreign = constant("a", false).replace("(local $chars i32) ", "");
        assertThrows(IllegalStateException.class, () -> BytecodeStrings.charactersWat("(module\n" + foreign + ")\n"));
    }
}
