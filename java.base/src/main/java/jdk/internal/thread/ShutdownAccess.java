package jdk.internal.thread;

import org.qbicc.runtime.patcher.Patch;

@Patch("java/lang/Shutdown")
final class ShutdownAccess {
    static native void exit(final int code);
}
