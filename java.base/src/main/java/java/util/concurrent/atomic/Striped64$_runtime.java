package java.util.concurrent.atomic;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(Striped64.class)
@RunTimeAspect
public class Striped64$_runtime {
    static final int NCPU = Runtime.getRuntime().availableProcessors();
}
