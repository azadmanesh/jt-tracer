package org.jruby.truffle.instrumentation;

public interface ShadowObject {

    /**
     * if there is not any shadow property set already, it will create one and return it. The node
     * considered as the creator of the shadow property will be set to ShadowTree.NODE_UNKNOWN
     * 
     * @param identifier
     * @return
     */
    public ShadowTree findOrAddShadowForProperty(Object identifier);

    public ShadowTree findShadowForProperty(Object identifier);

    public void addShadowProperty(Object identifier, ShadowTree newShadowtree);
}
