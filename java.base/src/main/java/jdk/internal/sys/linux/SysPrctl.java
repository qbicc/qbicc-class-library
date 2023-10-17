package jdk.internal.sys.linux;

import static jdk.internal.sys.posix.Errno.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/ioctl.h>")
public final class SysPrctl {

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int prctl(c_int option, unsigned_long arg2, unsigned_long arg3, unsigned_long arg4, unsigned_long arg5);

    public static final c_int PR_SET_VMA = constant();
    public static final c_int PR_SET_NAME = constant();
    public static final c_int PR_GET_NAME = constant();

    public static final unsigned_long PR_SET_VMA_ANON_NAME = constant();

    /**
     * Set the nane of an anonymous VMA.
     *
     * @param addr the region address (must not be {@code null})
     * @param size the region size
     * @param namez the name to set (up to 80 bytes including a {@code NUL} terminator, excluding {@code [}, {@code ]}, {@code \}, {@code $}, and {@code `})
     * @return 0 on success, -1 on error (and {@code errno} is set)
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static int prctl_set_vma_anon_name(ptr<?> addr, size_t size, ptr<@c_const c_char> namez) {
        if (defined(PR_SET_VMA) && defined(PR_SET_VMA_ANON_NAME)) {
            return prctl(PR_SET_VMA, PR_SET_VMA_ANON_NAME, addr.cast(), size.cast(), namez.cast()).intValue();
        } else {
            errno = ENOTSUP.intValue();
            return -1;
        }
    }

    /**
     * Set the name of the current thread.
     *
     * @param namez the new name (up to 16 bytes including a {@code NUL} terminator)
     * @return 0 on success, -1 on error (and {@code errno} is set)
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static c_int prctl_set_name(ptr<@c_const c_char> namez) {
        if (defined(PR_SET_NAME)) {
            return prctl(PR_SET_NAME, namez.cast(), zero(), zero(), zero());
        } else {
            errno = ENOTSUP.intValue();
            return word(-1);
        }
    }

    /**
     * Get the name of the current thread.
     *
     * @param buffer the name buffer, which must be a minimum of 16 bytes (must not be {@code null})
     * @return 0 on success, -1 on error (and {@code errno} is set)
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static c_int prctl_get_name(ptr<c_char> buffer) {
        if (defined(PR_GET_NAME)) {
            return prctl(PR_GET_NAME, buffer.cast(), zero(), zero(), zero());
        } else {
            errno = ENOTSUP.intValue();
            return word(-1);
        }
    }
}
