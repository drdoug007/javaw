package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

/** Primitive-array ABI shared by admission, runtime type tests and lowering. */
public final class BytecodeArrays implements Opcodes {
    public record Primitive(int atype, String descriptor, int width, String wasmType, String load, String store) {
        public int classId() { return -atype; }
    }

    public static final List<Primitive> PRIMITIVES = List.of(
            new Primitive(T_BOOLEAN, "[Z", 1, "i32", "i32.load8_s", "i32.store8"),
            new Primitive(T_BYTE, "[B", 1, "i32", "i32.load8_s", "i32.store8"),
            new Primitive(T_CHAR, "[C", 2, "i32", "i32.load16_u", "i32.store16"),
            new Primitive(T_SHORT, "[S", 2, "i32", "i32.load16_s", "i32.store16"),
            new Primitive(T_INT, "[I", 4, "i32", "i32.load", "i32.store"),
            new Primitive(T_LONG, "[J", 8, "i64", "i64.load", "i64.store"),
            new Primitive(T_FLOAT, "[F", 4, "f32", "f32.load", "f32.store"),
            new Primitive(T_DOUBLE, "[D", 8, "f64", "f64.load", "f64.store"));

    public static Primitive primitive(int atype) {
        return PRIMITIVES.stream().filter(p -> p.atype == atype).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid NEWARRAY atype " + atype));
    }

    public static Primitive primitive(String descriptor) {
        return PRIMITIVES.stream().filter(p -> p.descriptor.equals(descriptor)).findFirst().orElse(null);
    }

    public static Primitive element(int opcode) {
        return primitive(switch (opcode) {
            case BALOAD, BASTORE -> T_BYTE; // The same JVM opcode also handles boolean arrays.
            case CALOAD, CASTORE -> T_CHAR;
            case SALOAD, SASTORE -> T_SHORT;
            case IALOAD, IASTORE -> T_INT;
            case LALOAD, LASTORE -> T_LONG;
            case FALOAD, FASTORE -> T_FLOAT;
            case DALOAD, DASTORE -> T_DOUBLE;
            default -> throw new IllegalArgumentException("not a primitive element opcode " + opcode);
        });
    }

    static boolean usesRuntime(ClassNode type) {
        for (MethodNode method : type.methods)
            for (AbstractInsnNode instruction : method.instructions) {
                int opcode = instruction.getOpcode();
                if (opcode == MULTIANEWARRAY || opcode == NEWARRAY || opcode == ANEWARRAY || opcode == ARRAYLENGTH
                        || opcode >= IALOAD && opcode <= SALOAD
                        || opcode >= IASTORE && opcode <= SASTORE) return true;
            }
        return false;
    }

    /** Header: class ID at 0, length at 4, tightly packed zeroed elements at 8. */
    static String runtimeWat() {
        return """
            (func $__new_primitive_array (param $length i32) (param $tag i32) (param $width i32) (result i32)
                (local $bytes i32) (local $array i32)
                local.get $length
                i32.const 0
                i32.lt_s
                if unreachable end
                local.get $length
                i32.const 2147483639
                local.get $width
                i32.div_u
                i32.gt_u
                if unreachable end
                local.get $length
                local.get $width
                i32.mul
                i32.const 8
                i32.add
                local.tee $bytes
                call $__heap_alloc
                local.tee $array
                i32.eqz
                if unreachable end
                local.get $array
                i32.const 0
                local.get $bytes
                memory.fill
                local.get $array
                local.get $tag
                i32.store
                local.get $array
                local.get $length
                i32.store offset=4
                local.get $array)
            (func $__array_length (param $array i32) (result i32)
                local.get $array
                i32.eqz
                if unreachable end
                local.get $array
                i32.load offset=4)
            (func $__primitive_array_address (param $array i32) (param $index i32)
                    (param $tag i32) (param $width i32) (result i32)
                (local $actual i32)
                local.get $array
                i32.eqz
                if unreachable end
                local.get $array
                i32.load
                local.tee $actual
                local.get $tag
                i32.eq
                local.get $tag
                i32.const -8
                i32.eq
                local.get $actual
                i32.const -4
                i32.eq
                i32.and
                i32.or
                i32.eqz
                if unreachable end
                local.get $index
                local.get $array
                i32.load offset=4
                i32.ge_u
                if unreachable end
                local.get $array
                i32.const 8
                i32.add
                local.get $index
                local.get $width
                i32.mul
                i32.add)
            """;
    }
}
