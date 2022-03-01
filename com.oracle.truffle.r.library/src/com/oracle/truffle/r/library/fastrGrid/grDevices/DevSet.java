/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.grDevices;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.graphics.RGridGraphicsAdapter;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;

public abstract class DevSet extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(DevSet.class);
        casts.arg(0).mustBe(numericValue()).asIntegerVector().findFirst();
    }

    public static DevSet create() {
        return DevSetNodeGen.create();
    }

    @Specialization
    public int doInteger(int deviceIdx) {
        RContext rCtx = getRContext();
        RGridGraphicsAdapter.fixupDevicesVariable(rCtx);
        GridContext gridCtx = GridContext.getContext(rCtx);
        if (deviceIdx <= 0 || deviceIdx > gridCtx.getDevicesSize()) {
            throw error(Message.GENERIC, "dev.set with number of non-existing device is not supported in FastR.");
        }
        gridCtx.setCurrentDevice(deviceIdx - 1);
        return deviceIdx;
    }

}
