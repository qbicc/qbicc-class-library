package jdk.internal.sys.linux;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.linux.SysSyscall.*;
import static org.qbicc.runtime.InlineCondition.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;

import org.qbicc.runtime.Inline;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;

/**
 * Support for {@code futex(2)} on Linux.  See the manpage for more information.
 */
@include("<linux/futex.h>")
public class Futex {

    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    private static c_long futex(ptr<uint32_t> uaddr, c_int futex_op, uint32_t val, ptr<@c_const struct_timespec> timeout, ptr<uint32_t> uaddr2, uint32_t val3) {
        // no glibc wrapper!
        return syscall(SYS_futex, uaddr, futex_op, val, timeout, uaddr2, val3);
    }

    /**
     * Wait until the value at the given address is equal to {@code val}.  Returns when woken (may be spurious).  Spurious
     * wakeups may return {@code false} and set {@code errno} to {@code EAGAIN}, or may return {@code true}.  If the
     * value at the test memory location already does equal {@code val}, a wakeup will result.
     * The timeout is relative.
     *
     * @param uaddr the address of the memory to test
     * @param val the value to observe
     * @param timeout the relative amount of time to wait, or {@code null} for no timeout
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    public static boolean futex_wait(ptr<uint32_t> uaddr, uint32_t val, ptr<@c_const struct_timespec> timeout) {
        return futex(uaddr, word(FUTEX_WAIT.intValue() | FUTEX_PRIVATE_FLAG.intValue()), val, timeout, zero(), zero()).longValue() != -1;
    }

    /**
     * Wait until the value at the given address is equal to {@code val}.  Returns when woken (may be spurious).  Spurious
     * wakeups may return {@code false} and set {@code errno} to {@code EAGAIN}, or may return {@code true}.  If the
     * value at the test memory location already does equal {@code val}, a wakeup will result.
     * The timeout is an absolute time.
     *
     * @param uaddr the address of the memory to test
     * @param val the value to observe
     * @param timeout the absolute time to wake up, or {@code null} for no timeout
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    public static boolean futex_wait_absolute(ptr<uint32_t> uaddr, uint32_t val, ptr<@c_const struct_timespec> timeout) {
        return futex(uaddr, word(FUTEX_WAIT_BITSET.intValue() | FUTEX_PRIVATE_FLAG.intValue()), val, timeout, zero(), FUTEX_BITSET_MATCH_ANY).longValue() != -1;
    }

    /**
     * Wait until the value of certain bits at the given address is equal to {@code val}.  Returns when woken (may be spurious).  Spurious
     * wakeups may return {@code false} and set {@code errno} to {@code EAGAIN}, or may return {@code true}.  If the
     * value at the bits of the test memory location already does equal {@code val}, a wakeup will result.
     * The timeout is an absolute time.
     *
     * @param uaddr the address of the memory to test
     * @param bitMask the mask of the bits to test
     * @param val the value to observe
     * @param timeout the absolute time to wake up, or {@code null} for no timeout
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    public static boolean futex_wait_bits(ptr<uint32_t> uaddr, uint32_t bitMask, uint32_t val, ptr<@c_const struct_timespec> timeout) {
        return futex(uaddr, word(FUTEX_WAIT_BITSET.intValue() | FUTEX_PRIVATE_FLAG.intValue()), val, timeout, zero(), bitMask).longValue() != -1;
    }

    /**
     * Wake a single waiter after updating the wait value.
     *
     * @param uaddr the address of the memory being waited on
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    public static boolean futex_wake_single(ptr<uint32_t> uaddr) {
        return futex(uaddr, word(FUTEX_WAKE.intValue() | FUTEX_PRIVATE_FLAG.intValue()), word(1), zero(), zero(), zero()).longValue() != -1;
    }

    /**
     * Wake all waiters after updating the wait value.
     *
     * @param uaddr the address of the memory being waited on
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    @Inline(ALWAYS)
    @NoSafePoint
    @NoThrow
    public static boolean futex_wake_all(ptr<uint32_t> uaddr) {
        return futex(uaddr, word(FUTEX_WAKE.intValue() | FUTEX_PRIVATE_FLAG.intValue()), word(Integer.MAX_VALUE), zero(), zero(), zero()).longValue() != -1;
    }

    public static final c_int FUTEX_PRIVATE_FLAG = constant();
    public static final c_int FUTEX_CLOCK_REALTIME = constant();

    public static final c_int FUTEX_WAIT = constant();
    public static final c_int FUTEX_WAIT_BITSET = constant();
    public static final c_int FUTEX_WAKE = constant();

    public static final uint32_t FUTEX_BITSET_MATCH_ANY = constant();
}
