/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;

/**
 * Efficient iteration over the attributes without creating an array or iterator.
 */
public abstract class ForEachAttributeNode extends AttributeIterativeAccessNode {

    @Child private AttributeAction actionNode;

    protected ForEachAttributeNode(AttributeAction actionNode) {
        this.actionNode = actionNode;
    }

    public static ForEachAttributeNode create(AttributeAction action) {
        return ForEachAttributeNodeGen.create(action);
    }

    public abstract Object execute(RAttributable attributable, String attributeName);

    /**
     * Return {@code false} to stop the attributes iteration prematurely. Context gives access to
     * the parameter passed to {@link #execute(RAttributable, String)} and to the result, which
     * should be set by this function.
     */
    public abstract static class AttributeAction extends Node {
        public void init(@SuppressWarnings("unused") Context context) {
            // nop
        }

        public abstract boolean action(String name, Object value, Context context);
    }

    protected static boolean hasNullAttributes(RAttributable attributable) {
        return attributable.getAttributes() == null;
    }

    protected static DynamicObject getAttributes(RAttributable attributable) {
        return attributable.getAttributes();
    }

    @Specialization(limit = "getCacheLimit()", guards = {
                    "!hasNullAttributes(attributable)",
                    "attrs == getAttributes(attributable)",
                    "attrsLayout != null",
                    "attrsLayout.shape.check(attrs)"
    })
    @ExplodeLoop
    protected Object iterateConstLayout(@SuppressWarnings("unused") RAttributable attributable,
                    String attributeName,
                    @Cached("getAttributes(attributable)") DynamicObject attrs,
                    @Cached("findLayout(attrs, createLoopProfiles())") AttrsLayout attrsLayout) {
        final Property[] props = attrsLayout.properties;
        Context ctx = new Context(attributeName);
        actionNode.init(ctx);
        for (int i = 0; i < props.length; i++) {
            Object value = readProperty(attrs, attrsLayout.shape, props[i]);
            if (!actionNode.action((String) props[i].getKey(), value, ctx)) {
                break;
            }
        }
        return ctx.result;
    }

    @Specialization(replaces = "iterateConstLayout", guards = "!hasNullAttributes(attributable)")
    protected Object iterateAnyLayout(RAttributable attributable, String attributeName) {
        final DynamicObject attrs = attributable.getAttributes();
        Shape shape = attrs.getShape();
        List<Property> props = shape.getPropertyList();
        Context ctx = new Context(attributeName);
        actionNode.init(ctx);
        for (int i = 0; i < props.size(); i++) {
            Property p = props.get(i);
            Object value = readProperty(attrs, shape, p);
            if (!actionNode.action((String) p.getKey(), value, ctx)) {
                break;
            }
        }
        return ctx.result;
    }

    @Specialization(guards = "hasNullAttributes(attributable)")
    protected Object iterateNullAttributtes(RAttributable attributable, String attributeName) {
        if (attributable instanceof RPairList) {
            if (RRuntime.NAMES_ATTR_KEY.startsWith(attributeName)) {
                return ((RPairList) attributable).getNames();
            }
        }
        return RNull.instance;
    }

    @ValueType
    public static final class Context {
        public final Object attributeName;
        public Object result;

        private Context(String attributeName) {
            this.attributeName = attributeName;
        }
    }
}
