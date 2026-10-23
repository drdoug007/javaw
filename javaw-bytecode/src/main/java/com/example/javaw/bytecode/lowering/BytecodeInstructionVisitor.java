package com.example.javaw.bytecode.lowering;

import com.example.javaw.bytecode.TypeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class BytecodeInstructionVisitor {
    private final StackToLocalMapper stack;
    private final Map<String, Integer> stringPool;
    private final Map<String, Integer> fieldOffsets;
    private final StringBuilder out = new StringBuilder();

    public BytecodeInstructionVisitor(StackToLocalMapper stack, Map<String, Integer> stringPool, Map<String, Integer> fieldOffsets) {
        this.stack = stack;
        this.stringPool = stringPool;
        this.fieldOffsets = fieldOffsets;
    }

    public String getOutput() {
        return out.toString();
    }

    private void emit(String line) {
        out.append("        ").append(line).append("\n");
    }

    public void visitInsn(AbstractInsnNode insn) {
        if (insn instanceof IncompletInsn || insn instanceof LineNumberNode || insn instanceof FrameNode || insn instanceof LabelNode) {
            return;
        }

        int opcode = insn.getOpcode();
        switch (opcode) {
            case Opcodes.NOP:
                break;

            case Opcodes.ACONST_NULL:
                stack.push("i32");
                emit("i32.const 0");
                emit("local.set " + stack.peek());
                break;

            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                int iconstVal = opcode - Opcodes.ICONST_0;
                stack.push("i32");
                emit("i32.const " + iconstVal);
                emit("local.set " + stack.peek());
                break;

            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                long lconstVal = opcode - Opcodes.LCONST_0;
                stack.push("i64");
                emit("i64.const " + lconstVal);
                emit("local.set " + stack.peek());
                break;

            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                float fconstVal = (float) (opcode - Opcodes.FCONST_0);
                stack.push("f32");
                emit("f32.const " + fconstVal);
                emit("local.set " + stack.peek());
                break;

            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                double dconstVal = (double) (opcode - Opcodes.DCONST_0);
                stack.push("f64");
                emit("f64.const " + dconstVal);
                emit("local.set " + stack.peek());
                break;

            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                int ipushVal = ((IntInsnNode) insn).operand;
                stack.push("i32");
                emit("i32.const " + ipushVal);
                emit("local.set " + stack.peek());
                break;

            case Opcodes.LDC:
                visitLdc((LdcInsnNode) insn);
                break;

            case Opcodes.ILOAD:
            case Opcodes.ALOAD:
                int varIdx = ((VarInsnNode) insn).var;
                stack.push("i32");
                emit("local.get " + stack.getLocalVar(varIdx));
                emit("local.set " + stack.peek());
                break;

            case Opcodes.LLOAD:
                int lvarIdx = ((VarInsnNode) insn).var;
                stack.push("i64");
                emit("local.get " + stack.getLocalVar(lvarIdx));
                emit("local.set " + stack.peek());
                break;

            case Opcodes.FLOAD:
                int fvarIdx = ((VarInsnNode) insn).var;
                stack.push("f32");
                emit("local.get " + stack.getLocalVar(fvarIdx));
                emit("local.set " + stack.peek());
                break;

            case Opcodes.DLOAD:
                int dvarIdx = ((VarInsnNode) insn).var;
                stack.push("f64");
                emit("local.get " + stack.getLocalVar(dvarIdx));
                emit("local.set " + stack.peek());
                break;

            case Opcodes.ISTORE:
            case Opcodes.ASTORE:
                int storeIdx = ((VarInsnNode) insn).var;
                String topI = stack.pop();
                emit("local.get " + topI);
                emit("local.set " + stack.getLocalVar(storeIdx));
                break;

            case Opcodes.LSTORE:
                int lstoreIdx = ((VarInsnNode) insn).var;
                String topL = stack.pop();
                emit("local.get " + topL);
                emit("local.set " + stack.getLocalVar(lstoreIdx));
                break;

            case Opcodes.FSTORE:
                int fstoreIdx = ((VarInsnNode) insn).var;
                String topF = stack.pop();
                emit("local.get " + topF);
                emit("local.set " + stack.getLocalVar(fstoreIdx));
                break;

            case Opcodes.DSTORE:
                int dstoreIdx = ((VarInsnNode) insn).var;
                String topD = stack.pop();
                emit("local.get " + topD);
                emit("local.set " + stack.getLocalVar(dstoreIdx));
                break;

            case Opcodes.IINC:
                IincInsnNode iinc = (IincInsnNode) insn;
                emit("local.get " + stack.getLocalVar(iinc.var));
                emit("i32.const " + iinc.incr);
                emit("i32.add");
                emit("local.set " + stack.getLocalVar(iinc.var));
                break;

            case Opcodes.POP:
                stack.pop();
                break;

            case Opcodes.POP2:
                stack.pop();
                stack.pop();
                break;

            case Opcodes.DUP:
                String currTop = stack.peek();
                String dupNew = stack.push("i32");
                emit("local.get " + currTop);
                emit("local.set " + dupNew);
                break;

            case Opcodes.IADD:
                visitBinaryOp("i32", "i32.add");
                break;
            case Opcodes.ISUB:
                visitBinaryOp("i32", "i32.sub");
                break;
            case Opcodes.IMUL:
                visitBinaryOp("i32", "i32.mul");
                break;
            case Opcodes.IDIV: {
                String divisor = stack.pop();
                String dividend = stack.peek();
                emit("local.get " + dividend);
                emit("i32.const -2147483648");
                emit("i32.eq");
                emit("local.get " + divisor);
                emit("i32.const -1");
                emit("i32.eq");
                emit("i32.and");
                emit("if (result i32)");
                emit("i32.const -2147483648");
                emit("else");
                emit("local.get " + dividend);
                emit("local.get " + divisor);
                emit("i32.div_s");
                emit("end");
                emit("local.set " + dividend);
                break;
            }
            case Opcodes.IREM:
                visitBinaryOp("i32", "i32.rem_s");
                break;
            case Opcodes.INEG:
                emit("i32.const 0");
                emit("local.get " + stack.peek());
                emit("i32.sub");
                emit("local.set " + stack.peek());
                break;

            case Opcodes.ISHL:
                visitBinaryOp("i32", "i32.shl");
                break;
            case Opcodes.ISHR:
                visitBinaryOp("i32", "i32.shr_s");
                break;
            case Opcodes.IUSHR:
                visitBinaryOp("i32", "i32.shr_u");
                break;
            case Opcodes.IAND:
                visitBinaryOp("i32", "i32.and");
                break;
            case Opcodes.IOR:
                visitBinaryOp("i32", "i32.or");
                break;
            case Opcodes.IXOR:
                visitBinaryOp("i32", "i32.xor");
                break;

            case Opcodes.LADD:
                visitBinaryOp("i64", "i64.add");
                break;
            case Opcodes.LSUB:
                visitBinaryOp("i64", "i64.sub");
                break;
            case Opcodes.LMUL:
                visitBinaryOp("i64", "i64.mul");
                break;
            case Opcodes.LDIV:
                visitBinaryOp("i64", "i64.div_s");
                break;
            case Opcodes.LREM:
                visitBinaryOp("i64", "i64.rem_s");
                break;
            case Opcodes.LNEG:
                emit("i64.const 0");
                emit("local.get " + stack.peek());
                emit("i64.sub");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.LSHL:
                visitBinaryOp("i64", "i64.shl");
                break;
            case Opcodes.LSHR:
                visitBinaryOp("i64", "i64.shr_s");
                break;
            case Opcodes.LUSHR:
                visitBinaryOp("i64", "i64.shr_u");
                break;
            case Opcodes.LAND:
                visitBinaryOp("i64", "i64.and");
                break;
            case Opcodes.LOR:
                visitBinaryOp("i64", "i64.or");
                break;
            case Opcodes.LXOR:
                visitBinaryOp("i64", "i64.xor");
                break;

            case Opcodes.FADD:
                visitBinaryOp("f32", "f32.add");
                break;
            case Opcodes.FSUB:
                visitBinaryOp("f32", "f32.sub");
                break;
            case Opcodes.FMUL:
                visitBinaryOp("f32", "f32.mul");
                break;
            case Opcodes.FDIV:
                visitBinaryOp("f32", "f32.div");
                break;
            case Opcodes.FNEG:
                emit("local.get " + stack.peek());
                emit("f32.neg");
                emit("local.set " + stack.peek());
                break;

            case Opcodes.DADD:
                visitBinaryOp("f64", "f64.add");
                break;
            case Opcodes.DSUB:
                visitBinaryOp("f64", "f64.sub");
                break;
            case Opcodes.DMUL:
                visitBinaryOp("f64", "f64.mul");
                break;
            case Opcodes.DDIV:
                visitBinaryOp("f64", "f64.div");
                break;
            case Opcodes.DNEG:
                emit("local.get " + stack.peek());
                emit("f64.neg");
                emit("local.set " + stack.peek());
                break;

            case Opcodes.I2L:
                emit("local.get " + stack.peek());
                emit("i64.extend_i32_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.I2F:
                emit("local.get " + stack.peek());
                emit("f32.convert_i32_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.I2D:
                emit("local.get " + stack.peek());
                emit("f64.convert_i32_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.L2I:
                emit("local.get " + stack.peek());
                emit("i32.wrap_i64");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.F2I:
                emit("local.get " + stack.peek());
                emit("i32.trunc_f32_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.D2I:
                emit("local.get " + stack.peek());
                emit("i32.trunc_f64_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.I2B:
                emit("local.get " + stack.peek());
                emit("i32.extend8_s");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.I2C:
                emit("local.get " + stack.peek());
                emit("i32.const 65535");
                emit("i32.and");
                emit("local.set " + stack.peek());
                break;
            case Opcodes.I2S:
                emit("local.get " + stack.peek());
                emit("i32.extend16_s");
                emit("local.set " + stack.peek());
                break;

            case Opcodes.ARRAYLENGTH:
                String arrRef = stack.peek();
                emit("local.get " + arrRef);
                emit("i32.load offset=0");
                emit("local.set " + arrRef);
                break;

            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.AALOAD:
                String idx = stack.pop();
                String arr = stack.peek();
                emit("local.get " + arr);
                emit("i32.const 4");
                emit("i32.add");
                emit("local.get " + idx);
                emit("i32.const 4");
                emit("i32.mul");
                emit("i32.add");
                emit("i32.load offset=0");
                emit("local.set " + arr);
                break;

            case Opcodes.IASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.AASTORE:
                String arrStoreVal = stack.pop();
                String arrStoreIdx = stack.pop();
                String arrStoreArr = stack.pop();
                emit("local.get " + arrStoreArr);
                emit("i32.const 4");
                emit("i32.add");
                emit("local.get " + arrStoreIdx);
                emit("i32.const 4");
                emit("i32.mul");
                emit("i32.add");
                emit("local.get " + arrStoreVal);
                emit("i32.store offset=0");
                break;

            case Opcodes.NEW:
                TypeInsnNode newInsn = (TypeInsnNode) insn;
                String newRef = stack.push("i32");
                emit("i32.const 64"); // Allocate standard object struct
                emit("call $__heap_alloc");
                emit("local.set " + newRef);
                break;

            case Opcodes.NEWARRAY:
            case Opcodes.ANEWARRAY:
                String count = stack.peek();
                emit("local.get " + count);
                emit("i32.const 4");
                emit("i32.mul");
                emit("i32.const 4");
                emit("i32.add");
                emit("call $__heap_alloc");
                emit("local.tee " + count);
                emit("local.get " + count);
                emit("i32.load offset=0"); // store length
                break;

            case Opcodes.GETFIELD:
                FieldInsnNode getField = (FieldInsnNode) insn;
                String targetObj = stack.peek();
                int offset = fieldOffsets.getOrDefault(getField.owner + "." + getField.name, 4);
                emit("local.get " + targetObj);
                emit("i32.load offset=" + offset);
                emit("local.set " + targetObj);
                break;

            case Opcodes.PUTFIELD:
                FieldInsnNode putField = (FieldInsnNode) insn;
                String putVal = stack.pop();
                String putTarget = stack.pop();
                int pOffset = fieldOffsets.getOrDefault(putField.owner + "." + putField.name, 4);
                emit("local.get " + putTarget);
                emit("local.get " + putVal);
                emit("i32.store offset=" + pOffset);
                break;

            case Opcodes.GETSTATIC:
                FieldInsnNode getStatic = (FieldInsnNode) insn;
                String gName = TypeHelper.getGlobalFieldName(getStatic.owner, getStatic.name);
                String pushedStatic = stack.push(TypeHelper.getWasmType(getStatic.desc));
                emit("global.get " + gName);
                emit("local.set " + pushedStatic);
                break;

            case Opcodes.PUTSTATIC:
                FieldInsnNode putStatic = (FieldInsnNode) insn;
                String psName = TypeHelper.getGlobalFieldName(putStatic.owner, putStatic.name);
                String poppedStatic = stack.pop();
                emit("local.get " + poppedStatic);
                emit("global.set " + psName);
                break;

            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                visitMethodInsn((MethodInsnNode) insn);
                break;

            case Opcodes.IRETURN:
            case Opcodes.ARETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
                String retVal = stack.pop();
                emit("local.get " + retVal);
                emit("return");
                break;

            case Opcodes.RETURN:
                emit("return");
                break;

            default:
                throw new IllegalArgumentException("Unsupported bytecode opcode " + opcode);
        }
    }

    private void visitLdc(LdcInsnNode ldc) {
        Object cst = ldc.cst;
        if (cst instanceof Integer) {
            String slot = stack.push("i32");
            emit("i32.const " + cst);
            emit("local.set " + slot);
        } else if (cst instanceof Long) {
            String slot = stack.push("i64");
            emit("i64.const " + cst);
            emit("local.set " + slot);
        } else if (cst instanceof Float) {
            String slot = stack.push("f32");
            emit("f32.const " + cst);
            emit("local.set " + slot);
        } else if (cst instanceof Double) {
            String slot = stack.push("f64");
            emit("f64.const " + cst);
            emit("local.set " + slot);
        } else if (cst instanceof String) {
            String s = (String) cst;
            int ptr = stringPool.computeIfAbsent(s, k -> (stringPool.size() + 1) * 64);
            String slot = stack.push("i32");
            emit("i32.const " + ptr);
            emit("local.set " + slot);
        } else {
            throw new IllegalArgumentException("Unsupported LDC constant " + cst.getClass().getName());
        }
    }

    private void visitBinaryOp(String type, String wasmOp) {
        String rhs = stack.pop();
        String lhs = stack.peek();
        emit("local.get " + lhs);
        emit("local.get " + rhs);
        emit(wasmOp);
        emit("local.set " + lhs);
    }

    private void visitMethodInsn(MethodInsnNode minsn) {
        Type[] argTypes = Type.getArgumentTypes(minsn.desc);
        Type retType = Type.getReturnType(minsn.desc);
        boolean isStatic = minsn.getOpcode() == Opcodes.INVOKESTATIC;

        List<String> argVars = new ArrayList<>();
        for (int i = 0; i < argTypes.length; i++) {
            argVars.add(0, stack.pop());
        }

        String thisVar = null;
        if (!isStatic) {
            thisVar = stack.pop();
        }

        // java.lang.Object's empty constructor has no runtime action in this layout.
        if (minsn.owner.equals("java/lang/Object") && minsn.name.equals("<init>") && minsn.desc.equals("()V")) return;

        // Special handling for System.out.println / String methods / Math
        String funcName = TypeHelper.getMethodFunctionName(minsn.owner, minsn.name, minsn.desc);

        if (!isStatic) {
            emit("local.get " + thisVar);
        }
        for (String aVar : argVars) {
            emit("local.get " + aVar);
        }

        emit("call " + funcName);

        String retWasmType = TypeHelper.getWasmType(retType);
        if (retWasmType != null) {
            String resSlot = stack.push(retWasmType);
            emit("local.set " + resSlot);
        }
    }

    // Dummy class marker
    private static class IncompletInsn extends AbstractInsnNode {
        protected IncompletInsn() { super(-1); }
        @Override public int getType() { return -1; }
        @Override public void accept(org.objectweb.asm.MethodVisitor methodVisitor) {}
        @Override public AbstractInsnNode clone(Map<LabelNode, LabelNode> map) { return this; }
    }
}
