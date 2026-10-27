package one.dastec.javaw.bytecode.cfg;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.*;

public class BasicBlock {
    private final int id;
    private final String name;
    private final List<AbstractInsnNode> instructions = new ArrayList<>();
    private final Set<BasicBlock> predecessors = new LinkedHashSet<>();
    private final Set<BasicBlock> successors = new LinkedHashSet<>();

    private BasicBlock branchTarget;
    private BasicBlock fallthrough;
    private Map<Integer, BasicBlock> switchTargets = new LinkedHashMap<>();
    private BasicBlock defaultTarget;

    private boolean isReturn;
    private boolean isThrow;
    private boolean isLoopHeader;

    public BasicBlock(int id) {
        this.id = id;
        this.name = "$bb_" + id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<AbstractInsnNode> getInstructions() {
        return instructions;
    }

    public void addInstruction(AbstractInsnNode insn) {
        instructions.add(insn);
    }

    public Set<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public Set<BasicBlock> getSuccessors() {
        return successors;
    }

    public void addPredecessor(BasicBlock block) {
        predecessors.add(block);
    }

    public void addSuccessor(BasicBlock block) {
        if (block != null) {
            successors.add(block);
            block.getPredecessors().add(this);
        }
    }

    public BasicBlock getBranchTarget() {
        return branchTarget;
    }

    public void setBranchTarget(BasicBlock branchTarget) {
        this.branchTarget = branchTarget;
        addSuccessor(branchTarget);
    }

    public BasicBlock getFallthrough() {
        return fallthrough;
    }

    public void setFallthrough(BasicBlock fallthrough) {
        this.fallthrough = fallthrough;
        addSuccessor(fallthrough);
    }

    public Map<Integer, BasicBlock> getSwitchTargets() {
        return switchTargets;
    }

    public void setSwitchTargets(Map<Integer, BasicBlock> switchTargets) {
        this.switchTargets = switchTargets;
        for (BasicBlock target : switchTargets.values()) {
            addSuccessor(target);
        }
    }

    public BasicBlock getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(BasicBlock defaultTarget) {
        this.defaultTarget = defaultTarget;
        addSuccessor(defaultTarget);
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    public boolean isThrow() {
        return isThrow;
    }

    public void setThrow(boolean isThrow) {
        this.isThrow = isThrow;
    }

    public boolean isLoopHeader() {
        return isLoopHeader;
    }

    public void setLoopHeader(boolean loopHeader) {
        isLoopHeader = loopHeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock that = (BasicBlock) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BasicBlock{" + name + ", insns=" + instructions.size() + ", succ=" + successors.size() + "}";
    }
}
