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
import static jdk.internal.sys.linux.Stdlib.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import java.lang.reflect.Field;
import java.lang.reflect.Field$_patch;
import java.security.ProtectionDomain;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.main.CompilerIntrinsics;

@Tracking("src/java.base/share/classes/jdk/internal/misc/Unsafe.java")
final class Unsafe$_native {

    private static void registerNatives() {
        // no-op
    }

    int getLoadAverage0(double[] loadavg, int nelems) {
        if (Build.Target.isLinux()) {
            _Float64[] values = new _Float64[3];
            int n = nelems < 3 ? nelems : 3;
            int res = getloadavg(addr_of(values[0]), word(n)).intValue();
            for (int i=0; i<n; i++) {
                loadavg[i] = values[i].doubleValue();
            }
            return res;
        }
        return 0;
    }

    public void throwException(Throwable ee) throws Throwable {
        throw ee;
    }

    public Class<?> defineClass0(String name, byte[] b, int off, int len,
                                 ClassLoader loader,
                                 ProtectionDomain protectionDomain) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    private Class<?> defineAnonymousClass0(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    @SuppressWarnings("ConstantConditions")
    private Unsafe asUnsafe() {
        return (Unsafe) (Object) this;
    }

    public Object allocateInstance(Class<?> clazz) throws InstantiationException {
        return CompilerIntrinsics.emitNew(clazz);
    }

    private long allocateMemory0(long bytes) {
        return malloc(word(bytes).cast()).longValue();
    }

    private long reallocateMemory0(long address, long bytes) {
        return realloc(word(address).cast(), word(bytes).cast()).longValue();
    }

    private void freeMemory0(long address) {
        free(word(address).cast());
    }

    private void setMemory0(Object o, long offset, long bytes, byte value) {
        ptr<c_char> oPtr = refToPtr(o).cast();
        ptr<c_char> destPtr = oPtr.plus(offset);
        if (Build.isHost()) {
            // no set on the host
            for (long i = 0; i < bytes; i ++) {
                destPtr.plus(i).storePlain(word(value));
            }
        } else {
            memset(destPtr.cast(), word(value).cast(), word(bytes).cast());
        }
    }

    private void copyMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        ptr<c_char> srcBasePtr = refToPtr(srcBase).cast();
        ptr<c_char> destBasePtr = refToPtr(destBase).cast();
        ptr<c_char> srcPtr = srcBasePtr.plus(srcOffset);
        ptr<c_char> destPtr = destBasePtr.plus(destOffset);
        if (Build.isHost()) {
            // no memmove on the host
            if (srcBase == destBase && destOffset > srcOffset) {
                // have to copy backwards
                for (long i = bytes - 1; i >= 0; i --) {
                    destPtr.plus(i).storePlain(srcPtr.plus(i).loadPlain());
                }
            } else {
                for (long i = 0; i < bytes; i ++) {
                    destPtr.plus(i).storePlain(srcPtr.plus(i).loadPlain());
                }
            }
        } else {
            memmove(destPtr.cast(), srcPtr.cast(), word(bytes).cast());
        }
    }

    private void copySwapMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes, long elemSize) {
        if (elemSize > 16 || Long.bitCount(elemSize) != 1) {
            throw new IllegalArgumentException();
        }
        boolean reverse = srcBase == destBase && destOffset > srcOffset;
        switch ((int) elemSize) {
            case 1 -> copyMemory0(srcBase, srcOffset, destBase, destOffset, bytes);
            case 2 -> {
                ptr<c_char> srcBasePtr2 = refToPtr(srcBase).cast();
                ptr<c_char> destBasePtr2 = refToPtr(destBase).cast();
                ptr<int16_t> srcPtr2 = srcBasePtr2.plus(srcOffset).cast();
                ptr<int16_t> destPtr2 = destBasePtr2.plus(destOffset).cast();
                long cnt2 = bytes >> 1;
                if (reverse) {
                    for (long i2 = cnt2 - 1; i2 >= 0; i2 --) {
                        destPtr2.plus(i2).storePlain(word(Short.reverseBytes(srcPtr2.plus(i2).loadPlain().shortValue())));
                    }
                } else {
                    for (long i2b = 0; i2b < cnt2; i2b ++) {
                        destPtr2.plus(i2b).storePlain(word(Short.reverseBytes(srcPtr2.plus(i2b).loadPlain().shortValue())));
                    }
                }
            }
            case 4 -> {
                ptr<c_char> srcBasePtr4 = refToPtr(srcBase).cast();
                ptr<c_char> destBasePtr4 = refToPtr(destBase).cast();
                ptr<int32_t> srcPtr4 = srcBasePtr4.plus(srcOffset).cast();
                ptr<int32_t> destPtr4 = destBasePtr4.plus(destOffset).cast();
                long cnt4 = bytes >> 2;
                if (reverse) {
                    for (long i4 = cnt4 - 1; i4 >= 0; i4 --) {
                        destPtr4.plus(i4).storePlain(word(Integer.reverseBytes(srcPtr4.plus(i4).loadPlain().intValue())));
                    }
                } else {
                    for (long i4b = 0; i4b < cnt4; i4b ++) {
                        destPtr4.plus(i4b).storePlain(word(Integer.reverseBytes(srcPtr4.plus(i4b).loadPlain().intValue())));
                    }
                }
            }
            case 8 -> {
                ptr<c_char> srcBasePtr8 = refToPtr(srcBase).cast();
                ptr<c_char> destBasePtr8 = refToPtr(destBase).cast();
                ptr<int64_t> srcPtr8 = srcBasePtr8.plus(srcOffset).cast();
                ptr<int64_t> destPtr8 = destBasePtr8.plus(destOffset).cast();
                long cnt8 = bytes >> 3;
                if (reverse) {
                    for (long i8 = cnt8 - 1; i8 >= 0; i8 --) {
                        destPtr8.plus(i8).storePlain(word(Long.reverseBytes(srcPtr8.plus(i8).loadPlain().longValue())));
                    }
                } else {
                    for (long i8b = 0; i8b < cnt8; i8b ++) {
                        destPtr8.plus(i8b).storePlain(word(Long.reverseBytes(srcPtr8.plus(i8b).loadPlain().longValue())));
                    }
                }
            }
            case 16 -> {
                ptr<c_char> srcBasePtr16 = refToPtr(srcBase).cast();
                ptr<c_char> destBasePtr16 = refToPtr(destBase).cast();
                ptr<int64_t> srcPtr16 = srcBasePtr16.plus(srcOffset).cast();
                ptr<int64_t> destPtr16 = destBasePtr16.plus(destOffset).cast();
                long cnt16 = bytes >> 3;
                if (reverse) {
                    for (long i16 = cnt16 - 2; i16 >= 0; i16 -= 2) {
                        long a = srcPtr16.plus(i16).loadPlain().longValue();
                        long b = srcPtr16.plus(i16 + 1).loadPlain().longValue();
                        destPtr16.plus(i16).storePlain(word(Long.reverseBytes(b)));
                        destPtr16.plus(i16 + 1).storePlain(word(Long.reverseBytes(a)));
                    }
                } else {
                    for (long i16b = 0; i16b < cnt16; i16b += 2) {
                        long c = srcPtr16.plus(i16b).loadPlain().longValue();
                        long d = srcPtr16.plus(i16b + 1).loadPlain().longValue();
                        destPtr16.plus(i16b).storePlain(word(Long.reverseBytes(d)));
                        destPtr16.plus(i16b + 1).storePlain(word(Long.reverseBytes(c)));
                    }
                }
            }
        }
    }

    private long objectFieldOffset0(Field f) {
        return ((Field$_patch)(Object)f).getOffset();
    }

    private long staticFieldOffset0(Field f) {
        return ((Field$_patch)(Object)f).getOffset();
    }

    //TODO:

    //unpark
    //park
}
