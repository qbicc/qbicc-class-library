package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.bsd.SysSysctl.*;
import static org.qbicc.runtime.linux.Unistd.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

/**
 *
 */
@PatchClass(Runtime.class)
@RunTimeAspect
class Runtime$_runtime {
    @NoReflect
    static final int CONFIGURED_CPUS;

    static {
        // set up configured CPUs
        int configuredCpus;
        if (Build.Target.isLinux()) {
            configuredCpus = sysconf(_SC_NPROCESSORS_CONF).intValue();
        } else if (Build.Target.isMacOs()) {
            c_int[] mib = new c_int[2];
            mib[0] = CTL_HW;
            mib[1] = HW_NCPU;
            c_int cpu_val = auto();
            size_t len = sizeof(cpu_val);
            c_int result = sysctl(addr_of(mib).cast(), word(2), addr_of(cpu_val), addr_of(len), zero(), zero());
            if (result.isGe(zero()) && cpu_val.isGt(word(1))) {
                configuredCpus = result.intValue();
            } else {
                configuredCpus = 1;
            }
        } else {
            // no idea really
            configuredCpus = 1;
        }
        CONFIGURED_CPUS = configuredCpus;
    }
}
