/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
import static org.qbicc.runtime.posix.NetinetTcp.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.NetinetIn.*;

import java.net.ProtocolFamily;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.util.HashMap;
import java.util.Map;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/share/classes/sun/nio/ch/SocketOptionRegistry.java.template")
public class SocketOptionRegistry {
    private SocketOptionRegistry() { }

    private static class RegistryKey {
        private final SocketOption<?> name;
        private final ProtocolFamily family;
        RegistryKey(SocketOption<?> name, ProtocolFamily family) {
            this.name = name;
            this.family = family;
        }
        public int hashCode() {
            return name.hashCode() + family.hashCode();
        }
        public boolean equals(Object ob) {
            if (ob == null) return false;
            if (!(ob instanceof RegistryKey)) return false;
            RegistryKey other = (RegistryKey)ob;
            if (this.name != other.name) return false;
            if (this.family != other.family) return false;
            return true;
        }
    }

    private static class LazyInitialization {

        static final Map<RegistryKey,OptionKey> options = options();

        private static Map<RegistryKey,OptionKey> options() {
            Map<RegistryKey,OptionKey> map =
                new HashMap<RegistryKey,OptionKey>();
            map.put(new RegistryKey(StandardSocketOptions.SO_BROADCAST,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_BROADCAST.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_KEEPALIVE,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_KEEPALIVE.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_LINGER,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_LINGER.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_SNDBUF,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_SNDBUF.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_RCVBUF,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_RCVBUF.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_REUSEADDR,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_REUSEADDR.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.SO_REUSEPORT,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), defined(SO_REUSEPORT) ? SO_REUSEPORT.intValue() : 0));
            // IPPROTO_TCP is 6
            map.put(new RegistryKey(StandardSocketOptions.TCP_NODELAY,
                Net.UNSPEC), new OptionKey(6, TCP_NODELAY.intValue()));

            // IPPROTO_IP is 0
            map.put(new RegistryKey(StandardSocketOptions.IP_TOS,
                StandardProtocolFamily.INET), new OptionKey(0, defined(IP_TOS) ? IP_TOS.intValue() : 0));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_IF,
                StandardProtocolFamily.INET), new OptionKey(0, defined(IP_MULTICAST_IF) ? IP_MULTICAST_IF.intValue() : 0));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_TTL,
                StandardProtocolFamily.INET), new OptionKey(0, defined(IP_MULTICAST_TTL) ? IP_MULTICAST_TTL.intValue() : 0));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_LOOP,
                StandardProtocolFamily.INET), new OptionKey(0, defined(IP_MULTICAST_LOOP) ? IP_MULTICAST_LOOP.intValue() : 0));

            if (defined(AF_INET6)) {
                // IPPROTO_IPV6 is 41
                map.put(new RegistryKey(StandardSocketOptions.IP_TOS,
                    StandardProtocolFamily.INET6), new OptionKey(41, defined(IPV6_TCLASS) ? IPV6_TCLASS.intValue() : 0));
                map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_IF,
                    StandardProtocolFamily.INET6), new OptionKey(41, IPV6_MULTICAST_IF.intValue()));
                map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_TTL,
                    StandardProtocolFamily.INET6), new OptionKey(41, IPV6_MULTICAST_HOPS.intValue()));
                map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_LOOP,
                    StandardProtocolFamily.INET6), new OptionKey(41, IPV6_MULTICAST_LOOP.intValue()));
            }

            map.put(new RegistryKey(ExtendedSocketOption.SO_OOBINLINE,
                Net.UNSPEC), new OptionKey(SOL_SOCKET.intValue(), SO_OOBINLINE.intValue()));
            return map;
        }
    }

    public static OptionKey findOption(SocketOption<?> name, ProtocolFamily family) {
        RegistryKey key = new RegistryKey(name, family);
        return LazyInitialization.options.get(key);
    }
}
