package jdk.internal.sys.bsd;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysSocket.*;

@include("<ifaddrs.h>")
public class Ifaddrs {
    public static class struct_ifaddrs extends object {
        public ptr<struct_ifaddrs> ifa_next;
        public ptr<c_char> ifa_name;
        public unsigned_int ifa_flags;
        public ptr<struct_sockaddr> ifa_addr;
        public ptr<struct_sockaddr> ifa_netmask;
        public ptr<struct_sockaddr> ifa_dstaddr;
        public ptr<?> ifa_data;
    }

    public static native c_int getifaddrs(ptr<ptr<struct_ifaddrs>> x);
    public static native void freeifaddrs(ptr<struct_ifaddrs> x);
}
