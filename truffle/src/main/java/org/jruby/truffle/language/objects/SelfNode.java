/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.instrumentation.Tracer;
import org.jruby.truffle.instrumentation.Tracer.UseLocalDefStack;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNodeGen;

public class SelfNode extends RubyNode implements UseLocalDefStack {

    public static final HiddenKey SELF_IDENTIFIER = new HiddenKey("(self)");

    private final FrameSlot selfSlot;

    @Child private ReadFrameSlotNode readSelfSlotNode;

    public SelfNode(SourceSection sourceSection, FrameDescriptor frameDescriptor) {
        super(sourceSection);
        this.selfSlot = frameDescriptor.findOrAddFrameSlot(SelfNode.SELF_IDENTIFIER);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (readSelfSlotNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readSelfSlotNode = insert(ReadFrameSlotNodeGen.create(selfSlot));
        }

        return readSelfSlotNode.executeRead(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().SELF.createInstance();
    }

    @Override
    public FrameSlot getLocalSlot() {
        return selfSlot;
    }

    @Override
    public boolean isTaggedWith(Class<?> tag) {
        if (tag == Tracer.USE_LOCAL_DEF_STACK_TAG)
            return true;
        else
            return super.isTaggedWith(tag);
    }

}
