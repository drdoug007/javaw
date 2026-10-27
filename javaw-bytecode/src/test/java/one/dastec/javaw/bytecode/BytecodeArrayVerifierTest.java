package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeArrayVerifierTest implements Opcodes {
    private ClassNode owner(String desc) {
        ClassNode owner = new ClassNode(); owner.version = V17; owner.access = ACC_PUBLIC;
        owner.name = "ArrayTypes"; owner.superName = "java/lang/Object"; owner.sourceFile = "ArrayTypes.java";
        MethodNode run = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", desc, null, null);
        run.maxLocals = 1; run.maxStack = 4; owner.methods.add(run);
        LabelNode line = new LabelNode(); run.instructions.add(line); run.instructions.add(new LineNumberNode(7, line));
        return owner;
    }

    private byte[] bytes(ClassNode owner) {
        ClassWriter writer = new ClassWriter(0); owner.accept(writer); return writer.toByteArray();
    }

    private void jvm(ClassNode owner) {
        byte[] bytes = bytes(owner);
        new ClassLoader(null) { Class<?> load() { return defineClass(null, bytes, 0, bytes.length); } }.load().getDeclaredMethods();
    }

    private void reject(ClassNode owner) {
        assertThrows(LinkageError.class, () -> jvm(owner));
        String error = assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner))).getMessage();
        assertTrue(error.contains("ArrayTypes.java:7:"), error);
    }

    @Test void verifiesEveryPrimitiveLoadAndStoreArrayKindWithoutHostLoading() {
        int[] types = {T_BOOLEAN, T_BYTE, T_CHAR, T_SHORT, T_INT, T_LONG, T_FLOAT, T_DOUBLE};
        int[] loads = {BALOAD, BALOAD, CALOAD, SALOAD, IALOAD, LALOAD, FALOAD, DALOAD};
        int[] stores = {BASTORE, BASTORE, CASTORE, SASTORE, IASTORE, LASTORE, FASTORE, DASTORE};
        for (int array = 0; array < types.length; array++) for (int operation = 0; operation < types.length; operation++) {
            for (boolean store : new boolean[]{true, false}) {
                ClassNode owner = owner("()V"); var code = owner.methods.get(0).instructions;
                code.add(new InsnNode(ICONST_1)); code.add(new IntInsnNode(NEWARRAY, types[array])); code.add(new InsnNode(ICONST_0));
                if (store) code.add(new InsnNode(switch (types[operation]) {
                    case T_LONG -> LCONST_0; case T_FLOAT -> FCONST_0; case T_DOUBLE -> DCONST_0; default -> ICONST_0;
                }));
                code.add(new InsnNode(store ? stores[operation] : loads[operation]));
                if (!store) code.add(new InsnNode(types[operation] == T_LONG || types[operation] == T_DOUBLE ? POP2 : POP));
                code.add(new InsnNode(RETURN));
                if (array == operation || array < 2 && operation < 2) {
                    assertDoesNotThrow(() -> jvm(owner));
                    assertDoesNotThrow(() -> BytecodeCompiler.compileClass(bytes(owner)));
                } else reject(owner);
            }
        }
    }

    @Test void rejectsWrongSizesIndicesValuesReceiversArgumentsAndResults() {
        for (String scenario : new String[]{"size", "index", "value", "receiver", "length", "argument", "result", "uninitialized"}) {
            ClassNode owner = owner(scenario.equals("receiver") || scenario.equals("length") ? "(Ljava/lang/Object;)V"
                    : scenario.equals("result") ? "()[J" : "()V");
            var code = owner.methods.get(0).instructions;
            switch (scenario) {
                case "size" -> { code.add(new InsnNode(FCONST_0)); code.add(new IntInsnNode(NEWARRAY, T_INT)); code.add(new InsnNode(POP)); }
                case "receiver", "length", "uninitialized" -> {
                    if (scenario.equals("uninitialized")) code.add(new TypeInsnNode(NEW, owner.name));
                    else code.add(new VarInsnNode(ALOAD, 0));
                    if (scenario.equals("receiver")) { code.add(new InsnNode(ICONST_0)); code.add(new InsnNode(IALOAD)); }
                    else code.add(new InsnNode(ARRAYLENGTH));
                    code.add(new InsnNode(POP));
                }
                default -> {
                    code.add(new InsnNode(ICONST_1)); code.add(new IntInsnNode(NEWARRAY, T_INT));
                    if (scenario.equals("result")) { code.add(new InsnNode(ARETURN)); }
                    else if (scenario.equals("argument")) {
                        MethodNode helper = new MethodNode(ACC_PRIVATE | ACC_STATIC, "helper", "([J)V", null, null);
                        helper.instructions.add(new InsnNode(RETURN)); helper.maxLocals = 1; owner.methods.add(helper);
                        code.add(new MethodInsnNode(INVOKESTATIC, owner.name, "helper", "([J)V", false));
                    } else {
                        code.add(new InsnNode(scenario.equals("index") ? FCONST_0 : ICONST_0));
                        if (scenario.equals("value")) { code.add(new InsnNode(FCONST_0)); code.add(new InsnNode(IASTORE)); }
                        else { code.add(new InsnNode(IALOAD)); code.add(new InsnNode(POP)); }
                    }
                }
            }
            if (!scenario.equals("result")) code.add(new InsnNode(RETURN));
            reject(owner);
        }
    }

    @Test void verifiesReferenceArrayOperandsAndPreservesDynamicStoreChecks() {
        for (String scenario : new String[]{"primitiveLoad","primitiveStore","index","value","uninitialized","result"}) {
            ClassNode owner = owner(scenario.equals("result") ? "()[LArrayTypes;" : "()V");
            var code = owner.methods.get(0).instructions;
            code.add(new InsnNode(ICONST_1));
            if(scenario.startsWith("primitive")) code.add(new IntInsnNode(NEWARRAY,T_INT));
            else code.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));
            if(scenario.equals("result")) code.add(new InsnNode(ARETURN));
            else {
                code.add(new InsnNode(scenario.equals("index") ? FCONST_0 : ICONST_0));
                if(scenario.equals("primitiveLoad") || scenario.equals("index")) {
                    code.add(new InsnNode(AALOAD)); code.add(new InsnNode(POP));
                } else {
                    if(scenario.equals("uninitialized")) code.add(new TypeInsnNode(NEW,owner.name));
                    else code.add(new InsnNode(scenario.equals("value") ? ICONST_1 : ACONST_NULL));
                    code.add(new InsnNode(AASTORE));
                }
                code.add(new InsnNode(RETURN));
            }
            reject(owner);
        }
        // The verifier allows any initialized reference as a store value; runtime checks the component.
        ClassNode owner = owner("(Ljava/lang/Object;)V"); var code = owner.methods.get(0).instructions;
        code.add(new InsnNode(ICONST_1)); code.add(new TypeInsnNode(ANEWARRAY,owner.name));
        code.add(new InsnNode(ICONST_0)); code.add(new VarInsnNode(ALOAD,0));
        code.add(new InsnNode(AASTORE)); code.add(new InsnNode(RETURN));
        assertDoesNotThrow(() -> jvm(owner)); assertDoesNotThrow(() -> BytecodeCompiler.compileClass(bytes(owner)));
    }

    @Test void diagnosesUnlinkedInaccessibleAndMalformedArrayComponents() {
        for(String component : new String[]{"missing/Type", "[Lmissing/Type;", "[V", "other/Hidden"}) {
            ClassNode owner = owner("()V"); var code = owner.methods.get(0).instructions;
            code.add(new InsnNode(ICONST_0)); code.add(new TypeInsnNode(ANEWARRAY,component));
            code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN));
            ClassNode hidden = new ClassNode(); hidden.name="other/Hidden"; hidden.superName="java/lang/Object";
            String message = assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(owner)
                    .compileToWat(java.util.Map.of(owner.name,owner,hidden.name,hidden))).getMessage();
            assertTrue(message.contains("ArrayTypes.java:7:"),message);
            assertTrue(message.contains("reference array allocation"),message);
        }
        for(int opcode : new int[]{AALOAD,AASTORE}) {
            ClassNode owner=owner("()V"); var code=owner.methods.get(0).instructions;
            code.add(new InsnNode(ACONST_NULL)); code.add(new InsnNode(ICONST_0));
            if(opcode==AASTORE) code.add(new InsnNode(ACONST_NULL));
            code.add(new InsnNode(opcode)); if(opcode==AALOAD) code.add(new InsnNode(POP));
            code.add(new InsnNode(RETURN));
            assertDoesNotThrow(() -> jvm(owner)); assertDoesNotThrow(() -> BytecodeCompiler.compileClass(bytes(owner)));
        }
    }

    @Test void rejectsMalformedMultidimensionalAllocationsLikeJvm() {
        for(String descriptor : new String[]{"I","[I","[[I","[V","[".repeat(256)+"I","[[Lmissing/Type;"}) {
            int dimensions = descriptor.equals("[I") ? 2 : descriptor.equals("[[I") ? 0 : 1;
            ClassNode owner=owner("()V"); var code=owner.methods.get(0).instructions;
            for(int i=0;i<dimensions;i++) code.add(new InsnNode(ICONST_1));
            code.add(new MultiANewArrayInsnNode(descriptor,dimensions));
            code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN));
            if(descriptor.contains("missing")) {
                String message=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(bytes(owner))).getMessage();
                assertTrue(message.contains("ArrayTypes.java:7:"),message);
                assertTrue(message.contains("multidimensional array allocation"),message);
            } else reject(owner);
        }
        ClassNode owner=owner("()V"); var code=owner.methods.get(0).instructions;
        code.add(new InsnNode(ICONST_1)); code.add(new InsnNode(FCONST_0));
        code.add(new MultiANewArrayInsnNode("[[I",2)); code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN));
        reject(owner);
    }

    @Test void admitsMaximumRankPartialAllocation() {
        ClassNode owner=owner("()V"); var code=owner.methods.get(0).instructions;
        code.add(new InsnNode(ICONST_0)); code.add(new MultiANewArrayInsnNode("[".repeat(255)+"I",1));
        code.add(new InsnNode(ARRAYLENGTH)); code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN));
        assertDoesNotThrow(() -> jvm(owner)); assertDoesNotThrow(() -> BytecodeCompiler.compileClass(bytes(owner)));
    }

    @Test void verifiesArrayCloneReceiversAndArraycopyArgumentsLikeJvm() {
        for(String descriptor : new String[]{"(Ljava/lang/Object;)V","([J)V","()V"}) {
            ClassNode owner=owner(descriptor); var code=owner.methods.get(0).instructions;
            if(descriptor.equals("()V")) code.add(new TypeInsnNode(NEW,owner.name));
            else code.add(new VarInsnNode(ALOAD,0));
            code.add(new MethodInsnNode(INVOKEVIRTUAL,"[I","clone","()Ljava/lang/Object;",false));
            code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN)); reject(owner);
        }
        for(boolean uninitialized : new boolean[]{false,true}) {
            ClassNode owner=owner("()V");owner.methods.get(0).maxStack=5;
            var code=owner.methods.get(0).instructions;
            if(uninitialized) code.add(new TypeInsnNode(NEW,owner.name)); else code.add(new InsnNode(ACONST_NULL));
            code.add(new InsnNode(uninitialized?ICONST_0:FCONST_0));
            code.add(new InsnNode(ACONST_NULL));code.add(new InsnNode(ICONST_0));code.add(new InsnNode(ICONST_0));
            code.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","arraycopy","(Ljava/lang/Object;ILjava/lang/Object;II)V",false));
            code.add(new InsnNode(RETURN));reject(owner);
        }
    }

    @Test void rejectsArrayIntrinsicInvocationKindAndDescriptorImpostors() {
        for(MethodInsnNode call : new MethodInsnNode[]{
                new MethodInsnNode(INVOKESTATIC,"[I","clone","()Ljava/lang/Object;",false),
                new MethodInsnNode(INVOKESPECIAL,"[I","clone","()Ljava/lang/Object;",false),
                new MethodInsnNode(INVOKEVIRTUAL,"[I","clone","()[I",false),
                new MethodInsnNode(INVOKEINTERFACE,"[I","clone","()Ljava/lang/Object;",true),
                new MethodInsnNode(INVOKESTATIC,"java/lang/System","arraycopy","(Ljava/lang/Object;ILjava/lang/Object;II)V",true),
                new MethodInsnNode(INVOKEVIRTUAL,"java/lang/System","arraycopy","(Ljava/lang/Object;ILjava/lang/Object;II)V",false),
                new MethodInsnNode(INVOKESTATIC,"java/lang/System","arraycopy","(Ljava/lang/Object;ILjava/lang/Object;II)I",false)}) {
            ClassNode owner=owner("()V");var code=owner.methods.get(0).instructions;
            code.add(call);code.add(new InsnNode(RETURN));
            String message=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(bytes(owner))).getMessage();
            assertTrue(message.contains("ArrayTypes.java:7:"),message);
        }
    }

    @Test void rejectsInvalidPrimitiveAllocationTagsAndRetainsNullArrayTyping() {
        for (int type : new int[]{-1, 0, 3, 12, 255}) {
            ClassNode owner = owner("()V"); var code = owner.methods.get(0).instructions;
            code.add(new InsnNode(ICONST_0)); code.add(new IntInsnNode(NEWARRAY, type));
            code.add(new InsnNode(POP)); code.add(new InsnNode(RETURN)); reject(owner);
        }
        for (int opcode : new int[]{ARRAYLENGTH, IALOAD, LALOAD, FALOAD, DALOAD, BALOAD, CALOAD, SALOAD}) {
            ClassNode owner = owner("()V"); var code = owner.methods.get(0).instructions;
            code.add(new InsnNode(ACONST_NULL));
            if (opcode != ARRAYLENGTH) code.add(new InsnNode(ICONST_0));
            code.add(new InsnNode(opcode)); code.add(new InsnNode(opcode == LALOAD || opcode == DALOAD ? POP2 : POP));
            code.add(new InsnNode(RETURN));
            assertDoesNotThrow(() -> jvm(owner)); assertDoesNotThrow(() -> BytecodeCompiler.compileClass(bytes(owner)));
        }
    }
}
