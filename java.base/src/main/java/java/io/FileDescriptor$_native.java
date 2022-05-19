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
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.Fcntl.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.errno;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.host.HostIO;

@Tracking("src/java.base/unix/native/libjava/FileDescriptor_md.c")
@Tracking("src/java.base/windows/native/libjava/FileDescriptor_md.c")
public class FileDescriptor$_native {
    int fd;
    boolean append;

    private static long getHandle(int fd) {
        if (Build.Target.isPosix()) {
            return -1;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean getAppend(int fd) {
        if (Build.isHost()) {
            return HostIO.isAppend(fd);
        } else if (Build.Target.isPosix()) {
            c_int res = fcntl(word(fd), F_GETFL);
            return (res.intValue() & O_APPEND.intValue()) != 0;
        } else {
            return false;
        }
    }

    private void close0() throws IOException {
        int fd = this.fd;
        if (fd == -1) {
            return;
        }
        if (Build.isHost()) {
            this.fd = -1;
            if (0 <= fd && fd <= 2) {
                // stdin, stdout, or stderr... redirect to `/dev/null` in the same manner as OpenJDK
                HostIO.reopen(fd, "/dev/null", fd == 0 ? HostIO.O_RDONLY : HostIO.O_WRONLY);
            } else {
                HostIO.close(fd);
            }
        } else if (Build.Target.isPosix()) {
            this.fd = -1;
            if (0 <= fd && fd <= 2) {
                // stdin, stdout, or stderr... redirect to `/dev/null` in the same manner as OpenJDK
                c_int res = open(utf8z("/dev/null"), O_WRONLY);
                if (res.isLt(zero())) {
                    throw new IOException("open /dev/null failed");
                }
                dup2(res, word(fd));
                close(res);
            }
            c_int res;
            if (Build.Target.isAix()) {
                do {
                    res = close(word(fd));
                } while (res.intValue() == -1 && errno == EINTR.intValue());
                if (res.intValue() == -1) {
                    // todo: safe strerror...
                    throw new IOException("close failed");
                }
            } else {
                res = close(word(fd));
                if (res.intValue() == -1 && errno != EINTR.intValue()) {
                    // todo: safe strerror...
                    throw new IOException("close failed");
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void initIDs() {
        // no operation
    }

    public void sync() throws SyncFailedException {
        if (Build.isHost()) {
            try {
                HostIO.fsync(fd);
            } catch (IOException e) {
                throw new SyncFailedException(e.getMessage());
            }
        } else if (Build.Target.isPosix()) {
            c_int res = fsync(word(fd));
            if (res.intValue() == -1) {
                throw new SyncFailedException("sync failed");
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
