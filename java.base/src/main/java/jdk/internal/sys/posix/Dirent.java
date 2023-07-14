package jdk.internal.sys.posix;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;

@include("<dirent.h>")
@include(value = "<sys/dirent.h>", when = Build.Target.IsMacOs.class)
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Dirent {
    @incomplete
    public static final class DIR extends object {
    }

    public static final class struct_dirent extends object {
        public c_char[] d_name;
    }

    public static native c_int closedir(ptr<DIR> dir);
    public static native ptr<DIR> fdopendir(c_int fd);
    public static native ptr<DIR> opendir(ptr<@c_const c_char> path);
    public static native ptr<struct_dirent> readdir(ptr<DIR> dir);
}
