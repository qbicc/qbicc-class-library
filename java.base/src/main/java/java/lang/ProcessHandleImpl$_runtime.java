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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@Tracking("src/java.base/share/classes/java/lang/ProcessHandleImpl.java")
@PatchClass(java.lang.ProcessHandleImpl.class)
@RunTimeAspect
class ProcessHandleImpl$_runtime {
    private static final ProcessHandleImpl current;
    private static final Executor processReaperExecutor = ProcessHandleImpl$_runtime$runnables.initReaperExecutor();

    static {
        long pid = getCurrentPid0();
        current = (ProcessHandleImpl)(Object)new ProcessHandleImpl$_runtime(pid, isAlive0(pid));
    }

    // alias
    static native long getCurrentPid0();
    static native long isAlive0(long pid);
    private ProcessHandleImpl$_runtime(long pid, long startTime) { }
}

class ProcessHandleImpl$_runtime$runnables {
    static Executor initReaperExecutor() {
        return AccessController.doPrivileged((PrivilegedAction<Executor>) () -> {
            // Initialize ThreadLocalRandom now to avoid using the smaller stack
            // of the processReaper threads.
            ThreadLocalRandom.current();

            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            while (tg.getParent() != null) tg = tg.getParent();
            ThreadGroup systemThreadGroup = tg;

            // For a debug build, the stack shadow zone is larger;
            // Increase the total stack size to avoid potential stack overflow.
            int debugDelta = "release".equals(System.getProperty("jdk.debug")) ? 0 : (4*4096);
            final long stackSize = Boolean.getBoolean("jdk.lang.processReaperUseDefaultStackSize")
                    ? 0 : ProcessHandleImpl$_init.REAPER_DEFAULT_STACKSIZE + debugDelta;

            ThreadFactory threadFactory = grimReaper -> {
                Thread t = new Thread(systemThreadGroup, grimReaper,
                        "process reaper", stackSize, false);
                t.setDaemon(true);
                // A small attempt (probably futile) to avoid priority inversion
                t.setPriority(Thread.MAX_PRIORITY);
                return t;
            };

            return Executors.newCachedThreadPool(threadFactory);
        });
    }
}
