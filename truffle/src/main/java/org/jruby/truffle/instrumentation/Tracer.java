package org.jruby.truffle.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jruby.truffle.language.control.ReturnException;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter.SourcePredicate;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import jdk.nashorn.internal.ir.CallNode;

@Registration(id = "tracer", name = "tracer")
public class Tracer extends TruffleInstrument {

    public static final Class<?> FUNCTION_BOUNDARY_TAG = FunctionBoundary.class;
    public static final Class<?> NO_USE_DEF_STACK_TAG = NoUseDefStack.class;
    public static final Class<?> USE_STACK_DEF_STACK_TAG = UseStackDefStack.class;
    public static final Class<?> USE_LOCAL_DEF_STACK_TAG = UseLocalDefStack.class;
    public static final Class<?> USE_STACK_DEF_LOCAL_STACK_TAG = UseStackDefLocalStack.class;
    public static final Class<?> USE_ARG_DEF_STACK_TAG = UseArgDefStack.class;
    public static final Class<?> USE_STACK_DEF_PROPERTY_STACK_TAG = UseStackDefPropertyStack.class;
    public static final Class<?> USE_PROPERTY_STACK_DEF_STACK_TAG = UsePropertyStackDefStack.class;
    public static final Class<?> USE_STACK_DEF_RETURN_TAG = UseStackDefReturn.class;
    public static final Class<?> CALL_NODE_TAG = CallNode.class;
    public static final Class<?> USE_CLASS_VAR_DEF_STACK_TAG = UseClassVarDefStack.class;
    public static final Class<?> USE_STACK_DEF_CLASS_VAR_TAG = UseStackDefClassVar.class;
    public static final Class<?> USE_GLOBAL_DEF_STACK_TAG = UseGlobalDefStack.class;
    public static final Class<?> USE_STACK_DEF_GLOBAL_TAG = UseStackDefGlobal.class;

    // The mapping will be to a stack object holding shadow tree root
    public final static Object SHADOW_OPERAND_STACK_KEY = new Object();

    // The mapping will be to a map of each local name to the list of its occurrences.
    public final static Object SHADOW_LOCAL_KEY = new Object();

    final String[] INFIXES = {"truffle/src/main/ruby",
                    "lib/ruby/truffle"
    };

    @Override
    protected void onCreate(Env env) {
        System.out.println(">> Launched Instrumentation!");

        SourcePredicate sp = new SourcePredicate(){


            public boolean test(Source source) {
                for (String infix : INFIXES) {
                    if (source.getName().contains(infix))
                        return false;
                }

                if (source.getName().equals("main")) {
                    return false;
                }
                
                if (source.getName().equals("context")) {
                    return false;
                }

                if (source.getName().startsWith("(") )
                    return false;

                if (source.isInternal())
                    return false;

                return true;
            }
            
        };

        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(FUNCTION_BOUNDARY_TAG).sourceIs(sp).build(),
                        new FunctionBoundaryEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(NO_USE_DEF_STACK_TAG).sourceIs(sp).build(),
                        new NoUseDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_LOCAL_STACK_TAG).sourceIs(sp).build(),
                        new UseStackDefLocalStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UseStackDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_LOCAL_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UseLocalDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_RETURN_TAG).sourceIs(sp).build(),
                        new UseStackDefReturnEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_ARG_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UseArgDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_PROPERTY_STACK_TAG).sourceIs(sp).build(),
                        new UseStackDefPropertyStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_PROPERTY_STACK_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UsePropertyStackDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(CALL_NODE_TAG).sourceIs(sp).build(),
                        new CallEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_CLASS_VAR_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UseClassVarDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_CLASS_VAR_TAG).sourceIs(sp).build(),
                        new UseStackDefClassVarEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_GLOBAL_DEF_STACK_TAG).sourceIs(sp).build(),
                        new UseGlobalDefStackEventListener());
        env.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(USE_STACK_DEF_GLOBAL_TAG).sourceIs(sp).build(),
                        new UseStackDefGlobalEventListener());
    }

    static final class FunctionBoundaryEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println("Entering " + context.getInstrumentedNode().getRootNode().getName());
            System.out.println("Frame is:\t" + frame);
            NodeUtil.printCompactTree(System.out, context.getInstrumentedNode().getRootNode());
            // Initialize a shadow local scope. This includes an operand stack and local variable
            // table.
            // The operand stack is used to keep track of the origin of intermediate values. The
            // shadow local variable table keeps track of the origin of values for locals.
            ScopeModerator.addShadowLocalScopeFor(frame);
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            Frame caller = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY, true);
            if (caller == null)
                return;

            ShadowFrame currentShadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);

            if (currentShadowFrame.sizeOfOperandStack() > 0) {
                ShadowTree origin = currentShadowFrame.popOperandStack();
                ShadowFrame callerShadowFrame = ScopeModerator.findShadowLocalScopeFor(caller);
                // Caller shadow frame could be null because the caller might not have been traced
                if (callerShadowFrame == null)
                    return;

                callerShadowFrame.pushOnOperandStack(origin);
            }

        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            System.out.println("Exception: " + context.getInstrumentedNode().getSourceSection().getSource().getName());
        }

    }

    final class NoUseDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            // All expression type nodes must write a value on top of the stack.
            final ShadowTree tree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[0]);

            // def stack
            ShadowFrame shadowFrame = ScopeModerator.findOrAddShadowLocalScopeFor(frame);
            shadowFrame.pushOnOperandStack(tree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    final class UseStackDefLocalStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);

            if (shadowFrame.sizeOfOperandStack() == 0)
                throw new IllegalStateException("Operand stack is empty!");

            // Read the shadow tree of the current value from top of operand stack
            ShadowTree origin = shadowFrame.popOperandStack();
            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});
            UseStackDefLocalStack node = (UseStackDefLocalStack) context.getInstrumentedNode();

            // def local
            // Add the new shadow tree for the given local variable to the current frame to be
            // accessible for further reads
            shadowFrame.addLocalShadow(node.getLocalSlot().getIdentifier(), newShadowTree);

            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);

            ShadowTree.dumpTree(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }


    static final class UseStackDefStackEventListener implements ExecutionEventListener {

        private int stackSizeBefore = -1;

        public void onEnter(EventContext context, VirtualFrame frame) {
            stackSizeBefore = ScopeModerator.findShadowLocalScopeFor(frame).sizeOfOperandStack();
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (stackSizeBefore == -1)
                throw new IllegalStateException("Arguments not initialized correctly!");

            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            int currStackSize = shadowFrame.sizeOfOperandStack();

            final int argCount = currStackSize - stackSizeBefore;

            if (shadowFrame.sizeOfOperandStack() < argCount)
                throw new IllegalStateException("Wrong number of arguments on the stack!");

            ShadowTree[] origin = new ShadowTree[argCount];
            for (int i = 0; i < argCount; i++) {
                origin[i] = shadowFrame.popOperandStack();
            }

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, origin);
            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            stackSizeBefore = -1;
        }

    }

    static final class UseLocalDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            UseLocalDefStack localReadNode = (UseLocalDefStack) context.getInstrumentedNode();
            ShadowTree origin = shadowFrame.findLocalShadow(localReadNode.getLocalSlot().getIdentifier());
            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});
            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseStackDefReturnEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            ShadowTree origin = shadowFrame.popOperandStack();  // there should be a shadow value
                                                                // for the return value on the
                                                                // operand stack
            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), origin.getRootValue(), new ShadowTree[]{origin});
            shadowFrame.pushOnOperandStack(newShadowTree);     // To be used by the method body
                                                               // wrapper for passing the return
                                                               // value to the caller
            // We set the return value on the shadow frame to be used later for further analyses
            shadowFrame.setReturnValue(newShadowTree);
        }

    }

    static final class UseArgDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
            System.out.println("UseArgDefStackEventListener:onEnter");
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            System.out.println("UseArgDefStackEventListener:onReturnValue");

            Frame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY, true);
            ShadowFrame callerShadowFrame = ScopeModerator.findShadowLocalScopeFor(callerFrame);
            ShadowFrame currentShadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            
            UseArgDefStack node = ((UseArgDefStack) context.getInstrumentedNode());

            ShadowTree origin;
            if (callerShadowFrame != null) {
                origin = getArgShadowAtIndex(frame, callerShadowFrame, node);
            } else {
                origin = new ShadowTree(ShadowTree.UNKNOWN_NODE, ShadowTree.UNKNOWN_VALUE, new ShadowTree[0]);
            }

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});
            currentShadowFrame.pushOnOperandStack(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            System.out.println("UseArgDefStackEventListener:onReturnExceptional");
        }

        private ShadowTree getArgShadowAtIndex(VirtualFrame frame, ShadowFrame shadowFrame, UseArgDefStack node) {
            final int dist = node.getArgumentsCount(frame) - node.getIndex() - 1;
            Stack<ShadowTree> tempStack = new Stack<>();
            for (int i = 0; i < dist; i++) {
                tempStack.push(shadowFrame.popOperandStack());
            }
            ShadowTree arg = shadowFrame.peekOperandStack();

            for (int i = 0; i < dist; i++) {
                shadowFrame.pushOnOperandStack(tempStack.pop());
            }
            return arg;
        }
    }


    static final class UseStackDefPropertyStackEventListener implements ExecutionEventListener {

        int stackSizeBefore = -1;

        public void onEnter(EventContext context, VirtualFrame frame) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            stackSizeBefore = shadowFrame.sizeOfOperandStack();
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            final int currentSize = shadowFrame.sizeOfOperandStack();

            assert currentSize > stackSizeBefore;

            final int operandCount = currentSize - stackSizeBefore;
            ShadowTree[] origins = new ShadowTree[operandCount];
            for (int i = 0; i < operandCount; i++) {
                origins[i] = shadowFrame.popOperandStack();
            }

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, origins);

            // we assume that the first operands is always the receiver object
            assert origins[0].getRootValue() instanceof DynamicObject;
            ShadowObject shadowObject = ScopeModerator.findOrAddShadowObjectFor((DynamicObject) origins[0].getRootValue());
            UseStackDefPropertyStack propertyNode = (UseStackDefPropertyStack) context.getInstrumentedNode();

            // Def property
            shadowObject.addShadowProperty(propertyNode.getPropertyName(), newShadowTree);

            // Def stack
            shadowFrame.pushOnOperandStack(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }

    static final class UsePropertyStackDefStackEventListener implements ExecutionEventListener {

        int stackSizeBefore = -1;

        public void onEnter(EventContext context, VirtualFrame frame) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            stackSizeBefore = shadowFrame.sizeOfOperandStack();
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            final int currentSize = shadowFrame.sizeOfOperandStack();

            assert currentSize > stackSizeBefore;

            final int originCount = currentSize - stackSizeBefore + 1;  // one of the origins come
                                                                        // from a shadow property
            ShadowTree[] origins = new ShadowTree[originCount];
            for (int i = 0; i < originCount; i++) {
                origins[i] = shadowFrame.popOperandStack();
            }

            // we assume that the first operands is always the receiver object
            assert origins[0].getRootValue() instanceof DynamicObject;
            ShadowObject shadowObject = ScopeModerator.findOrAddShadowObjectFor((DynamicObject) origins[0].getRootValue());
            UsePropertyStackDefStack propertyNode = (UsePropertyStackDefStack) context.getInstrumentedNode();
            origins[originCount - 1] = shadowObject.findOrAddShadowForProperty(propertyNode.getPropertyName());

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, origins);

            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);

        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }

    static final class CallEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseClassVarDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            UseClassVarDefStack node = (UseClassVarDefStack) context.getInstrumentedNode();
            final String name = node.getName();
            final DynamicObject lexicalScope = node.getLexicalScope();

            ShadowObject shadowObject = ScopeModerator.findOrAddShadowClassObjectFor(lexicalScope);
            ShadowTree origin = shadowObject.findOrAddShadowForProperty(name);

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});

            // def stack
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            shadowFrame.pushOnOperandStack(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseStackDefClassVarEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findShadowLocalScopeFor(frame);
            
            ShadowTree origin = shadowFrame.popOperandStack();

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});

            UseStackDefClassVar node = (UseStackDefClassVar) context.getInstrumentedNode();
            final String name = node.getName();
            final DynamicObject lexicalScope = node.getLexicalScope();

            ShadowObject shadowObject = ScopeModerator.findOrAddShadowClassObjectFor(lexicalScope);

            // def class var
            shadowObject.addShadowProperty(name, newShadowTree);

            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);
            ShadowTree.dumpTree(newShadowTree);
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseGlobalDefStackEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            UseGlobalDefStack node = (UseGlobalDefStack) context.getInstrumentedNode();
            ShadowTree origin = ScopeModerator.findOrAddShadowGlobalFor(node.getName());
            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});
            ShadowFrame shadowFrame = ScopeModerator.findOrAddShadowLocalScopeFor(frame);
            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);

        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

    static final class UseStackDefGlobalEventListener implements ExecutionEventListener {

        public void onEnter(EventContext context, VirtualFrame frame) {
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            ShadowFrame shadowFrame = ScopeModerator.findOrAddShadowLocalScopeFor(frame);
            ShadowTree origin = shadowFrame.popOperandStack();

            ShadowTree newShadowTree = new ShadowTree(context.getInstrumentedNode(), result, new ShadowTree[]{origin});

            UseStackDefGlobal node = (UseStackDefGlobal) context.getInstrumentedNode();
            // def global
            ScopeModerator.addShadowGlobalFor(node.getName(), newShadowTree);

            // def stack
            shadowFrame.pushOnOperandStack(newShadowTree);

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

    public static interface UseLocalDefStack {

        FrameSlot getLocalSlot();
    }

    public static interface UseStackDefLocalStack {
        
        FrameSlot getLocalSlot();
    }

    public static interface UseArgDefStack {

        int getIndex();

        // We need this method because for some languages like Ruby, the arguments set has more
        // elements than the actual number of argumetns. In order to make it language agnostic, we
        // use this method to get the number of actual arguments.
        int getArgumentsCount(Frame frame);
    }

    public static interface UseStackDefPropertyStack {

        public String getPropertyName();
    }

    public static interface UsePropertyStackDefStack {

        public String getPropertyName();
    }

    public static final class UseStackDefReturn {

    }

    public static interface CallNode {

        @Deprecated
        public int getArgumentsCount();

    }

    public static interface UseClassVarDefStack {

        public String getName();

        public DynamicObject getLexicalScope();
    }

    public static interface UseStackDefClassVar {

        public String getName();

        public DynamicObject getLexicalScope();
    }

    public static interface UseGlobalDefStack {
        public String getName();
    }

    public static interface UseStackDefGlobal {
        public String getName();
    }

}
