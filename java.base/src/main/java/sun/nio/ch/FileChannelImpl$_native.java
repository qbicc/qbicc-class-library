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

package sun.nio.ch;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.bsd.SysSocket.*;
import static org.qbicc.runtime.linux.SysSendfile.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;

import java.io.FileDescriptor;
import java.io.IOException;
import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/native/libnio/ch/FileChannelImpl.c")
@Tracking("src/java.base/windows/native/libnio/ch/FileChannelImpl.c")
public class FileChannelImpl$_native {
    public static long initIDs() {
        return 4096; // TODO: This is supposed to be the value of _SC_PAGESIZE
    }

    private static int maxDirectTransferSize0() {
        if (Build.Target.isLinux()) {
            return 0x7ffff000;
        } else if(Build.Target.isWindows()) {
            return Integer.MAX_VALUE-1;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private long transferTo0(FileDescriptor src, long position, long count, FileDescriptor dst) throws IOException {
        c_int srcFD = word(((FileDescriptor$_aliases)(Object)src).fd);
        c_int dstFD = word(((FileDescriptor$_aliases)(Object)dst).fd);

        if (Build.Target.isLinux()) {
            off_t offset = auto(word(position));
            ssize_t n = sendfile(dstFD, srcFD, addr_of(offset), word(count));
            if (n.longValue() < 0) {
                if (errno == EAGAIN.intValue()) {
                    return IOStatus.UNAVAILABLE;
                } else if (errno == EINVAL.intValue() && count >= 0) {
                    return IOStatus.UNSUPPORTED_CASE;
                } else if (errno == EINTR.intValue()) {
                    return IOStatus.INTERRUPTED;
                } else {
                    throw new IOException("Transfer failed");
                }
            }
            return n.longValue();
        } else if (Build.Target.isMacOs()) {
            c_int result;
            off_t numBytes = auto(word(count));
            result = sendfile(srcFD, dstFD, word(position), addr_of(numBytes), word(0), word(0));
            if (numBytes.longValue() > 0) {
                return numBytes.longValue();
            }
            if (result.intValue() == -1) {
                if (errno == EAGAIN.intValue()) {
                    return IOStatus.UNAVAILABLE;
                } else if (errno == EOPNOTSUPP.intValue() || errno == ENOTSOCK.intValue() || errno == ENOTCONN.intValue()) {
                    return IOStatus.UNSUPPORTED_CASE;
                } else if (errno == EINVAL.intValue() && count >= 0) {
                    return IOStatus.UNSUPPORTED_CASE;
                } else if (errno == EINTR.intValue()) {
                    return IOStatus.INTERRUPTED;
                } else {
                    throw new IOException("Transfer failed");
                }
            }
            return result.longValue();
        } else {
            return IOStatus.UNSUPPORTED_CASE;
        }
    }
}
