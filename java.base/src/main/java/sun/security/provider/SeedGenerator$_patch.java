package sun.security.provider;

import sun.security.util.Debug;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(sun.security.provider.SeedGenerator.class)
@ReplaceInit
class SeedGenerator$_patch {
    static final Debug debug = Debug.getInstance("provider");

    @Patch("sun.security.provider.SeedGenerator$ThreadedSeedGenerator")
    static class ThreadedSeedGenerator$_patch {
        ThreadedSeedGenerator$_patch() {}
    }
}
