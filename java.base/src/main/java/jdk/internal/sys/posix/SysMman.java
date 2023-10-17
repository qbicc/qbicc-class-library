package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/mman.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@define(value = "_DARWIN_C_SOURCE", when = Build.Target.IsApple.class)
public final class SysMman {
    private SysMman() {}

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native <P extends ptr<?>> P mmap(ptr<?> addr, size_t length, c_int prot, c_int flags, c_int fd, off_t offset);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int munmap(ptr<?> addr, size_t length);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int mprotect(ptr<?> addr, size_t length, c_int prot);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int mlock(ptr<@c_const ?> addr, size_t length);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int munlock(ptr<@c_const ?> addr, size_t length);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int mlockall(c_int flags);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int munlockall();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int msync(ptr<?> addr, size_t length, c_int flags);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int posix_madvise(ptr<?> addr, size_t length, c_int advice);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int posix_mem_offset(ptr<@c_const ?> addr, size_t length, ptr<off_t> offsetPtr, ptr<size_t> contigLen, ptr<c_int> fdPtr);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int shm_open(ptr<@c_const c_char> name, c_int oflag, mode_t mode);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int shm_unlink(ptr<@c_const c_char> name);

    // NOTE: Not POSIX but widely supported
    public static final c_int MAP_ANON = constant();
    public static final c_int MAP_NORESERVE = constant();

    public static final c_int MAP_SHARED = constant();
    public static final c_int MAP_PRIVATE = constant();
    public static final c_int MAP_FIXED = constant();

    public static final ptr<?> MAP_FAILED = constant();

    public static final c_int PROT_READ = constant();
    public static final c_int PROT_WRITE = constant();
    public static final c_int PROT_EXEC = constant();
    public static final c_int PROT_NONE = constant();

    public static final c_int POSIX_MADV_NORMAL = constant();
    public static final c_int POSIX_MADV_SEQUENTIAL = constant();
    public static final c_int POSIX_MADV_RANDOM = constant();
    public static final c_int POSIX_MADV_WILLNEED = constant();
    public static final c_int POSIX_MADV_DONTNEED = constant();
}
