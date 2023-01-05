/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.qbicc.runtime.Build;

/**
 * Allows different platforms to call different native methods
 * for read and write operations.
 */
@Tracking("src/java.base/unix/classes/sun/nio/ch/SocketDispatcher.java")
@Tracking("src/java.base/windows/classes/sun/nio/ch/SocketDispatcher.java")
class SocketDispatcher extends NativeDispatcher {
    private static final JavaIOFileDescriptorAccess fdAccess =
            SharedSecrets.getJavaIOFileDescriptorAccess();

    SocketDispatcher() { }

    /**
     * Reads up to len bytes from a socket with special handling for "connection
     * reset".
     *
     * @throws sun.net.ConnectionResetException if connection reset is detected
     * @throws IOException if another I/O error occurs
     */
    int read(FileDescriptor fd, long address, int len) throws IOException {
        return read0(fd, address, len);
    }

    /**
     * Scattering read from a socket into len buffers with special handling for
     * "connection reset".
     *
     * @throws sun.net.ConnectionResetException if connection reset is detected
     * @throws IOException if another I/O error occurs
     */
    long readv(FileDescriptor fd, long address, int len) throws IOException {
        return readv0(fd, address, len);
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        if (Build.Target.isUnix()) {
            return FileDispatcherImpl.write0(fd, address, len);
        } else {
            return write0(fd, address, len);
        }
    }

    long writev(FileDescriptor fd, long address, int len) throws IOException {
        if (Build.Target.isUnix()) {
            return FileDispatcherImpl.writev0(fd, address, len);
        } else {
            return writev0(fd, address, len);
        }
    }

    void close(FileDescriptor fd) throws IOException {
        if (Build.Target.isUnix()) {
            FileDispatcherImpl.close0(fd);
        } else {
            invalidateAndClose(fd);
        }
    }

    static void invalidateAndClose(FileDescriptor fd) throws IOException {
        if (Build.Target.isWindows()) {
            assert fd.valid();
            int fdVal = fdAccess.get(fd);
            fdAccess.set(fd, -1);
            close0(fdVal);
        } else {
            throw new UnsupportedOperationException("invalidateAndClose not part of Unix API for SocketDispatcher");
        }
    }

    void preClose(FileDescriptor fd) throws IOException {
        if (Build.Target.isUnix()) {
            FileDispatcherImpl.preClose0(fd);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    // -- Native methods --

    private static native int read0(FileDescriptor fd, long address, int len)
            throws IOException;

    private static native long readv0(FileDescriptor fd, long address, int len)
            throws IOException;

    private static native int write0(FileDescriptor fd, long address, int len)
            throws IOException;

    private static native long writev0(FileDescriptor fd, long address, int len)
            throws IOException;

    private static native void close0(int fdVal) throws IOException;

    static {
        IOUtil.load();
    }
}
