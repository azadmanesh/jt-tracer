package org.jruby.truffle.instrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;

@Registration(id = "tracer", name = "tracer")
public class Tracer extends TruffleInstrument {

    public static final Class<?> FUNCTION_BOUNDARY_TAG = FunctionBoundary.class;
    public static final Class<?> NO_USE_DEF_STACK = NoUseDefStack.class;
    public static final Class<?> USE_STACK_DEF_STACK_TAG = UseStackDefStack.class;
    public static final Class<?> USE_LOCAL_DEF_STACK_TAG = UseLocalDefStack.class;
    public static final Class<?> USE_STACK_DEF_LOCAL_STACK_TAG = UseStackDefLocalStack.class;
    public static final Class<?> USE_ARG_DEF_STACK_TAG = UseArgDefStack.class;
    public static final Class<?> USE_STACK_DEF_PROPERTY_STACK_TAG = UseStackDefPropertyStack.class;
    public static final Class<?> USE_PROPERTY_STACK_DEF_STACK_TAG = UsePropertyStackDefStack.class;

    // The mapping will be to a stack object holding shadow tree root
    public final static Object SHADOW_OPERAND_STACK_KEY = new Object();

    // The mapping will be to a map of each local name to the list of its occurrences.
    public final static Object SHADOW_LOCAL_KEY = new Object();


    @Override
    protected void onCreate(Env env) {
        System.out.println(">> Launched Instrumentation!");
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_STACK_TAG).build(), new UseStackDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(NO_USE_DEF_STACK).build(), new NoUseDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(FUNCTION_BOUNDARY_TAG).build(), new FunctionBoundaryEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_LOCAL_DEF_STACK_TAG).build(), new UseLocalDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_LOCAL_STACK_TAG).build(), new UseStackDefLocalStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_ARG_DEF_STACK_TAG).build(), new UseArgDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_PROPERTY_STACK_TAG).build(), new UseStackDefPropertyStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_PROPERTY_STACK_DEF_STACK_TAG).build(), new UsePropertyStackDefStackEventListener());
    }

    static final class UseStackDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> From Instrumentation: Before " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> From Instrumentation: After " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    final class NoUseDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            // All expression type nodes must write a value on top of the stack.
            final ShadowTree tree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[0]);

            // def stack
            Tracer.this.pushOnOperandStack(tree, frame);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class FunctionBoundaryEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> Function Boundary Before: " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> Function Boundary after: " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseLocalDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> UseLocalDefStack Before: " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> UseLocalDefStack after: " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    final class UseStackDefLocalStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            try {
                ShadowTree newShadowTree = null;
                // Read the shadow tree of the current value from top of operand stack
                FrameSlot stackSlot = frame.getFrameDescriptor().findFrameSlot(SHADOW_OPERAND_STACK_KEY);
                Stack<ShadowTree> operandStack = (Stack<ShadowTree>) frame.getObject(stackSlot);
                ShadowTree origin = null;
                if (!operandStack.isEmpty()) {
                    origin = operandStack.pop();
                }

                newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, origin == null ? new ShadowTree[0] : new ShadowTree[]{origin});

                // Add the new shadow tree for the given local variable to the current frame to be
                // accessible for further reads
                FrameSlot localSlot = frame.getFrameDescriptor().findFrameSlot(SHADOW_LOCAL_KEY);
                Map<Object, List<ShadowTree>> localOccurrencesMap = (Map<Object, List<ShadowTree>>) frame.getObject(localSlot);
                UseStackDefLocalStack node = (UseStackDefLocalStack) context.getInstrumentedNode();
                List<ShadowTree> localOccurrences = localOccurrencesMap.get(node.getLocalSlot().getIdentifier());
                if (localOccurrences == null) {
                    localOccurrences = new ArrayList<>();
                    localOccurrencesMap.put(node.getLocalSlot().getIdentifier(), localOccurrences);
                }

                // def local
                localOccurrences.add(newShadowTree);

                // def stack
                Tracer.this.pushOnOperandStack(newShadowTree, frame);

            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException(e);
            }

        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseArgDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> UseArgDefStack Before: " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> UseArgDefStack after: " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }

    static final class UseStackDefPropertyStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> UseStackDefPropertyStack Before: " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> UseStackDefPropertyStack after: " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }

    static final class UsePropertyStackDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println(">> UsePropertyStackDefStack Before: " + context);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println(">> UsePropertyStackDefStack after: " + context);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }

    public static final class UseStackDefStack {
    }

    public static final class NoUseDefStack {
    }

    public static final class FunctionBoundary {
    }

    public static final class UseLocalDefStack {
    }

    public static interface UseStackDefLocalStack {
        
        FrameSlot getLocalSlot();
    }

    public static final class UseArgDefStack {
    }

    public static final class UseStackDefPropertyStack {
    }

    public static final class UsePropertyStackDefStack {
    }

    private void pushOnOperandStack(ShadowTree tree, VirtualFrame frame) {
        try {
            FrameSlot stackSlot = frame.getFrameDescriptor().findOrAddFrameSlot(SHADOW_OPERAND_STACK_KEY);
            Stack<ShadowTree> stack = (Stack<ShadowTree>) frame.getObject(stackSlot);
            stack.push(tree);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }
}
