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
import static org.qbicc.runtime.posix.NetinetIn.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;

import java.io.IOException;
import java.net.SocketException;
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

    /*
     * Returns 1 for Windows and -1 for Linux/Mac OS
     */
    static int isExclusiveBindAvailable() {
        if (Build.Target.isWindows()) {
            return 1;
        } else if (Build.Target.isLinux() || Build.Target.isMacOs()) {
            return -1;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean isIPv6Available0() {
        // TODO: Figure this out for real
        return false;
    }

    private static int socket0(boolean preferIPv6, boolean stream, boolean reuse,
                               boolean fastLoopback) throws IOException {
        if (Build.Target.isPosix()) {
            c_int fd;
            c_int type = stream ? SOCK_STREAM : SOCK_DGRAM;
            c_int domain = (isIPv6Available0() && preferIPv6) ? AF_INET6 : AF_INET;

            fd = socket(domain, type, word(0));
            if (fd.intValue() < 0) {
                return Net$_patch.handleSocketError(errno);
            }

            if (domain == AF_INET6 && true /* TODO!! should be ipv4_available() */) {
                c_int arg = auto(word(0));
                c_int rc = setsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, addr_of(arg).cast(), sizeof(arg).cast());
                if (rc.intValue() < 0) {
                    close(fd);
                    throw new SocketException("Unable to set IPV6_V6ONLY");
                }
            }

            if (reuse) {
                c_int arg = auto(word(1));
                c_int rc = setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, addr_of(arg).cast(), sizeof(arg).cast());
                if (rc.intValue() < 0) {
                    close(fd);
                    throw new SocketException("Unable to set SO_REUSEADDR");
                }
            }

            if (Build.Target.isLinux()) {
                if (type == SOCK_DGRAM) {
                    throw new UnsupportedOperationException("Finish SOCK_DRAM for linux");
                    /* TODO: define IP_MULTICAST_ALL in linux CNative
                    c_int arg = auto(word(0));
                    c_int level = (domain == AF_INET6) ? IPPROTO_IPV6 : IPPROTO_IP;
                    c_int rc = setsockopt(fd, level, IP_MULTICAST_ALL, addr_of(arg).cast(), sizeof(arg).cast());
                    if (rc.intValue() < 0 && errno != ENOPROTOOPT.intValue()) {
                        close(fd);
                        throw new SocketException("Unable to set IP_MULTICAST_ALL");
                    }
                    */
                }

                if (domain == AF_INET6 && type == SOCK_DGRAM) {
                    c_int arg = auto(word(1));
                    c_int rc = setsockopt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, addr_of(arg).cast(), sizeof(arg).cast());
                    if (rc.intValue() < 0) {
                        close(fd);
                        throw new SocketException("Unable to set IPV6_MULTICAST_HOPS");
                    }
                }
            }

            if (Build.Target.isMacOs()) {
                /*
                 * Attempt to set SO_SNDBUF to a minimum size to allow sending large datagrams
                 * (net.inet.udp.maxdgram defaults to 9216).
                 */
                if (type == SOCK_DGRAM) {
                    c_int size = auto();
                    socklen_t arglen = auto(sizeof(size).cast());
                    c_int rc = getsockopt(fd, SOL_SOCKET, SO_SNDBUF, addr_of(size).cast(), addr_of(arglen));
                    if (rc.intValue() == 0) {
                        c_int minSize = auto((domain == AF_INET6) ? word(65527)  : word(65507));
                        if (size.intValue() < minSize.intValue()) {
                            setsockopt(fd, SOL_SOCKET, SO_SNDBUF, addr_of(minSize).cast(), sizeof(minSize).cast());
                        }
                    }
                }

            }
            return fd.intValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
