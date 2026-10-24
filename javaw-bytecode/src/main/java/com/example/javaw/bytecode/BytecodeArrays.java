package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

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

    public static boolean isClone(MethodInsnNode call) {
        return call.getOpcode() == INVOKEVIRTUAL && !call.itf && call.owner.startsWith("[")
                && call.name.equals("clone") && call.desc.equals("()Ljava/lang/Object;");
    }

    public static boolean isArraycopy(MethodInsnNode call) {
        return call.getOpcode() == INVOKESTATIC && !call.itf && call.owner.equals("java/lang/System")
                && call.name.equals("arraycopy") && call.desc.equals("(Ljava/lang/Object;ILjava/lang/Object;II)V");
    }

    static boolean usesRuntime(ClassNode type) {
        for (MethodNode method : type.methods)
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && (isClone(call) || isArraycopy(call))) return true;
                int opcode = instruction.getOpcode();
                if (opcode == MULTIANEWARRAY || opcode == NEWARRAY || opcode == ANEWARRAY || opcode == ARRAYLENGTH
                        || opcode >= IALOAD && opcode <= SALOAD
                        || opcode >= IASTORE && opcode <= SASTORE) return true;
            }
        return false;
    }

    /** Header: class ID at 0, length at 4, tightly packed zeroed elements at 8. */
    static String runtimeWat(BytecodeDispatch dispatch) {
        String wat = """
            (func $__new_primitive_array (param $length i32) (param $tag i32) (param $width i32) (result i32)
                (local $bytes i32) (local $array i32)
                local.get $length
                i32.const 0
                i32.lt_s
                if @NEGATIVE_SIZE@ end
                local.get $length
                i32.const 2147483639
                local.get $width
                i32.div_u
                i32.gt_u
                if @OUT_OF_MEMORY@ end
                local.get $length
                local.get $width
                i32.mul
                i32.const 8
                i32.add
                local.tee $bytes
                call $__heap_alloc
                local.tee $array
                i32.eqz
                if @OUT_OF_MEMORY@ end
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
                if @NULL_POINTER@ end
                local.get $array
                i32.load offset=4)
            (func $__primitive_array_address (param $array i32) (param $index i32)
                    (param $tag i32) (param $width i32) (result i32)
                (local $actual i32)
                local.get $array
                i32.eqz
                if @NULL_POINTER@ end
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
                if @TRAP@ end
                local.get $index
                local.get $array
                i32.load offset=4
                i32.ge_u
                if @BOUNDS@ end
                local.get $array
                i32.const 8
                i32.add
                local.get $index
                local.get $width
                i32.mul
                i32.add)
            """ + transferRuntimeWat();
        for (var kind : BytecodeExceptions.Implicit.values())
            wat = wat.replace("@" + kind.name() + "@", dispatch.implicitFailureWat(kind));
        return wat.replace("@TRAP@", "unreachable");
    }

    private static String transferRuntimeWat() {
        StringBuilder width = new StringBuilder("(func $__array_width (param $tag i32) (result i32)\n");
        width.append("local.get $tag i32.const -100 i32.le_s if i32.const 4 return end\n");
        for (Primitive primitive : PRIMITIVES)
            width.append("local.get $tag i32.const ").append(primitive.classId()).append(" i32.eq if i32.const ")
                    .append(primitive.width()).append(" return end\n");
        width.append("@ARRAY_STORE@)\n");
        return width + """
            (func $__array_clone (param $source i32) (result i32)
                (local $length i32) (local $tag i32) (local $width i32) (local $copy i32)
                local.get $source call $__array_length local.set $length
                local.get $source i32.load local.tee $tag call $__array_width local.set $width
                local.get $length local.get $tag local.get $width call $__new_primitive_array local.set $copy
                local.get $copy i32.const 8 i32.add
                local.get $source i32.const 8 i32.add
                local.get $length local.get $width i32.mul memory.copy
                local.get $copy)
            (func $__arraycopy (param $source i32) (param $srcPos i32)
                    (param $dest i32) (param $dstPos i32) (param $count i32)
                (local $srcLength i32) (local $dstLength i32) (local $srcTag i32) (local $dstTag i32)
                (local $width i32) (local $index i32)
                local.get $source call $__array_length local.set $srcLength
                local.get $dest call $__array_length local.set $dstLength
                local.get $source i32.load local.tee $srcTag call $__array_width local.set $width
                local.get $dest i32.load local.tee $dstTag call $__array_width drop
                local.get $srcTag i32.const -100 i32.gt_s
                local.get $dstTag i32.const -100 i32.gt_s i32.or
                if local.get $srcTag local.get $dstTag i32.ne if @ARRAY_STORE@ end end
                local.get $srcPos local.get $srcLength i32.gt_u if @BOUNDS@ end
                local.get $dstPos local.get $dstLength i32.gt_u if @BOUNDS@ end
                local.get $count local.get $srcLength local.get $srcPos i32.sub i32.gt_u if @BOUNDS@ end
                local.get $count local.get $dstLength local.get $dstPos i32.sub i32.gt_u if @BOUNDS@ end
                local.get $srcTag local.get $dstTag i32.eq
                if
                    local.get $dest i32.const 8 i32.add local.get $dstPos local.get $width i32.mul i32.add
                    local.get $source i32.const 8 i32.add local.get $srcPos local.get $width i32.mul i32.add
                    local.get $count local.get $width i32.mul memory.copy
                    return
                end
                block $done loop $next
                    local.get $index local.get $count i32.ge_u br_if $done
                    local.get $dest local.get $dstPos local.get $index i32.add
                    local.get $source i32.const 8 i32.add
                    local.get $srcPos local.get $index i32.add i32.const 4 i32.mul i32.add i32.load
                    call $__reference_array_store
                    local.get $index i32.const 1 i32.add local.set $index br $next
                end end)
            """;
    }
}
