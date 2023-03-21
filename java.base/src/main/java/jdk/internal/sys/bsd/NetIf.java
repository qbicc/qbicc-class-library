package jdk.internal.sys.bsd;

import static org.qbicc.runtime.CNative.*;

@include("<net/if.h>")
public class NetIf {
    public static native unsigned_int if_nametoindex(ptr<@c_const c_char> ifname);
}
