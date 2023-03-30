package jdk.internal.thread;

import static jdk.internal.sys.linux.Futex.*;
import static jdk.internal.sys.posix.Errno.*;
import static jdk.internal.sys.posix.PThread.*;
import static jdk.internal.sys.posix.Time.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;
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
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.NoThrow;
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

    @NoSafePoint
    @NoSideEffects
    @NoThrow
    @Hidden
    public static native ptr<thread_native> currentThreadNativePtr();

    public static int nextThreadNum() {
        return addr_of(threadNumberCounter).getAndAdd(word(1)).intValue();
    }

    public static void notifySafePointOutbound(final ptr<thread_native> threadNativePtr) {
        if (Build.Target.isWasi()) {
            // TODO
            //  memory.atomic.notify(statusPtr, 1)
            abort();
        } else if (Build.Target.isPosix()) {
            if (pthread_cond_signal(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
        }
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
    public static void thread_attach(Thread thread) {
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
                if (pthread_mutex_init(addr_of(deref(threadNativePtr).mutex), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).inbound_cond), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).outbound_cond), zero()).isNonZero()) abort();
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

        // add thread to linked list; note that unlike `start`, we do not safepoint here
        // acquire lock
        if (Build.Target.isPosix()) {
            if (pthread_mutex_lock(addr_of(thread_list_mutex)).isNonZero()) abort();
        } else {
            // ???
            abort();
        }
        // we hold the big list lock; do ye olde linked list insertion
        deref(thread_list_terminus.prev).next = threadNativePtr;
        deref(threadNativePtr).prev = thread_list_terminus.prev;
        thread_list_terminus.prev = threadNativePtr;
        deref(threadNativePtr).next = addr_of(thread_list_terminus);
        // release lock
        if (Build.Target.isPosix()) {
            if (pthread_mutex_unlock(addr_of(thread_list_mutex)).isNonZero()) abort();
        } else {
            // ???
            abort();
        }

        // The thread is now alive; use the normal execution methodology from now on
        bind(threadNativePtr);
        // (not reachable)
        abort();
    }

    @NoSafePoint
    @NoThrow
    public static ptr<thread_native> getThreadNativePtr(Thread thread) {
        ThreadAccess ta = cast(thread);
        return ta.threadNativePtr;
    }

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

    @NoSafePoint
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
    public static native void bind(ptr<thread_native> threadPtr);

    @NoSafePoint
    public static long nextThreadID() {
        return addr_of(threadSeqNumber).getAndAdd(word(1)).longValue();
    }

    /**
     * Park in a safepoint for some amount of time.
     *
     * @param millis the number of milliseconds to park for
     * @param nanos the number of nanos to park for
     * @param setBits the bits to set while parking
     * @param clearBits the bits to clear while parking
     * @param wakeBits the set of bits, any of which should cause the park to return early
     * @param clearWakeBits the set of bits to clear on unpark and cause the park to return early
     */
    @SuppressWarnings("ConstantConditions")
    @NoSafePoint
    @Hidden
    public static void park(long millis, int nanos, int setBits, int clearBits, int wakeBits, int clearWakeBits) {
        if (millis < 0 || nanos < 0 || nanos > 999_999) {
            throw new IllegalArgumentException();
        }
        // assert
        final ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        // check to see if we should just exit right away
        if ((statePtr.loadSingleAcquire().intValue() & wakeBits) != 0) {
            return;
        }
        if (Build.Target.isPosix()) {
            // POSIX park uses seconds not millis...
            long posixSeconds = millis / 1_000L;
            int posixNanos = nanos + ((int)millis % 1_000) * 1_000_000;
            parkPosix(posixSeconds, posixNanos, setBits, clearBits, wakeBits, clearWakeBits);
        } else {
            // todo
            abort();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NoSafePoint
    @NoThrow
    @Hidden
    public static void parkPosix(long seconds, int nanos, int setBits, int clearBits, int wakeBits, int clearWakeBits) {
        struct_timespec ts = auto();
        struct_timespec now = auto();
        final ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        enterSafePoint(threadNativePtr, setBits, clearBits);
        // we are now ready to park

        // acquire the mutex
        if (pthread_mutex_lock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
        if (seconds != 0 || nanos != 0) {
            // get the start time; both Linux and not-linux are absolute but the clock used varies
            clock_gettime(Build.Target.isLinux() ? CLOCK_MONOTONIC : CLOCK_REALTIME, addr_of(now));
            // add the base time to the duration
            nanos += now.tv_nsec.intValue();
            if (nanos > 1_000_000_000) {
                // carry the one
                seconds ++;
                nanos -= 1_000_000_000;
            }
            seconds += now.tv_sec.longValue();
            // store it back
            ts.tv_sec = word(seconds);
            ts.tv_nsec = word(nanos);
        }
        wakeBits |= clearWakeBits;
        int oldVal = statePtr.loadSingleAcquire().intValue();
        while ((oldVal & wakeBits) == 0) {
            // wait
            if (seconds != 0 || nanos != 0) {
                final c_int res = pthread_cond_timedwait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex), addr_of(ts));
                if (res.isNonZero() && res != ETIMEDOUT) abort();
            } else {
                if (pthread_cond_wait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
            }
            // check the bits
            oldVal = statePtr.loadSingleAcquire().intValue();
            if ((oldVal & wakeBits) != 0) {
                // done (awoken)
                break;
            }
            if (seconds != 0 || nanos != 0) {
                // check the time
                clock_gettime(Build.Target.isLinux() ? CLOCK_MONOTONIC : CLOCK_REALTIME, addr_of(now));
                if (now.tv_sec.longValue() > ts.tv_sec.longValue() || now.tv_sec == ts.tv_sec && now.tv_nsec.intValue() >= ts.tv_nsec.intValue()) {
                    // done (timeout)
                    break;
                }
            }
        }
        // release the mutex
        if (pthread_mutex_unlock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
        // todo: this reacquires the mutex right away; maybe introduce a "locked" version to avoid this
        exitSafePoint(threadNativePtr, clearBits, setBits | clearWakeBits);
    }

    /**
     * Request that a thread enter into a safepoint.
     * Within a safepoint, a thread may not access the VM heap in any way (including allocation).
     * On return, the thread was requested to enter a safepoint.
     *
     * @param setBits the {@code STATE_*} flag that indicates the reason for the safepoint
     */
    @NoSafePoint
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
                if (Build.Target.isWasi()) {
                    // TODO
                    //  memory.atomic.notify(statusPtr, 1)
                    abort();
                } else if (Build.Target.isPosix()) {
                    if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
                }
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
    @NoSafePoint
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
                    if (Build.Target.isWasi()) {
                        // TODO
                        //  memory.atomic.notify(statusPtr, 1)
                        abort();
                    } else if (Build.Target.isPosix()) {
                        if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
                    }
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
    @NoSafePoint
    @Hidden
    @NoThrow
    public static void awaitSafePoint(ptr<thread_native> threadNative) {
        awaitStatusWord(threadNative, STATE_IN_SAFEPOINT);
    }

    @NoSafePoint
    @Hidden
    @NoThrow
    public static void awaitStatusWord(ptr<thread_native> threadNative, int bits) {
        awaitStatusWord(threadNative, bits, bits);
    }

    @NoSafePoint
    @Hidden
    @NoThrow
    public static int awaitStatusWord(ptr<thread_native> threadNative, int mask, int bits) {
        // assert isSafePoint();
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNative).state);
        int state = statusPtr.loadVolatile().intValue();
        while ((state & mask) != bits) {
            if (Build.Target.isWasi()) {
                // TODO
                //  memory.atomic.wait32(statusPtr, state & mask | bits, -1)
                abort();
            } else if (Build.Target.isPosix()) {
                // double-checked pattern, but using pthread_mutex
                int res = pthread_mutex_lock(addr_of(deref(threadNative).mutex)).intValue();
                if (res != 0) {
                    // fatal error
                    abort();
                }
                state = statusPtr.loadVolatile().intValue();
                while ((state & mask) != bits) {
                    res = pthread_cond_wait(addr_of(deref(threadNative).outbound_cond), addr_of(deref(threadNative).mutex)).intValue();
                    if (res != 0) {
                        // fatal error
                        abort();
                    }
                    state = statusPtr.loadVolatile().intValue();
                }
                res = pthread_mutex_unlock(addr_of(deref(threadNative).mutex)).intValue();
                if (res != 0) {
                    // fatal error
                    abort();
                }
                // done by interior loop
                return state;
            }
            state = statusPtr.loadVolatile().intValue();
        }
        return state;
    }

    /**
     * Poll for a safepoint.
     */
    @NoSafePoint
    @Hidden
    @Inline(InlineCondition.ALWAYS)
    @NoThrow
    @AutoQueued
    public static void pollSafePoint() {
        final ptr<thread_native> threadNativePtr = currentThreadNativePtr();
        final int threadStatus = addr_of(deref(threadNativePtr).state).loadAcquire().intValue();
        if (threadStatus < 0) {
            externalSafePoint(threadNativePtr);
        }
    }

    /**
     * Do the work of an externally-triggered safepoint.
     */
    @NoSafePoint
    @Hidden
    @Inline(InlineCondition.NEVER)
    @NoThrow
    public static void externalSafePoint(final ptr<thread_native> threadNativePtr) {
        enterSafePoint(threadNativePtr, 0, 0);
        exitSafePoint(threadNativePtr, 0, 0);
    }

    /**
     * Enter a safepoint voluntarily from the current thread.
     * While in a safepoint, the thread execution state will not change.
     * However, the interrupt or unpark flags may be asynchronously set on a safepointed thread.
     * The {@code reason} parameter must either be zero or a combination of {@link #STATE_SAFEPOINT_REQUEST} and
     * one or more {@code PARK_SAFEPOINT_REQUEST_*} flags; otherwise, the thread will be trapped in a safepoint indefinitely.
     *
     * @param threadNativePtr the pointer to the thread's native structure (must not be {@code null})
     * @param setBits an optional set of bits to add to the status
     * @param clearBits an optional set of bits to remove from the status
     */
    @NoSafePoint
    @Hidden
    @NoThrow
    public static void enterSafePoint(ptr<thread_native> threadNativePtr, int setBits, int clearBits) {
        // XXX WARNING: Thread.currentThread() is INVALID in this method XXX
        // the reason is that GC may relocate it, but this method is not in the captured context so stack walk will miss it

        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);

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
        if (Build.Target.isWasi()) {
            // TODO
            //  memory.atomic.notify(statusPtr, Integer.MAX_VALUE)
            abort();
        } else if (Build.Target.isPosix()) {
            // signal after transition
            if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).outbound_cond)).intValue() != 0) abort();
        }
    }

    @NoSafePoint
    @Hidden
    public static int exitSafePoint(ptr<thread_native> threadNativePtr, int setBits, int clearBits) {
        // XXX WARNING: Thread.currentThread() is INVALID in this method XXX
        // the reason is that GC may relocate it, but this method is not in the captured context so stack walk will miss it

        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        // wait until it's OK to exit the safepoint
        // assert isSafePoint();
        if (Build.Target.isPosix() && ! Build.Target.isWasi()) {
            // double-checked pattern, but using pthread_mutex; also, we need the lock for waiting
            int res = pthread_mutex_lock(addr_of(deref(threadNativePtr).mutex)).intValue();
            if (res != 0) {
                // fatal error
                abort();
            }
        }
        int oldVal = statusPtr.loadVolatile().intValue();
        for (;;) {
            while ((oldVal & STATE_SAFEPOINT_REQUEST) != 0) {
                if (Build.Target.isWasi()) {
                    // TODO
                    //  memory.atomic.wait32(statusPtr, state & ~STATE_SAFEPOINT_REQUEST, -1)
                    abort();
                } else if (Build.Target.isPosix()) {
                    if (pthread_cond_wait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
                }
                oldVal = statusPtr.loadVolatile().intValue();
            }
            // the request is cleared; now, attempt to clear the safepoint state and notify waiters
            int witness = statusPtr.compareAndSwap(word(oldVal), word(oldVal & ~STATE_IN_SAFEPOINT & ~clearBits | setBits)).intValue();
            if (witness == oldVal) {
                // success!
                break;
            }
            // try again
            oldVal = witness;
        }
        // done! notify waiters
        if (Build.Target.isWasi()) {
            // TODO
            //  memory.atomic.notify(statusPtr, Integer.MAX_VALUE)
            abort();
        } else if (Build.Target.isPosix()) {
            if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
            if (pthread_mutex_unlock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
        }
        return oldVal;
    }

    // todo: move this to main?
    @constructor
    @export
    public static void init_thread_list_mutex() {
        // initialize the mutex at early run time
        pthread_mutex_init(addr_of(thread_list_mutex), zero());
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
        // todo: unless = Build.Target.HasIntegerWait or similar
        @incomplete(when = { Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
        public pthread_mutex_t mutex;
        /**
         * Other threads signal this waiting thread.
         */
        @incomplete(when = { Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
        public pthread_cond_t inbound_cond;
        /**
         * This thread signals other waiting threads.
         */
        @incomplete(when = { Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
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
        public struct__Unwind_Exception unwindException;
    }
}
