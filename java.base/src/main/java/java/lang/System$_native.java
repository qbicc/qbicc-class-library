package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.posix.Time.*;

import java.util.Properties;
import org.qbicc.runtime.Build;

/**
 *
 */
public final class System$_native {

    private static void registerNatives() {
        // no-op
    }

    // Temporary manual implementation
    @SuppressWarnings("ManualArrayCopy")
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        if (src instanceof Object[] srcArray && dest instanceof Object[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof byte[] srcArray && dest instanceof byte[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof short[] srcArray && dest instanceof short[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof int[] srcArray && dest instanceof int[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof long[] srcArray && dest instanceof long[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof char[] srcArray && dest instanceof char[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof float[] srcArray && dest instanceof float[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof double[] srcArray && dest instanceof double[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof boolean[] srcArray && dest instanceof boolean[] destArray) {
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else {
            throw new ClassCastException("Invalid array types for copy");
        }
    }

    public static long currentTimeMillis() {
        if (Build.Target.isPosix()) {
            struct_timespec spec = auto();
            clock_gettime(CLOCK_REALTIME, (struct_timespec_ptr) addr_of(spec));
            return spec.tv_sec.longValue() * 1_000L + spec.tv_nsec.longValue() / 1_000_000L;
        } else {
            throw new UnsupportedOperationException("currentTimeMillis");
        }
    }

    public static long nanoTime() {
        if (Build.Target.isPosix()) {
            // todo: check _POSIX_TIMERS / _POSIX_MONOTONIC_CLOCK from <unistd.h> and fall back if needed
            struct_timespec spec = auto();
            clock_gettime(CLOCK_MONOTONIC, (struct_timespec_ptr) addr_of(spec));
            // todo: add nanoTime bias from end of ADD phase
            return spec.tv_sec.longValue() * 1_000_000_000L + spec.tv_nsec.longValue();
        } else {
            throw new UnsupportedOperationException("nanoTime");
        }
    }

    // This has to be an empty method rather than an intrinsic, so that interpreter can intercept it.
    public static Properties initProperties(Properties properties) {
        return properties;
    }

    public static String mapLibraryName(String libname) {
        if (Build.Target.isMacOs()) {
            return "lib" + libname + ".dylib";
        } else {
            return "lib" + libname + ".so";
        }
        // todo: windows
    }

    public static int identityHashCode(Object x) {
        // TODO: obviously non-optimal; replace once we have object headers sorted out
        return 0;
    }

}
