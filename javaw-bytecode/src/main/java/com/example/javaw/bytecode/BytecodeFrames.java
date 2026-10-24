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
    /** The constructor flag survives overwriting or discarding every alias of local zero. */
    private static final class VerificationFrame extends Frame<BasicValue> {
        boolean thisUninitialized;
        VerificationFrame(int locals, int stack) { super(locals, stack); }
        VerificationFrame(Frame<BasicValue> source) {
            super(source);
            thisUninitialized = ((VerificationFrame) source).thisUninitialized;
        }
    }

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
        VerificationFrame initial = emptyFrame();
        int slot = 0;
        if ((method.access & ACC_STATIC) == 0) {
            slot = parameter(initial, slot, Type.getObjectType(owner.name));
            if (method.name.equals("<init>")) {
                initial.setLocal(0, verifier.uninitializedThis);
                initial.thisUninitialized = true;
            }
        }
        for (Type type : Type.getArgumentTypes(method.desc)) slot = parameter(initial, slot, type);
        merge(0, initial, instructions[0]);
        while (!pending.isEmpty()) {
            int index = pending.removeFirst(); queued[index] = false;
            AbstractInsnNode instruction = instructions[index];
            VerificationFrame after = new VerificationFrame(frames[index]);
            try {
                if (instruction.getOpcode() == RETURN && after.thisUninitialized)
                    throw new AnalyzerException(instruction, "constructor returns before this/super initialization");
                UninitializedValue receiver = null;
                if (instruction instanceof MethodInsnNode call && call.name.equals("<init>")) {
                    int position = after.getStackSize() - Type.getArgumentTypes(call.desc).length - 1;
                    if (position >= 0 && after.getStack(position) instanceof UninitializedValue value) receiver = value;
                }
                if (instruction instanceof TypeInsnNode allocation && instruction.getOpcode() == NEW) {
                    UninitializedValue identity = verifier.allocation(allocation);
                    for (int i = 0; i < after.getStackSize(); i++)
                        if (after.getStack(i) == identity)
                            throw new AnalyzerException(instruction, "repeated NEW cannot retain its previous uninitialized value on the stack");
                    for (int i = 0; i < after.getLocals(); i++)
                        if (after.getLocal(i) == identity) after.setLocal(i, BasicValue.UNINITIALIZED_VALUE);
                }
                checkStackManipulation(after, instruction);
                if (instruction.getOpcode() >= 0) after.execute(instruction, verifier);
                if (receiver != null) {
                    BasicValue initialized = verifier.newValue(receiver.getType());
                    for (int i = 0; i < after.getLocals(); i++)
                        if (after.getLocal(i) == receiver) after.setLocal(i, initialized);
                    for (int i = 0; i < after.getStackSize(); i++)
                        if (after.getStack(i) == receiver) after.setStack(i, initialized);
                    if (receiver == verifier.uninitializedThis) after.thisUninitialized = false;
                }
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

    private VerificationFrame emptyFrame() {
        VerificationFrame frame = new VerificationFrame(method.maxLocals, method.maxStack);
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
            if (((VerificationFrame) incoming).thisUninitialized && !((VerificationFrame) expected).thisUninitialized)
                fail(declared.node, "incoming constructor initialization flag disagrees with declaration");

            for (int i = 0; i < expected.getLocals(); i++)
                if (!verifier.frameAssignable(incoming.getLocal(i), expected.getLocal(i)))
                    fail(declared.node, "incoming local " + i + " is incompatible with its declared type");
            List<BasicValue> actualWords = stackWords(incoming), expectedWords = stackWords(expected);
            if (actualWords.size() != expectedWords.size()) fail(declared.node, "incoming stack height disagrees with declaration");
            for (int i = 0; i < expectedWords.size(); i++) {
                BasicValue actual = actualWords.get(i), target = expectedWords.get(i);
                // TOP erases one JVM word, including either half of a category-two value.
                if (target == BasicValue.UNINITIALIZED_VALUE) continue;
                if (target == null ? actual != null : actual == null || !verifier.frameAssignable(actual, target))
                    fail(declared.node, "incoming stack word " + i + " is incompatible with its declared type");
            }
            value = expected; // Subsequent verification must use the declared (possibly wider) types.
        }
        boolean changed;
        if (frames[index] == null) { frames[index] = new VerificationFrame(value); changed = true; }
        else {
            try {
                VerificationFrame current = (VerificationFrame) frames[index];
                boolean pendingInitialization = ((VerificationFrame) value).thisUninitialized;
                changed = mergeInferred(current, value, source);
                if (pendingInitialization && !current.thisUninitialized) {
                    current.thisUninitialized = true;
                    changed = true;
                }
            }
            catch (AnalyzerException error) { throw new AnalyzerException(source, error.getMessage(), error); }
        }
        if (changed && !queued[index]) { pending.addLast(index); queued[index] = true; }
    }

    /** Expand category-two values to JVM words; null denotes their otherwise unusable high word. */
    private List<BasicValue> stackWords(Frame<BasicValue> frame) {
        List<BasicValue> words = new ArrayList<>();
        for (int i = 0; i < frame.getStackSize(); i++) {
            BasicValue value = frame.getStack(i);
            words.add(value);
            if (value.getSize() == 2) words.add(null);
        }
        return words;
    }

    private boolean mergeInferred(VerificationFrame current, Frame<BasicValue> incoming, AbstractInsnNode source)
            throws AnalyzerException {
        if (current.getStackSize() != incoming.getStackSize())
            throw new AnalyzerException(source, "incompatible operand stack shapes");
        for (int i = 0; i < current.getStackSize(); i++) {
            BasicValue left = current.getStack(i), right = incoming.getStack(i);
            if (left.getSize() != right.getSize()
                    || verifier.merge(left, right) == BasicValue.UNINITIALIZED_VALUE && !left.equals(right))
                throw new AnalyzerException(source, "incompatible operand stack types at join");
        }
        return current.merge(incoming, verifier);
    }

    private void checkStackManipulation(Frame<BasicValue> frame, AbstractInsnNode instruction) throws AnalyzerException {
        int words = switch (instruction.getOpcode()) {
            case POP, DUP -> 1;
            case POP2, DUP_X1, DUP2, SWAP -> 2;
            case DUP_X2, DUP2_X1 -> 3;
            case DUP2_X2 -> 4;
            default -> 0;
        };
        for (int i = frame.getStackSize() - 1; i >= 0 && words > 0; i--) {
            BasicValue value = frame.getStack(i);
            if (value == BasicValue.UNINITIALIZED_VALUE)
                throw new AnalyzerException(instruction, "cannot pop or shuffle an unusable TOP stack word");
            words -= value.getSize();
        }
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
            VerificationFrame declared = emptyFrame();
            int slot = 0;
            for (Object token : locals) {
                BasicValue value = value(node, token);
                if (value == verifier.uninitializedThis) declared.thisUninitialized = true;
                if (slot + value.getSize() > method.maxLocals) fail(node, "declared locals exceed max_locals");
                declared.setLocal(slot, value); slot += value.getSize();
            }
            int words = 0;
            for (Object token : stack) {
                BasicValue value = value(node, token); words += value.getSize();
                // StackMapTable derives flagThisUninit from locals only, not stack aliases.
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
            case 6 -> {
                if (!method.name.equals("<init>")) throw failure(node, "uninitialized-this is only valid in constructors");
                yield verifier.uninitializedThis;
            }
            default -> throw failure(node, "unknown verification type tag " + tag);
        };
        if (token instanceof LabelNode label) {
            int instruction = nextExecutable(target(label));
            if (instruction >= instructions.length || !(instructions[instruction] instanceof TypeInsnNode allocation)
                    || allocation.getOpcode() != NEW)
                throw failure(node, "uninitialized frame label must identify NEW in this method");
            return verifier.allocation(allocation);
        }
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
