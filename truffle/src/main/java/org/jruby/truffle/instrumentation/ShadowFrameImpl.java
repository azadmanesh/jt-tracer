package org.jruby.truffle.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ShadowFrameImpl implements ShadowFrame {

    private final Stack<ShadowTree> opStack;
    private final Map<Object, List<ShadowTree>> localShadows;
    private ShadowTree returnValue;
    private int argsCount;

    public ShadowFrameImpl() {
        this.opStack = new Stack<>();
        this.localShadows = new HashMap<>();
        this.argsCount = -1;
    }

    public ShadowTree peekOperandStack() {
        return opStack.peek();
    }

    public ShadowTree popOperandStack() {
        return opStack.pop();
    }

    public void pushOnOperandStack(ShadowTree tree) {
        this.opStack.push(tree);
    }

    public ShadowTree findLocalShadow(Object identifier) {
        List<ShadowTree> history = localShadows.get(identifier);
        if (history == null) {
            // This can happen in case the instrumentation framework doesn't insrument part of an
            // AST, usually the header of a method where the "this" local variable gets intialized
            // We just return an unknown origin for such a local variable.
            history = new ArrayList<>();
            history.add(new ShadowTree(ShadowTree.UNKNOWN_NODE, ShadowTree.UNKNOWN_VALUE, new ShadowTree[0]));
            localShadows.put(identifier, history);
        }

        return history.get(history.size() - 1);
    }

    public void addLocalShadow(Object identifier, ShadowTree tree) {
        List<ShadowTree> history = localShadows.get(identifier);
        if (history == null) {
            history = new ArrayList<>();
            localShadows.put(identifier, history);
        }

        history.add(tree);
    }

    public ShadowTree getReturnValue() {
        return this.returnValue;
    }

    public void setReturnValue(ShadowTree tree) {
        this.returnValue = tree;
    }

    public int sizeOfOperandStack() {
        return opStack.size();
    }

    public boolean hasOngoingInvocation() {
        return argsCount >= 0;
    }

    public int getArgumentCount() {
        return this.argsCount;
    }

    public void setOngoingInvocation(int argsCount) {
        if (hasOngoingInvocation())
            throw new IllegalStateException("Two invocations cannot have happenned simultenously!");

        this.argsCount = argsCount;
    }

    public void resetOngoingInvocation() {
        if (!hasOngoingInvocation())
            throw new IllegalStateException("No onging invocation already set!");

        this.argsCount = -1;
    }

}
