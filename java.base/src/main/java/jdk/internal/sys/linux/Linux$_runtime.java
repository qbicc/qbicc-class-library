package jdk.internal.sys.linux;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.SysUtsname.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(Linux.class)
@RunTimeAspect
class Linux$_runtime {
    @Add
    static final int KERN_MINOR;
    @Add
    static final int KERN_MAJOR;

    static {
        struct_utsname buf = auto();
        uname(addr_of(buf));
        ptr<c_char> minorPos = auto();
        KERN_MAJOR = strtol(addr_of(buf).cast(), addr_of(minorPos), word(10)).intValue();
        KERN_MINOR = strtol(minorPos.plus(1), zero(), word(10)).intValue();
    }
}
