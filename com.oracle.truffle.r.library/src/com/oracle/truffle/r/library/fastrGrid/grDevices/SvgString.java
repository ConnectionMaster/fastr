/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.SVGDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;

/**
 * FastR specific external used to implement {@code svg.string}, which returns the SVG code of
 * current drawing.
 */
public class SvgString extends RExternalBuiltinNode.Arg0 {
    @TruffleBoundary
    @Override
    public Object execute() {
        GridContext ctx = GridContext.getContext();
        if (ctx.getCurrentDeviceIndex() <= 0) {
            throw error(Message.GENERIC, "No device opened.");
        }
        GridDevice dev = ctx.getCurrentDevice();
        if (!(dev instanceof SVGDevice)) {
            throw error(Message.GENERIC, "No SVG device opened, use svg() to open one.");
        }
        return ((SVGDevice) dev).getContents();
    }
}
