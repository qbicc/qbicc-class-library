package java.util.concurrent;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(Phaser.class)
@RunTimeAspect
public class Phaser$_runtime {
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    static final int SPINS_PER_ARRIVAL = (NCPU < 2) ? 1 : 1 << 8;
}
