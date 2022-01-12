package jdk.internal.misc;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 * Unsafe support for park/unpark.
 */
@PatchClass(Unsafe.class)
final class UnsafeParkUnpark {

    @Replace
    void park(boolean isAbsolute, long time) {
        UnsafeThreadAccess.park(isAbsolute, time);
    }

    @Replace
    void unpark(Object thread) {
        ((UnsafeThreadAccess) thread).unpark();
    }
}
