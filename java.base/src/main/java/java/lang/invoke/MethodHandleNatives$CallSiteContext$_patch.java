package java.lang.invoke;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(MethodHandleNatives.CallSiteContext.class)
class MethodHandleNatives$CallSiteContext$_patch {
    @Replace
    static MethodHandleNatives.CallSiteContext make(CallSite cs) {
        return null;
    }
}
