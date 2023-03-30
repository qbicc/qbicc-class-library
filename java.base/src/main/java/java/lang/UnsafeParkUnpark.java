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

package java.lang;

import jdk.internal.misc.Unsafe;
import jdk.internal.thread.ThreadNative;
import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 * Unsafe support for park/unpark.
 */
@PatchClass(Unsafe.class)
@Tracking("src/java.base/share/classes/jdk/internal/misc/Unsafe.java")
final class UnsafeParkUnpark {

    @Replace
    void park(boolean isAbsolute, long time) {
        if (isAbsolute) {
            // absolute timeout in milliseconds
            long millis = time - System.currentTimeMillis();
            if (millis <= 0) {
                return;
            }
            ThreadNative.park(millis, 0, ThreadNative.STATE_PARKED | ThreadNative.STATE_WAITING | ThreadNative.STATE_WAITING_WITH_TIMEOUT, ThreadNative.STATE_RUNNABLE, ThreadNative.STATE_INTERRUPTED, ThreadNative.STATE_UNPARK);
        } else if (time == 0) {
            // no timeout
            ThreadNative.park(0, 0, ThreadNative.STATE_PARKED | ThreadNative.STATE_WAITING | ThreadNative.STATE_WAITING_INDEFINITELY, ThreadNative.STATE_RUNNABLE, ThreadNative.STATE_INTERRUPTED, ThreadNative.STATE_UNPARK);
        } else {
            // relative timeout in nanos
            int nanos = (int) (time % 1_000_000);
            long millis = time / 1_000_000;
            ThreadNative.park(millis, nanos, ThreadNative.STATE_PARKED | ThreadNative.STATE_WAITING | ThreadNative.STATE_WAITING_WITH_TIMEOUT, ThreadNative.STATE_RUNNABLE, ThreadNative.STATE_INTERRUPTED, ThreadNative.STATE_UNPARK);
        }
    }

    @Replace
    void unpark(Object thread) {
        ((Thread) thread).unpark(ThreadNative.STATE_UNPARK);
    }
}
