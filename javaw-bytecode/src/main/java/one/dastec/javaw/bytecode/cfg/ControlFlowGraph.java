package one.dastec.javaw.bytecode.cfg;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ControlFlowGraph {
    private final MethodNode methodNode;
    private BasicBlock entryBlock;
    private final List<BasicBlock> blocks = new ArrayList<>();
    private final Map<LabelNode, BasicBlock> labelToBlock = new HashMap<>();

    public ControlFlowGraph(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public static ControlFlowGraph build(MethodNode methodNode) {
        ControlFlowGraph cfg = new ControlFlowGraph(methodNode);
        cfg.buildGraph();
        return cfg;
    }

    private void buildGraph() {
        InsnList insns = methodNode.instructions;
        if (insns.size() == 0) {
            entryBlock = new BasicBlock(0);
            blocks.add(entryBlock);
            return;
        }

        // Pass 1: Mark leaders (instructions that begin a basic block)
        Set<AbstractInsnNode> leaders = new HashSet<>();
        leaders.add(insns.getFirst());

        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof JumpInsnNode) {
                JumpInsnNode jump = (JumpInsnNode) insn;
                leaders.add(jump.label);
                if (jump.getNext() != null) {
                    leaders.add(jump.getNext());
                }
            } else if (insn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode ts = (TableSwitchInsnNode) insn;
                leaders.add(ts.dflt);
                for (LabelNode l : ts.labels) {
                    leaders.add(l);
                }
                if (ts.getNext() != null) {
                    leaders.add(ts.getNext());
                }
            } else if (insn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode ls = (LookupSwitchInsnNode) insn;
                leaders.add(ls.dflt);
                for (LabelNode l : ls.labels) {
                    leaders.add(l);
                }
                if (ls.getNext() != null) {
                    leaders.add(ls.getNext());
                }
            } else if (insn instanceof LabelNode) {
                leaders.add(insn);
            } else {
                int opcode = insn.getOpcode();
                if (isReturnOpcode(opcode) || opcode == Opcodes.ATHROW) {
                    if (insn.getNext() != null) {
                        leaders.add(insn.getNext());
                    }
                }
            }
        }

        // Pass 2: Partition instructions into BasicBlocks
        int blockIdCounter = 0;
        BasicBlock currentBlock = null;

        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (leaders.contains(insn)) {
                currentBlock = new BasicBlock(blockIdCounter++);
                blocks.add(currentBlock);
            }
            if (currentBlock == null) {
                currentBlock = new BasicBlock(blockIdCounter++);
                blocks.add(currentBlock);
            }
            if (insn instanceof LabelNode) {
                labelToBlock.put((LabelNode) insn, currentBlock);
            }
            currentBlock.addInstruction(insn);
        }

        entryBlock = blocks.get(0);

        // Pass 3: Resolve successor/predecessor edges and branch targets
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            List<AbstractInsnNode> blockInsns = block.getInstructions();
            if (blockInsns.isEmpty()) continue;

            AbstractInsnNode lastInsn = null;
            for (int j = blockInsns.size() - 1; j >= 0; j--) {
                AbstractInsnNode in = blockInsns.get(j);
                if (!(in instanceof LineNumberNode) && !(in instanceof FrameNode)) {
                    lastInsn = in;
                    break;
                }
            }

            BasicBlock nextBlock = (i + 1 < blocks.size()) ? blocks.get(i + 1) : null;

            if (lastInsn instanceof JumpInsnNode) {
                JumpInsnNode jump = (JumpInsnNode) lastInsn;
                BasicBlock targetBlock = labelToBlock.get(jump.label);
                if (jump.getOpcode() == Opcodes.GOTO) {
                    block.setBranchTarget(targetBlock);
                } else {
                    // Conditional jump
                    block.setBranchTarget(targetBlock);
                    if (nextBlock != null) {
                        block.setFallthrough(nextBlock);
                    }
                }
            } else if (lastInsn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode ts = (TableSwitchInsnNode) lastInsn;
                Map<Integer, BasicBlock> switchMap = new LinkedHashMap<>();
                for (int c = 0; c < ts.labels.size(); c++) {
                    int val = ts.min + c;
                    switchMap.put(val, labelToBlock.get(ts.labels.get(c)));
                }
                block.setSwitchTargets(switchMap);
                block.setDefaultTarget(labelToBlock.get(ts.dflt));
            } else if (lastInsn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode ls = (LookupSwitchInsnNode) lastInsn;
                Map<Integer, BasicBlock> switchMap = new LinkedHashMap<>();
                for (int c = 0; c < ls.keys.size(); c++) {
                    switchMap.put(ls.keys.get(c), labelToBlock.get(ls.labels.get(c)));
                }
                block.setSwitchTargets(switchMap);
                block.setDefaultTarget(labelToBlock.get(ls.dflt));
            } else if (lastInsn != null && isReturnOpcode(lastInsn.getOpcode())) {
                block.setReturn(true);
            } else if (lastInsn != null && lastInsn.getOpcode() == Opcodes.ATHROW) {
                block.setThrow(true);
            } else {
                if (nextBlock != null) {
                    block.setFallthrough(nextBlock);
                }
            }
        }

        // Exceptional successors keep handler-only blocks and their normal successors alive.
        for (TryCatchBlockNode handler : methodNode.tryCatchBlocks) {
            int start = insns.indexOf(handler.start), end = insns.indexOf(handler.end);
            for (BasicBlock block : blocks) {
                for (AbstractInsnNode instruction : block.getInstructions()) {
                    int index = insns.indexOf(instruction);
                    if (instruction.getOpcode() >= 0 && index >= start && index < end) {
                        block.addSuccessor(labelToBlock.get(handler.handler));
                        break;
                    }
                }
            }
        }

        // Pass 4: Eliminate unreachable blocks & identify loop headers
        pruneUnreachableBlocks();
        detectLoopHeaders();
    }

    private void pruneUnreachableBlocks() {
        Set<BasicBlock> reachable = new LinkedHashSet<>();
        Queue<BasicBlock> queue = new ArrayDeque<>();
        if (entryBlock != null) {
            reachable.add(entryBlock);
            queue.add(entryBlock);
        }

        while (!queue.isEmpty()) {
            BasicBlock curr = queue.poll();
            for (BasicBlock succ : curr.getSuccessors()) {
                if (reachable.add(succ)) {
                    queue.add(succ);
                }
            }
        }

        blocks.removeIf(b -> !reachable.contains(b));

        for (BasicBlock b : blocks) {
            b.getPredecessors().removeIf(pred -> !reachable.contains(pred));
        }
    }

    private void detectLoopHeaders() {
        // DFS cycle detection for natural loops
        Set<BasicBlock> visited = new HashSet<>();
        Set<BasicBlock> onStack = new HashSet<>();

        dfsLoopDetect(entryBlock, visited, onStack);
    }

    private void dfsLoopDetect(BasicBlock curr, Set<BasicBlock> visited, Set<BasicBlock> onStack) {
        if (curr == null) return;
        visited.add(curr);
        onStack.add(curr);

        for (BasicBlock succ : curr.getSuccessors()) {
            if (onStack.contains(succ)) {
                // Back-edge detected: succ is a loop header
                succ.setLoopHeader(true);
            } else if (!visited.contains(succ)) {
                dfsLoopDetect(succ, visited, onStack);
            }
        }

        onStack.remove(curr);
    }

    private static boolean isReturnOpcode(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public BasicBlock getEntryBlock() {
        if (entryBlock == null && !blocks.isEmpty()) {
            return blocks.get(0);
        }
        return entryBlock;
    }

    public void setEntryBlock(BasicBlock entryBlock) {
        this.entryBlock = entryBlock;
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public BasicBlock getBlockForLabel(LabelNode label) {
        return labelToBlock.get(label);
    }
}
