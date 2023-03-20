/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2000, 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
import static jdk.internal.sys.posix.Errno.*;
import static jdk.internal.sys.posix.Fcntl.*;
import static jdk.internal.sys.posix.SysResource.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static jdk.internal.sys.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Limits.*;

import java.io.FileDescriptor;
import java.io.FileDescriptor$_patch;
import java.io.IOException;

import org.qbicc.rt.annotation.Tracking;

/**
 *
 */
@Tracking("src/java.base/unix/native/libnio/ch/IOUtil.c")
@Tracking("src/java.base/windows/native/libnio/ch/IOUtil.c")
final class IOUtil$_native {
    private IOUtil$_native() {}

    static void initIDs() {
    }

    public static void configureBlocking(FileDescriptor jfd, boolean blocking) throws IOException {
        int fd = ((FileDescriptor$_aliases)(Object)jfd).fd;
        c_int rc = IOUtil$_patch.configureBlocking(word(fd), blocking);
        if (rc.intValue() < 0) {
            // todo: strerror
            throw new IOException("configureBlocking failed");
        }
    }

    static long makePipe(boolean blocking) throws IOException {
        // only called on POSIX.
        c_int[] fd = new c_int[2];
        if (pipe(addr_of(fd[0]).asArray()).intValue() < 0) {
            // todo: strerror
            throw new IOException("Pipe failed");
        }
        c_int fd0 = addr_of(fd[0]).loadUnshared();
        c_int fd1 = addr_of(fd[1]).loadUnshared();
        if (! blocking) {
            if (IOUtil$_patch.configureBlocking(fd0, false).intValue() < 0
                 || IOUtil$_patch.configureBlocking(fd1, false).intValue() < 0) {
                close(fd0);
                close(fd1);
                // todo: strerror
                throw new IOException("Configure blocking failed");
            }
        }
        return fd0.longValue() << 32 | fd1.longValue();
    }

    static int write1(int fd, byte b) throws IOException {
        c_char c = auto(word(b));
        ssize_t rc = write(word(fd), addr_of(c).cast(), word(1));
        if (rc.intValue() < 0) {
            if (errno == EAGAIN.intValue() || errno == EWOULDBLOCK.intValue()) {
                return IOStatus.UNAVAILABLE;
            } else if (errno == EINTR.intValue()) {
                return IOStatus.INTERRUPTED;
            } else {
                throw new IOException("Write failed");
            }
        }
        return rc.intValue();
    }

    static boolean drain(int fd) throws IOException {
        int bufSize = 16;
        c_char[] buf = new c_char[bufSize];
        int tn = 0;

        for (;;) {
            ssize_t n = read(word(fd), addr_of(buf[0]).cast(), word(bufSize));
            tn += n.intValue();
            if ((n.intValue() < 0) && (errno != EAGAIN.intValue() && errno != EWOULDBLOCK.intValue())) {
                throw new IOException("Drain");
            }
            if (n.intValue() == bufSize) {
                continue;
            }
            return tn > 0;
        }
    }

    static int drain1(int fd) throws IOException {
        c_char buf = auto();
        int res = read(word(fd), addr_of(buf).cast(), word(1)).intValue();
        if (res < 0) {
            if (errno == EAGAIN.intValue() || errno == EWOULDBLOCK.intValue()) {
                res = 0;
            } else if (errno == EINTR.intValue()) {
                return IOStatus.INTERRUPTED;
            } else {
                throw new IOException("read");
            }
        }
        return res;
    }


    public static int fdVal(FileDescriptor fd) {
        return ((FileDescriptor$_patch)(Object)fd).getFD();
    }

    static void setfdVal(FileDescriptor fd, int value) {
        ((FileDescriptor$_patch)(Object)fd).setFD(value);
    }

    static int fdLimit() throws IOException {
        struct_rlimit rlp = auto();
        if (getrlimit(RLIMIT_NOFILE, addr_of(rlp)).intValue() < 0) {
            throw new IOException("getrlimit failed");
        }
        if (rlp.rlim_max == RLIM_INFINITY || rlp.rlim_max.isGt(word(Integer.MAX_VALUE))) {
            return Integer.MAX_VALUE;
        } else {
            return rlp.rlim_max.intValue();
        }
    }

    static int iovMax() {
        if (defined(IOV_MAX)) {
            // the original version uses sysconf to get the run time value, but we're statically compiled.
            return IOV_MAX.intValue();
        } else {
            // windows perhaps?
            return 16;
        }
    }
}
