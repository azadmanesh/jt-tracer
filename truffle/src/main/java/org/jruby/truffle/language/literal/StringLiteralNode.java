/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
/*
 * Copyright (c) 2013, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.instrumentation.Tracer;
import org.jruby.truffle.language.RubyNode;

public class StringLiteralNode extends RubyNode {

    private final Rope rope;

    public StringLiteralNode(RubyContext context, SourceSection sourceSection, Rope rope) {
        super(context, sourceSection);
        this.rope = rope;
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        return Layouts.STRING.createString(coreLibrary().getStringFactory(), rope);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == Tracer.NO_USE_DEF_STACK_TAG)
            return true;
        else
            return false;
    }

}
