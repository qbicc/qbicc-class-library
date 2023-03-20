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
import static jdk.internal.sys.linux.EPoll.*;
import static jdk.internal.sys.posix.Errno.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import java.io.IOException;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/linux/native/libnio/ch/Epoll.c")
class EPoll$_native {

    private static int eventSize() {
        return sizeof(struct_epoll_event.class).intValue();
    }

    private static int eventsOffset() {
        struct_epoll_event ev = auto();
        return offsetof(ev.events).intValue();
    }

    private static int dataOffset() {
        struct_epoll_event ev = auto();
        return offsetof(ev.data).intValue();
    }

    static int create() throws IOException {
        int epfd = epoll_create1(EPOLL_CLOEXEC).intValue();
        if (epfd < 0) {
            throw new IOException("epoll_create1 failed");
        }
        return epfd;
    }

    static int ctl(int epfd, int opcode, int fd, int events) {
        struct_epoll_event event = auto();
        event.events = word(events);
        event.data.fd = word(fd);

        c_int res = epoll_ctl(word(epfd), word(opcode), word(fd), addr_of(event));
        return (res.intValue() == 0) ? 0 : errno;
    }

    static int wait(int epfd, long pollAddress, int numfds, int timeout) throws IOException {
        struct_epoll_event_ptr events = word(pollAddress);
        int res = epoll_wait(word(epfd), events, word(numfds), word(timeout)).intValue();
        if (res < 0) {
            if (errno == EINTR.intValue()) {
                return IOStatus.INTERRUPTED;
            } else {
                throw new IOException("epoll_wait failed");
            }
        }
        return res;
    }
}
