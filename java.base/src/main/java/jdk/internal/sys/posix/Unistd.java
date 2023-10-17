package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
@include(value = "<unistd.h>", when = Build.Target.IsUnix.class)
public final class Unistd {

    private Unistd() {
        /* empty */ }

    @incomplete
    public static final class struct_fd_pair extends object {
        public c_long @array_size(2) [] fd;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int access(ptr<@c_const c_char> pathname, c_int mode);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int faccessat(c_int dirfd, ptr<@c_const c_char> pathname, c_int mode, c_int flags);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int close(c_int fd);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int dup(c_int fd);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int dup2(c_int fd1, c_int fd2);

    @define(value = "_POSIX_C_SOURCE", as = "200112L", when = Build.Target.IsPosix.class)
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int fsync(c_int fd);

    @define(value = "_POSIX_C_SOURCE", as = "199309L", when = Build.Target.IsPosix.class)
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int fdatasync(c_int fd);

    @SafePoint(SafePointBehavior.REQUIRED)
    public static native pid_t fork();

    @define(value = "_POSIX_C_SOURCE", as = "200112L", when = Build.Target.IsPosix.class)
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int gethostname(ptr<c_char> buf, size_t buflen);

    // POSIX
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int pipe(c_int[] fds);

    // Alpha, IA-64, MIPS, SuperH, SPARC, SPARC64
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native struct_fd_pair pipe();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ssize_t readlink(ptr<@c_const c_char> pathName, ptr<c_char> buf, size_t bufSize);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int rmdir(ptr<@c_const c_char> path);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int unlink(ptr<@c_const c_char> pathname);

    @SafePoint(SafePointBehavior.ENTER)
    public static ssize_t write(c_int fd, ptr<@c_const c_char> buf, size_t count) {
        return write_sp(fd, buf, count);
    }

    @name("write")
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ssize_t write_sp(c_int fd, ptr<@c_const c_char> buf, size_t count);

    @SafePoint(SafePointBehavior.ENTER)
    public static ssize_t read(c_int fd, ptr<?> buf, size_t count) {
        return read_sp(fd, buf, count);
    }

    @name("read")
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ssize_t read_sp(c_int fd, ptr<?> buf, size_t count);

    public static final c_int R_OK = constant();
    public static final c_int W_OK = constant();
    public static final c_int X_OK = constant();
    public static final c_int F_OK = constant();

    public static final c_int SEEK_SET = constant();
    public static final c_int SEEK_CUR = constant();
    public static final c_int SEEK_END = constant();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native off_t lseek(c_int fd, off_t offset, c_int whence);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static c_int fcntl(c_int fd, c_int cmd) {
        return fcntl(fd, cmd, word(0));
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int fcntl(c_int fd, c_int cmd, object... arg);

    public static final c_int _SC_ARG_MAX = constant();
    public static final c_int _SC_CHILD_MAX = constant();
    public static final c_int _SC_CLK_TCK = constant();
    public static final c_int _SC_GETPW_R_SIZE_MAX = constant();
    public static final c_int _SC_HOST_NAME_MAX = constant();
    public static final c_int _SC_LOGIN_NAME_MAX = constant();
    public static final c_int _SC_NGROUPS_MAX = constant();
    public static final c_int _SC_OPEN_MAX = constant();
    public static final c_int _SC_PAGE_SIZE = constant();
    public static final c_int _SC_SYMLOOP_MAX = constant();
    public static final c_int _SC_TTY_NAME_MAX = constant();
    public static final c_int _SC_TZNAME_MAX = constant();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_long sysconf(c_int name);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native pid_t getpid();
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native pid_t getppid();

    @extern
    public static ptr<ptr<c_char>> environ;

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<c_char> getcwd(ptr<c_char> buf, size_t size);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int isatty(c_int fd);
}
