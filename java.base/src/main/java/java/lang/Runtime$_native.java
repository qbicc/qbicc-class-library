package java.lang;

import org.qbicc.runtime.Build;

/**
 *
 */
class Runtime$_native {

    public long maxMemory() {
        // todo: based on GC configuration
        return Long.MAX_VALUE;
    }

    public void gc() {
        // do nothing
    }
}
