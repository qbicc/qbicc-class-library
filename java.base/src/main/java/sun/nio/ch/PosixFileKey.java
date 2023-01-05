/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.SysStat.*;

import java.io.FileDescriptor;
import java.io.IOException;

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;
import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/unix/classes/sun/nio/ch/FileKey.java")
@Tracking("src/java.base/unix/native/libnio/ch/FileKey.c")
public final class PosixFileKey extends FileKey {
    private static final JavaIOFileDescriptorAccess fdAccess =
            SharedSecrets.getJavaIOFileDescriptorAccess();

    private final long st_dev;    // ID of device
    private final long st_ino;    // Inode number

    PosixFileKey(final FileDescriptor fd) throws IOException {
        struct_stat stat = auto();
        c_int res;
        do {
            res = fstat(word(fdAccess.get(fd)), addr_of(stat));
        } while (res == EAGAIN);
        if (res.isNonZero()) {
            // todo: errno
            throw new IOException("fstat failed");
        }
        this.st_dev = stat.st_dev.longValue();
        this.st_ino = stat.st_ino.longValue();
    }

    public int hashCode() {
        return Long.hashCode(st_dev) + Long.hashCode(st_ino);
    }

    public boolean equals(final Object obj) {
        return obj instanceof PosixFileKey pfk && equals(pfk);
    }

    boolean equals(final PosixFileKey other) {
        return this == other || other != null && st_dev == other.st_dev && st_ino == other.st_ino;
    }
}
