/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.lang.ref.Cleaner.Cleanable;
import jdk.internal.misc.Unsafe;
import jdk.internal.ref.CleanerFactory;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(NativeBuffer.class)
@Tracking("src/java.base/share/classes/sun/nio/fs/NativeBuffer.java")
class NativeBuffer$_patch {
    // alias
    private static Unsafe unsafe;
    private final long address;
    private final int size;
    private final Cleanable cleanable;

    @Replace
    NativeBuffer$_patch(int size) {
        this.address = unsafe.allocateMemory(size);
        this.size = size;
        if (Build.isHost()) {
            // No need for a cleanable.  There are two cases:
            //   1. The NativeBuffer is not serialized, so everything is just Java objects on the host JVM heap
            //   2. The NativeBuffer is serialized, so the native memory becomes a part of the initial heap,
            //       which means it is not malloced memory, so it is not valid to call free on it.
            this.cleanable = null;
        } else {
            this.cleanable = CleanerFactory.cleaner().register(this, new NativeBuffer$_patch$Deallocator(address));
        }
    }

    @Replace
    void free() {
        if (cleanable != null) {
            cleanable.clean();
        }
    }
}
