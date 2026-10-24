package com.example.javaw.bytecode.lowering;

import java.util.*;

public class StackToLocalMapper {
    private final int maxLocals;
    private final int maxStack;
    private final List<String> stackTypes = new ArrayList<>();
    private int currentStackDepth = 0;

    public StackToLocalMapper(int maxLocals, int maxStack) {
        this.maxLocals = Math.max(maxLocals, 32);
        this.maxStack = Math.max(maxStack, 32);
    }

    public String getLocalVar(int index) {
        return getLocalVar(index, "i32");
    }

    public String getLocalVar(int index, String type) {
        return "$l_" + index + (type.equals("i32") ? "" : "_" + type);
    }

    public String getStackVar(int depth) {
        String type = depth < stackTypes.size() ? stackTypes.get(depth) : "i32";
        return "$stk_" + depth + (type.equals("i32") ? "" : "_" + type);
    }

    public String getCurrentStackVar() {
        return peek();
    }

    public int getStackDepth() {
        return currentStackDepth;
    }

    public void setStackDepth(int depth) {
        while (stackTypes.size() < depth) stackTypes.add("i32");
        this.currentStackDepth = depth;
    }

    public void setStackTypes(List<String> types) {
        stackTypes.clear();
        stackTypes.addAll(types);
        currentStackDepth = types.size();
    }

    public String peekType() { return stackTypes.get(currentStackDepth - 1); }

    public String push(String type) {
        if (currentStackDepth >= stackTypes.size()) {
            stackTypes.add(type);
        } else {
            stackTypes.set(currentStackDepth, type);
        }
        return getStackVar(currentStackDepth++);
    }

    public String pop() {
        if (currentStackDepth > 0) {
            currentStackDepth--;
            return getStackVar(currentStackDepth);
        }
        return "$stk_0";
    }

    public String peek() {
        return getStackVar(Math.max(0, currentStackDepth - 1));
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public int getMaxStack() {
        return Math.max(maxStack, stackTypes.size());
    }

    public List<String> getDeclaredLocals() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < maxLocals; i++) {
            list.add("$l_" + i);
        }
        return list;
    }

    public List<String> getDeclaredStackLocals() {
        List<String> list = new ArrayList<>();
        int limit = Math.max(maxStack, stackTypes.size());
        for (int i = 0; i < limit; i++) {
            list.add("$stk_" + i);
        }
        return list;
    }
}
