package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

/** Frame inference constrained by StackMapTable declarations, without loading host classes. */
final class BytecodeFrames implements Opcodes {
    private record Declared(FrameNode node, Frame<BasicValue> frame) {}

    private final ClassNode owner;
    private final MethodNode method;
    private final Map<String, ClassNode> linked;
    private final BytecodeVerifier verifier;
    private final AbstractInsnNode[] instructions;
    private final Map<AbstractInsnNode, Integer> indices = new IdentityHashMap<>();
    private final Map<Integer, Declared> declarations = new HashMap<>();
    private final Frame<BasicValue>[] frames;
    private final Deque<Integer> pending = new ArrayDeque<>();
    private final boolean[] queued;

    @SuppressWarnings("unchecked")
    private BytecodeFrames(ClassNode owner, MethodNode method, Map<String, ClassNode> linked) {
        this.owner = owner; this.method = method; this.linked = linked;
        verifier = new BytecodeVerifier(owner, linked);
        instructions = method.instructions.toArray();
        frames = (Frame<BasicValue>[]) new Frame<?>[instructions.length];
        queued = new boolean[instructions.length];
        for (int i = 0; i < instructions.length; i++) indices.put(instructions[i], i);
    }

    static Frame<BasicValue>[] analyze(ClassNode owner, MethodNode method, Map<String, ClassNode> linked)
            throws AnalyzerException {
        return new BytecodeFrames(owner, method, linked).analyze();
    }

    private Frame<BasicValue>[] analyze() throws AnalyzerException {
        decode();
        requireBranchFrames();
        // Declared frames are also verification roots for otherwise unreachable code.
        for (var entry : declarations.entrySet()) merge(entry.getKey(), entry.getValue().frame, entry.getValue().node);
        Frame<BasicValue> initial = emptyFrame();
        int slot = 0;
        if ((method.access & ACC_STATIC) == 0) slot = parameter(initial, slot, Type.getObjectType(owner.name));
        for (Type type : Type.getArgumentTypes(method.desc)) slot = parameter(initial, slot, type);
        merge(0, initial, instructions[0]);
        while (!pending.isEmpty()) {
            int index = pending.removeFirst(); queued[index] = false;
            AbstractInsnNode instruction = instructions[index];
            Frame<BasicValue> after = new Frame<>(frames[index]);
            try {
                if (instruction.getOpcode() >= 0) after.execute(instruction, verifier);
                int words = 0;
                for (int i = 0; i < after.getStackSize(); i++) words += after.getStack(i).getSize();
                if (words > method.maxStack) throw new AnalyzerException(instruction, "operand stack exceeds max_stack");
            } catch (AnalyzerException error) { throw error; }
            catch (RuntimeException error) {
                throw new AnalyzerException(instruction, "invalid typed frame: " + error.getMessage(), error);
            }
            if (instruction instanceof JumpInsnNode jump) {
                merge(target(jump.label), after, instruction);
                if (jump.getOpcode() != GOTO) fallthrough(index, after);
            } else if (instruction instanceof TableSwitchInsnNode table) {
                merge(target(table.dflt), after, instruction);
                for (LabelNode label : table.labels) merge(target(label), after, instruction);
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                merge(target(lookup.dflt), after, instruction);
                for (LabelNode label : lookup.labels) merge(target(label), after, instruction);
            } else if (!terminal(instruction.getOpcode())) fallthrough(index, after);
        }
        return frames;
    }

    private Frame<BasicValue> emptyFrame() {
        Frame<BasicValue> frame = new Frame<>(method.maxLocals, method.maxStack);
        frame.setReturn(verifier.newValue(Type.getReturnType(method.desc)));
        for (int i = 0; i < method.maxLocals; i++) frame.setLocal(i, BasicValue.UNINITIALIZED_VALUE);
        return frame;
    }

    private int parameter(Frame<BasicValue> frame, int slot, Type type) {
        if (slot + type.getSize() > method.maxLocals) fail(null, "parameters exceed max_locals");
        frame.setLocal(slot, verifier.newValue(type));
        return slot + type.getSize();
    }

    private void fallthrough(int index, Frame<BasicValue> frame) throws AnalyzerException {
        if (index + 1 >= instructions.length)
            throw new AnalyzerException(instructions[index], "execution can fall off the end of the method");
        merge(index + 1, frame, instructions[index]);
    }

    private void merge(int index, Frame<BasicValue> incoming, AbstractInsnNode source) throws AnalyzerException {
        Declared declared = declarations.get(index);
        Frame<BasicValue> value = incoming;
        if (declared != null) {
            Frame<BasicValue> expected = declared.frame;
            if (incoming.getStackSize() != expected.getStackSize()) fail(declared.node, "incoming stack height disagrees with declaration");
            for (int i = 0; i < expected.getLocals(); i++)
                if (!verifier.frameAssignable(incoming.getLocal(i), expected.getLocal(i)))
                    fail(declared.node, "incoming local " + i + " is incompatible with its declared type");
            for (int i = 0; i < expected.getStackSize(); i++)
                if (incoming.getStack(i).getSize() != expected.getStack(i).getSize()
                        || !verifier.frameAssignable(incoming.getStack(i), expected.getStack(i)))
                    fail(declared.node, "incoming stack value " + i + " is incompatible with its declared type");
            value = expected; // Subsequent verification must use the declared (possibly wider) types.
        }
        boolean changed;
        if (frames[index] == null) { frames[index] = new Frame<>(value); changed = true; }
        else {
            try { changed = frames[index].merge(value, verifier); }
            catch (AnalyzerException error) { throw new AnalyzerException(source, error.getMessage(), error); }
        }
        if (changed && !queued[index]) { pending.addLast(index); queued[index] = true; }
    }

    private List<Object> initialLocals() {
        List<Object> locals = new ArrayList<>();
        if ((method.access & ACC_STATIC) == 0) locals.add(method.name.equals("<init>") ? UNINITIALIZED_THIS : owner.name);
        for (Type type : Type.getArgumentTypes(method.desc)) locals.add(switch (type.getSort()) {
            case Type.FLOAT -> FLOAT;
            case Type.LONG -> LONG;
            case Type.DOUBLE -> DOUBLE;
            case Type.OBJECT -> type.getInternalName();
            case Type.ARRAY -> type.getDescriptor();
            default -> INTEGER;
        });
        return locals;
    }

    private void decode() {
        List<Object> locals = initialLocals();
        Map<Integer, Declared> byOffset = new HashMap<>();
        for (int i = 0; i < instructions.length; i++) {
            if (!(instructions[i] instanceof FrameNode node)) continue;
            List<Object> stack = node.stack == null ? List.of() : node.stack;
            List<Object> entries = node.local == null ? List.of() : node.local;
            switch (node.type) {
                case F_NEW, F_FULL -> locals = new ArrayList<>(entries);
                case F_APPEND -> {
                    if (entries.isEmpty() || entries.size() > 3 || !stack.isEmpty()) fail(node, "invalid append frame shape");
                    locals.addAll(entries);
                }
                case F_CHOP -> {
                    if (entries.isEmpty() || entries.size() > 3 || entries.size() > locals.size() || !stack.isEmpty())
                        fail(node, "invalid chop frame shape");
                    locals = new ArrayList<>(locals.subList(0, locals.size() - entries.size()));
                }
                case F_SAME -> { if (!entries.isEmpty() || !stack.isEmpty()) fail(node, "invalid same frame shape"); }
                case F_SAME1 -> { if (!entries.isEmpty() || stack.size() != 1) fail(node, "invalid same1 frame shape"); }
                default -> fail(node, "unknown frame encoding");
            }
            Frame<BasicValue> declared = emptyFrame();
            int slot = 0;
            for (Object token : locals) {
                BasicValue value = value(node, token);
                if (slot + value.getSize() > method.maxLocals) fail(node, "declared locals exceed max_locals");
                declared.setLocal(slot, value); slot += value.getSize();
            }
            int words = 0;
            for (Object token : stack) {
                BasicValue value = value(node, token); words += value.getSize();
                if (value.equals(BasicValue.UNINITIALIZED_VALUE)) fail(node, "TOP operand-stack entries are unsupported; recompile typed bytecode");
                if (words > method.maxStack) fail(node, "declared stack exceeds max_stack");
                declared.push(value);
            }
            int executable = nextExecutable(i);
            if (executable >= instructions.length) fail(node, "frame must precede a bytecode instruction");
            if (byOffset.putIfAbsent(executable, new Declared(node, declared)) != null) fail(node, "duplicate frames at one bytecode offset");
        }
        // Labels/line numbers at the same offset must see the declaration before branch merges.
        for (var entry : byOffset.entrySet()) {
            int first = entry.getKey();
            while (first > 0 && instructions[first - 1].getOpcode() < 0) first--;
            for (int i = first; i <= entry.getKey(); i++) declarations.put(i, entry.getValue());
        }
    }

    private BasicValue value(FrameNode node, Object token) {
        if (token instanceof Integer tag) return switch (tag) {
            case 0 -> BasicValue.UNINITIALIZED_VALUE;
            case 1 -> BasicValue.INT_VALUE;
            case 2 -> BasicValue.FLOAT_VALUE;
            case 3 -> BasicValue.DOUBLE_VALUE;
            case 4 -> BasicValue.LONG_VALUE;
            case 5 -> new BasicValue(Type.getObjectType("null"));
            case 6 -> throw unsupportedUninitialized(node);
            default -> throw failure(node, "unknown verification type tag " + tag);
        };
        if (token instanceof LabelNode) throw unsupportedUninitialized(node);
        if (token instanceof String name) {
            Type type;
            if (name.startsWith("[")) {
                if (!BytecodeDeclarations.validDescriptor(name, false)) throw failure(node, "invalid array verification type");
                type = Type.getType(name);
            } else {
                if (!BytecodeDeclarations.validClassName(name)) throw failure(node, "invalid class verification type");
                type = Type.getObjectType(name);
            }
            Type component = type.getSort() == Type.ARRAY ? type.getElementType() : type;
            if (component.getSort() == Type.OBJECT && !component.getInternalName().equals("java/lang/Object")
                    && !linked.containsKey(component.getInternalName()))
                throw failure(node, "link stack-map class/interface " + component.getInternalName());
            return verifier.newValue(type);
        }
        throw failure(node, "invalid verification type token");
    }

    private IllegalArgumentException unsupportedUninitialized(FrameNode node) {
        return failure(node, "uninitialized stack-map types require the separate constructor/NEW-alias verifier, which is not implemented yet");
    }

    private void requireBranchFrames() {
        boolean required = (owner.version & 0xffff) >= V1_7;
        for (int i = 0; i < instructions.length; i++) {
            AbstractInsnNode instruction = instructions[i];
            if (instruction instanceof JumpInsnNode jump) requireTarget(jump.label, required, instruction);
            else if (instruction instanceof TableSwitchInsnNode table) {
                requireTarget(table.dflt, required, instruction);
                for (LabelNode label : table.labels) requireTarget(label, required, instruction);
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                requireTarget(lookup.dflt, required, instruction);
                for (LabelNode label : lookup.labels) requireTarget(label, required, instruction);
            }
            if (required && (instruction.getOpcode() == GOTO || terminal(instruction.getOpcode())
                    || instruction instanceof TableSwitchInsnNode || instruction instanceof LookupSwitchInsnNode)) {
                int next = nextExecutable(i + 1);
                if (next < instructions.length && !declarations.containsKey(next)) fail(instructions[next], "missing frame after non-fallthrough instruction");
            }
        }
    }

    private void requireTarget(LabelNode label, boolean required, AbstractInsnNode source) {
        int executable = nextExecutable(target(label));
        if (executable >= instructions.length) fail(source, "branch target must identify a bytecode instruction");
        if (required && !declarations.containsKey(executable)) fail(source, "missing frame at branch target");
    }

    private int target(LabelNode label) {
        Integer index = indices.get(label);
        if (index == null) throw failure(label, "branch label is not in this method");
        return index;
    }

    private int nextExecutable(int index) {
        while (index < instructions.length && instructions[index].getOpcode() < 0) index++;
        return index;
    }

    private static boolean terminal(int opcode) { return opcode >= IRETURN && opcode <= RETURN || opcode == ATHROW; }

    private void fail(AbstractInsnNode node, String reason) { throw failure(node, reason); }
    private IllegalArgumentException failure(AbstractInsnNode node, String reason) {
        return BytecodeCompatibility.failure(owner, method, node, "stack map", reason);
    }
}
