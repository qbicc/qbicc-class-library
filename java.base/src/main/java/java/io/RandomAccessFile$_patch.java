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
package java.io;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.Fcntl.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import jdk.internal.sys.posix.Fcntl;

@Tracking("src/java.base/share/native/libjava/RandomAccessFile.c")
@Tracking("src/java.base/share/classes/java/io/RandomAccessFile.java")
@PatchClass(RandomAccessFile.class)
public class RandomAccessFile$_patch {

    // Alias
    private static int O_RDONLY;
    private static int O_RDWR;
    private static int O_SYNC;
    private static int O_DSYNC;

    // Alias
    private FileDescriptor fd;

    @Replace
    private static void initIDs() {
        // no operation
    }

    @Replace
    private void open0(String path, int mode) throws FileNotFoundException {
        int flags = 0;
        if ((mode & O_RDONLY) != 0) {
            flags = Fcntl.O_RDONLY.intValue();
        } else if ((mode & O_RDWR) != 0) {
            flags = Fcntl.O_RDWR.intValue() | Fcntl.O_CREAT.intValue();
            if ((mode & O_SYNC) != 0) {
                flags |= Fcntl.O_SYNC.intValue();
            } else if ((mode & O_DSYNC) != 0) {
                flags |= Fcntl.O_DSYNC.intValue();
            }
        }

        IO_Util.fileOpen(this.fd, path, word(flags));
    }

    @Replace
    private int read0() throws IOException {
        return IO_Util.readSingle(fd);
    }

    @Replace
    private int readBytes(byte b[], int off, int len) throws IOException {
        return IO_Util.readBytes(fd, b, off, len);
    }

    @Replace
    private void write0(int b) throws IOException {
        IO_Util.writeSingle(fd, (byte)b, false);
    }

    @Replace
    private void writeBytes(byte b[], int off, int len) throws IOException {
        IO_Util.writeBytes(fd, b, off, len, false);
    }

    @Replace
    public long getFilePointer() throws IOException {
        if (((FileDescriptor$_native) (Object) fd).fd == -1) {
            throw new IOException("Stream Closed");
        }

        long ret = IO_Util.handleLseek(fd, 0L, SEEK_CUR);
        if (ret == -1) {
            throw new IOException("Seek failed");
        }
        return ret;
    }

    @Replace
    private void seek0(long pos) throws IOException {
        if (((FileDescriptor$_native) (Object) fd).fd == -1) {
            throw new IOException("Stream Closed");
        }

        if (pos < 0L) {
            throw new IOException("Negative seek offset");
        } else if (IO_Util.handleLseek(fd, pos, SEEK_SET) == -1) {
            throw new IOException("Seek failed");
        }

    }

    @Replace
    public long length() throws IOException {
        if (((FileDescriptor$_native) (Object) fd).fd == -1) {
            throw new IOException("Stream Closed");
        }

        long length = IO_Util.IO_GetLength(fd);
        if (length == -1) {
            throw new IOException("GetLength failed");
        }
        return length;
    }
}
