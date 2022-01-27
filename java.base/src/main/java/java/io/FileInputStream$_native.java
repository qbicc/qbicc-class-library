/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.Math.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Fcntl.*;

import org.qbicc.rt.annotation.Tracking;

@SuppressWarnings({ "unused", "ConstantConditions" })
@Tracking("src/java.base/share/native/libjava/FileInputStream.c")
class FileInputStream$_native {
    private FileDescriptor fd;

    private void open0(String name) throws FileNotFoundException {
        IO_Util.fileOpen(fd, name, O_RDONLY);
    }

    private int read0() throws IOException {
        return IO_Util.readSingle(fd);
    }

    private int readBytes(byte[] b, int off, int len) throws IOException {
        return IO_Util.readBytes(fd, b, off, len);
    }

    private long length0() throws IOException {
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        long length;
        if ((length = IO_Util.IO_GetLength(this.fd)) == -1) {
            // todo: JNU_ThrowIOExceptionWithLastError
            throw new IOException("GetLength failed");
        }
        return length;
    }

    private long position0() throws IOException {
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        long length;
        if ((length = IO_Util.handleLseek(fd, 0L, SEEK_CUR)) == -1) {
            // todo: JNU_ThrowIOExceptionWithLastError
            throw new IOException("GetLength failed");
        }
        return length;
    }

    private long skip0(long n) throws IOException {
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        long cur, end;
        if ((cur = IO_Util.handleLseek(fd, 0, SEEK_CUR)) == -1 || (end = IO_Util.handleLseek(fd, n, SEEK_CUR)) == -1) {
            // todo: JNU_ThrowIOExceptionWithLastError
            throw new IOException("Seek error");
        }
        return end - cur;
    }

    private int available0() throws IOException {
        c_long ret = auto();
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (! IO_Util.handleAvailable(fd, addr_of(ret))) {
            // todo: JNU_ThrowIOExceptionWithLastError
            throw new IOException();
        }
        return (int) min(Integer.MAX_VALUE, max(0, ret.longValue()));
    }

    private static void initIDs() {
        // no operation
    }
}
