package sun.nio.ch;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.patcher.Patch;

@Patch("java.net.NetUtil")
class NetUtil$_aliases {
    // alias to make java.net.NetUtil methods accessible in this package
    static native boolean reuseport_supported();
    static native boolean ipv6_available();
    static native c_int getSockOpt(c_int fd, c_int level, c_int opt, void_ptr result, ptr<c_int> len);
    static native c_int setSockOpt(c_int fd, c_int level, c_int opt, const_void_ptr arg, c_int len);
}
