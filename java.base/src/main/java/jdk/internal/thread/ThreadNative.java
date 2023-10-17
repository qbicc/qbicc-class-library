package jdk.internal.thread;

import static java.lang.Math.abs;
import static jdk.internal.sys.posix.Errno.*;
import static jdk.internal.sys.posix.PThread.*;
import static jdk.internal.sys.posix.Time.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.printf;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;
import static org.qbicc.runtime.unwind.Unwind.*;

import java.util.concurrent.locks.LockSupport;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.ThreadScoped;

/**
 * Native aspects of threads which are shared in the JDK.
 */
public final class ThreadNative {
    /**
     * Get the system thread group object.
     *
     * @return the system thread group (not {@code null})
     */
    public static native ThreadGroup getSystemThreadGroup();

    /**
     * Mutex that protects the running thread doubly-linked list.
     */
    @SuppressWarnings("unused")
    public static final pthread_mutex_t thread_list_mutex = zero();
    /**
     * The special thread list terminus node.
     */
    public static final thread_native thread_list_terminus = zero();
    // JVMTI flag definitions; this is our thread state. Here are the constraints:
    // - no more than one of STATE_WAITING, STATE_BLOCKED_ON_MONITOR_ENTER, or STATE_RUNNABLE may be set at once
    // - no more than one of STATE_WAITING_INDEFINITELY or STATE_WAITING_WITH_TIMEOUT may be set at once
    // - no more than one of STATE_IN_OBJECT_WAIT, STATE_PARKED, or STATE_SLEEPING may be set at once
    // - if any of STATE_INTERRUPTED or STATE_IN_NATIVE is set, then STATE_ALIVE is set
    // - no more than one of STATE_ALIVE or STATE_TERMINATED may be set at once

    // ==================================
    //  State Flags
    //    These flag bits align with the
    //    corresponding JVMTI bit numbers.
    // ==================================

    // ----------------------------------
    //  Liveness status flags
    //    0 or 1 of these may be set
    // ----------------------------------

    /**
     * Thread is alive (has been started).
     * Mutually exclusive with {@link #STATE_TERMINATED}.
     */
    public static final int STATE_ALIVE = 1 << 0;
    /**
     * Thread is terminated (has exited after being started).
     * Mutually exclusive with {@link #STATE_ALIVE}.
     */
    public static final int STATE_TERMINATED = 1 << 1;

    // ----------------------------------
    //  Terminated state flags
    //    0 or 1 of these may be set
    //    (STATE_TERMINATED is set)
    // ----------------------------------

    /**
     * Thread has exited.
     * May only be set while {@link #STATE_TERMINATED} is set.
     */
    public static final int STATE_EXITED = 1 << 27;

    // ----------------------------------
    //  Alive state flags
    //    Exactly 1 must be set
    //    (STATE_ALIVE is set)
    // ----------------------------------

    /**
     * Thread is runnable.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Mutually exclusive with {@link #STATE_WAITING} and {@link #STATE_BLOCKED_ON_MONITOR_ENTER}.
     */
    public static final int STATE_RUNNABLE = 1 << 2;
    /**
     * The thread is waiting for something (is not currently runnable).
     * May only be set while {@link #STATE_ALIVE} is set.
     * Mutually exclusive with {@link #STATE_RUNNABLE} and {@link #STATE_BLOCKED_ON_MONITOR_ENTER}.
     */
    public static final int STATE_WAITING = 1 << 7;

    // ----------------------------------
    //  Waiting reason flags
    //    0 or 1 may be set while WAITING
    // ----------------------------------

    /**
     * The thread is blocked waiting to acquire or reacquire an object monitor.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Mutually exclusive with {@link #STATE_RUNNABLE} and {@link #STATE_WAITING}.
     */
    public static final int STATE_BLOCKED_ON_MONITOR_ENTER = 1 << 10;
    /**
     * The thread is waiting without a timeout.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_WAITING_WITH_TIMEOUT}.
     */
    public static final int STATE_WAITING_INDEFINITELY = 1 << 4;
    /**
     * The thread is waiting with a timeout.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_WAITING_INDEFINITELY}.
     */
    public static final int STATE_WAITING_WITH_TIMEOUT = 1 << 5;
    /**
     * The thread is asleep (i.e. in {@link Thread#sleep(long)} or {@link Thread#sleep(long, int)}).
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_IN_OBJECT_WAIT} and {@link #STATE_PARKED}.
     */
    public static final int STATE_SLEEPING = 1 << 6;
    /**
     * The thread is waiting on an object's monitor.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_SLEEPING} and {@link #STATE_PARKED}.
     */
    public static final int STATE_IN_OBJECT_WAIT = 1 << 8;
    /**
     * The thread is waiting in one of the {@link LockSupport} park methods.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_SLEEPING} and {@link #STATE_IN_OBJECT_WAIT}.
     */
    public static final int STATE_PARKED = 1 << 9;
    // STATE_SUSPENDED = 1 << 20;

    // ----------------------------------
    //  Run-time notification flags
    //    0 or more may be set while ALIVE
    //    Affects running thread state
    // ----------------------------------

    /**
     * The unpark permit.
     * If set, then a {@code LockSupport.park()} will consume the permit rather than park.
     * Other forms of {@code park} and other blocking operations are unaffected.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Normally clear.
     * This is an inbound-notification flag.
     * This bit is unused by JVMTI.
     */
    public static final int STATE_UNPARK = 1 << 11; // unused by JVMTI
    /**
     * The interrupt flag.
     * If set, then interruptible blocking operations will return immediately or throw an interruption exception.
     * Other blocking operations are unaffected.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Normally clear.
     * This is an inbound-notification flag.
     */
    public static final int STATE_INTERRUPTED = 1 << 21;

    // ----------------------------------
    //  Thread safepoint state flags
    // ----------------------------------

    /**
     * The in-native flag.
     * If set, then a native method is running.
     * Native methods may also run without setting this flag.
     * May only be set while {@link #STATE_IN_SAFEPOINT} is set.
     * Normally clear.
     * No notifications are associated with this flag.
     */
    public static final int STATE_IN_NATIVE = 1 << 22;
    /**
     * The thread state bit mask which reflects the bits that are valid to JVMTI.
     */
    public static final int JVMTI_STATE_BITS = 0
        | STATE_ALIVE
        | STATE_TERMINATED
        | STATE_RUNNABLE
        | STATE_WAITING_INDEFINITELY
        | STATE_WAITING_WITH_TIMEOUT
        | STATE_SLEEPING
        | STATE_WAITING
        | STATE_IN_OBJECT_WAIT
        | STATE_PARKED
        | STATE_BLOCKED_ON_MONITOR_ENTER
     // | STATE_SUSPENDED
        | STATE_INTERRUPTED
        | STATE_IN_NATIVE
        ;
    /**
     * The in-safepoint flag.
     * If set, then the thread is in a safepoint.
     * A thread may cooperatively enter a safepoint at any time.
     * A thread may not exit the safepoint state until the {@link #STATE_SAFEPOINT_REQUEST} flag is clear.
     * May only be set while {@link #STATE_ALIVE} is set.
     * This is an outbound-notification flag.
     * This bit corresponds to the JVMTI "vendor 1" bit.
     */
    public static final int STATE_IN_SAFEPOINT = 1 << 28;

    // ----------------------------------
    //  Thread safepoint request flags
    // ----------------------------------

    // STATE_SAFEPOINT_REQUEST_SUSPEND = 1 << ??;
    /**
     * The safepoint-request flag for GC.
     * If set, then the thread is participating in garbage collection and may not exit a safepoint.
     * Normally clear.
     * The thread may atomically set its own request-GC flag and park in a safepoint if it cannot complete an allocation.
     * The GC thread(s) may set this flag and perform GC work from within a safepoint.
     * May only be set while {@link #STATE_ALIVE} is set.
     * No notifications are associated with this flag.
     * This bit corresponds to the JVMTI "vendor 2" bit.
     */
    public static final int STATE_SAFEPOINT_REQUEST_GC = 1 << 29;
    /**
     * The safepoint-request flag for thread stack walking.
     * If set, then the thread is participating in stack capture and may not exit a safepoint.
     * This flag must be owned exclusively by one thread to prevent premature safepoint exits.
     * Use a monitor to ensure exclusive access.
     * Normally clear.
     * May only be set while {@link #STATE_ALIVE} is set.
     * No notifications are associated with this flag.
     * This bit corresponds to the JVMTI "vendor 3" bit.
     */
    public static final int STATE_SAFEPOINT_REQUEST_STACK = 1 << 30;
    /**
     * A mask containing the bits of all the specific safepoint-request flags.
     */
    public static final int STATE_SAFEPOINT_REQUEST_ANY = STATE_SAFEPOINT_REQUEST_GC | STATE_SAFEPOINT_REQUEST_STACK;
    /**
     * The general safepoint-request flag.
     * This flag must be set whenever any of the other {@code #PARK_SAFEPOINT_REQUEST_*} flags are set.
     * This is because CPUs generally have a single instruction to detect a negative number, making polling more efficient.
     * May only be set while {@link #STATE_ALIVE} is set.
     * This is an inbound-notification flag.
     * This bit is unused by JVMTI.
     */
    public static final int STATE_SAFEPOINT_REQUEST = 1 << 31;
    /**
     * Thread number counter for generation of thread names.
     */
    @SuppressWarnings("unused")
    public static volatile int threadNumberCounter;
    /* For generating thread ID */
    @SuppressWarnings("unused")
    public static volatile long threadSeqNumber;
    /**
     * The number of non-daemon threads; when this reaches zero, we exit.
     */
    @SuppressWarnings("unused")
    public static volatile int nonDaemonThreadCount;
    @SuppressWarnings("unused")
    public static volatile int shutdownInitiated;
    /**
     * Internal holder for the current thread.
     */
    @export
    @ThreadScoped
    public static ptr<thread_native> _qbicc_bound_java_thread;

    private ThreadNative() {}

    static {
        thread_list_terminus.next = addr_of(thread_list_terminus);
        thread_list_terminus.prev = addr_of(thread_list_terminus);
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoSideEffects
    @NoThrow
    @Hidden
    public static native ptr<thread_native> currentThreadNativePtr();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static int nextThreadNum() {
        return addr_of(threadNumberCounter).getAndAdd(word(1)).intValue();
    }

    public static void monitorEnter(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        // todo: stack-locking or similar
        ObjectAccess oa = cast(obj);
        oa.monitorEnter();
    }

    public static void monitorExit(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        // todo: stack-locking or similar
        ObjectAccess oa = cast(obj);
        oa.monitorExit();
    }

    /**
     * A pointer to this function is passed to pthread_create by start0.
     * The role of this wrapper is to transition a newly started Thread from C to Java calling
     * conventions and then invoke the Thread's run0 method.
     * @param threadParam a reference stuffed into a pointer
     * @return {@code null} always
     */
    @export(withScope = ExportScope.LOCAL)
    @Hidden
    public static ptr<?> runThreadBody(ptr<?> threadParam) {
        pthread_detach(pthread_self());
        ptr<thread_native> threadNativePtr = threadParam.cast();
        _qbicc_bound_java_thread = threadNativePtr;
        bind(threadNativePtr);
        // not reachable
        abort();
        return null;
    }

    /**
     * Attach an unstarted thread and run the given method body under the thread.
     *
     * @param thread the thread to attach
     */
    @export
    @NoThrow
    @Hidden
    @SafePoint(SafePointBehavior.REQUIRED)
    public static void thread_attach(Thread thread) {
        pthread_condattr_t cond_attr = auto();
        ThreadAccess ta = cast(thread);
        ptr<thread_native> threadNativePtr = ta.threadNativePtr;
        if (threadNativePtr.isNonNull()) {
            // we cannot recover
            abort();
        }
        threadNativePtr = malloc(sizeof(thread_native.class));
        if (threadNativePtr.isNull()) {
            // not enough memory to start
            abort();
        }
        // zero-init the whole structure
        threadNativePtr.storeUnshared(zero());
        if (Build.Target.isPosix()) {
            if (! Build.Target.isWasm()) {
                if (pthread_condattr_init(addr_of(cond_attr)).isNonZero()) abort();
                if (! Build.Target.isMacOs()) {
                    // macos doesn't have this function :(
                    if (pthread_condattr_setclock(addr_of(cond_attr), CLOCK_MONOTONIC).isNonZero()) abort();
                }
                if (pthread_mutex_init(addr_of(deref(threadNativePtr).mutex), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).inbound_cond), addr_of(cond_attr)).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).outbound_cond), addr_of(cond_attr)).isNonZero()) abort();
                if (pthread_condattr_destroy(addr_of(cond_attr)).isNonZero()) abort();
            }
        }
        deref(threadNativePtr).ref = reference.of(thread);
        // register it on to this thread *before* the safepoint
        if (addr_of(deref(refToPtr(ta)).threadNativePtr).compareAndSwap(zero(), threadNativePtr).isNonNull()) {
            // lost the race :(
            // release the mutex and conditions
            if (Build.Target.isPosix()) {
                if (! Build.Target.isWasm()) {
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
                    if (pthread_mutex_destroy(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
                }
            }
            free(threadNativePtr);
            abort();
        }
        _qbicc_bound_java_thread = threadNativePtr;

        // initialize status
        deref(threadNativePtr).state = word(STATE_ALIVE);

        int oldVal;
        int newVal;
        int witness;

        // lock in the config
        final ptr<int32_t> configPtr = addr_of(deref(refToPtr(ta)).config);
        oldVal = configPtr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & ThreadAccess.CONFIG_LOCKED) != 0) {
                // should not be possible, but it's a valid state
                if ((oldVal & ThreadAccess.CONFIG_DAEMON) != 0) {
                    // the main thread may not be a daemon thread
                    abort();
                }
                break;
            }
            newVal = oldVal | ThreadAccess.CONFIG_LOCKED;
            witness = configPtr.compareAndSwapRelease(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                break;
            }
            oldVal = witness;
        }
        // manually register the thread count
        addr_of(nonDaemonThreadCount).getAndAdd(word(1));

        // acquire lock
        lockThreadList_sp();
        try {
            // we hold the big list lock; do ye olde linked list insertion
            deref(thread_list_terminus.prev).next = threadNativePtr;
            deref(threadNativePtr).prev = thread_list_terminus.prev;
            thread_list_terminus.prev = threadNativePtr;
            deref(threadNativePtr).next = addr_of(thread_list_terminus);
        } finally {
            // release lock
            unlockThreadList();
        }

        // The thread is now alive; use the normal execution methodology from now on
        bind(threadNativePtr);
        // (not reachable)
        abort();
    }

    @SafePoint(SafePointBehavior.FORBIDDEN)
    @NoThrow
    public static ptr<thread_native> getThreadNativePtr(Thread thread) {
        ThreadAccess ta = cast(thread);
        return ta.threadNativePtr;
    }

    @SafePoint(SafePointBehavior.NONE)
    public static void exitNonDaemonThread() {
        // todo: put the shutdown bit right on the count word?
        int cnt = addr_of(nonDaemonThreadCount).getAndAdd(word(- 1)).intValue();
        if (cnt == 1) {
            boolean shouldShutdown = addr_of(shutdownInitiated).compareAndSet(word(0), word(1));
            if (shouldShutdown) {
                // start a new exiter thread to avoid blocking Thread.start()
                new Thread(() -> ShutdownAccess.exit(0), "Exit thread").start();
            }
        }
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static void freeThreadNative(ptr<thread_native> threadNativePtr) {
        // assert threadNativePtr != null;
        if (Build.Target.isPosix() && ! Build.Target.isWasm()) {
            // destroy our conditions and mutex
            pthread_cond_destroy(addr_of(deref(threadNativePtr).inbound_cond));
            pthread_cond_destroy(addr_of(deref(threadNativePtr).outbound_cond));
            pthread_mutex_destroy(addr_of(deref(threadNativePtr).mutex));
        }
        // release the memory
        free(threadNativePtr);
    }

    // intrinsic, but implies Hidden & NoThrow & NoReturn
    @SafePoint(value = SafePointBehavior.EXIT, setBits = STATE_RUNNABLE)
    public static native void bind(ptr<thread_native> threadPtr);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static long nextThreadID() {
        return addr_of(threadSeqNumber).getAndAdd(word(1)).longValue();
    }

    @SafePoint(SafePointBehavior.NONE)
    public static void park(boolean isAbsolute, long time) {
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        int state = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        if ((state & (STATE_UNPARK | STATE_INTERRUPTED)) != 0) {
            return;
        }
        if (isAbsolute) {
            // absolute timeout in milliseconds
            long millis = time - System.currentTimeMillis();
            // check to see if we should exit immediately
            if (millis <= 0) {
                return;
            }
            // otherwise, enter safepoint and wait
            park0(millis);
        } else if (time == 0) {
            // no timeout
            park1();
        } else {
            // relative timeout in nanos
            int nanos = (int) abs(time % 1_000_000_000);
            long seconds = Long.divideUnsigned(time, 1_000_000_000);
            park2(seconds, nanos);
        }
    }

    @SafePoint(setBits = STATE_PARKED | STATE_WAITING | STATE_WAITING_WITH_TIMEOUT, clearBits = STATE_RUNNABLE)
    private static void park0(long time) {
        lockThread_sp();
        try {
            awaitThreadInboundTimed(Long.divideUnsigned(time, 1_000), (int) abs(time % 1_000) * 1_000, STATE_INTERRUPTED | STATE_UNPARK);
        } finally {
            unlockThread();
        }
        addr_of(deref(currentThreadNativePtr()).state).getAndBitwiseAnd(word(~STATE_UNPARK));
    }

    @SafePoint(setBits = STATE_PARKED | STATE_WAITING | STATE_WAITING_INDEFINITELY, clearBits = STATE_RUNNABLE)
    private static void park1() {
        lockThread_sp();
        try {
            awaitThreadInbound(STATE_INTERRUPTED | STATE_UNPARK);
        } finally {
            unlockThread();
        }
        addr_of(deref(currentThreadNativePtr()).state).getAndBitwiseAnd(word(~STATE_UNPARK));
    }

    @SafePoint(setBits = STATE_PARKED | STATE_WAITING | STATE_WAITING_WITH_TIMEOUT, clearBits = STATE_RUNNABLE)
    private static void park2(long seconds, int nanos) {
        lockThread_sp();
        try {
            awaitThreadInboundTimed(seconds, nanos, STATE_INTERRUPTED | STATE_UNPARK);
        } finally {
            unlockThread();
        }
        addr_of(deref(currentThreadNativePtr()).state).getAndBitwiseAnd(word(~STATE_UNPARK));
    }

    public static void unpark(ptr<thread_native> threadNativePtr) {
        if (threadNativePtr.isNull()) {
            return;
        }
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        int oldVal, newVal, witness;
        oldVal = statePtr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & (STATE_UNPARK | STATE_EXITED)) != 0) {
                // no op necessary; unpark already pending or the thread is gone
                return;
            }
            newVal = oldVal | STATE_UNPARK;
            witness = statePtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                // done; now we just have to signal waiters
                break;
            }
            // retry
            oldVal = witness;
        }
        // signal the waiter
        notifyThreadInbound(threadNativePtr);
        return;
    }

    /**
     * Lock the current thread's mutex.
     * May only be called from outside a safepoint.
     * Enters a safepoint for the duration of the lock operation, if the operation would block.
     */
    @SafePoint(SafePointBehavior.NONE)
    public static void lockThread() {
        c_int result = pthread_mutex_trylock(addr_of(deref(currentThreadNativePtr()).mutex));
        if (result == EBUSY) {
            lockThread_sp();
        } else if (result.isNonZero()) {
            abort();
        }
    }

    /**
     * Lock the current thread's mutex.
     * May be called from within or without a safepoint.
     * Always enters a safepoint for the duration of the lock operation.
     */
    @SafePoint
    public static void lockThread_sp() {
        lockThread_sp(currentThreadNativePtr());
    }

    @SafePoint
    public static void lockThread_sp(final ptr<thread_native> threadNativePtr) {
        if (pthread_mutex_lock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) {
            abort();
        }
    }

    /**
     * Unlock the current thread's mutex.
     * May be called from within or without a safepoint.
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static void unlockThread() {
        unlockThread(currentThreadNativePtr());
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static void unlockThread(final ptr<thread_native> threadNativePtr) {
        if (pthread_mutex_unlock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) {
            abort();
        }
    }

    @SafePoint
    public static void awaitThreadInbound() {
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        if (pthread_cond_wait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) {
            abort();
        }
    }

    @SafePoint
    public static void awaitThreadInbound(int wakeOn) {
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        int state = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        while ((state & wakeOn) == 0) {
            awaitThreadInbound();
            state = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        }
    }

    /**
     * Wait for the inbound thread condition for up to the given duration,
     * waking up early if one of the bits in {@code wakeOn} is set.
     *
     * @param seconds the number of seconds to wait
     * @param nanos the number of nanos to wait
     * @param wakeOn the bits to wake on
     * @return {@code true} if one or more of the wake-on bits was set, or {@code false} if the timeout occurred
     */
    @SafePoint
    public static boolean awaitThreadInboundTimed(long seconds, int nanos, int wakeOn) {
        struct_timespec ts = auto();
        // get the start time
        if (Build.Target.isMacOs()) {
            // less accurate in the face of time changes...
            clock_gettime(CLOCK_REALTIME, addr_of(ts));
        } else {
            clock_gettime(CLOCK_MONOTONIC, addr_of(ts));
        }
        // add the base time to the duration
        nanos += ts.tv_nsec.intValue();
        if (nanos > 1_000_000_000) {
            // carry the one
            seconds ++;
            nanos -= 1_000_000_000;
        }
        seconds += ts.tv_sec.longValue();
        ts.tv_sec = word(seconds);
        ts.tv_nsec = word(nanos);
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        int state = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        while ((state & wakeOn) == 0) {
            c_int res = pthread_cond_timedwait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex), addr_of(ts));
            if (res == ETIMEDOUT) {
                return false;
            } else if (res.isNonZero()) {
                abort();
                return false; // not reachable
            }
            state = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        }
        return true;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static void notifyThreadInbound(final ptr<thread_native> threadNativePtr) {
        if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static void notifyThreadOutbound() {
        if (pthread_cond_broadcast(addr_of(deref(currentThreadNativePtr()).outbound_cond)).isNonZero()) abort();
    }

    @SafePoint
    public static void awaitThreadOutbound(final ptr<thread_native> threadNativePtr) {
        if (pthread_cond_wait(addr_of(deref(threadNativePtr).outbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) {
            abort();
        }
    }

    /**
     * Lock the thread list.
     * May only be called from outside a safepoint.
     * Enters a safepoint for the duration of the lock operation, if the operation would block.
     */
    @SafePoint(SafePointBehavior.NONE)
    public static void lockThreadList() {
        c_int result = pthread_mutex_trylock(addr_of(thread_list_mutex));
        if (result == EBUSY) {
            lockThread_sp();
        } else if (result.isNonZero()) {
            abort();
        }
    }

    /**
     * Lock the thread list mutex.
     * May be called from within or without a safepoint.
     * Always enters a safepoint for the duration of the lock operation.
     */
    @SafePoint
    public static void lockThreadList_sp() {
        if (pthread_mutex_lock(addr_of(thread_list_mutex)).isNonZero()) {
            abort();
        }
    }

    /**
     * Unlock the current thread's mutex.
     * May be called from within or without a safepoint.
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static void unlockThreadList() {
        if (pthread_mutex_unlock(addr_of(thread_list_mutex)).isNonZero()) {
            abort();
        }
    }

    /**
     * Request that a thread enter into a safepoint.
     * Within a safepoint, a thread may not access the VM heap in any way (including allocation).
     * On return, the thread was requested to enter a safepoint.
     *
     * @param setBits the {@code STATE_*} flag that indicates the reason for the safepoint
     */
    @SafePoint
    @Hidden
    @NoThrow
    public static void requestSafePoint(ptr<thread_native> threadNativePtr, int setBits) {
        // assert Integer.bitCount(reason) == 1;
        // assert Thread.currentThread() != threadNativePtr->ref
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        setBits |= STATE_SAFEPOINT_REQUEST;
        int witness;
        int oldVal = statusPtr.loadVolatile().intValue();
        for (;;) {
            if ((oldVal & setBits) == setBits) {
                // already set
                return;
            }
            witness = statusPtr.compareAndSwap(word(oldVal), word(oldVal | setBits)).intValue();
            if (witness == oldVal) {
                // done; wake it up if it is sleeping
                notifyThreadInbound(threadNativePtr);
                return;
            }
            oldVal = witness;
        }
    }

    /**
     * Release a safepoint for another thread.
     * The bits given should be the same as the one given to {@link #requestSafePoint}.
     *
     * @param clearBits the {@code STATE_*} flag that indicates the reason for the safepoint
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    @Hidden
    @NoThrow
    public static void releaseSafePoint(final ptr<thread_native> threadNativePtr, int clearBits) {
        // assert Integer.bitCount(reason) == 1;
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        int witness;
        int oldVal = statusPtr.loadVolatile().intValue();
        int newVal;
        for (;;) {
            newVal = oldVal & ~clearBits;
            if ((newVal & STATE_SAFEPOINT_REQUEST_ANY) == 0) {
                // case 1: we were the last reason for safepoint
                newVal &= ~STATE_SAFEPOINT_REQUEST;
                witness = statusPtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
                if (witness == oldVal) {
                    // done; now we must signal the thread so it can exit the safepoint
                    notifyThreadInbound(threadNativePtr);
                    return;
                }
            } else {
                // case 2: other safepoint reasons are still in effect; just clear the bit and return
                witness = statusPtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
                if (witness == oldVal) {
                    // done
                    return;
                }
            }
            oldVal = witness;
        }
    }

    /**
     * Wait until the given thread is in a safepoint.
     * Must not be called from within the same thread.
     * May only be called within a safepoint.
     */
    @SafePoint(SafePointBehavior.REQUIRED)
    @Hidden
    @NoThrow
    public static void awaitSafePoint(ptr<thread_native> threadNative) {
        awaitStatusWord(threadNative, STATE_IN_SAFEPOINT);
    }

    @SafePoint(SafePointBehavior.REQUIRED)
    @Hidden
    @NoThrow
    public static void awaitStatusWord(ptr<thread_native> threadNative, int bits) {
        awaitStatusWord(threadNative, bits, bits);
    }

    @SafePoint(SafePointBehavior.REQUIRED)
    @Hidden
    @NoThrow
    public static int awaitStatusWord(ptr<thread_native> threadNative, int mask, int bits) {
        // assert isSafePoint();
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNative).state);
        int state = statusPtr.loadVolatile().intValue();
        if ((state & mask) != bits) {
            // double-checked pattern, but using pthread_mutex
            lockThread_sp(threadNative);
            state = statusPtr.loadVolatile().intValue();
            while ((state & mask) != bits) {
                awaitThreadOutbound(threadNative);
                state = statusPtr.loadVolatile().intValue();
            }
            unlockThread(threadNative);
            // done by interior loop
            return state;
        }
        return state;
    }

    /**
     * Poll for a safepoint.
     */
    @SafePoint(SafePointBehavior.NONE)
    @Hidden
    @Inline(InlineCondition.ALWAYS)
    @NoThrow
    @AutoQueued
    public static void pollSafePoint() {
        final int threadStatus = addr_of(deref(currentThreadNativePtr()).state).loadAcquire().intValue();
        if (threadStatus < 0) {
            externalSafePoint();
        }
    }

    /**
     * Enter and exit the safepointed state.
     */
    @SafePoint
    @Hidden
    @NoThrow
    public static void externalSafePoint() {
        // the @SafePoint annotation does the heavy lifting here
    }

    /**
     * Enter a safepoint voluntarily from the current thread.
     * While in a safepoint, the thread execution state will not change.
     * However, the interrupt or unpark flags may be asynchronously set on a safepointed thread.
     * The {@code reason} parameter must either be zero or a combination of {@link #STATE_SAFEPOINT_REQUEST} and
     * one or more {@code PARK_SAFEPOINT_REQUEST_*} flags; otherwise, the thread will be trapped in a safepoint indefinitely.
     * <p>
     * Normally accessed via the {@link SafePoint} annotation.
     *
     * @param setBits an optional set of bits to add to the status
     * @param clearBits an optional set of bits to remove from the status
     */
    @SafePoint(SafePointBehavior.NONE)
    @Hidden
    @NoThrow
    @AutoQueued
    public static void enterSafePoint(int setBits, int clearBits) {
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);

        // capture our state
        // todo: if (Build.Target.isUnwContext) ...
        if (unw_getcontext(addr_of(deref(threadNativePtr).saved_context)).isNonZero()) abort();

        // now, indicate that we are in a safepoint
        int witness = statusPtr.loadVolatile().intValue();
        int oldVal, newVal;
        do {
            oldVal = witness;
            // no need to check oldVal, since we cannot be in a safepoint
            // assert (oldVal & (STATE_EXITED | STATE_IN_SAFEPOINT)) == 0;
            newVal = (oldVal & ~clearBits) | STATE_IN_SAFEPOINT | setBits;
            witness = statusPtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
        } while (witness != oldVal);
        // notify waiters (if any)
        notifyThreadOutbound();
    }

    /**
     * <p>
     * Normally accessed via the {@link SafePoint} annotation.
     *
     * @param setBits   an optional set of bits to add to the status
     * @param clearBits an optional set of bits to remove from the status
     */
    @SafePoint(SafePointBehavior.REQUIRED)
    @Hidden
    @AutoQueued
    public static void exitSafePoint(int setBits, int clearBits) {
        ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        // wait until it's OK to exit the safepoint
        lockThread_sp();
        try {
            int witness = statusPtr.loadVolatile().intValue();
            int oldVal;
            do {
                oldVal = witness;
                while ((oldVal & STATE_SAFEPOINT_REQUEST) != 0) {
                    awaitThreadInbound();
                    oldVal = statusPtr.loadVolatile().intValue();
                }
                // the request is cleared; now, attempt to clear the safepoint state and notify waiters
                witness = statusPtr.compareAndSwap(word(oldVal), word(oldVal & ~STATE_IN_SAFEPOINT & ~clearBits | setBits)).intValue();
            } while (witness != oldVal);
            // success!
        } finally {
            unlockThread();
        }
        // (at this point we're actually not in safepoint anymore)
        // done! notify waiters
        notifyThreadOutbound();
    }

    // todo: move this to main?
    @constructor
    @export
    public static void init_thread_list_mutex() {
        // initialize the mutex at early run time
        pthread_mutex_init(addr_of(thread_list_mutex), zero());
    }

    @AutoQueued
    @export
    @SafePoint(SafePointBehavior.ALLOWED)
    public static void dump_thread_native(ptr<thread_native> threadNativePtr) {
        printf(utf8z("Thread at address %p\n"), threadNativePtr);
        printf(utf8z("  Status:\n"));
        int state = deref(threadNativePtr).state.intValue();
        if ((state & STATE_ALIVE) != 0) printf(utf8z("    %s\n"), utf8z("Alive"));
        if ((state & STATE_TERMINATED) != 0) printf(utf8z("    %s\n"), utf8z("Terminated"));
        if ((state & STATE_RUNNABLE) != 0) printf(utf8z("    %s\n"), utf8z("Runnable"));
        if ((state & STATE_WAITING_INDEFINITELY) != 0) printf(utf8z("    %s\n"), utf8z("Waiting indefinitely"));
        if ((state & STATE_WAITING_WITH_TIMEOUT) != 0) printf(utf8z("    %s\n"), utf8z("Waiting with timeout"));
        if ((state & STATE_SLEEPING) != 0) printf(utf8z("    %s\n"), utf8z("Sleeping"));
        if ((state & STATE_WAITING) != 0) printf(utf8z("    %s\n"), utf8z("Waiting"));
        if ((state & STATE_PARKED) != 0) printf(utf8z("    %s\n"), utf8z("Parking"));
        if ((state & STATE_BLOCKED_ON_MONITOR_ENTER) != 0) printf(utf8z("    %s\n"), utf8z("Blocked on monitor"));
        if ((state & STATE_UNPARK) != 0) printf(utf8z("    %s\n"), utf8z("Unparked"));
        if ((state & STATE_INTERRUPTED) != 0) printf(utf8z("    %s\n"), utf8z("Interrupted"));
        if ((state & STATE_IN_NATIVE) != 0) printf(utf8z("    %s\n"), utf8z("In native"));
        if ((state & STATE_EXITED) != 0) printf(utf8z("    %s\n"), utf8z("Exited"));
        if ((state & STATE_IN_SAFEPOINT) != 0) printf(utf8z("    %s\n"), utf8z("In safepoint"));
        if ((state & STATE_SAFEPOINT_REQUEST_GC) != 0) printf(utf8z("    %s\n"), utf8z("GC requested"));
        if ((state & STATE_SAFEPOINT_REQUEST_STACK) != 0) printf(utf8z("    %s\n"), utf8z("Stack requested"));
        if ((state & STATE_SAFEPOINT_REQUEST) != 0) printf(utf8z("    %s\n"), utf8z("Safepoint requested"));
    }

    /**
     * Structure containing thread-specific items which cannot be moved in memory.
     * A doubly-linked list, protected by {@link ThreadNative#thread_list_mutex}, is maintained by
     * the {@code next} and {@code prev} pointers, which are always non-{@code null}.
     * The list link pointers may only be accessed (for read or write) under the mutex.
     * The special node {@link ThreadNative#thread_list_terminus} indicates the start and end of the list.
     * The {@code next} and {@code prev} pointers of that node contain the actual ends of the list.
     * <p>
     * This structure is allocated on thread start and freed some time after termination.
     * Thus, it should normally only be accessed from the same thread or when thread state {@code STATE_ACTIVE} is set.
     */
    @internal
    public static class thread_native extends object {
        // TODO
        // ptr<?> top_of_stack; // basis for compressed stack refs
        /**
         * The next (newer) thread link.
         * On the terminus node, this is the <em>first (least recent)</em> node in the list.
         */
        public ptr<thread_native> next;
        /**
         * The previous (older) thread link.
         * On the terminus node, this is the <em>last (most recent)</em> node in the list.
         * New threads are added here.
         */
        public ptr<thread_native> prev;

        /**
         * The POSIX thread identifier.
         */
        @incomplete(unless = Build.Target.IsPThreads.class)
        public pthread_t thread;

        /**
         * Reference to the actual Java thread. Must be updated during GC.
         */
        public reference<Thread> ref;

        // Fallback park/unpark mutex

        /**
         * Protects {@link #state} (double-checked path) and the two conditions on platforms without lockless waiting.
         */
        @incomplete(unless = Build.Target.IsPThreads.class)
        public pthread_mutex_t mutex;
        /**
         * Other threads signal this waiting thread.
         */
        @incomplete(unless = Build.Target.IsPThreads.class)
        public pthread_cond_t inbound_cond;
        /**
         * This thread signals other waiting threads.
         */
        @incomplete(unless = Build.Target.IsPThreads.class)
        public pthread_cond_t outbound_cond;

        /**
         * The thread state.
         */
        public uint32_t state;

        // safepoint state
        @incomplete(when = Build.Target.IsWasi.class)
        public unw_context_t saved_context;

        // exception info
        // @incomplete(unless = Build.Target.IsUnwind.class)
        @incomplete(when = Build.Target.IsWasi.class)
        public struct__Unwind_Exception unwindException;
    }
}
