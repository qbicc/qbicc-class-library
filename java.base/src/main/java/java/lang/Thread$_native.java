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

import static org.qbicc.runtime.posix.Sched.sched_yield;

import java.util.concurrent.locks.LockSupport;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libjava/Thread.c")
@Tracking("src/java.base/share/classes/java/lang/Thread.java")
class Thread$_native {
    private static void registerNatives() {
        // no operation
    }

    public final boolean isAlive() {
        return (((Thread$_patch) (Object) this).threadStatus & Thread$_patch.STATE_ALIVE) != 0;
    }

    public static void yield() {
        if (Build.Target.isPosix()) {
            sched_yield();
        }
        // else no operation
    }

    public static void sleep(long remaining) throws InterruptedException {
        // see Thread$_patch#sleep(long,int)
        Thread.sleep(remaining, 0);
    }

    // TODO: private native void start0();

    public static boolean holdsLock(Object obj) {
        return ((Object$_aliases) obj).holdsLock();
    }

    // TODO: private static native StackTraceElement[][] dumpThreads(Thread[] threads);
    // TODO: private static native Thread[] getThreads();

    private void setPriority0(int newPriority) {
        // no operation
    }

    private void stop0(Object o) {
        throw new UnsupportedOperationException();
    }

    private void suspend0() {
        throw new UnsupportedOperationException();
    }

    private void resume0() {
        throw new UnsupportedOperationException();
    }

    private void interrupt0() {
        // unpark the thread so it can observe the interruption
        LockSupport.unpark((Thread) (Object) this);
    }

    private static void clearInterruptEvent() {
        // no operation
    }

    private void setNativeName(String name) {
        // no operation
    }
}
