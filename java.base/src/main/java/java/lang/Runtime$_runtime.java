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
import static org.qbicc.runtime.bsd.SysSysctl.*;
import static org.qbicc.runtime.linux.Unistd.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;


@PatchClass(Runtime.class)
@RunTimeAspect
@Tracking("src/java.base/share/classes/java/lang/Runtime.java")
class Runtime$_runtime {
    @NoReflect
    @Add
    static final int CONFIGURED_CPUS;

    static {
        // set up configured CPUs
        int configuredCpus;
        if (Build.Target.isLinux()) {
            configuredCpus = sysconf(_SC_NPROCESSORS_CONF).intValue();
        } else if (Build.Target.isMacOs()) {
            c_int[] mib = new c_int[2];
            mib[0] = CTL_HW;
            mib[1] = HW_NCPU;
            c_int cpu_val = auto();
            size_t len = sizeof(cpu_val);
            c_int result = sysctl(addr_of(mib).cast(), word(2), addr_of(cpu_val), addr_of(len), zero(), zero());
            if (result.isGe(zero()) && cpu_val.isGt(word(1))) {
                configuredCpus = result.intValue();
            } else {
                configuredCpus = 1;
            }
        } else {
            // no idea really
            configuredCpus = 1;
        }
        CONFIGURED_CPUS = configuredCpus;
    }
}
