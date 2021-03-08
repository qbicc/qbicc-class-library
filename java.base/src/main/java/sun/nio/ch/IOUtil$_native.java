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

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.Fcntl.*;
import static cc.quarkus.qcc.runtime.posix.Unistd.*;
import static cc.quarkus.qcc.runtime.stdc.Limits.*;

import java.io.IOException;

import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("src/java.base/unix/native/libnio/ch/IOUtil.c")
@Tracking("src/java.base/windows/native/libnio/ch/IOUtil.c")
final class IOUtil$_native {
    private IOUtil$_native() {}

    static void initIDs() {
    }

    static c_int configureBlocking(c_int fd, boolean blocking) {
        c_int flags = fcntl(fd, F_GETFL);
        c_int newFlags = word(blocking ? (flags.intValue() & ~ O_NONBLOCK.intValue()) : (flags.intValue() | O_NONBLOCK.intValue()));
        return (flags == newFlags) ? zero() : fcntl(fd, F_SETFL, newFlags);
    }

    static c_long makePipe(boolean blocking) throws IOException {
        // only called on POSIX.
        c_int[] fd = new c_int[2];
        if (pipe(fd).intValue() < 0) {
            // todo: strerror
            throw new IOException("Pipe failed");
        }
        if (! blocking) {
            if (configureBlocking(fd[0], false).intValue() < 0
                 || configureBlocking(fd[1], false).intValue() < 0) {
                close(fd[0]);
                close(fd[1]);
                // todo: strerror
                throw new IOException("Configure blocking failed");
            }
        }
        return word(fd[0].longValue() << 32 | fd[1].longValue());
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
