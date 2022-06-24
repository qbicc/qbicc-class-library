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

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.posix.Poll;

@Tracking("src/java.base/unix/native/libnio/ch/Net.c")
class Net$_native {

    private static void initIDs() {
    }

    static short pollinValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLIN.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static short polloutValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLOUT.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static short pollerrValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLERR.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static short pollhupValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLHUP.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static short pollnvalValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLNVAL.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static short pollconnValue() {
        if (Build.Target.isPosix()) {
            return Poll.POLLOUT.shortValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
