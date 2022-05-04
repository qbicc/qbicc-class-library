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

import java.io.FileDescriptor;
import java.io.IOException;

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;
import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/windows/classes/sun/nio/ch/FileKey.java")
@Tracking("src/java.base/windows/native/libnio/ch/FileKey.c")
final class WindowsFileKey extends FileKey {
    private final long dwVolumeSerialNumber;
    private final long nFileIndexHigh;
    private final long nFileIndexLow;

    private static final JavaIOFileDescriptorAccess fdAccess =
            SharedSecrets.getJavaIOFileDescriptorAccess();

    WindowsFileKey(final FileDescriptor fd) throws IOException {
        throw new UnsupportedOperationException();
        // HANDLE fileHandle = (HANDLE)(fdAccess.getHandle(fd)));
        // BOOL result;
        // BY_HANDLE_FILE_INFORMATION fileInfo = zero();
        // result = GetFileInformationByHandle(fileHandle, addr_of(fileInfo));
        // if (! result) {
        //     throw new IOException("GetFileInformationByHandle failed");
        // }
        // this.dwVolumeSerialNumber = fileInfo.dwVolumeSerialNumber;
        // this.nFileIndexHigh = fileInfo.nFileIndexHigh;
        // this.nFileIndexLow = fileInfo.nFileIndexLow;
    }

    public int hashCode() {
        return Long.hashCode(dwVolumeSerialNumber) + Long.hashCode(nFileIndexHigh) + Long.hashCode(nFileIndexLow);
    }

    public boolean equals(final Object obj) {
        return obj instanceof WindowsFileKey wfk && equals(wfk);
    }

    boolean equals(final WindowsFileKey other) {
        return this == other || other != null && dwVolumeSerialNumber == other.dwVolumeSerialNumber
            && nFileIndexHigh == other.nFileIndexHigh && nFileIndexLow == other.nFileIndexLow;
    }
}
