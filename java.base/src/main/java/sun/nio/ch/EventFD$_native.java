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
import static org.qbicc.runtime.linux.EventFD.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import java.io.IOException;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/linux/native/libnio/ch/EventFD.c")
class EventFD$_native {

    private static int eventfd0() throws IOException {
        int efd = eventfd(word(0), word(0)).intValue();
        if (efd == -1) {
            throw new IOException("eventfd failed");
        }
        return efd;
    }

    private static int set0(int efd) throws IOException {
        uint64_t one = auto(word(1));
        int rv = write(word(efd), addr_of(one).cast(), sizeof(one)).intValue();
        if (rv >= 0) {
            return rv;
        } else if (errno == EAGAIN.intValue() || errno == EWOULDBLOCK.intValue()) {
            return IOStatus.UNAVAILABLE;
        } else if (errno == EINTR.intValue()) {
            return IOStatus.INTERRUPTED;
        } else {
            throw new IOException("Write failed");
        }
    }
}
