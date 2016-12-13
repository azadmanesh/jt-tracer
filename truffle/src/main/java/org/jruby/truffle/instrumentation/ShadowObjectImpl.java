package org.jruby.truffle.instrumentation;

import java.util.HashMap;
import java.util.Map;

public class ShadowObjectImpl implements ShadowObject {

    private final Map<Object, ShadowTree> shadowPropertiesMap = new HashMap<>();

    @Override
    public void addShadowProperty(Object identifier, ShadowTree newShadowtree) {
        shadowPropertiesMap.put(identifier, newShadowtree);
    }

    @Override
    public ShadowTree findOrAddShadowForProperty(Object identifier) {
        ShadowTree shadowProperty = findShadowForProperty(identifier);
        if (shadowProperty == null) {
            shadowProperty = new ShadowTree(ShadowTree.UNKNOWN_NODE, ShadowTree.UNKNOWN_VALUE, new ShadowTree[0]);
            shadowPropertiesMap.put(identifier, shadowProperty);
        }
        return shadowProperty;
    }

    @Override
    public ShadowTree findShadowForProperty(Object identifier) {
        return shadowPropertiesMap.get(identifier);
    }

}
