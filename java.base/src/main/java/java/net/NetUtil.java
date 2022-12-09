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

package java.net;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.ArpaInet.*;
import static org.qbicc.runtime.posix.NetinetIn.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libnet/net_util.c")
@Tracking("src/java.base/unix/native/libnet/net_util_md.c")
@Tracking("src/java.base/unix/native/libnet/SocketImpl.c")
@Tracking("src/java.base/windows/native/libnet/net_util_md.c")
@Tracking("src/java.base/windows/native/libnet/SocketImpl.c")
public class NetUtil {
    private static boolean IPV4Available;
    private static boolean IPV4AvailableComputed;

    private static boolean IPV6Available;
    private static boolean IPV6AvailableComputed;

    public static boolean reuseport_supported() {
        if (Build.Target.isWindows()) {
            return false;
        } else if (Build.Target.isMacOs() || Build.Target.isLinux()) {
            return true;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static boolean ipv4_available() {
        if (IPV4AvailableComputed) {
            return IPV4Available;
        }

        if (Build.Target.isPosix()) {
            c_int fd = socket(AF_INET, SOCK_STREAM, word(0)) ;
            if (fd.intValue() < 0) {
                IPV4Available = false;
            } else {
                IPV4Available = true;
                close(fd);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        IPV4AvailableComputed = true;
        return IPV4Available;
    }

    public static boolean ipv6_available() {
        if (IPV6AvailableComputed) {
            return IPV6Available;
        }

        boolean v6Avail = false;
        // TODO: Here is where we would execute code ported from ipv6_available() in net_util.md
        //       and compute a real platform-dependent value for v6Avail;

        IPV6Available = v6Avail && !Boolean.getBoolean("java.net.preferIPv4Stack");
        IPV6AvailableComputed = true;
        return IPV6Available;
    }

    // NET_IsIPv4Mapped
    static boolean isIPv4Mapped(ptr<uint8_t> caddr) {
        for (int i = 0; i < 10; i++) {
            if (caddr.get(i).byteValue() != 0x00) {
                return false;
            }
        }

        return ((caddr.get(10).byteValue() & 0xff) == 0xff) && ((caddr.get(11).byteValue() & 0xff) == 0xff);
    }

    // NET_IPv4MappedToIPv4
    static int IPv4MappedToIPv4(ptr<uint8_t> caddr) {
        return ((caddr.get(12).byteValue() & 0xff) << 24) | ((caddr.get(13).byteValue() & 0xff) << 16)
                | ((caddr.get(14).byteValue() & 0xff) << 8) | (caddr.get(15).byteValue() & 0xff);
    }

    // NET_SockaddrToInetAddress
    static InetAddress sockaddrToInetAddress(/*SOCKADDRESS* */void_ptr sa, ptr<c_int> port) {
        InetAddress iaObj;
        c_int family = addr_of(sa.cast(struct_sockaddr_ptr.class).sel().sa_family).loadUnshared().cast();
        if (family == AF_INET6) {
            struct_sockaddr_in6_ptr sa6 = sa.cast();
            ptr<uint8_t> caddr = addr_of(sa6.sel().sin6_addr.s6_addr[0]);
            if (isIPv4Mapped(caddr)) {
                iaObj = new Inet4Address();
                iaObj.holder().family = InetAddress.IPv4;
                iaObj.holder().address = IPv4MappedToIPv4(caddr);
            } else {
                iaObj = new Inet6Address();
                Inet6Address$_patch ia6Obj = (Inet6Address$_patch)(Object)iaObj;
                iaObj.holder().family = InetAddress.IPv6;
                ia6Obj.setInet6Address_ipaddress(caddr);
                ia6Obj.setInet6Address_scopeid(addr_of(sa6.sel().sin6_scope_id).loadUnshared().intValue());
            }
            port.storeUnshared(ntohs(addr_of(sa6.sel().sin6_port).loadUnshared().cast()).cast());
        } else {
            struct_sockaddr_in_ptr sa4 = sa.cast();
            iaObj = new Inet4Address();
            iaObj.holder().family = InetAddress.IPv4;
            unsigned_int addr = ntohl(addr_of(sa4.sel().sin_addr.s_addr).loadUnshared().cast()).cast();
            iaObj.holder().address = addr.intValue();
            port.storeUnshared(ntohs(addr_of(sa4.sel().sin_port).loadUnshared().cast()).cast());
        }

        return iaObj;
    }
}
