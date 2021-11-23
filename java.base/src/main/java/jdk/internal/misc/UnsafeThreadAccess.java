package jdk.internal.misc;

import org.qbicc.runtime.patcher.PatchClass;

/**
 *
 */
@PatchClass(Thread.class)
class UnsafeThreadAccess {
    // alias
    static native void park(boolean isAbsolute, long time);
    native void unpark();
}
