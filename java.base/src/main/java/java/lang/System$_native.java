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
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.posix.Time.*;

import java.util.Properties;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libjava/System.c")
@Tracking("src/java.base/share/classes/java/lang/System.java")
public final class System$_native {

    // Temporary manual implementation
    @SuppressWarnings("ManualArrayCopy")
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        if (src == dest && srcPos == destPos) {
            // no operation
            return;
        }
        if (src instanceof Object[] srcArray && dest instanceof Object[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof byte[] srcArray && dest instanceof byte[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof short[] srcArray && dest instanceof short[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof int[] srcArray && dest instanceof int[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof long[] srcArray && dest instanceof long[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof char[] srcArray && dest instanceof char[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof float[] srcArray && dest instanceof float[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof double[] srcArray && dest instanceof double[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else if (src instanceof boolean[] srcArray && dest instanceof boolean[] destArray) {
            if (src == dest && destPos > srcPos) {
                // copy backwards
                for (int i = length - 1; i >= 0; i --) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i ++) {
                    destArray[destPos + i] = srcArray[srcPos + i];
                }
            }
        } else {
            throw new ClassCastException("Invalid array types for copy");
        }
    }

    public static long currentTimeMillis() {
        if (Build.Target.isPosix()) {
            struct_timespec spec = auto();
            clock_gettime(CLOCK_REALTIME, (struct_timespec_ptr) addr_of(spec));
            return spec.tv_sec.longValue() * 1_000L + spec.tv_nsec.longValue() / 1_000_000L;
        } else {
            throw new UnsupportedOperationException("currentTimeMillis");
        }
    }

    public static long nanoTime() {
        if (Build.Target.isPosix()) {
            // todo: check _POSIX_TIMERS / _POSIX_MONOTONIC_CLOCK from <unistd.h> and fall back if needed
            struct_timespec spec = auto();
            clock_gettime(CLOCK_MONOTONIC, (struct_timespec_ptr) addr_of(spec));
            // todo: add nanoTime bias from end of ADD phase
            return spec.tv_sec.longValue() * 1_000_000_000L + spec.tv_nsec.longValue();
        } else {
            throw new UnsupportedOperationException("nanoTime");
        }
    }

    // This has to be an empty method rather than an intrinsic, so that interpreter can intercept it.
    public static Properties initProperties(Properties properties) {
        return properties;
    }

    public static String mapLibraryName(String libname) {
        if (Build.Target.isMacOs()) {
            return "lib" + libname + ".dylib";
        } else {
            return "lib" + libname + ".so";
        }
        // todo: windows
    }

    public static int identityHashCode(Object x) {
        // TODO: obviously non-optimal; replace once we have object headers sorted out
        return 0;
    }

}
