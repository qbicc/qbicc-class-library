package jdk.internal.thread;

import static jdk.internal.thread.ThreadNative.*;
import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Thread.class)
final class ThreadAccess {
    static int CONFIG_LOCKED;
    static int CONFIG_DAEMON;
    ptr<thread_native> threadNativePtr;
    volatile int config;
}
