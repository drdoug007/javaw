package one.dastec.javaw.bytecode;

import one.dastec.javaw.bytecode.cfg.BasicBlock;
import one.dastec.javaw.bytecode.cfg.ControlFlowGraph;
import one.dastec.javaw.bytecode.cfg.Relooper;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.*;

public class RelooperTest {

    @Test
    public void testSimpleLinearGraph() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "add", "(II)I", null, null);
        mn.instructions = new InsnList();
        mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_2));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new InsnNode(Opcodes.IRETURN));

        ControlFlowGraph cfg = ControlFlowGraph.build(mn);
        assertNotNull(cfg.getEntryBlock());
        assertEquals(1, cfg.getBlocks().size());

        Relooper relooper = new Relooper();
        Relooper.Shape shape = relooper.reloop(cfg);
        assertNotNull(shape);
        assertTrue(shape instanceof Relooper.SimpleShape);
    }

    @Test
    public void testLoopGraph() {
        BasicBlock b0 = new BasicBlock(0);
        BasicBlock b1 = new BasicBlock(1);
        BasicBlock b2 = new BasicBlock(2);

        b0.setFallthrough(b1);
        b1.setBranchTarget(b1); // loop back
        b1.setFallthrough(b2);
        b1.setLoopHeader(true);
        b2.setReturn(true);

        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "testLoop", "()V", null, null);
        ControlFlowGraph cfg = new ControlFlowGraph(mn);
        cfg.getBlocks().add(b0);
        cfg.getBlocks().add(b1);
        cfg.getBlocks().add(b2);

        Relooper relooper = new Relooper();
        Relooper.Shape shape = relooper.reloop(cfg);
        assertNotNull(shape);
        assertTrue(shape instanceof Relooper.SimpleShape);
        Relooper.SimpleShape s0 = (Relooper.SimpleShape) shape;
        assertNotNull(s0.getNext());
        assertTrue(s0.getNext() instanceof Relooper.LoopShape);
    }
}
