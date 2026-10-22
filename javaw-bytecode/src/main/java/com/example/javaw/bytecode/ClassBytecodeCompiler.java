package com.example.javaw.bytecode;

import com.example.javaw.bytecode.cfg.BasicBlock;
import com.example.javaw.bytecode.cfg.ControlFlowGraph;
import com.example.javaw.bytecode.cfg.Relooper;
import com.example.javaw.bytecode.lowering.BytecodeInstructionVisitor;
import com.example.javaw.bytecode.lowering.StackToLocalMapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ClassBytecodeCompiler {
    private final ClassNode classNode;
    private final Map<String, Integer> stringPool = new LinkedHashMap<>();
    private final Map<String, Integer> fieldOffsets = new LinkedHashMap<>();
    private final List<String> globalVariables = new ArrayList<>();

    public ClassBytecodeCompiler(ClassNode classNode) {
        this.classNode = classNode;
    }

    public static ClassBytecodeCompiler fromBytes(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        return new ClassBytecodeCompiler(cn);
    }

    public String compileToWat() {
        StringBuilder wat = new StringBuilder();
        wat.append("(module\n");
        wat.append("    (import \"env\" \"memory\" (memory 2 20))\n");
        wat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        // Collect fields
        int instanceOffset = 4; // 0 is vtable
        for (FieldNode fn : classNode.fields) {
            boolean isStatic = (fn.access & Opcodes.ACC_STATIC) != 0;
            if (isStatic) {
                String gName = TypeHelper.getGlobalFieldName(classNode.name, fn.name);
                String wasmType = TypeHelper.getWasmType(fn.desc);
                globalVariables.add(gName);
                wat.append("    (global ").append(gName).append(" (mut ").append(wasmType != null ? wasmType : "i32")
                   .append(") (").append(wasmType != null ? wasmType : "i32").append(".const 0))\n");
            } else {
                fieldOffsets.put(classNode.name + "." + fn.name, instanceOffset);
                String wasmType = TypeHelper.getWasmType(fn.desc);
                int size = ("i64".equals(wasmType) || "f64".equals(wasmType)) ? 8 : 4;
                instanceOffset += size;
            }
        }

        // Collect methods
        for (MethodNode mn : classNode.methods) {
            if ((mn.access & Opcodes.ACC_ABSTRACT) != 0 || (mn.access & Opcodes.ACC_NATIVE) != 0) {
                continue;
            }
            compileMethod(mn, wat);
        }

        wat.append(")\n");
        return wat.toString();
    }

    private void compileMethod(MethodNode mn, StringBuilder wat) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        String funcName = TypeHelper.getMethodFunctionName(classNode.name, mn.name, mn.desc);
        List<String> paramTypes = TypeHelper.getParamWasmTypes(mn.desc);
        String retType = TypeHelper.getReturnWasmType(mn.desc);

        wat.append("    (func ").append(funcName);
        if ((mn.access & Opcodes.ACC_PUBLIC) != 0) {
            String exportName = classNode.name.replace('/', '.') + "." + mn.name;
            wat.append(" (export \"").append(exportName).append("\")");
        }

        int paramIndex = 0;
        if (!isStatic) {
            wat.append(" (param $this i32)");
            paramIndex++;
        }
        for (int i = 0; i < paramTypes.size(); i++) {
            wat.append(" (param $p").append(i).append(" ").append(paramTypes.get(i)).append(")");
            paramIndex++;
        }

        if (retType != null) {
            wat.append(" (result ").append(retType).append(")");
        }
        wat.append("\n");

        // Build CFG and Relooper
        ControlFlowGraph cfg = ControlFlowGraph.build(mn);
        Relooper relooper = new Relooper();
        Relooper.Shape rootShape = relooper.reloop(cfg);

        // Locals mapping
        StackToLocalMapper mapper = new StackToLocalMapper(mn.maxLocals, mn.maxStack);

        // Declare locals
        for (int i = 0; i < mapper.getMaxLocals(); i++) {
            wat.append("        (local $l_").append(i).append(" i32)\n");
        }
        for (int i = 0; i < mapper.getMaxStack(); i++) {
            wat.append("        (local $stk_").append(i).append(" i32)\n");
        }
        wat.append("        (local $__state i32)\n");

        // Copy parameters to initial local variables
        int lIdx = 0;
        if (!isStatic) {
            wat.append("        local.get $this\n");
            wat.append("        local.set $l_").append(lIdx++).append("\n");
        }
        for (int i = 0; i < paramTypes.size(); i++) {
            wat.append("        local.get $p").append(i).append("\n");
            wat.append("        local.set $l_").append(lIdx++).append("\n");
        }

        // Render shapes
        renderShape(rootShape, wat, mapper, new HashSet<>());

        // Default fallback return for non-void
        if (retType != null) {
            wat.append("        ").append(retType).append(".const 0\n");
        }
        wat.append("    )\n");
    }

    private void renderShape(Relooper.Shape shape, StringBuilder wat, StackToLocalMapper mapper, Set<Integer> activeLoops) {
        if (shape == null) return;

        if (shape instanceof Relooper.LoopShape) {
            Relooper.LoopShape loop = (Relooper.LoopShape) shape;
            wat.append("        (block $block_exit_").append(loop.getId()).append("\n");
            wat.append("            (loop $loop_").append(loop.getId()).append("\n");
            activeLoops.add(loop.getId());

            renderShape(loop.getInner(), wat, mapper, activeLoops);

            activeLoops.remove(loop.getId());
            wat.append("            )\n");
            wat.append("        )\n");

            renderShape(loop.getNext(), wat, mapper, activeLoops);

        } else if (shape instanceof Relooper.SimpleShape) {
            Relooper.SimpleShape simple = (Relooper.SimpleShape) shape;
            BasicBlock block = simple.getBlock();

            // Lower instructions
            BytecodeInstructionVisitor visitor = new BytecodeInstructionVisitor(mapper, stringPool, fieldOffsets);
            for (AbstractInsnNode insn : block.getInstructions()) {
                if (!(insn instanceof JumpInsnNode) && !(insn instanceof TableSwitchInsnNode) && !(insn instanceof LookupSwitchInsnNode)) {
                    visitor.visitInsn(insn);
                }
            }
            wat.append(visitor.getOutput());

            // Handle branching at block exit
            handleBranching(block, wat, mapper, activeLoops);

            renderShape(simple.getNext(), wat, mapper, activeLoops);

        } else if (shape instanceof Relooper.MultipleShape) {
            Relooper.MultipleShape multi = (Relooper.MultipleShape) shape;
            wat.append("        (block $multi_").append(multi.getId()).append("\n");
            for (Map.Entry<BasicBlock, Relooper.Shape> entry : multi.getHandlers().entrySet()) {
                wat.append("            (block $handler_").append(entry.getKey().getId()).append("\n");
                renderShape(entry.getValue(), wat, mapper, activeLoops);
                wat.append("                br $multi_").append(multi.getId()).append("\n");
                wat.append("            )\n");
            }
            wat.append("        )\n");

            renderShape(multi.getNext(), wat, mapper, activeLoops);
        }
    }

    private void handleBranching(BasicBlock block, StringBuilder wat, StackToLocalMapper mapper, Set<Integer> activeLoops) {
        AbstractInsnNode lastInsn = null;
        for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
            AbstractInsnNode in = block.getInstructions().get(i);
            if (!(in instanceof LineNumberNode) && !(in instanceof FrameNode)) {
                lastInsn = in;
                break;
            }
        }

        if (lastInsn instanceof JumpInsnNode) {
            JumpInsnNode jump = (JumpInsnNode) lastInsn;
            int opcode = jump.getOpcode();

            if (opcode == Opcodes.GOTO) {
                // If branchTarget is a loop header
                if (block.getBranchTarget() != null && block.getBranchTarget().isLoopHeader()) {
                    for (int loopId : activeLoops) {
                        wat.append("        br $loop_").append(loopId).append("\n");
                    }
                }
            } else {
                // Conditional jump
                emitJumpCondition(opcode, wat, mapper);
                if (block.getBranchTarget() != null && block.getBranchTarget().isLoopHeader()) {
                    for (int loopId : activeLoops) {
                        wat.append("        br_if $loop_").append(loopId).append("\n");
                    }
                }
            }
        }
    }

    private void emitJumpCondition(int opcode, StringBuilder wat, StackToLocalMapper mapper) {
        switch (opcode) {
            case Opcodes.IFEQ:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.eqz\n");
                break;
            case Opcodes.IFNE:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.ne\n");
                break;
            case Opcodes.IFLT:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.lt_s\n");
                break;
            case Opcodes.IFGE:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.ge_s\n");
                break;
            case Opcodes.IFGT:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.gt_s\n");
                break;
            case Opcodes.IFLE:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.le_s\n");
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ACMPEQ:
                String rhs = mapper.pop();
                String lhs = mapper.pop();
                wat.append("        local.get ").append(lhs).append("\n");
                wat.append("        local.get ").append(rhs).append("\n");
                wat.append("        i32.eq\n");
                break;
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ACMPNE:
                String rhsNe = mapper.pop();
                String lhsNe = mapper.pop();
                wat.append("        local.get ").append(lhsNe).append("\n");
                wat.append("        local.get ").append(rhsNe).append("\n");
                wat.append("        i32.ne\n");
                break;
            case Opcodes.IF_ICMPLT:
                String rhsLt = mapper.pop();
                String lhsLt = mapper.pop();
                wat.append("        local.get ").append(lhsLt).append("\n");
                wat.append("        local.get ").append(rhsLt).append("\n");
                wat.append("        i32.lt_s\n");
                break;
            case Opcodes.IF_ICMPGE:
                String rhsGe = mapper.pop();
                String lhsGe = mapper.pop();
                wat.append("        local.get ").append(lhsGe).append("\n");
                wat.append("        local.get ").append(rhsGe).append("\n");
                wat.append("        i32.ge_s\n");
                break;
            case Opcodes.IF_ICMPGT:
                String rhsGt = mapper.pop();
                String lhsGt = mapper.pop();
                wat.append("        local.get ").append(lhsGt).append("\n");
                wat.append("        local.get ").append(rhsGt).append("\n");
                wat.append("        i32.gt_s\n");
                break;
            case Opcodes.IF_ICMPLE:
                String rhsLe = mapper.pop();
                String lhsLe = mapper.pop();
                wat.append("        local.get ").append(lhsLe).append("\n");
                wat.append("        local.get ").append(rhsLe).append("\n");
                wat.append("        i32.le_s\n");
                break;
            case Opcodes.IFNULL:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.eqz\n");
                break;
            case Opcodes.IFNONNULL:
                wat.append("        local.get ").append(mapper.pop()).append("\n");
                wat.append("        i32.const 0\n");
                wat.append("        i32.ne\n");
                break;
            default:
                break;
        }
    }
}
