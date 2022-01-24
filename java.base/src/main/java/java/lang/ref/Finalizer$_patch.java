package java.lang.ref;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Finalizer.class)
final class Finalizer$_patch {
    // Alias
    static native void runFinalization();
}
