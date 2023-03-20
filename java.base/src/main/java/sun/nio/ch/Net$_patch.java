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
import static jdk.internal.sys.posix.Errno.*;
import static jdk.internal.sys.posix.SysSocket.*;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(Net.class)
@Tracking("src/java.base/unix/native/libnio/ch/Net.c")
class Net$_patch {

    @Add
    static int handleSocketError(int errorValue) throws IOException {
        if (errorValue == EINPROGRESS.intValue()) {
            return 0;
        } else if (errorValue == EPROTO.intValue()) {
            // TODO: strerror
            throw new ProtocolException();
        } else if (errorValue == ECONNREFUSED.intValue() ||
                    errorValue == ETIMEDOUT.intValue() ||
                    errorValue == ENOTCONN.intValue()) {
            // TODO: strerror
            throw new ConnectException();
        } else if (errorValue == EHOSTUNREACH.intValue()) {
            // TODO: strerror
            throw new NoRouteToHostException();
        } else if (errorValue == EADDRINUSE.intValue() ||
                    errorValue == EADDRNOTAVAIL.intValue() ||
                    errorValue == EACCES.intValue()) {
            // TODO: strerror
            throw new BindException();
        } else {
            // TODO: strerror
            throw new SocketException();
        }
    }
}