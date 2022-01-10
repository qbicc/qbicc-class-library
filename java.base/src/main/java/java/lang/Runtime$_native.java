package java.lang;

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

    public int availableProcessors() {
        // todo: containers
        return Runtime$_runtime.CONFIGURED_CPUS;
    }
}
