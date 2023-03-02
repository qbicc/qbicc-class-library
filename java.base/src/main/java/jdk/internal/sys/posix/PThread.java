package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Signal.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Time.*;

import org.qbicc.runtime.NoReturn;

/**
 * See <a href="https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/pthread.h.html">the specification</a>.
 */
@include("<pthread.h>")
@lib("pthread")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class PThread {

    public static final c_int PTHREAD_MUTEX_RECURSIVE = constant();

    public static class pthread_t extends word {}
    public static class pthread_attr_t extends word {}
    public static class pthread_mutex_t extends word {}
    public static class pthread_mutexattr_t extends word {}
    public static class pthread_cond_t extends word {}
    public static class pthread_condattr_t extends word {}

    public static final pthread_mutex_t PTHREAD_MUTEX_INITIALIZER = constant();
    public static final pthread_cond_t PTHREAD_COND_INITIALIZER = constant();

    public static native c_int pthread_attr_init(ptr<pthread_attr_t> attr);
    public static native c_int pthread_attr_destroy(ptr<pthread_attr_t> attr);

    public static native c_int pthread_attr_getstack(ptr<@c_const pthread_attr_t> attr, ptr<ptr<?>> stackAddr, ptr<size_t> stackSize);
    public static native c_int pthread_attr_setstack(ptr<pthread_attr_t> attr, ptr<?> stackAddr, size_t stackSize);

    public static native c_int pthread_attr_getstacksize(@restrict ptr<@c_const pthread_attr_t> attr, @restrict ptr<size_t> size_ptr);
    public static native c_int pthread_attr_setstacksize(ptr<pthread_attr_t> attr, size_t size);

    public static native c_int pthread_create(ptr<pthread_t> thread, ptr<@c_const pthread_attr_t> attr,
                                              ptr<function<pthread_run>> start_routine, ptr<?> arg);

    @FunctionalInterface
    public interface pthread_run {
        ptr<?> run(ptr<?> arg);
    }

    public static native pthread_t pthread_self();

    public static native c_int pthread_detach(pthread_t thread);

    @NoReturn
    public static native void pthread_exit(ptr<?> arg);

    public static native c_int pthread_kill(pthread_t thread, c_int signal);

    public static native c_int pthread_sigmask(c_int how, ptr<@c_const sigset_t> set, ptr<sigset_t> oldSet);

    public static native c_int pthread_mutex_init(ptr<pthread_mutex_t> mutex, ptr<@c_const pthread_mutexattr_t> attr);
    public static native c_int pthread_mutex_lock(ptr<pthread_mutex_t> mutex);
    public static native c_int pthread_mutex_unlock(ptr<pthread_mutex_t> mutex);
    public static native c_int pthread_mutex_destroy(ptr<pthread_mutex_t> mutex);

    public static native c_int pthread_mutexattr_init(ptr<pthread_mutexattr_t> attr);
    public static native c_int pthread_mutexattr_settype(ptr<pthread_mutexattr_t> attr, c_int type);
    public static native c_int pthread_mutexattr_destroy(ptr<pthread_mutexattr_t> attr);

    public static native c_int pthread_cond_init(ptr<pthread_cond_t> cond, ptr<pthread_condattr_t> attr);
    public static native c_int pthread_cond_destroy(ptr<pthread_cond_t> cond);
    public static native c_int pthread_cond_signal(ptr<pthread_cond_t> cond);
    public static native c_int pthread_cond_broadcast(ptr<pthread_cond_t> cond);
    public static native c_int pthread_cond_wait(ptr<pthread_cond_t> cond, ptr<pthread_mutex_t> mutex);
    public static native c_int pthread_cond_timedwait(ptr<pthread_cond_t> cond, ptr<pthread_mutex_t> mutex, ptr<@c_const struct_timespec> abstime);

    public static native c_int pthread_condattr_init(ptr<pthread_condattr_t> attr);
    public static native c_int pthread_condattr_destroy(ptr<pthread_condattr_t> attr);
}
