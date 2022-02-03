package java.util.concurrent;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(ConcurrentHashMap.class)
@RunTimeAspect
public class ConcurrentHashMap$_runtime {
    static final int NCPU = Runtime.getRuntime().availableProcessors();
}
