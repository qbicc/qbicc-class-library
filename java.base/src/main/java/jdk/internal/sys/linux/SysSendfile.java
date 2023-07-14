package jdk.internal.sys.linux;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<sys/sendfile.h>")
public class SysSendfile {
    public static native ssize_t sendfile(c_int out_fd, c_int in_fd, ptr<off_t> offset, size_t count);
}
