package org.jruby.truffle.instrumentation;

public interface ShadowFrame {

    public ShadowTree peekOperandStack();

    public ShadowTree popOperandStack();

    public void pushOnOperandStack(ShadowTree tree);

    public int sizeOfOperandStack();

    public ShadowTree findLocalShadow(Object identifier);

    public void addLocalShadow(Object identifier, ShadowTree tree);

    public ShadowTree getReturnValue();

    public void setReturnValue(ShadowTree tree);

    /**
     * Sets the flag that a method has been invoked from within the context of this frame and not
     * returned yet
     */
    public void setOngoingInvocation(int argCount);

    /**
     * Resets the flag that a method had been invoked. It means the invocation has been returned.
     */
    public void resetOngoingInvocation();

    /**
     * Once a method invokes another method and before it returns, this returns true, otherwise it
     * returns false.
     * 
     * @return
     */
    public boolean hasOngoingInvocation();

    /**
     * if The hasOngingInvocation is true, this method returns the number of arguments passed to
     * this onging invocation. Otherwise, it returns -1.
     * 
     * @return
     */
    public int getArgumentCount();

}
