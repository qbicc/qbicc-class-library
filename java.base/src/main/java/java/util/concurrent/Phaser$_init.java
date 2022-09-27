/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * ------
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */

package java.util.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(Phaser.class)
@ReplaceInit
@Tracking("src/java.base/share/classes/java/util/concurrent/Phaser.java")
public class Phaser$_init {

    private static final int  MAX_PARTIES     = 0xffff;
    private static final int  MAX_PHASE       = Integer.MAX_VALUE;
    private static final int  PARTIES_SHIFT   = 16;
    private static final int  PHASE_SHIFT     = 32;
    private static final int  UNARRIVED_MASK  = 0xffff;      // to mask ints
    private static final long PARTIES_MASK    = 0xffff0000L; // to mask longs
    private static final long COUNTS_MASK     = 0xffffffffL;
    private static final long TERMINATION_BIT = 1L << 63;
    private static final int  ONE_ARRIVAL     = 1;
    private static final int  ONE_PARTY       = 1 << PARTIES_SHIFT;
    private static final int  ONE_DEREGISTER  = ONE_ARRIVAL|ONE_PARTY;
    private static final int  EMPTY           = 1;

    private static final VarHandle STATE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(Phaser.class, "state", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
