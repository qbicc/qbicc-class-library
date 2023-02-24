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
import static org.qbicc.runtime.bsd.SysSysctl.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.ArpaInet.*;
import static org.qbicc.runtime.posix.NetinetIn.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.posix.SysSocket;

@Tracking("src/java.base/share/native/libnet/net_util.c")
@Tracking("src/java.base/unix/native/libnet/net_util_md.c")
@Tracking("src/java.base/unix/native/libnet/SocketImpl.c")
@Tracking("src/java.base/windows/native/libnet/net_util_md.c")
@Tracking("src/java.base/windows/native/libnet/SocketImpl.c")
class NetUtil {
    private static boolean IPV4Available;
    private static boolean IPV4AvailableComputed;

    private static boolean IPV6Available;
    private static boolean IPV6AvailableComputed;

    static boolean reuseport_supported() {
        if (Build.Target.isWindows()) {
            return false;
        } else if (Build.Target.isMacOs() || Build.Target.isLinux()) {
            return true;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static boolean ipv4_available() {
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

    static boolean ipv6_available() {
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

    // NET_GetPortFromSockaddr
    static c_int getPortFromSockaddr(/*SOCKETADDRESS* */ void_ptr sa) {
        c_int family = sa.cast(struct_sockaddr_ptr.class).sel().sa_family.cast();
        if (family == AF_INET6) {
            struct_sockaddr_in6_ptr sa6 = sa.cast();
            return ntohs(sa6.sel().sin6_port.cast()).cast();
        } else {
            struct_sockaddr_in_ptr sa4 = sa.cast();
            return ntohs(sa4.sel().sin_port.cast()).cast();
        }
    }

    // NET_InetAddressToSockaddr
    static c_int inetAddressToSockaddr(InetAddress iaObj, int port, /*SOCKETADDRESS* */ void_ptr sa,
                                       ptr<c_int> len, boolean v4MappedAddress) throws SocketException {
        int family = iaObj.holder().family;

        if (ipv6_available()) {
            memset(sa.cast(), word(0), sizeof(struct_sockaddr_in6.class));
            throw new UnsupportedOperationException("TODO: Need to port IPv6 code path from unix/net_util_md.c");
        } else {
            if (family != InetAddress.IPv4) {
                throw new SocketException("Protocol family unavailable");
            }
            memset(sa.cast(), word(0), sizeof(struct_sockaddr_in.class));
            int address = iaObj.holder().address;
            sa.cast(struct_sockaddr_in_ptr.class).sel().sin_port = htons(word(port)).cast();
            sa.cast(struct_sockaddr_in_ptr.class).sel().sin_addr.s_addr = htonl(word(address)).cast();
            sa.cast(struct_sockaddr_in_ptr.class).sel().sin_family = AF_INET.cast();
            if (!len.isNull()) {
                len.storeUnshared(sizeof(struct_sockaddr_in.class).cast());
            }
        }
        return word(0);
    }

    // NET_SockaddrToInetAddress
    static InetAddress sockaddrToInetAddress(/*SOCKADDRESS* */void_ptr sa, ptr<c_int> port) {
        InetAddress iaObj;
        c_int family = sa.cast(struct_sockaddr_ptr.class).sel().sa_family.cast();
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
                ia6Obj.setInet6Address_scopeid(sa6.sel().sin6_scope_id.intValue());
            }
            port.storeUnshared(ntohs(sa6.sel().sin6_port.cast()).cast());
        } else {
            struct_sockaddr_in_ptr sa4 = sa.cast();
            iaObj = new Inet4Address();
            iaObj.holder().family = InetAddress.IPv4;
            unsigned_int addr = ntohl(sa4.sel().sin_addr.s_addr.cast()).cast();
            iaObj.holder().address = addr.intValue();
            port.storeUnshared(ntohs(sa4.sel().sin_port.cast()).cast());
        }

        return iaObj;
    }

    // NET_GetSockOpt
    static c_int getSockOpt(c_int fd, c_int level, c_int opt, void_ptr result, ptr<c_int> len) {
        socklen_t socklen = auto(len.loadUnshared().cast());
        c_int rv = getsockopt(fd, level, opt, result, addr_of(socklen));
        len.storeUnshared(socklen.cast());

        if (rv.intValue() < 0) {
            return rv;
        }

        if (Build.Target.isLinux()) {
            if ((level == SOL_SOCKET) && ((opt == SO_SNDBUF) || (opt == SO_RCVBUF))) {
                c_int n = result.loadUnshared(c_int.class);
                n = word(n.intValue() / 2);
                result.cast(int_ptr.class).storeUnshared(n);
            }
        }

        if (Build.Target.isMacOs()) {
            if (level == SOL_SOCKET && opt == SO_LINGER) {
                ptr<struct_linger> to_cast = result.cast();
                c_int tmp = to_cast.sel().l_linger;
                unsigned_short tmp2 = tmp.cast();
                to_cast.sel().l_linger = tmp2.cast();
            }
        }

        return rv;
    }

    // NET_SetSockOpt
    static c_int setSockOpt(c_int fd, c_int level, c_int  opt, const_void_ptr arg, c_int len) {
        if (level == IPPROTO_IP && opt == IP_TOS) {
            if (Build.Target.isLinux() && ipv6_available()) {
                throw new UnsupportedOperationException("TODO: finish Linux port of setSockOpt");
                // Commented out: missing constants in qbicc runtime headers...
                /*
                c_int optval = auto(word(1));
                if (setsockopt(fd, IPPROTO_IPV6, IPV6FLOW_INFO_SEND, addr_of(optval).cast(), sizeof(optval)).intValue() < 0) {
                    return -1;
                }
                if (setsockopt(fd, IPPROTO_IPV6, IPV6_TCLASS, arg, len).intValue() < 0) {
                    return -1;
                }

                 */
            }

            ptr<c_int> iptos = arg.cast();
            int iptos_tos_mask = defined(IPTOS_TOS_MASK) ? IPTOS_TOS_MASK.intValue() : 0x1e;
            int iptos_prec_mask = defined(IPTOS_PREC_MASK) ? IPTOS_PREC_MASK.intValue() : 0xe0;
            iptos.storeUnshared(word(iptos.loadUnshared().intValue() & (iptos_tos_mask | iptos_prec_mask)));
        }

        if (Build.Target.isLinux() && level == SOL_SOCKET && opt == SO_RCVBUF) {
            ptr<c_int> bufsize = arg.cast();
            if (bufsize.loadUnshared().intValue() < 1024) {
                bufsize.storeUnshared(word(1024));
            }
        }

        if (Build.Target.isMacOs() && level == SOL_SOCKET && (opt == SO_SNDBUF || opt == SO_RCVBUF)) {
            c_int[] mib = new c_int[] { CTL_KERN, KERN_IPC, KIPC_MAXSOCKBUF };
            c_int maxsockbuf = auto(word(-1));
            size_t rlen = auto(sizeof(maxsockbuf));
            if (sysctl(addr_of(mib[0]), word(3), addr_of(maxsockbuf), addr_of(rlen), word(0), word(0)).intValue() == -1) {
                maxsockbuf = word(1024);
            }
            maxsockbuf = word((maxsockbuf.intValue()/5)*4);

            ptr<c_int> bufsize = arg.cast();
            if (bufsize.loadUnshared().isGt(maxsockbuf)) {
                bufsize.storeUnshared(maxsockbuf);
            }
            if (opt == SO_RCVBUF && bufsize.loadUnshared().intValue() < 1024) {
                bufsize.storeUnshared(word(1024));
            }
        }

        if (Build.Target.isMacOs() && level == SOL_SOCKET && opt == SO_REUSEADDR) {
            c_int sotype = auto();
            socklen_t arglen = auto(sizeof(sotype).cast());
            if (getsockopt(fd, SOL_SOCKET, SO_TYPE, addr_of(sotype).cast(), addr_of(arglen)).intValue() < 0) {
                return word(-1);
            }
            if (sotype == SOCK_DGRAM) {
                setsockopt(fd, level, SO_REUSEPORT, arg, len.cast());
            }
        }

        return setsockopt(fd, level, opt, arg, len.cast());
    }

    // NET_Bind
    static c_int bind(c_int fd, /*SOCKETADDRESS* */ void_ptr sa, c_int len) {
        if (Build.Target.isLinux()) {
            c_int family = sa.cast(struct_sockaddr_ptr.class).sel().sa_family.cast();
            if (family == AF_INET) {
                struct_sockaddr_in_ptr sa_in = sa.cast(struct_sockaddr_in_ptr.class);
                if ((ntohl(sa_in.sel().sin_addr.s_addr.cast()).intValue() & 0x7f0000ff) == 0x7f0000ff) {
                    errno = EADDRNOTAVAIL.intValue();
                    return word(-1);
                }
            }
        }
        return SysSocket.bind(fd, sa.cast(const_struct_sockaddr_ptr.class), len.cast(socklen_t.class));
    }

}
