package java.util.concurrent;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(Exchanger.class)
public class Exchanger$_patch {
    // alias
    static int MMASK;
}
