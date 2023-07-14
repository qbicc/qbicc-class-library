package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@include(value = "<sys/uio.h>")
public final class SysUio {
    public static class struct_iovec extends object {
        public ptr<?> iov_base;
        public size_t iov_len;
    }

    public static native ssize_t readv(c_int fd, ptr<@c_const struct_iovec> iov, c_int iov_cnt);

    public static native ssize_t writev(c_int fd, ptr<@c_const struct_iovec> iov, c_int iov_cnt);
}
