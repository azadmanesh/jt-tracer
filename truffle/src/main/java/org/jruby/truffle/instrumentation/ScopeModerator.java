package org.jruby.truffle.instrumentation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

public final class ScopeModerator {

    // TODO Replace with WeakIdentityHashMap
    private static final Map<Frame, ShadowFrame> localScope = new WeakHashMap<>();
    private static final Map<DynamicObject, ShadowObject> objectsMap = new WeakHashMap<>();
    private static final Map<DynamicObject, ShadowObject> classObjectsMap = new WeakHashMap<>();
    private static final Map<String, ShadowTree> globalsMap = new WeakHashMap<>();

    // Given that the map is a weak map, the entries might be eliminated by GC. So, we keep a
    // reference to the shadow frames using this set instance.
    private static final Set<ShadowFrame> shadowFrames = new HashSet<>();
    private static final Set<ShadowObject> shadowObjects = new HashSet<>();
    private static final Set<ShadowObject> shadowClassObjects = new HashSet<>();
    private static final Set<ShadowTree> shadowGlobals = new HashSet<>();

    public static ShadowFrame findOrAddShadowLocalScopeFor(VirtualFrame frame){
        ShadowFrame shadowFrame = findShadowLocalScopeFor(frame);
        if (shadowFrame == null) {
            shadowFrame = addShadowLocalScopeFor(frame);
        }
        return shadowFrame;
    }

    public static ShadowFrame findShadowLocalScopeFor(Frame frame) {
        return localScope.get(frame);
    }

    public static ShadowFrame addShadowLocalScopeFor(Frame frame) {
        ShadowFrame shadowFrame = new ShadowFrameImpl();
        shadowFrames.add(shadowFrame);
        localScope.put(frame, shadowFrame);
        return shadowFrame;
    }

    public static ShadowObject findShadowObjectFor(DynamicObject obj) {
        return objectsMap.get(obj);
    }

    public static ShadowObject findOrAddShadowObjectFor(DynamicObject obj) {
        ShadowObject shadowObject = findShadowObjectFor(obj);
        if (shadowObject == null) {
            shadowObject = addShadowObjectFor(obj);
        }
        return shadowObject;
    }

    public static ShadowObject addShadowObjectFor(DynamicObject obj) {
        ShadowObject shadowObject = new ShadowObjectImpl();
        shadowObjects.add(shadowObject);
        objectsMap.put(obj, shadowObject);
        return shadowObject;
    }

    public static ShadowObject findShadowClassObjectFor(DynamicObject obj) {
        return classObjectsMap.get(obj);
    }

    public static ShadowObject findOrAddShadowClassObjectFor(DynamicObject obj) {
        ShadowObject shadowObject = findShadowClassObjectFor(obj);
        if (shadowObject == null) {
            shadowObject = addShadowClassObjectFor(obj);
        }
        return shadowObject;
    }

    public static ShadowObject addShadowClassObjectFor(DynamicObject obj) {
        ShadowObject shadowObject = new ShadowObjectImpl();
        shadowClassObjects.add(shadowObject);
        classObjectsMap.put(obj, shadowObject);
        return shadowObject;
    }

    public static ShadowTree findOrAddShadowGlobalFor(String name) {
        ShadowTree shadowGlobal = findShadowGlobalFor(name);
        if (shadowGlobal == null) {
            shadowGlobal = new ShadowTree(ShadowTree.UNKNOWN_NODE, ShadowTree.UNKNOWN_VALUE, new ShadowTree[0]);
            addShadowGlobalFor(name, shadowGlobal);
        }
        return shadowGlobal;
    }

    public static ShadowTree findShadowGlobalFor(String name) {
        return globalsMap.get(name);
    }

    public static void addShadowGlobalFor(String name, ShadowTree tree) {
        globalsMap.put(name, tree);
        shadowGlobals.add(tree);
    }
}
