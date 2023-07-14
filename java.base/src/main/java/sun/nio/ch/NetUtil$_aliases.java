package sun.nio.ch;

import static org.qbicc.runtime.CNative.*;

import java.net.InetAddress;
import java.net.SocketException;

import org.qbicc.runtime.patcher.Patch;

@Patch("java.net.NetUtil")
// aliases to make java.net.NetUtil methods accessible in this package
class NetUtil$_aliases {
    static native boolean reuseport_supported();
    static native boolean ipv6_available();
    static native c_int getSockOpt(c_int fd, c_int level, c_int opt, ptr<?> result, ptr<c_int> len);
    static native c_int setSockOpt(c_int fd, c_int level, c_int opt, ptr<@c_const ?> arg, c_int len);
    static native c_int getPortFromSockaddr(/*SOCKETADDRESS* */ ptr<?> sa);
    static native c_int inetAddressToSockaddr(InetAddress iaObj, int port, /*SOCKETADDRESS* */ ptr<?> sa,
                                              ptr<c_int> len, boolean v4MappedAddress) throws SocketException;
    static native InetAddress sockaddrToInetAddress(/*SOCKADDRESS* */ptr<?> sa, ptr<c_int> port);
    static native c_int bind(c_int fd, /*SOCKETADDRESS* */ ptr<?> sa, c_int len);
}
