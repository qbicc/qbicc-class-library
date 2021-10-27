package jdk.internal.misc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.linux.Futex.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.linux.Stdlib.*;

import java.security.ProtectionDomain;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.stdc.Errno;

/**
 *
 */
public final class Unsafe$_native {

    public int pageSize() {
        if (Build.isHost()) {
            throw new UnsupportedOperationException("Cannot retrieve page size of target during build; it is not known");
        }
        return Unsafe$_runtime.PAGE_SIZE;
    }

    int getLoadAverage0(double[] loadavg, int nelems) {
        if (Build.Target.isLinux()) {
            _Float64[] values = new _Float64[nelems];
            return getloadavg(values, word(nelems)).intValue();
        }
        return 0;
    }

    public void throwException(Throwable ee) throws Throwable {
        throw ee;
    }

    public Class<?> defineClass0(String name, byte[] b, int off, int len,
                                 ClassLoader loader,
                                 ProtectionDomain protectionDomain) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    private Class<?> defineAnonymousClass0(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    @SuppressWarnings("ConstantConditions")
    private Unsafe asUnsafe() {
        return (Unsafe) (Object) this;
    }

    static final class Linux {
        // One-bit futex for park/unpark. Note that Thread.interrupt() will need to manually unpark the thread.

        static void unpark(Unsafe theUnsafe, Object thread) {
            long fvOffset = theUnsafe.objectFieldOffset(Thread.class, "futexValue");
            long address = theUnsafe.getAddress(thread, fvOffset);
            uint32_t_ptr ptr = word(address);
            if (theUnsafe.compareAndSetInt(thread, fvOffset, 0, 1)) {
                // nothing we can do about errors really, other than panic
                futex_wake_all(ptr);
            }
        }

        static void park(Unsafe theUnsafe, boolean isAbsolute, long time) {
            struct_timespec timespec = auto();
            Thread thread = Thread.currentThread();
            long fvOffset = theUnsafe.objectFieldOffset(Thread.class, "futexValue");
            long address = theUnsafe.getAddress(thread, fvOffset);
            uint32_t_ptr ptr = word(address);
            // if we have a pending unpark, the wait value will be 1 and we will not block.
            if (theUnsafe.compareAndSetInt(thread, fvOffset, 1, 0)) {
                return;
            }
            // no pending unpark that we've detected
            boolean result;
            if (isAbsolute) {
                // time is in milliseconds since epoch
                timespec.tv_sec = word(time / 1_000L);
                timespec.tv_nsec = word(time * 1_000_000L);
                result = futex_wait_absolute(ptr, word(1), addr_of(timespec));
            } else if (time == 0) {
                // relative time of zero means wait indefinitely
                result = futex_wait(ptr, word(1), zero());
            } else {
                // time is in relative nanoseconds
                timespec.tv_sec = word(time / 1_000_000_000L);
                timespec.tv_nsec = word(time % 1_000_000_000L);
                result = futex_wait(ptr, word(1), addr_of(timespec));
            }
            if (! result) {
                throw new InternalError("futex operation failed with errno " + errno);
            }
            theUnsafe.putIntRelease(thread, fvOffset, 0);
        }
    }

    public void park(boolean isAbsolute, long time) {
        if (Build.Target.isLinux()) {
            Linux.park(asUnsafe(), isAbsolute, time);
        } else {
            throw new UnsupportedOperationException("park() not implemented for this platform");
        }
    }

    public void unpark(Object thread) {
        if (Build.Target.isLinux()) {
            Linux.unpark(asUnsafe(), thread);
        } else {
            throw new UnsupportedOperationException("unpark() not implemented for this platform");
        }
    }

    //TODO:

    //allocateInstance

    //allocateMemory0
    //reallocateMemory0
    //freeMemory0
    //setMemory0
    //copyMemory0
    //copySwapMemory0

}
