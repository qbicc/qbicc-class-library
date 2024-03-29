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

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.linux.Unistd.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static jdk.internal.sys.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;
import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/aix/native/libjava/ProcessHandleImpl_aix.c")
@Tracking("src/java.base/linux/native/libjava/ProcessHandleImpl_linux.c")
@Tracking("src/java.base/macosx/native/libjava/ProcessHandleImpl_macosx.c")
@Tracking("src/java.base/unix/native/libjava/ProcessHandleImpl_unix.c")
@Tracking("src/java.base/windows/native/libjava/ProcessHandleImpl_win.c")
public class ProcessHandleImpl$_native {

    private static long getCurrentPid0() {
        if (Build.Target.isPosix()) {
            return getpid().longValue();
        } else {
            throw new UnsupportedOperationException("getCurrentPid0");
        }
    }

    private static long isAlive0(long jpid) {
        pid_t pid = word(jpid);
        long totalTime = auto(-1L);
        long startTime = auto(-1L);
        pid_t ppid = ProcessHandleImpl$Info$_patch.getParentPidAndTimings(pid,  addr_of(totalTime), addr_of(startTime));
        return ppid.intValue() < 0 ? -1 : startTime;
    }
}
