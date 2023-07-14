package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

@include("<pwd.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Pwd {
    public static final class struct_passwd extends object {
        public ptr<c_char> pw_name;
        public uid_t pw_uid;
    }

    public static native c_int getpwuid_r(uid_t uuid, ptr<struct_passwd> pwd, ptr<c_char> buffer, size_t bufsize, ptr<ptr<struct_passwd>> result);
}
