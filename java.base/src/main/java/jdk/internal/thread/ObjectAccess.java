package jdk.internal.thread;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Object.class)
final class ObjectAccess {
    native void monitorEnter();
    native void monitorExit();
}
