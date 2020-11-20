package sun.nio.ch;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.NetinetTcp.*;
import static cc.quarkus.qcc.runtime.posix.SysSocket.*;
import static cc.quarkus.qcc.runtime.posix.NetinetIn.*;

import java.net.ProtocolFamily;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("java.base/share/classes/sun/nio/ch/SocketOptionRegistry.java.template")
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
                StandardProtocolFamily.INET), new OptionKey(0, IP_TOS.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_IF,
                StandardProtocolFamily.INET), new OptionKey(0, IP_MULTICAST_IF.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_TTL,
                StandardProtocolFamily.INET), new OptionKey(0, IP_MULTICAST_TTL.intValue()));
            map.put(new RegistryKey(StandardSocketOptions.IP_MULTICAST_LOOP,
                StandardProtocolFamily.INET), new OptionKey(0, IP_MULTICAST_LOOP.intValue()));

            if (defined(AF_INET6)) {
                // IPPROTO_IPV6 is 41
                map.put(new RegistryKey(StandardSocketOptions.IP_TOS,
                    StandardProtocolFamily.INET6), new OptionKey(41, IPV6_TCLASS.intValue()));
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
