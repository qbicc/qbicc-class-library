/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.CharBuffer;

import org.qbicc.runtime.Build;
import org.qbicc.rt.annotation.Tracking;
import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;
import sun.security.action.GetPropertyAction;

@Tracking("src/java.base/unix/classes/sun/nio/ch/FileDispatcherImpl.java")
@Tracking("src/java.base/unix/native/libnio/ch/FileDispatcherImpl.c")
@Tracking("src/java.base/windows/classes/sun/nio/ch/FileDispatcherImpl.java")
class FileDispatcherImpl extends FileDispatcher {

    static {
        IOUtil.load();
        if (Build.Target.isWindows()) {
            fastFileTransfer = isFastFileTransferRequested();
        } else {
            fastFileTransfer = false;
        }
    }

    private static final JavaIOFileDescriptorAccess fdAccess =
            SharedSecrets.getJavaIOFileDescriptorAccess();

    // set to true if fast file transmission (TransmitFile) is enabled
    private static final boolean fastFileTransfer;

    FileDispatcherImpl() {
    }

    int read(FileDescriptor fd, long address, int len) throws IOException {
        return read0(fd, address, len);
    }

    @Override
    boolean needsPositionLock() {
        return Build.Target.isWindows();
    }

    int pread(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        return pread0(fd, address, len, position);
    }

    long readv(FileDescriptor fd, long address, int len) throws IOException {
        return readv0(fd, address, len);
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        return write0(fd, address, len);
    }

    int pwrite(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        return pwrite0(fd, address, len, position);
    }

    long writev(FileDescriptor fd, long address, int len)
        throws IOException
    {
        return writev0(fd, address, len);
    }

    long seek(FileDescriptor fd, long offset) throws IOException {
        return seek0(fd, offset);
    }

    int force(FileDescriptor fd, boolean metaData) throws IOException {
        return force0(fd, metaData);
    }

    int truncate(FileDescriptor fd, long size) throws IOException {
        return truncate0(fd, size);
    }

    long size(FileDescriptor fd) throws IOException {
        return size0(fd);
    }

    int lock(FileDescriptor fd, boolean blocking, long pos, long size,
             boolean shared) throws IOException
    {
        return lock0(fd, blocking, pos, size, shared);
    }

    void release(FileDescriptor fd, long pos, long size) throws IOException {
        release0(fd, pos, size);
    }

    void close(FileDescriptor fd) throws IOException {
        fdAccess.close(fd);
    }

    void preClose(FileDescriptor fd) throws IOException {
        preClose0(fd);
    }

    FileDescriptor duplicateForMapping(FileDescriptor fd) throws IOException {
        if (Build.Target.isWindows()) {
            // on Windows we need to keep a handle to the file
            FileDescriptor result = new FileDescriptor();
            long handle = duplicateHandle(fdAccess.getHandle(fd));
            fdAccess.setHandle(result, handle);
            fdAccess.registerCleanup(result);
            return result;
        } else {
            // file descriptor not required for mapping operations; okay
            // to return invalid file descriptor.
            return new FileDescriptor();
        }
    }

    boolean canTransferToDirectly(java.nio.channels.SelectableChannel sc) {
        return ! Build.Target.isWindows() || fastFileTransfer && sc.isBlocking();
    }

    boolean transferToDirectlyNeedsPositionLock() {
        return Build.Target.isWindows();
    }

    int setDirectIO(FileDescriptor fd, String path) {
        int result;
        if (Build.Target.isWindows()) {
            String filePath = path.substring(0, path.lastIndexOf(File.separator));
            CharBuffer buffer = CharBuffer.allocate(filePath.length());
            buffer.put(filePath);
            try {
                result = setDirect0(fd, buffer);
            } catch (IOException e) {
                throw new UnsupportedOperationException
                    ("Error setting up DirectIO", e);
            }
        } else {
            try {
                result = setDirect0(fd);
            } catch (IOException e) {
                throw new UnsupportedOperationException
                    ("Error setting up DirectIO", e);
            }
        }
        return result;
    }

    // -- Native methods --

    static native int read0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pread0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long readv0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int write0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pwrite0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native int force0(FileDescriptor fd, boolean metaData)
        throws IOException;

    static native long seek0(FileDescriptor fd, long offset)
        throws IOException;

    static native int truncate0(FileDescriptor fd, long size)
        throws IOException;

    static native long size0(FileDescriptor fd) throws IOException;

    static native int lock0(FileDescriptor fd, boolean blocking, long pos,
                            long size, boolean shared) throws IOException;

    static native void release0(FileDescriptor fd, long pos, long size)
        throws IOException;

    // Shared with SocketDispatcher and DatagramDispatcher but
    // NOT used by FileDispatcherImpl
    static native void close0(FileDescriptor fd) throws IOException;

    static native void closeIntFD(int fd) throws IOException;

    static native int setDirect0(FileDescriptor fd) throws IOException;

    // UNIX-specific

    static native void preClose0(FileDescriptor fd) throws IOException;

    static native long writev0(FileDescriptor fd, long address, int len)
        throws IOException;

    // Windows-specific

    static boolean isFastFileTransferRequested() {
        String fileTransferProp = GetPropertyAction
                .privilegedGetProperty("jdk.nio.enableFastFileTransfer", "false");
        return fileTransferProp.isEmpty() || Boolean.parseBoolean(fileTransferProp);
    }

    static native long writev0(FileDescriptor fd, long address, int len, boolean append)
        throws IOException;

    static native long duplicateHandle(long fd) throws IOException;

    static native int setDirect0(FileDescriptor fd, CharBuffer buffer) throws IOException;

}
