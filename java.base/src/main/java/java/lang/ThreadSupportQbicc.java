package java.lang;

import static org.qbicc.runtime.CNative.*;
import org.qbicc.runtime.ThreadScoped;

/**
 * Logically these declarations belongs in Thread, however our patching/compiler support
 * has some gaps that make delcaring them in a separate class expedient.
 *   1. @ThreadScoped fields can't be declated on Thrad (circular dependency)
 *   2. The @Add annotation doesn't preserve the other annotations (like @export).
 */
class ThreadSupportQbicc {

    /**
     * Internal holder for the pointer to the current thread.
     * Thread objects are not allowed to move in memory after being constructed.
     * <p>
     * GC must take care to include this object in the root set of each thread.
     */
    @ThreadScoped
    @export
    @SuppressWarnings("unused")
    static Thread _qbicc_bound_java_thread;

    /**
     * A pointer to this function is passed to pthread_create by start0.
     * The role of this wrapper is to transition a newly started Thread from C to Java calling
     * conventions and then invoke the Thread's run0 method.
     * @param threadParam - java.lang.Thread object for the newly started thread (cast to a void pointer to be compatible with pthread_create)
     * @return null - this return value will not be used
     */
    @export
    static void_ptr qbiccThreadRunWrapper(void_ptr threadParam) {
        Object thrObj = ptrToRef(threadParam);
        _qbicc_bound_java_thread = (Thread)thrObj;
        ((Thread$_patch)thrObj).run0();
        return word(0).cast();
    }
}
