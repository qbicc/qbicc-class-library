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
import static org.qbicc.runtime.bsd.SysEvent.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.Time.*;

import java.io.IOException;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/macos/native/libnio/ch/KQueue.c")
class KQueue$_native {

    private static int keventSize() {
        return sizeof(struct_kevent.class).intValue();
    }

    private static int identOffset() {
        struct_kevent ke = auto();
        return offsetof(ke.ident).intValue();
    }

    private static int filterOffset() {
        struct_kevent ke = auto();
        return offsetof(ke.filter).intValue();
    }

    private static int flagsOffset() {
        struct_kevent ke = auto();
        return offsetof(ke.flags).intValue();
    }

    static int create() throws IOException {
        c_int kqfd = kqueue();
        if (kqfd.intValue() < 0) {
            // todo: strerror
            throw new IOException("kqueue failed");
        }
        return kqfd.intValue();
    }

    static int register(int kqfd, int fd, int filter, int flags) {
        struct_kevent changes = auto();
        changes.ident = word(kqfd);
        changes.filter = word(filter);
        changes.flags = word(flags);
        c_int result;
        do {
            result = kevent(word(kqfd), addr_of(changes), word(1), word(0), word(0), word(0));
        } while (result.intValue() == -1 && errno == EINTR.intValue());
        return result.intValue() == -1 ? errno : 0;
    }

    static int poll(int kqfd, long address, int nevents, long timeout) throws IOException {
        struct_kevent_ptr events = word(address);
        struct_timespec ts = auto();
        struct_timespec_ptr tsp = auto();

        if (timeout >= 0) {
            ts.tv_sec = word(timeout/1000);
            ts.tv_nsec = word((timeout % 1000) * 1000000);
            tsp = addr_of(ts);
        }

        c_int res = kevent(word(kqfd), word(0), word(0), events, word(nevents), tsp.cast());
        if (res.intValue() < 0) {
            if (errno == EINTR.intValue()) {
                return IOStatus.INTERRUPTED;
            } else {
                // todo: strerror
                throw new IOException("kqueue failed");
            }
        }

        return res.intValue();
    }
}
