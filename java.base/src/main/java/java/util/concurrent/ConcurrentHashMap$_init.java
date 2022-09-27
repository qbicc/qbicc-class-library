/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 *
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
 *
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 *
 * ------------------
 * 
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */

package java.util.concurrent;

import java.io.ObjectStreamField;
import jdk.internal.misc.Unsafe;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(ConcurrentHashMap.class)
@ReplaceInit
@Tracking("src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java")
public class ConcurrentHashMap$_init {
    private static final long serialVersionUID = 7249069246763182397L;

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private static final int DEFAULT_CAPACITY = 16;

    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private static final float LOAD_FACTOR = 0.75f;

    static final int TREEIFY_THRESHOLD = 8;

    static final int UNTREEIFY_THRESHOLD = 6;

    static final int MIN_TREEIFY_CAPACITY = 64;

    private static final int MIN_TRANSFER_STRIDE = 16;

    private static final int RESIZE_STAMP_BITS = 16;

    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    static final int MOVED     = -1; // hash for forwarding nodes
    static final int TREEBIN   = -2; // hash for roots of trees
    static final int RESERVED  = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("segments", ConcurrentHashMap.Segment[].class),
        new ObjectStreamField("segmentMask", Integer.TYPE),
        new ObjectStreamField("segmentShift", Integer.TYPE),
    };

    // Unsafe mechanics
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long SIZECTL
        = U.objectFieldOffset(ConcurrentHashMap.class, "sizeCtl");
    private static final long TRANSFERINDEX
        = U.objectFieldOffset(ConcurrentHashMap.class, "transferIndex");
    private static final long BASECOUNT
        = U.objectFieldOffset(ConcurrentHashMap.class, "baseCount");
    private static final long CELLSBUSY
        = U.objectFieldOffset(ConcurrentHashMap.class, "cellsBusy");
    private static final long CELLVALUE
        = U.objectFieldOffset(ConcurrentHashMap.CounterCell.class, "value");
    private static final int ABASE = U.arrayBaseOffset(ConcurrentHashMap.Node[].class);
    private static final int ASHIFT;

    static {
        int scale = U.arrayIndexScale(ConcurrentHashMap.Node[].class);
        if ((scale & (scale - 1)) != 0)
            throw new ExceptionInInitializerError("array index scale not a power of two");
        ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    }
}
