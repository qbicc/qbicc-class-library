package java.util.concurrent;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(Exchanger.class)
@RunTimeAspect
public class Exchanger$_runtime {
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    static final int FULL = (NCPU >= (java.util.concurrent.Exchanger$_patch.MMASK << 1)) ? java.util.concurrent.Exchanger$_patch.MMASK : NCPU >>> 1;
}
