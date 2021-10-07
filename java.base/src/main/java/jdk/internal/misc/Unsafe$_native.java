package jdk.internal.misc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.linux.Stdlib.*;

import java.security.ProtectionDomain;

import org.qbicc.runtime.Build;

/**
 *
 */
public final class Unsafe$_native {

    public int pageSize() {
        if (Build.isHost()) {
            throw new UnsupportedOperationException("Cannot retrieve page size of target during build; it is not known");
        }
        return Unsafe$_runtime.PAGE_SIZE;
    }

    int getLoadAverage0(double[] loadavg, int nelems) {
        if (Build.Target.isLinux()) {
            _Float64[] values = new _Float64[nelems];
            return getloadavg(values, word(nelems)).intValue();
        }
        return 0;
    }

    public void throwException(Throwable ee) throws Throwable {
        throw ee;
    }

    public Class<?> defineClass0(String name, byte[] b, int off, int len,
                                 ClassLoader loader,
                                 ProtectionDomain protectionDomain) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    private Class<?> defineAnonymousClass0(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        throw new UnsupportedOperationException("Cannot define classes at run time");
    }

    @SuppressWarnings("ConstantConditions")
    private Unsafe asUnsafe() {
        return (Unsafe) (Object) this;
    }


    //TODO:

    //unpark
    //park

    //allocateInstance

    //allocateMemory0
    //reallocateMemory0
    //freeMemory0
    //setMemory0
    //copyMemory0
    //copySwapMemory0

}
