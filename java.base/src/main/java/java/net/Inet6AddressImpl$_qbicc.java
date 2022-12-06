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
import static org.qbicc.runtime.bsd.Ifaddrs.*;
import static org.qbicc.runtime.posix.Netdb.*;
import static org.qbicc.runtime.posix.NetIf.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import java.nio.charset.StandardCharsets;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/native/libnet/Inet6AddressImpl.c")
class Inet6AddressImpl$_qbicc {

    static char_ptr getStringPlatformChars(final String str) throws OutOfMemoryError {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        char_ptr ptr = malloc(word(bytes.length + 1));
        if (ptr.isNull()) {
            throw new OutOfMemoryError("malloc failed");
        }
        copy(ptr.cast(), bytes, 0, bytes.length);
        ptr.asArray()[bytes.length] = zero();
        return ptr;
    }

    static InetAddress[] lookupIfLocalhost(const_char_ptr hostname, boolean includeV6) throws SocketException {
        if (Build.Target.isMacOs()) {
            InetAddress[] result = null;
            c_char[] myhostname = new c_char[256];
            ptr<struct_ifaddrs> ifa = auto();
            c_int familyOrder = word(0);
            int count = 0;
            int addrs4 = 0;
            int addrs6 = 0;
            int numV4Loopbacks = 0;
            int numV6Loopbacks = 0;
            boolean includeLoopback = false;

            /* If the requested name matches this host's hostname, return IP addresses
             * from all attached interfaces. (#2844683 et al) This prevents undesired
             * PPP dialup, but may return addresses that don't actually correspond to
             * the name (if the name actually matches something in DNS etc.
             */
            myhostname[0] = word('\0');
            if (gethostname(addr_of(myhostname[0]), word(myhostname.length)).intValue() == -1) {
                /* Something went wrong, maybe networking is not setup? */
                return null;
            }
            myhostname[myhostname.length-1] = word('\0');

            if (strcmp(addr_of(myhostname[0]), hostname).intValue() != 0) {
                // Non-self lookup
                return null;
            }

            if (getifaddrs(addr_of(ifa).cast()).intValue() != 0) {
                throw new SocketException("Can't get local interface addresses");
            }

            String name = utf8zToJavaString(hostname.cast());

            /* Iterate over the interfaces, and total up the number of IPv4 and IPv6
             * addresses we have. Also keep a count of loopback addresses. We need to
             * exclude them in the normal case, but return them if we don't get an IP
             * address.
             */
            for (struct_ifaddrs_ptr iter = ifa.cast(); !iter.isNull(); iter = addr_of(iter.sel().ifa_next).loadUnshared().cast()) {
                if (!addr_of(iter.sel().ifa_addr).loadUnshared().isNull()) {
                    int family = addr_of(addr_of(iter.sel().ifa_addr).loadUnshared().sel().sa_family).loadUnshared().intValue();
                    if (addr_of(iter.sel().ifa_name).loadUnshared().asArray()[0] != word('\0')) {
                        boolean isLoopback = (addr_of(iter.sel().ifa_flags).loadUnshared().intValue() & IFF_LOOPBACK.intValue()) != 0;
                        if (family == AF_INET.intValue()) {
                            addrs4++;
                            if (isLoopback) numV4Loopbacks++;
                        } else if (family == AF_INET6.intValue() && includeV6) {
                            addrs6++;
                            if (isLoopback) numV6Loopbacks++;
                        } // else we don't care, e.g. AF_LINK
                    }
                }
            }

            if (addrs4 == numV4Loopbacks && addrs6 == numV6Loopbacks) {
                // We don't have a real IP address, just loopback. We need to include
                // loopback in our results.
                includeLoopback = true;
            }

            /* Create and fill the Java array. */
            int arraySize = addrs4 + addrs6 - (includeLoopback ? 0 : (numV4Loopbacks + numV6Loopbacks));
            result = new InetAddress[arraySize];

            int i, j;
            if (InetAddress.preferIPv6Address != 0) {
                i = includeLoopback ? addrs6 : (addrs6 - numV6Loopbacks);
                j = 0;
            } else {
                i = 0;
                j = includeLoopback ? addrs4 : (addrs4 - numV4Loopbacks);
            }

            // Now loop around the ifaddrs
            for (struct_ifaddrs_ptr iter = ifa.cast(); !iter.isNull(); iter = addr_of(iter.sel().ifa_next).loadUnshared().cast()) {
                if (!addr_of(iter.sel().ifa_addr).loadUnshared().isNull()) {
                    int family = addr_of(addr_of(iter.sel().ifa_addr).loadUnshared().sel().sa_family).loadUnshared().intValue();
                    boolean isLoopback = (addr_of(iter.sel().ifa_flags).loadUnshared().intValue() & IFF_LOOPBACK.intValue()) != 0;
                    if (addr_of(iter.sel().ifa_name).loadUnshared().asArray()[0] != word('\0') &&
                            (family == AF_INET.intValue() || (family == AF_INET6.intValue() && includeV6)) &&
                            (!isLoopback || includeLoopback)) {
                        c_int port = auto();
                        int index = (family == AF_INET.intValue()) ? i++ : j++;
                        InetAddress o = NetUtil.sockaddrToInetAddress(addr_of(iter.sel().ifa_addr).loadUnshared().cast(), addr_of(port));
                        o.holder().hostName = name;
                        result[index] = o;
                    }
                }
            }

            freeifaddrs(ifa.cast());
            return result;
        } else {
            return null;
        }
    }

    static InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        if (host == null) {
            throw new NullPointerException("host argument is null");
        }

        InetAddress[] ret = null;
        struct_addrinfo_ptr res = auto();
        struct_addrinfo_ptr resNew = word(0);
        struct_addrinfo_ptr last = word(0);
        struct_addrinfo_ptr iterator = word(0);
        const_char_ptr hostname = getStringPlatformChars(host).cast();

        try {
            struct_addrinfo hints = auto();
            memset(addr_of(hints).cast(), word(0), sizeof(hints));
            hints.ai_flags = AI_CANONNAME;
            hints.ai_family = AF_UNSPEC;
            c_int error = getaddrinfo(hostname, word(0), addr_of(hints).cast(), addr_of(res));
            if (error.intValue() != 0) {
                if (Build.Target.isMacOs()) {
                    try {
                        ret = lookupIfLocalhost(hostname, true);
                        if (ret != null) {
                            return ret;
                        }
                    } catch (SocketException e) {
                        // JDK supresses SocketException in favor of UnknownHostException
                    }
                }
                // TODO: strerror in detailed message.
                throw new UnknownHostException(host);
            } else {
                // TODO: This else branch corresponds to a hunk of about 120 lines in Inet6AddressImpl.c
                //       that needs to be ported and tested by someone with easy access to a non-MacOS POSIX system.
                throw new UnsupportedOperationException("TODO: non-MacOS impl of InetAddress.lookupAllHostAddr");
            }
        } finally {
            // cleanup native memory
            free(hostname);
            while (!resNew.isNull()) {
                struct_addrinfo_ptr toFree = resNew;
                resNew = addr_of(resNew.sel().ai_next).loadUnshared().cast();
                free(toFree);
            }
            if (!res.isNull()) {
                freeaddrinfo(res);
            }
        }
    }
}
