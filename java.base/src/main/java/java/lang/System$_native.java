package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.posix.Time.*;

import org.qbicc.runtime.Build;

/**
 *
 */
public final class System$_native {

    public static long currentTimeMillis() {
        if (Build.Target.isPosix()) {
            struct_timespec spec = auto();
            clock_gettime(CLOCK_REALTIME, addr_of(spec));
            return spec.tv_sec.longValue() * 1_000L + spec.tv_nsec.longValue() / 1_000_000L;
        } else {
            throw new UnsupportedOperationException("currentTimeMillis");
        }
    }

    public static long nanoTime() {
        if (Build.Target.isPosix()) {
            // todo: check _POSIX_TIMERS / _POSIX_MONOTONIC_CLOCK from <unistd.h> and fall back if needed
            struct_timespec spec = auto();
            clock_gettime(CLOCK_MONOTONIC, addr_of(spec));
            // todo: add nanoTime bias from end of ADD phase
            return spec.tv_sec.longValue() * 1_000_000_000L + spec.tv_nsec.longValue();
        } else {
            throw new UnsupportedOperationException("nanoTime");
        }
    }
}
