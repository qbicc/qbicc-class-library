package jdk.internal.loader;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(BootLoader.class)
public class BootLoader$_patch {
    @Replace
    public static void loadLibrary(String name) {
        // no operation
    }
}
