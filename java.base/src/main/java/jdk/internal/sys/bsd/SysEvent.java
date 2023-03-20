package jdk.internal.sys.bsd;



import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.CNative.*;

@include("<sys/event.h>")
public class SysEvent {
    public static final class struct_kevent extends object {
        public uintptr_t ident;
        public int16_t filter;
        public uint16_t flags;
        public uint32_t fflags;
        public intptr_t data;
        public ptr<?> udata;
    }

    public static native c_int kqueue();

    public static native c_int kevent(c_int kq, ptr<struct_kevent> changelist, c_int nchanges, ptr<struct_kevent> eventlist, c_int nevents, ptr<@c_const struct_timespec> timeout);
}
