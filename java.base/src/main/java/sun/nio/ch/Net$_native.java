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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.NetUtil;
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
        return java.net.NetUtil.ipv6_available();
    }

    private static boolean isReusePortAvailable0() {
        return java.net.NetUtil.reuseport_supported();
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

    private static int getIntOption0(FileDescriptor fd, boolean mayNeedConversion, int level, int opt) throws IOException {
        c_int result = auto();
        struct_linger linger = auto();
        c_char carg = auto();
        void_ptr arg = auto(addr_of(result).cast());
        socklen_t arglen = auto(sizeof(result)).cast();

        if (level == IPPROTO_IP.intValue() &&
                (opt == IP_MULTICAST_TTL.intValue() || opt == IP_MULTICAST_LOOP.intValue())) {
            throw new UnsupportedOperationException("TODO: fix casting of carg in getIntOption0");

            /*
            TODO: This generates invalid LLVM IR from qbicc
            arg = addr_of(carg).cast();
            arglen = sizeof(carg).cast();
            */
        }

        if (level == SOL_SOCKET.intValue() && opt == SO_LINGER.intValue()) {
            throw new UnsupportedOperationException("TODO: fix casting of linger in getIntOption0");
            /*
            TODO: This generates invalid LLVM IR from qbicc
            arg = addr_of(linger).cast();
            arglen = sizeof(linger).cast();
             */
        }

        c_int rc;
        c_int cfd = word(((FileDescriptor$_aliases)(Object)fd).fd);
        if (mayNeedConversion) {
            rc = NetUtil.getSockOpt(cfd, word(level), word(opt), arg, addr_of(arglen).cast());
        } else {
            rc = getsockopt(cfd, word(level), word(opt), arg, addr_of(arglen));
        }
        if (rc.intValue() < 0) {
            throw new SocketException("sun.nio.ch.Net.getIntOption");
        }

        if (level == IPPROTO_IP.intValue() &&
                (opt == IP_MULTICAST_TTL.intValue() || opt == IP_MULTICAST_LOOP.intValue())) {
            return carg.intValue();
        }

        if (level == SOL_SOCKET.intValue() && opt == SO_LINGER.intValue()) {
            return linger.l_onoff.booleanValue() ? linger.l_linger.intValue() : -1;
        }

        return result.intValue();
    }

    private static void setIntOption0(FileDescriptor fd, boolean mayNeedConversion, int jlevel, int jopt, int jarg, boolean isIPv6) throws IOException {
        struct_linger linger = auto();
        c_char carg = auto();
        c_int arg = auto(word(jarg));
        c_int level = word(jlevel);
        c_int opt = word(jopt);

        /* Option value is an int except for a few specific cases */
        const_void_ptr parg = addr_of(arg).cast();
        socklen_t arglen = sizeof(arg).cast();

        if (level == IPPROTO_IP && (opt == IP_MULTICAST_TTL || opt == IP_MULTICAST_LOOP)) {
            parg = addr_of(carg).cast();
            arglen = sizeof(carg).cast();
            carg = arg.cast();
        }

        if (level == SOL_SOCKET && opt == SO_LINGER) {
            parg = addr_of(linger).cast();
            arglen = sizeof(linger).cast();
            if (jarg >= 0) {
                linger.l_onoff = word(1);
                linger.l_linger = arg;
            } else {
                linger.l_onoff = word(0);
                linger.l_linger = word(0);
            }
        }

        c_int rc;
        c_int cfd = word(((FileDescriptor$_aliases)(Object)fd).fd);
        if (mayNeedConversion) {
            rc = NetUtil.setSockOpt(cfd, level, opt, parg, arglen.cast());
        } else {
            rc = setsockopt(cfd, level, opt, parg, arglen);
        }
        if (rc.intValue() < 0) {
            throw new SocketException("sun.nio.ch.Net.setIntOption");
        }
    }
}
