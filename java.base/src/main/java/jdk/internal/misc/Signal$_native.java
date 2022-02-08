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

package jdk.internal.misc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libjava/Signal.c")
@Tracking("src/hotspot/os/posix/signals_posix.cpp")
@Tracking("src/hotspot/os/windows/os_windows.cpp")
class Signal$_native {

    private static int findSignal0(String sigName) {
        if (Build.Target.isPosix()) {
            return switch(sigName) {
                case "ABRT" -> org.qbicc.runtime.stdc.Signal.SIGABRT.intValue();
                case "ALRM" -> org.qbicc.runtime.posix.Signal.SIGALRM.intValue();
                case "FPE" -> org.qbicc.runtime.stdc.Signal.SIGFPE.intValue();
                case "HUP" -> org.qbicc.runtime.posix.Signal.SIGHUP.intValue();
                case "ILL" -> org.qbicc.runtime.stdc.Signal.SIGILL.intValue();
                case "INT" -> org.qbicc.runtime.stdc.Signal.SIGINT.intValue();
                case "KILL" -> org.qbicc.runtime.posix.Signal.SIGKILL.intValue();
                case "PIPE" -> org.qbicc.runtime.posix.Signal.SIGPIPE.intValue();
                case "QUIT" -> org.qbicc.runtime.posix.Signal.SIGQUIT.intValue();
                case "SEGV" -> org.qbicc.runtime.stdc.Signal.SIGSEGV.intValue();
                case "TERM" ->  org.qbicc.runtime.stdc.Signal.SIGTERM.intValue();
                default -> -1;
            };
        } else if (Build.Target.isWindows()) {
            return switch(sigName) {
                case "ABRT" -> org.qbicc.runtime.stdc.Signal.SIGABRT.intValue();
                case "BREAK" -> 21; //  Proper symbol + c-compiler lookup for this on windows.
                case "FPE" -> org.qbicc.runtime.stdc.Signal.SIGFPE.intValue();
                case "ILL" -> org.qbicc.runtime.stdc.Signal.SIGILL.intValue();
                case "INT" -> org.qbicc.runtime.stdc.Signal.SIGINT.intValue();
                case "SEGV" -> org.qbicc.runtime.stdc.Signal.SIGSEGV.intValue();
                case "TERM" -> org.qbicc.runtime.stdc.Signal.SIGTERM.intValue();
                default -> -1;
            };
        } else {
            return -1;
        }
    }

    private static long handle0(int sig, long nativeH) {
        // TODO: Real implementation that actually updates a data structure that the hypothetical master signal handler uses.
        return 0; // pretend old handler was default handler
    }
}
