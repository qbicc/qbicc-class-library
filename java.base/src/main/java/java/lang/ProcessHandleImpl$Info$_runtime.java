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
import static jdk.internal.sys.posix.Stdio.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static jdk.internal.sys.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.RunTimeAspect;

@Patch("java.lang.ProcessHandleImpl$Info")
@Tracking("src/java.base/linux/native/libjava/ProcessHandleImpl_linux.c")
@RunTimeAspect
class ProcessHandleImpl$Info$_runtime {
    @Add(when = Build.Target.IsLinux.class)
    static long clock_ticks_per_second;
    @Add(when = Build.Target.IsLinux.class)
    static int pageSize;
    @Add(when = Build.Target.IsLinux.class)
    static long bootTime_ms;

    static {
        if (Build.Target.isLinux()) {
            linux_runtimeInit();
        }
    }

    @Add(when = Build.Target.IsLinux.class)
    private static void linux_runtimeInit() {
        clock_ticks_per_second = sysconf(_SC_CLK_TCK).longValue();
        pageSize = sysconf(_SC_PAGE_SIZE).intValue();

        /* Read the boottime from /proc/stat. */
        ptr<FILE> fp = fopen(utf8z("/proc/stat"), utf8z("r"));
        if (fp.isNull()) {
            bootTime_ms = -1;
        } else {
            ptr<c_char> line = auto();
            size_t len = auto();
            int64_t bootTime = auto();
            const_char_ptr scanStr = utf8z("btime %llu");
            while (getline(addr_of(line), addr_of(len), fp).intValue() != -1) {
                if (sscanf(line.cast(), scanStr, addr_of(bootTime)).intValue() == 1) {
                    break;
                }
            }
            free(line);
            fclose(fp.cast());
            bootTime_ms = bootTime.longValue() * 1000;
        }
    }
}