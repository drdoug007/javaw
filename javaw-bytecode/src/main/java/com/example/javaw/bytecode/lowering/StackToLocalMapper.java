package com.example.javaw.bytecode.lowering;

import java.util.*;

public class StackToLocalMapper {
    private final int maxLocals;
    private final int maxStack;
    private final List<String> localTypes = new ArrayList<>();
    private final List<String> stackTypes = new ArrayList<>();
    private int currentStackDepth = 0;

    public StackToLocalMapper(int maxLocals, int maxStack) {
        this.maxLocals = Math.max(maxLocals, 32);
        this.maxStack = Math.max(maxStack, 32);
        for (int i = 0; i < this.maxLocals; i++) {
            localTypes.add("i32");
        }
    }

    public String getLocalVar(int index) {
        return "$l_" + index;
    }

    public String getStackVar(int depth) {
        return "$stk_" + depth;
    }

    public String getCurrentStackVar() {
        return "$stk_" + (currentStackDepth - 1);
    }

    public int getStackDepth() {
        return currentStackDepth;
    }

    public void setStackDepth(int depth) {
        this.currentStackDepth = depth;
    }

    public String push(String type) {
        String var = "$stk_" + currentStackDepth;
        if (currentStackDepth >= stackTypes.size()) {
            stackTypes.add(type);
        } else {
            stackTypes.set(currentStackDepth, type);
        }
        currentStackDepth++;
        return var;
    }

    public String pop() {
        if (currentStackDepth > 0) {
            currentStackDepth--;
            return "$stk_" + currentStackDepth;
        }
        return "$stk_0";
    }

    public String peek() {
        return "$stk_" + Math.max(0, currentStackDepth - 1);
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
