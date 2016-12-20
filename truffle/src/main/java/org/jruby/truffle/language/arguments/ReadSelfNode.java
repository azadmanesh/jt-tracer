/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;

import org.jruby.truffle.instrumentation.Tracer;
import org.jruby.truffle.instrumentation.Tracer.UseArgDefStack;
import org.jruby.truffle.language.RubyNode;

public class ReadSelfNode extends RubyNode implements UseArgDefStack {

    @Override
    public Object execute(VirtualFrame frame) {
        return RubyArguments.getSelf(frame);
    }

    @Override
    public boolean isTaggedWith(Class<?> tag) {
        if (tag == Tracer.USE_ARG_DEF_STACK_TAG)
            return true;
        else
            return super.isTaggedWith(tag);
    }

    public int getIndex() {
        return 0;
    }

	
    public int getArgumentsCount(Frame frame) {
        return 0;
    }

}
