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
import static org.qbicc.runtime.posix.Time.*;
import static org.qbicc.runtime.stdc.Time.*;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/unix/native/libjava/VM_md.c")
@Tracking("src/java.base/windows/native/libjava/VM_md.c")
@Tracking("src/java.base/share/native/libjava/VM.c")
@Tracking("src/java.base/share/classes/jdk/internal/misc/VM.java")
@Tracking("src/hotspot/share/prims/jvm.cpp") // getNanoTimeAdjustment
public class VM$_native {
    private static void initialize() {
        // no-op
    }

    private static void initializeFromArchive(Class ignored) {
        // no-op
    }

    public static long getNanoTimeAdjustment(long offsetInSeconds) {
        // os_posix.cpp javaTimeSystemUTC
        struct_timespec timespec = auto();
        clock_gettime(CLOCK_REALTIME, addr_of(timespec));
        long seconds = timespec.tv_sec.longValue();
        long nanos = timespec.tv_nsec.longValue();

        // jvm.cpp getNanoTimeAdjustment
        long MAX_DIFF_SECS = 0x0100000000L; // 2^32
        long MIN_DIFF_SECS = -MAX_DIFF_SECS;
        long diff = seconds - offsetInSeconds;
        if (diff >= MAX_DIFF_SECS || diff <= MIN_DIFF_SECS) {
            return -1; // sentinel value: the offset is too far off the target
        }
        return (diff * 1000000000L) + nanos;
    }
}
