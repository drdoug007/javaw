package com.example.javaw.bytecode;

import com.example.javaw.FloatingPointSupport;
import com.example.javaw.bytecode.cfg.BasicBlock;
import com.example.javaw.bytecode.cfg.ControlFlowGraph;
import com.example.javaw.bytecode.lowering.BytecodeInstructionVisitor;
import com.example.javaw.bytecode.lowering.StackToLocalMapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ClassBytecodeCompiler {
    private final ClassNode classNode;
    private BytecodeDispatch dispatch;
    private Map<String, ClassNode> linked;
    private final Map<String, Integer> stringPool = new LinkedHashMap<>();
    private final Map<String, Integer> fieldOffsets = new LinkedHashMap<>();
    private final Map<String, Integer> objectSizes = new LinkedHashMap<>();
    private final List<String> globalVariables = new ArrayList<>();

    public ClassBytecodeCompiler(ClassNode classNode) {
        BytecodeInput.normalize(classNode);
        this.classNode = classNode;
    }

    public static ClassBytecodeCompiler fromBytes(byte[] classBytes) {
        return new ClassBytecodeCompiler(BytecodeInput.read(classBytes));
    }

    public String compileToWat() {
        return compileToWat(java.util.Map.of(classNode.name, classNode));
    }

    String compileToWat(Map<String, ClassNode> linked) {
        return compileToWat(linked, true);
    }

    static boolean usesFloatingRemainder(ClassNode type) {
        for (MethodNode method : type.methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction.getOpcode() == Opcodes.FREM || instruction.getOpcode() == Opcodes.DREM) return true;
        return false;
    }

    String compileToWat(Map<String, ClassNode> linked, boolean includeNumericRuntime) {
        BytecodeCompatibility.validate(classNode, linked);
        this.linked = linked;
        dispatch = new BytecodeDispatch(linked, classNode.name);
        BytecodeObjectLayouts layouts = new BytecodeObjectLayouts(linked);
        fieldOffsets.putAll(layouts.offsets());
        objectSizes.putAll(layouts.sizes());
        StringBuilder wat = new StringBuilder();
        wat.append("(module\n");
        wat.append("    (import \"env\" \"memory\" (memory 2 20))\n");
        wat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        Set<BytecodeExceptions.Implicit> implicit = BytecodeExceptions.implicitRuntime(classNode, dispatch);
        if (includeNumericRuntime && (BytecodeExceptions.usesRuntime(classNode) || !implicit.isEmpty())) wat.append(BytecodeExceptions.TAG_WAT);
        if (includeNumericRuntime)
            for (var kind : implicit) wat.append(dispatch.implicitExceptionRuntimeWat(kind));

        if (includeNumericRuntime && usesFloatingRemainder(classNode))
            wat.append(FloatingPointSupport.remainderFunctionsWat());

        if (includeNumericRuntime && BytecodeArrays.usesRuntime(classNode))
            wat.append(BytecodeArrays.runtimeWat(dispatch)).append(dispatch.referenceArrayRuntimeWat()).append(dispatch.multiArrayRuntimeWat());

        wat.append(BytecodeInitialization.runtimeWat(classNode, linked));

        // Collect fields
        for (FieldNode fn : classNode.fields) {
            boolean isStatic = (fn.access & Opcodes.ACC_STATIC) != 0;
            if (isStatic) {
                String gName = TypeHelper.getGlobalFieldName(classNode.name, fn.name, fn.desc);
                String wasmType = TypeHelper.getWasmType(fn.desc);
                globalVariables.add(gName);
                wat.append("    (global ").append(gName).append(" (mut ").append(wasmType != null ? wasmType : "i32")
                   .append(") (").append(wasmType != null ? wasmType : "i32").append(".const ").append(BytecodeInitialization.constantValue(fn)).append("))\n");
            }
        }

        // Collect methods
        for (MethodNode mn : classNode.methods) {
            if (!mn.name.equals("<clinit>") && (mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            compileMethod(mn, wat);
        }

        wat.append(")\n");
        return wat.toString();
    }

    private void compileMethod(MethodNode mn, StringBuilder wat) {
        boolean isStatic = mn.name.equals("<clinit>") || (mn.access & Opcodes.ACC_STATIC) != 0;
        String funcName = TypeHelper.getMethodFunctionName(classNode.name, mn.name, mn.desc);
        List<String> paramTypes = TypeHelper.getParamWasmTypes(mn.desc);
        String retType = TypeHelper.getReturnWasmType(mn.desc);

        wat.append("    (func ").append(funcName);
        if ((mn.access & Opcodes.ACC_PUBLIC) != 0 && !mn.name.equals("<clinit>")) {
            String exportName = classNode.name.replace('/', '.') + "." + mn.name;
            if (classNode.methods.stream().filter(method -> method.name.equals(mn.name)
                    && (method.access & Opcodes.ACC_PUBLIC) != 0).count() > 1) exportName += mn.desc;
            try { wat.append(" (export \"").append(TypeHelper.watString(exportName)).append("\")"); }
            catch (IllegalArgumentException error) {
                throw BytecodeCompatibility.failure(classNode, mn, null, "public export name", error.getMessage());
            }
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

        org.objectweb.asm.tree.analysis.Frame<org.objectweb.asm.tree.analysis.BasicValue>[] frames;
        try {
            frames = BytecodeFrames.analyze(classNode, mn, linked);
        } catch (org.objectweb.asm.tree.analysis.AnalyzerException error) {
            throw BytecodeCompatibility.failure(classNode, mn, error.node, "invalid operand stack", "recompile valid typed Java bytecode and link required hierarchy metadata: " + error.getMessage());
        }

        ControlFlowGraph cfg = ControlFlowGraph.build(mn);

        // Locals mapping
        StackToLocalMapper mapper = new StackToLocalMapper(mn.maxLocals, mn.maxStack);

        // Declare locals
        for (int i = 0; i < mapper.getMaxLocals(); i++) {
            wat.append("        (local $l_").append(i).append(" i32)\n");
            for (String type : List.of("i64", "f32", "f64"))
                wat.append("        (local $l_").append(i).append("_").append(type).append(" ").append(type).append(")\n");
        }
        for (int i = 0; i < mapper.getMaxStack(); i++) {
            wat.append("        (local $stk_").append(i).append(" i32)\n");
            for (String type : List.of("i64", "f32", "f64"))
                wat.append("        (local $stk_").append(i).append("_").append(type).append(" ").append(type).append(")\n");
        }
        for (int i = 0; i < 4; i++) {
            wat.append("        (local $__shuffle_i32_").append(i).append(" i32)\n");
            for (String type : List.of("i64", "f32", "f64"))
                wat.append("        (local $__shuffle_").append(type).append("_").append(i).append(" ").append(type).append(")\n");
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
            wat.append("        local.set ").append(mapper.getLocalVar(lIdx, paramTypes.get(i))).append("\n");
            lIdx += org.objectweb.asm.Type.getArgumentTypes(mn.desc)[i].getSize();
        }

        if (isStatic && !mn.name.equals("<clinit>"))
            wat.append("        ").append(dispatch.initializationCall(classNode.name)).append("\n");

        boolean trace = BytecodeTraces.instrument(classNode, mn, linked);
        if (trace) wat.append("        i32.const ").append(BytecodeTraces.id(classNode, mn, linked)).append("\n        call $__trace_enter\n        try\n");
        Map<AbstractInsnNode, Integer> sourceLines = new IdentityHashMap<>();
        int sourceLine = -1;
        for (AbstractInsnNode instruction : mn.instructions) {
            if (instruction instanceof LineNumberNode line) sourceLine = line.line;
            sourceLines.put(instruction, sourceLine);
        }

        // Explicit state dispatch preserves every branch target, including joins and back edges.
        wat.append("        (loop $loop_dispatch\n");
        for (BasicBlock block : cfg.getBlocks()) {
            AbstractInsnNode first = block.getInstructions().get(0);
            var frame = frames[mn.instructions.indexOf(first)];
            if (frame == null) continue; // Unreachable JVM instructions.
            List<String> entryTypes = new ArrayList<>();
            for (int i = 0; i < frame.getStackSize(); i++)
                // TOP words are unusable placeholders below live values; verification forbids reading them.
                entryTypes.add(frame.getStack(i).getType() == null ? "i32" : TypeHelper.getWasmType(frame.getStack(i).getType()));
            mapper.setStackTypes(entryTypes);
            wat.append("        local.get $__state\n        i32.const ").append(block.getId()).append("\n        i32.eq\n        if\n");
            BytecodeInstructionVisitor visitor = new BytecodeInstructionVisitor(mapper, stringPool, fieldOffsets, objectSizes, dispatch);
            visitor.setTraceEnabled(trace);
            AbstractInsnNode terminal = null;
            for (AbstractInsnNode instruction : block.getInstructions()) {
                if (instruction.getOpcode() >= 0) {
                    terminal = instruction;
                    if (trace) visitor.sourceLine(sourceLines.get(instruction));
                }
                if (!(instruction instanceof JumpInsnNode)
                        && !(instruction instanceof TableSwitchInsnNode)
                        && !(instruction instanceof LookupSwitchInsnNode)) visitor.visitInsn(instruction);
            }
            List<TryCatchBlockNode> activeHandlers = new ArrayList<>();
            for (TryCatchBlockNode handler : mn.tryCatchBlocks) {
                int start = mn.instructions.indexOf(handler.start), end = mn.instructions.indexOf(handler.end);
                for (AbstractInsnNode instruction : block.getInstructions()) {
                    int index = mn.instructions.indexOf(instruction);
                    if (instruction.getOpcode() >= 0 && index >= start && index < end) {
                        activeHandlers.add(handler);
                        break;
                    }
                }
            }
            if (!activeHandlers.isEmpty()) wat.append("        try\n");
            wat.append(visitor.getOutput());
            if (!activeHandlers.isEmpty()) {
                wat.append("        catch $__java_exception\n        local.set $stk_0\n");
                for (TryCatchBlockNode handler : activeHandlers) {
                    if (handler.type != null) {
                        wat.append("        i32.const 0\n");
                        for (int id : dispatch.typeTestIds(handler.type))
                            wat.append("        local.get $stk_0\n        i32.load\n        i32.const ").append(id).append("\n        i32.eq\n        i32.or\n");
                        wat.append("        if\n");
                    }
                    setState(wat, cfg.getBlockForLabel(handler.handler));
                    wat.append("        br $loop_dispatch\n");
                    if (handler.type == null) break;
                    wat.append("        end\n");
                }
                wat.append("        rethrow 0\n        end\n");
            }
            if (terminal instanceof JumpInsnNode jump) {
                if (jump.getOpcode() == Opcodes.GOTO) {
                    setState(wat, block.getBranchTarget());
                } else {
                    emitJumpCondition(jump.getOpcode(), wat, mapper);
                    wat.append("        if\n");
                    setState(wat, block.getBranchTarget());
                    wat.append("        else\n");
                    setState(wat, block.getFallthrough());
                    wat.append("        end\n");
                }
                wat.append("        br $loop_dispatch\n");
            } else if (terminal instanceof TableSwitchInsnNode || terminal instanceof LookupSwitchInsnNode) {
                // Consume only the selector; values below it survive into every successor.
                String selector = mapper.pop();
                setState(wat, block.getDefaultTarget());
                for (var entry : block.getSwitchTargets().entrySet()) {
                    wat.append("        local.get ").append(selector)
                            .append("\n        i32.const ").append(entry.getKey())
                            .append("\n        i32.eq\n        if\n");
                    setState(wat, entry.getValue());
                    wat.append("        end\n");
                }
                wat.append("        br $loop_dispatch\n");
            } else if (terminal == null || terminal.getOpcode() != Opcodes.RETURN
                    && terminal.getOpcode() != Opcodes.IRETURN && terminal.getOpcode() != Opcodes.ARETURN
                    && terminal.getOpcode() != Opcodes.LRETURN && terminal.getOpcode() != Opcodes.FRETURN
                    && terminal.getOpcode() != Opcodes.DRETURN) {
                if (block.getFallthrough() != null) {
                    setState(wat, block.getFallthrough());
                    wat.append("        br $loop_dispatch\n");
                } else wat.append("        unreachable\n");
            }
            wat.append("        end\n");
        }
        wat.append("        unreachable\n        )\n        unreachable\n");
        if (trace) wat.append("        catch_all\n        call $__trace_exit\n        rethrow 0\n        end\n        unreachable\n");
        wat.append("    )\n");
    }

    private void setState(StringBuilder wat, BasicBlock target) {
        if (target == null) throw new IllegalArgumentException("Missing bytecode branch target");
        wat.append("        i32.const ").append(target.getId()).append("\n        local.set $__state\n");
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
                throw new IllegalArgumentException("Unsupported branch opcode " + opcode);
        }
    }
}
