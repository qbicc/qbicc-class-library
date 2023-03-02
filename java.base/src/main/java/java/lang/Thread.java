package java.lang;

import static jdk.internal.sys.linux.Futex.*;
import static jdk.internal.sys.posix.Errno.*;
import static jdk.internal.sys.posix.Limits.*;
import static jdk.internal.sys.posix.PThread.*;
import static jdk.internal.sys.posix.Sched.*;
import static jdk.internal.sys.posix.Time.*;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.llvm.LLVM.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;
import static org.qbicc.runtime.unwind.Unwind.*;

import java.lang.module.ModuleDescriptor;
import java.lang.ref.Reference;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.ref.CleanerFactory;
import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.ThreadScoped;
import org.qbicc.runtime.stackwalk.JavaStackWalker;
import org.qbicc.runtime.stackwalk.StackWalker;
import sun.nio.ch.Interruptible;

/**
 * The qbicc implementation of the {@code Thread} class.
 */
@Tracking("src/java.base/share/classes/java/lang/Thread.java")
public class Thread implements Runnable {

    /**
     * Holder class for the bound thread to avoid class circularity (thread containing thread-local variable).
     */
    static final class Bound {
        /**
         * Internal holder for the current thread.
         */
        @export
        @ThreadScoped
        static ptr<thread_native> _qbicc_bound_java_thread;
    }

    /**
     * Mutex that protects the running thread doubly-linked list.
     */
    @SuppressWarnings("unused")
    static final pthread_mutex_t thread_list_mutex = zero();

    /**
     * The special thread list terminus node.
     */
    static final thread_native thread_list_terminus = zero();

    static {
        thread_list_terminus.next = addr_of(thread_list_terminus);
        thread_list_terminus.prev = addr_of(thread_list_terminus);
    }

    /**
     * Structure containing thread-specific items which cannot be moved in memory.
     * A doubly-linked list, protected by {@link #thread_list_mutex}, is maintained by
     * the {@code next} and {@code prev} pointers, which are always non-{@code null}.
     * The list link pointers may only be accessed (for read or write) under the mutex.
     * The special node {@link #thread_list_terminus} indicates the start and end of the list.
     * The {@code next} and {@code prev} pointers of that node contain the actual ends of the list.
     * <p>
     * This structure is allocated on thread start and freed some time after termination.
     * Thus, it should normally only be accessed from the same thread or when thread state {@code STATE_ACTIVE} is set.
     */
    @internal
    static class thread_native extends object {
        // TODO
        // ptr<?> top_of_stack; // basis for compressed stack refs
        /**
         * The next (newer) thread link.
         * On the terminus node, this is the <em>first (least recent)</em> node in the list.
         */
        ptr<thread_native> next;
        /**
         * The previous (older) thread link.
         * On the terminus node, this is the <em>last (most recent)</em> node in the list.
         * New threads are added here.
         */
        ptr<thread_native> prev;

        /**
         * The POSIX thread identifier.
         */
        @incomplete(unless = Build.Target.IsPThreads.class)
        pthread_t thread;

        /**
         * Reference to the actual Java thread. Must be updated during GC.
         */
        reference<Thread> ref;

        // Fallback park/unpark mutex

        /**
         * Protects {@link #state} (double-checked path) and the two conditions on platforms without lockless waiting.
         */
        // todo: unless = Build.Target.HasIntegerWait or similar
        @incomplete(when = {Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
        pthread_mutex_t mutex;
        /**
         * Other threads signal this waiting thread.
         */
        @incomplete(when = {Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
        pthread_cond_t inbound_cond;
        /**
         * This thread signals other waiting threads.
         */
        @incomplete(when = {Build.Target.IsLinux.class, Build.Target.IsWasi.class}, unless = Build.Target.IsPosix.class)
        pthread_cond_t outbound_cond;

        /**
         * The thread state.
         */
        uint32_t state;

        // safepoint state
        @incomplete(when = Build.Target.IsWasi.class)
        unw_context_t saved_context;

        // exception info
        // @incomplete(unless = Build.Target.IsUnwind.class)
        struct__Unwind_Exception unwindException;
    }

    final long stackSize;
    private volatile String name;

    /**
     * The thread configuration.
     * <ul>
     *     <li>bits 0-3: thread priority; valid values are 1 through 10 (note that thread priority is configurable after thread start)</li>
     *     <li>bit 4: 1 = daemon thread; 0 = non-daemon thread</li>
     *     <li>bits 5-30: unused</li>
     *     <li>bit 31: 1 = configuration locked (thread start requested); 0 = configuration mutable</li>
     * </ul>
     */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int config;

    private static final int CONFIG_PRIORITY = 0xF << 0;
    private static final int CONFIG_DAEMON = 1 << 4;
    private static final int CONFIG_LOCKED = 1 << 31;

    private Runnable target;
    private ThreadGroup group;
    private volatile ClassLoader contextClassLoader;
    /**
     * This is a pointer to the native structure of the thread.
     */
    // note: OpenJDK has a similar field called `eetop`; some tools might look for that field?
    ptr<thread_native> threadNativePtr;

    /**
     * Thread number counter for generation of thread names.
     */
    @SuppressWarnings("unused")
    private static volatile int threadNumberCounter;

    private static int nextThreadNum() {
        return addr_of(threadNumberCounter).getAndAdd(word(1)).intValue();
    }

    /**
     * @see InheritableThreadLocal
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;

    /**
     * @see InheritableThreadLocal
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    /**
     * Thread ID.
     */
    // must be named "tid" due to reference from Unsafe
    final long tid;

    /* For generating thread ID */
    @SuppressWarnings("unused")
    private static volatile long threadSeqNumber;

    /**
     * The number of non-daemon threads; when this reaches zero, we exit.
     */
    @SuppressWarnings("unused")
    private static volatile int nonDaemonThreadCount;

    @SuppressWarnings("unused")
    private static volatile int shutdownInitiated;

    // must be named "parkBlocker" due to reference from Unsafe
    @SuppressWarnings("unused")
    volatile Object parkBlocker;

    /* The object in which this thread is blocked in an interruptible I/O
     * operation, if any.  The blocker's interrupt method should be invoked
     * after setting this thread's interrupt status.
     */
    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    /* Referenced by ThreadLocalRandom for usage by ForkJoinPool */
    @SuppressWarnings("unused")
    private AccessControlContext inheritedAccessControlContext;

    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    // todo: these three TLR fields need to be isolated from the possibility of false sharing
    // @see java.util.concurrent.ThreadLocalRandom
    long threadLocalRandomSeed;
    int threadLocalRandomProbe;
    int threadLocalRandomSecondarySeed;

    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

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
    static final int STATE_ALIVE = 1 << 0;
    /**
     * Thread is terminated (has exited after being started).
     * Mutually exclusive with {@link #STATE_ALIVE}.
     */
    static final int STATE_TERMINATED = 1 << 1;

    // ----------------------------------
    //  Terminated state flags
    //    0 or 1 of these may be set
    //    (STATE_TERMINATED is set)
    // ----------------------------------

    /**
     * Thread has exited.
     * May only be set while {@link #STATE_TERMINATED} is set.
     */
    static final int STATE_EXITED = 1 << 27;

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
    static final int STATE_RUNNABLE = 1 << 2;
    /**
     * The thread is waiting for something (is not currently runnable).
     * May only be set while {@link #STATE_ALIVE} is set.
     * Mutually exclusive with {@link #STATE_RUNNABLE} and {@link #STATE_BLOCKED_ON_MONITOR_ENTER}.
     */
    static final int STATE_WAITING = 1 << 7;
    /**
     * The thread is blocked waiting to acquire or reacquire an object monitor.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Mutually exclusive with {@link #STATE_RUNNABLE} and {@link #STATE_WAITING}.
     */
    static final int STATE_BLOCKED_ON_MONITOR_ENTER = 1 << 10;

    // ----------------------------------
    //  Waiting reason flags
    //    0 or 1 may be set while WAITING
    // ----------------------------------

    /**
     * The thread is waiting without a timeout.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_WAITING_WITH_TIMEOUT}.
     */
    static final int STATE_WAITING_INDEFINITELY = 1 << 4;
    /**
     * The thread is waiting with a timeout.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_WAITING_INDEFINITELY}.
     */
    static final int STATE_WAITING_WITH_TIMEOUT = 1 << 5;
    /**
     * The thread is asleep (i.e. in {@link #sleep(long)} or {@link #sleep(long, int)}).
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_IN_OBJECT_WAIT} and {@link #STATE_PARKED}.
     */
    static final int STATE_SLEEPING = 1 << 6;
    /**
     * The thread is waiting on an object's monitor.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_SLEEPING} and {@link #STATE_PARKED}.
     */
    static final int STATE_IN_OBJECT_WAIT = 1 << 8;
    /**
     * The thread is waiting in one of the {@link LockSupport} park methods.
     * May only be set while {@link #STATE_WAITING} is set.
     * Mutually exclusive with {@link #STATE_SLEEPING} and {@link #STATE_IN_OBJECT_WAIT}.
     */
    static final int STATE_PARKED = 1 << 9;
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
    static final int STATE_UNPARK = 1 << 11; // unused by JVMTI
    /**
     * The interrupt flag.
     * If set, then interruptible blocking operations will return immediately or throw an interruption exception.
     * Other blocking operations are unaffected.
     * May only be set while {@link #STATE_ALIVE} is set.
     * Normally clear.
     * This is an inbound-notification flag.
     */
    static final int STATE_INTERRUPTED = 1 << 21;

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
    static final int STATE_IN_NATIVE = 1 << 22;
    /**
     * The in-safepoint flag.
     * If set, then the thread is in a safepoint.
     * A thread may cooperatively enter a safepoint at any time.
     * A thread may not exit the safepoint state until the {@link #STATE_SAFEPOINT_REQUEST} flag is clear.
     * May only be set while {@link #STATE_ALIVE} is set.
     * This is an outbound-notification flag.
     * This bit corresponds to the JVMTI "vendor 1" bit.
     */
    static final int STATE_IN_SAFEPOINT = 1 << 28;

    // ----------------------------------
    //  Thread safepoint request flags
    // ----------------------------------

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
    static final int STATE_SAFEPOINT_REQUEST_GC = 1 << 29;
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
    static final int STATE_SAFEPOINT_REQUEST_STACK = 1 << 30;
    // STATE_SAFEPOINT_REQUEST_SUSPEND = 1 << ??;
    /**
     * The general safepoint-request flag.
     * This flag must be set whenever any of the other {@code #PARK_SAFEPOINT_REQUEST_*} flags are set.
     * This is because CPUs generally have a single instruction to detect a negative number, making polling more efficient.
     * May only be set while {@link #STATE_ALIVE} is set.
     * This is an inbound-notification flag.
     * This bit is unused by JVMTI.
     */
    static final int STATE_SAFEPOINT_REQUEST = 1 << 31;

    /**
     * The thread state bit mask which reflects the bits that are valid to JVMTI.
     */
    static final int JVMTI_STATE_BITS = 0
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
     * A mask containing the bits of all the specific safepoint-request flags.
     */
    static final int STATE_SAFEPOINT_REQUEST_ANY = STATE_SAFEPOINT_REQUEST_GC | STATE_SAFEPOINT_REQUEST_STACK;

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    // ==================================
    //  Public API
    // ==================================

    public static native Thread currentThread();

    public static void yield() {
        if (Build.Target.isPosix()) {
            final ptr<thread_native> threadNativePtr = currentThread().threadNativePtr;
            // safepoint for the duration of the yield, but thread is still runnable
            enterSafePoint(threadNativePtr, 0, 0);
            sched_yield();
            exitSafePoint(threadNativePtr, 0, 0);
        }
        // else no operation
    }

    // Sleep implementation

    public static void sleep(long millis) throws InterruptedException {
        sleep(millis, 0);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis == 0 && nanos == 0) {
            return;
        }
        park(millis, nanos, STATE_WAITING | STATE_SLEEPING, STATE_RUNNABLE, STATE_INTERRUPTED, 0);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    @NoThrow
    @NoSafePoint
    @Inline(InlineCondition.ALWAYS)
    public static void onSpinWait() {
        if (Build.isHost()) {
            return;
        } else if (Build.Target.isAmd64()) {
            asm(c_void.class, "pause", "", ASM_FLAG_SIDE_EFFECT);
        } else if (Build.Target.isAarch64()) {
            asm(c_void.class, "yield", "", ASM_FLAG_SIDE_EFFECT);
        } else {
            // no operation
            return;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    // these ctors are all provided in the JDK

    public Thread() {
        this(null, null, "Thread-" + nextThreadNum(), 0);
    }

    public Thread(Runnable target) {
        this(null, target, "Thread-" + nextThreadNum(), 0);
    }

    Thread(Runnable target, AccessControlContext ignored) {
        this(null, target, "Thread-" + nextThreadNum(), 0, false);
    }

    public Thread(ThreadGroup group, Runnable target) {
        this(group, target, "Thread-" + nextThreadNum(), 0);
    }

    public Thread(String name) {
        this(null, null, name, 0);
    }

    public Thread(ThreadGroup group, String name) {
        this(group, null, name, 0);
    }

    public Thread(Runnable target, String name) {
        this(null, target, name, 0);
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        this(group, target, name, stackSize, true);
    }

    public Thread(ThreadGroup g, Runnable target, String name, long stackSize, boolean inheritThreadLocals) {
        this.name = Objects.requireNonNull(name);

        Thread parent = currentThread();
        if (g == null) {
            g = parent.getThreadGroup();
        }
        g.addUnstarted();
        this.group = g;
        int parentPriority = parent.getPriority();
        int groupMaxPriority = g.getMaxPriority();
        if (parentPriority > groupMaxPriority) {
            parentPriority = groupMaxPriority;
        }
        this.config = (parent.isDaemon() ? CONFIG_DAEMON : 0) | parentPriority;
        this.contextClassLoader = parent.getContextClassLoader();
        this.target = target;
        if (inheritThreadLocals) {
            final ThreadLocal.ThreadLocalMap parentThreadLocals = parent.inheritableThreadLocals;
            if (parentThreadLocals != null) {
                this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parentThreadLocals);
            }
        }
        this.stackSize = stackSize;

        /* Set thread ID */
        this.tid = nextThreadID();

        // fields that contain pointers to OS/native resources are initialized in start0()
        // This allows Thread instances to be created (but not started) at build time.
        // One typical use case is <clinit> methods that register shutdown hooks.
    }

    public synchronized void start() {
        if (Build.isHost()) {
            // this must be replaced on the host
            throw new IllegalStateException("Host");
        }
        ptr<thread_native> threadNativePtr = addr_of(deref(refToPtr(this)).threadNativePtr).loadSingleAcquire();
        if (threadNativePtr != null) {
            throw new IllegalThreadStateException();
        }
        threadNativePtr = malloc(sizeof(thread_native.class));
        if (threadNativePtr.isNull()) {
            throw new OutOfMemoryError();
        }
        // zero-init the whole structure
        threadNativePtr.storeUnshared(zero());
        if (Build.Target.isPosix()) {
            if (! Build.Target.isLinux() && ! Build.Target.isWasm()) {
                if (pthread_mutex_init(addr_of(deref(threadNativePtr).mutex), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).inbound_cond), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).outbound_cond), zero()).isNonZero()) abort();
            }
        }
        deref(threadNativePtr).ref = reference.of(this);
        // register it on to this thread *before* the safepoint
        if (addr_of(deref(refToPtr(this)).threadNativePtr).compareAndSwap(zero(), threadNativePtr).isNonNull()) {
            // lost the race :(
            // release the mutex and conditions
            if (Build.Target.isPosix()) {
                if (! Build.Target.isLinux() && ! Build.Target.isWasm()) {
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
                    if (pthread_mutex_destroy(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
                }
            }
            free(threadNativePtr);
            throw new IllegalThreadStateException();
        }
        Bound._qbicc_bound_java_thread = threadNativePtr;

        // now, the tricky part: we have to add this thread to the big linked list
        final ptr<thread_native> currentThreadNativePtr = Thread.currentThread().threadNativePtr;
        // enter safepoint in case lock acquire blocks
        enterSafePoint(currentThreadNativePtr, 0, 0);
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
        // exit safepoint
        exitSafePoint(currentThreadNativePtr, 0, 0);

        // now the thread is registered, so we can try to start it up
        int oldVal;
        int newVal;
        int witness;

        // lock in the config
        final ptr<int32_t> configPtr = addr_of(deref(refToPtr(this)).config);
        oldVal = configPtr.loadSingleAcquire().intValue();
        final boolean daemon;
        for (;;) {
            if ((oldVal & CONFIG_LOCKED) != 0) {
                // should not be possible, but it's a valid state
                daemon = (oldVal & CONFIG_DAEMON) != 0;
                break;
            }
            newVal = oldVal | CONFIG_LOCKED;
            witness = configPtr.compareAndSwapRelease(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                daemon = (oldVal & CONFIG_DAEMON) != 0;
                break;
            }
            oldVal = witness;
        }

        // update the thread status
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        oldVal = statePtr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & (STATE_ALIVE | STATE_TERMINATED)) != 0) {
                // shouldn't be possible, but it's a valid state
                throw new IllegalThreadStateException();
            }
            newVal = oldVal | STATE_ALIVE | STATE_RUNNABLE;
            witness = statePtr.compareAndSwapRelease(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                break;
            }
            oldVal = witness;
        }
        if (! daemon) {
            addr_of(nonDaemonThreadCount).getAndAdd(word(1));
        }
        try {
            // provisionally successful! now, check with the group; if this fails, unset the "alive" state
            group.add(this);
            // actually start the thread
            if (Build.Target.isPosix()) {
                if (! startPosix()) {
                    throw new OutOfMemoryError();
                }
            } else if (Build.Target.isWasi()) {
                // status result = wasi_thread_spawn(nativePtr); ...
                throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Throwable t) {
            // terminated - clear ALIVE and set TERMINATED+EXITED in one swap
            statePtr.getAndBitwiseXor(word(STATE_ALIVE | STATE_TERMINATED | STATE_EXITED));
            // we also must notify any waiters (waiting on GC etc.)
            // done; now we must signal the thread so it can exit the safepoint
            notifySafePointOutbound(threadNativePtr);
            // notify joiners
            notifyAll();
            try {
                group.threadStartFailed(this);
            } catch (Throwable ignored) {}
            if (! daemon) {
                exitNonDaemonThread();
            }
            throw t;
        }
        // started!
    }

    private static void notifySafePointOutbound(final ptr<thread_native> threadNativePtr) {
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        if (Build.Target.isLinux()) {
            futex_wake_all(statePtr);
        } else if (Build.Target.isWasi()) {
            // TODO
            //  memory.atomic.notify(statusPtr, 1)
            abort();
        } else if (Build.Target.isPosix()) {
            if (pthread_cond_signal(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
        }
    }

    @NoSafePoint
    @Hidden
    @NoThrow
    private boolean startPosix() {
        // assert Build.Target.isPosix();
        final pthread_attr_t thread_attr = auto();
        int result = pthread_attr_init(addr_of(thread_attr)).intValue();
        if (result != 0) {
            return false;
        }
        final long stackSize = this.stackSize;
        if (stackSize != 0) {
            long configStackSize;
            // todo: configure our minimum stack size; just going with 64KB for now
            if (defined(PTHREAD_STACK_MIN)) {
                configStackSize = Math.max(65536, Math.max(PTHREAD_STACK_MIN.longValue(), this.stackSize));
            } else {
                configStackSize = Math.max(65536, this.stackSize);
            }
            result = pthread_attr_setstacksize(addr_of(thread_attr), word(configStackSize)).intValue();
            if (result != 0) {
                pthread_attr_destroy(addr_of(thread_attr));
                return false;
            }
        }
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        final ptr<pthread_t> pthreadPtr = addr_of(deref(threadNativePtr).thread);
        final ptr<function<pthread_run>> run_fn = addr_of(function.of(Thread::runThreadBody));
        // todo: safepoint while thread is created?
        result = pthread_create(pthreadPtr, addr_of(thread_attr), run_fn, threadNativePtr.cast()).intValue();
        pthread_attr_destroy(addr_of(thread_attr));
        return result == 0;
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    @Deprecated(since="1.2")
    public final void stop() {
        throw new UnsupportedOperationException();
    }

    public void interrupt() {
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        if (threadNativePtr.isNull()) {
            // nothing we can do
            return;
        }
        ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        int oldVal = statusPtr.loadSingleAcquire().intValue();
        int newVal, witness;
        for (;;) {
            if ((oldVal & (STATE_INTERRUPTED | STATE_EXITED)) != 0) {
                return;
            }
            newVal = oldVal | STATE_INTERRUPTED;
            witness = statusPtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                // success
                if (this != Thread.currentThread()) {
                    synchronized (blockerLock) {
                        Interruptible b = blocker;
                        if (b != null) {
                            b.interrupt(this);
                        }
                    }
                    notifySafePointOutbound(threadNativePtr);
                }
                return;
            }
            oldVal = witness;
        }
    }

    @NoSafePoint
    public static boolean interrupted() {
        final ptr<thread_native> threadNativePtr = Thread.currentThread().threadNativePtr;
        // assert threadNativePtr.isNonNull();
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        final int oldVal = statusPtr.getAndBitwiseAnd(word(~ STATE_INTERRUPTED)).intValue();
        return (oldVal & STATE_INTERRUPTED) != 0;
    }

    @NoSafePoint
    public boolean isInterrupted() {
        return (getStatus() & STATE_INTERRUPTED) != 0;
    }

    @NoSafePoint
    public final boolean isAlive() {
        return (getStatus() & STATE_ALIVE) != 0;
    }

    @Deprecated(since="1.2", forRemoval=true)
    public final void suspend() {
        throw new UnsupportedOperationException();
    }

    @Deprecated(since="1.2", forRemoval=true)
    public final void resume() {
        throw new UnsupportedOperationException();
    }

    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        ThreadGroup group = getThreadGroup();
        if(group != null) {
            if (newPriority > group.getMaxPriority()) {
                newPriority = group.getMaxPriority();
            }
            int oldVal = config;
            int witness;
            for (;;) {
                if ((oldVal & CONFIG_PRIORITY) == newPriority) {
                    return;
                }
                witness = compareAndSwapConfig(oldVal, oldVal & ~CONFIG_PRIORITY | newPriority);
                if (witness == oldVal) {
                    return;
                }
                oldVal = witness;
            }
        }
        // else do not change priority if there is no group
    }

    public final int getPriority() {
        return config & CONFIG_PRIORITY;
    }

    public final void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        this.name = name;
        // todo: set native name
    }

    public final String getName() {
        return name;
    }

    public final ThreadGroup getThreadGroup() {
        return group;
    }

    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    public static int enumerate(Thread[] array) {
        return currentThread().getThreadGroup().enumerate(array);
    }

    @Deprecated(since="1.2", forRemoval=true)
    public int countStackFrames() {
        throw new UnsupportedOperationException();
    }

    public final synchronized void join(final long millis) throws InterruptedException {
        if (millis > 0) {
            if (isAlive()) {
                final long startTime = System.nanoTime();
                long delay = millis;
                do {
                    wait(delay);
                } while (isAlive() && (delay = millis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)) > 0);
            }
        } else if (millis == 0) {
            while (isAlive()) {
                wait();
            }
        } else {
            throw new IllegalArgumentException("timeout value is negative");
        }
    }

    public final void join(long millis, int nanos) throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }

        if (nanos > 0 && millis < Long.MAX_VALUE) {
            millis++;
        }

        join(millis);
    }

    public final void join() throws InterruptedException {
        join(0);
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    public final void setDaemon(boolean on) {
        int oldVal = config;
        int newVal;
        int witness;
        for (;;) {
            if ((oldVal & CONFIG_DAEMON) != 0) {
                // done already
                return;
            }
            if ((oldVal & CONFIG_LOCKED) != 0) {
                throw new IllegalThreadStateException();
            }
            newVal = oldVal | CONFIG_DAEMON;
            witness = compareAndSwapConfig(oldVal, newVal);
            if (witness == oldVal) {
                // success
                return;
            }
            oldVal = witness;
        }
    }

    public final boolean isDaemon() {
        return (config & CONFIG_DAEMON) != 0;
    }

    @Deprecated(since="17", forRemoval=true)
    public final void checkAccess() {
        // no operation
    }

    public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," + group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + ",]";
        }
    }

    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public void setContextClassLoader(ClassLoader cl) {
        contextClassLoader = cl;
    }

    public static boolean holdsLock(Object obj) {
        Object$_aliases oa = cast(obj);
        return oa.holdsLock();
    }

    public StackTraceElement[] getStackTrace() {
        if (this != Thread.currentThread()) {
            // optimization so we do not call into the vm for threads that
            // have not yet started or have terminated
            if (!isAlive()) {
                return EMPTY_STACK_TRACE;
            }
            ArrayList<StackTraceElement> stackTrace = new ArrayList<>();
            // use the Thread.class global monitor to ensure that only one thread does this at a time
            final ptr<thread_native> threadNativePtr = this.threadNativePtr;
            synchronized (Thread.class) {
                requestSafePoint(threadNativePtr, STATE_SAFEPOINT_REQUEST_STACK);
                awaitSafePoint(threadNativePtr);
                StackWalker sw = new StackWalker(addr_of(deref(threadNativePtr).saved_context));
                JavaStackWalker jsw = new JavaStackWalker(true);
                while (jsw.next(sw)) {
                    final Class<?> clazz = jsw.getFrameClass();
                    final ClassLoader cl = clazz.getClassLoader0();
                    final Module module = clazz.getModule();
                    stackTrace.add(new StackTraceElement(
                        cl == null ? "boot" : cl.getName(),
                        module.isNamed() ? module.getName() : null,
                        module.getDescriptor().version().map(ModuleDescriptor.Version::toString).orElse(null),
                        clazz.getName(),
                        jsw.getFrameMethodName(),
                        jsw.getFrameSourceFileName(),
                        jsw.getFrameLineNumber()
                    ));
                }
                releaseSafePoint(threadNativePtr, STATE_SAFEPOINT_REQUEST_STACK);
            }
            return stackTrace.toArray(StackTraceElement[]::new);
        } else {
            return (new Exception()).getStackTrace();
        }
    }

    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // Get a snapshot of the list of all threads
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for (int i = 0; i < threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if (stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // else terminated so we don't put it in the map
        }
        return m;
    }

    public long getId() {
        return tid;
    }

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED,
        ;
    }

    public State getState() {
        // get current thread state
        return jdk.internal.misc.VM.toThreadState(getStatus() & JVMTI_STATE_BITS);
    }

    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread t, Throwable e);
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        defaultUncaughtExceptionHandler = eh;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ? uncaughtExceptionHandler : group;
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        uncaughtExceptionHandler = eh;
    }

    // ==================================
    //  Private
    // ==================================

    static void monitorEnter(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        // todo: stack-locking or similar
        Object$_aliases oa = cast(obj);
        oa.monitorEnter();
    }

    static void monitorExit(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        // todo: stack-locking or similar
        Object$_aliases oa = cast(obj);
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
    static ptr<?> runThreadBody(ptr<?> threadParam) {
        pthread_detach(pthread_self());
        ptr<thread_native> threadNativePtr = threadParam.cast();
        Bound._qbicc_bound_java_thread = threadNativePtr;
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
    private static void thread_attach(Thread thread) {
        ptr<thread_native> threadNativePtr = thread.threadNativePtr;
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
            if (! Build.Target.isLinux() && ! Build.Target.isWasm()) {
                if (pthread_mutex_init(addr_of(deref(threadNativePtr).mutex), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).inbound_cond), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).outbound_cond), zero()).isNonZero()) abort();
            }
        }
        deref(threadNativePtr).ref = reference.of(thread);
        // register it on to this thread *before* the safepoint
        if (addr_of(deref(refToPtr(thread)).threadNativePtr).compareAndSwap(zero(), threadNativePtr).isNonNull()) {
            // lost the race :(
            // release the mutex and conditions
            if (Build.Target.isPosix()) {
                if (! Build.Target.isLinux() && ! Build.Target.isWasm()) {
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).outbound_cond)).isNonZero()) abort();
                    if (pthread_cond_destroy(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
                    if (pthread_mutex_destroy(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
                }
            }
            free(threadNativePtr);
            abort();
        }
        Bound._qbicc_bound_java_thread = threadNativePtr;

        // initialize status
        deref(threadNativePtr).state = word(STATE_ALIVE);

        int oldVal;
        int newVal;
        int witness;

        // lock in the config
        final ptr<int32_t> configPtr = addr_of(deref(refToPtr(thread)).config);
        oldVal = configPtr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & CONFIG_LOCKED) != 0) {
                // should not be possible, but it's a valid state
                if ((oldVal & CONFIG_DAEMON) != 0) {
                    // the main thread may not be a daemon thread
                    abort();
                }
                break;
            }
            newVal = oldVal | CONFIG_LOCKED;
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

    @NoReturn
    void end() {
        Thread self = this;
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        // allocate this early
        CleanupAction action = new CleanupAction(threadNativePtr);
        // terminated - clear ALIVE and RUNNABLE and set TERMINATED in one swap
        // remove this thread from the thread list (and set termination flag)
        enterSafePoint(threadNativePtr, STATE_TERMINATED, STATE_ALIVE | STATE_RUNNABLE);
        pthread_mutex_lock(addr_of(thread_list_mutex));
        deref(deref(threadNativePtr).prev).next = deref(threadNativePtr).next;
        deref(deref(threadNativePtr).next).prev = deref(threadNativePtr).prev;
        deref(threadNativePtr).next = zero();
        deref(threadNativePtr).prev = zero();
        pthread_mutex_unlock(addr_of(thread_list_mutex));
        exitSafePoint(threadNativePtr, 0, 0);
        // clean out fields for better GC behavior
        if (threadLocals != null && TerminatingThreadLocal.REGISTRY.isPresent()) {
            TerminatingThreadLocal.threadTerminated();
        }
        group.threadTerminated(this);
        // notify any threads that have called Thread.join() on me
        synchronized (this) {
            this.notifyAll();
        }
        // free native structure once it is unreachable
        CleanerFactory.cleaner().register(this, action);
        if (! self.isDaemon()) {
            exitNonDaemonThread();
        }
        group = null;
        target = null;
        threadLocals = null;
        inheritableThreadLocals = null;
        blocker = null;
        uncaughtExceptionHandler = null;
        addr_of(deref(threadNativePtr).state).getAndBitwiseOr(word(STATE_EXITED));
        // prevent free before bits update
        Reference.reachabilityFence(this);
        if (Build.Target.isPosix()) {
            // exit the thread so it does not kill the whole process
            pthread_exit(zero());
        }
    }

    private static void exitNonDaemonThread() {
        // todo: put the shutdown bit right on the count word?
        int cnt = addr_of(nonDaemonThreadCount).getAndAdd(word(- 1)).intValue();
        if (cnt == 1) {
            boolean shouldShutdown = addr_of(shutdownInitiated).compareAndSet(word(0), word(1));
            if (shouldShutdown) {
                // start a new exiter thread to avoid blocking Thread.start()
                new Thread(() -> Shutdown.exit(0), "Exit thread").start();
            }
        }
    }

    @NoSafePoint
    static void freeThreadNative(ptr<thread_native> threadNativePtr) {
        // assert threadNativePtr != null;
        if (Build.Target.isPosix() && ! Build.Target.isLinux() && ! Build.Target.isWasm()) {
            // destroy our conditions and mutex
            pthread_cond_destroy(addr_of(deref(threadNativePtr).inbound_cond));
            pthread_cond_destroy(addr_of(deref(threadNativePtr).outbound_cond));
            pthread_mutex_destroy(addr_of(deref(threadNativePtr).mutex));
        }
        // release the memory
        free(threadNativePtr);
    }

    // intrinsic, but implies Hidden & NoThrow & NoReturn
    static native void bind(ptr<thread_native> threadPtr);

    @SuppressWarnings("unused") // called by `bind`
    @Hidden
    @NoThrow
    @NoReturn
    static void run0() {
        Thread thread = Thread.currentThread();
        try {
            //noinspection CallToThreadRun
            thread.run();
        } catch (Throwable t) {
            Thread.UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
            if (handler != null) {
                try {
                    handler.uncaughtException(thread, t);
                } catch (Throwable t2) {
                    // exception handler threw an exception... just bail out then
                    fprintf(stderr, utf8z("The uncaught exception handler threw an exception or error\n"));
                    fflush(stderr);
                }
            }
        }
        thread.end();
    }

    private static StackTraceElement[][] dumpThreads(Thread[] threads) {
        StackTraceElement[][] res = new StackTraceElement[threads.length][];
        for (int i=0; i<threads.length; i++) {
            res[i] = threads[i].getStackTrace();
        }
        return res;
    }

    private static Thread[] getThreads() {
        if (Build.Target.isPosix()) {
            // acquire lock
            if (pthread_mutex_lock(addr_of(thread_list_mutex)).isNonZero()) abort();
        } else {
            throw new UnsupportedOperationException();
        }
        ArrayList<Thread> threads = new ArrayList<>();
        ptr<thread_native> current = thread_list_terminus.next;
        while (current != addr_of(thread_list_terminus)) {
            threads.add(deref(current).ref.toObject());
            current = deref(current).next;
        }
        if (Build.Target.isPosix()) {
            // acquire lock
            if (pthread_mutex_unlock(addr_of(thread_list_mutex)).isNonZero()) abort();
        }
        return threads.toArray(Thread[]::new);
    }

    private static Thread[] getThreads(ptr<thread_native> next, int index) {
        if (next == addr_of(thread_list_terminus)) {
            return new Thread[index];
        } else {
            Thread[] array = getThreads(deref(next).next, index + 1);
            array[index] = deref(next).ref.toObject();
            return array;
        }
    }

    // Park/unpark
    @NoSafePoint
    private static long nextThreadID() {
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
    static void park(long millis, int nanos, int setBits, int clearBits, int wakeBits, int clearWakeBits) {
        if (millis < 0 || nanos < 0 || nanos > 999_999) {
            throw new IllegalArgumentException();
        }
        // assert
        final ptr<thread_native> threadNativePtr = Thread.currentThread().threadNativePtr;
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
    private static void parkPosix(long seconds, int nanos, int setBits, int clearBits, int wakeBits, int clearWakeBits) {
        struct_timespec ts = auto();
        struct_timespec now = auto();
        final ptr<thread_native> threadNativePtr = Thread.currentThread().threadNativePtr;
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        enterSafePoint(threadNativePtr, setBits, clearBits);
        // we are now ready to park

        // wait for any of the bits to be set
        if (! Build.Target.isLinux()) {
            // acquire the mutex
            if (pthread_mutex_lock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
        }
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
                if (Build.Target.isLinux()) {
                    futex_wait_bits(statePtr, word(wakeBits), word(wakeBits), addr_of(ts));
                } else {
                    final c_int res = pthread_cond_timedwait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex), addr_of(ts));
                    if (res.isNonZero() && res != ETIMEDOUT) abort();
                }
            } else {
                if (Build.Target.isLinux()) {
                    futex_wait_bits(statePtr, word(wakeBits), word(wakeBits), zero());
                } else {
                    if (pthread_cond_wait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
                }
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
        // unlock
        if (! Build.Target.isLinux()) {
            // release the mutex
            if (pthread_mutex_unlock(addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
        }
        // todo: this reacquires the mutex right away; maybe introduce a "locked" version to avoid this
        exitSafePoint(threadNativePtr, clearBits, setBits | clearWakeBits);
    }

    /**
     * Attempt to interrupt a {@link #park(long, int, int, int, int, int)} operation.
     *
     * @param wakeBits the wakeup bits to set on the target thread
     */
    void unpark(int wakeBits) {
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        if (threadNativePtr.isNull()) {
            return;
        }
        final ptr<uint32_t> statePtr = addr_of(deref(threadNativePtr).state);
        int oldVal, newVal, witness;
        oldVal = statePtr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & (wakeBits | STATE_EXITED)) != 0) {
                // no op necessary; unpark already pending or the thread is gone
                return;
            }
            newVal = oldVal | wakeBits;
            witness = statePtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                // done; now we just have to signal waiters
                break;
            }
            // retry
            oldVal = witness;
        }
        // signal the waiter
        if (Build.Target.isLinux()) {
            ptr<uint32_t> parkWordPtr = addr_of(deref(threadNativePtr).state);
            // use wake_all here because other threads might be waiting as well for other things
            futex_wake_all(parkWordPtr.cast());
        } else if (Build.Target.isPosix()) {
            // wake
            if (pthread_cond_broadcast(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
        } else {
            throw new UnsupportedOperationException();
        }
        return;
    }

    @NoSafePoint
    private int getStatus() {
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        return threadNativePtr.isNull() ? 0 : addr_of(deref(threadNativePtr).state).loadSingleAcquire().intValue();
    }

    @NoSafePoint
    private int compareAndSwapConfig(int expect, int update) {
        return addr_of(deref(refToPtr(this)).config).compareAndSwap(word(expect), word(update)).intValue();
    }

    /**
     * Set the blocker field; invoked via jdk.internal.access.SharedSecrets from java.nio code
     */
    static void blockedOn(Interruptible b) {
        Thread.currentThread().blocker = b;
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
    static void requestSafePoint(ptr<thread_native> threadNativePtr, int setBits) {
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
                // done
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
    static void releaseSafePoint(final ptr<thread_native> threadNativePtr, int clearBits) {
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
                    if (Build.Target.isLinux()) {
                        futex_wake_all(statusPtr);
                    } else if (Build.Target.isWasi()) {
                        // TODO
                        //  memory.atomic.notify(statusPtr, 1)
                        abort();
                    } else if (Build.Target.isPosix()) {
                        if (pthread_cond_signal(addr_of(deref(threadNativePtr).inbound_cond)).isNonZero()) abort();
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
    static void awaitSafePoint(ptr<thread_native> threadNative) {
        awaitStatusWord(threadNative, STATE_IN_SAFEPOINT);
    }

    @NoSafePoint
    @Hidden
    @NoThrow
    static void awaitStatusWord(ptr<thread_native> threadNative, int bits) {
        awaitStatusWord(threadNative, bits, bits);
    }

    @NoSafePoint
    @Hidden
    @NoThrow
    static int awaitStatusWord(ptr<thread_native> threadNative, int mask, int bits) {
        // assert isSafePoint();
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNative).state);
        int state = statusPtr.loadVolatile().intValue();
        while ((state & mask) != bits) {
            if (Build.Target.isLinux()) {
                // use futex operations to await our desired bit pattern
                if (! futex_wait_bits(statusPtr, word(mask), word(bits), zero()) && errno != EINTR.intValue()) {
                    // fatal error
                    abort();
                }
            } else if (Build.Target.isWasi()) {
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
    static void pollSafePoint() {
        final ptr<thread_native> threadNativePtr = currentThread().threadNativePtr;
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
    private static void externalSafePoint(final ptr<thread_native> threadNativePtr) {
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
    static void enterSafePoint(ptr<thread_native> threadNativePtr, int setBits, int clearBits) {
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
            newVal = oldVal | STATE_IN_SAFEPOINT | setBits;
            witness = statusPtr.compareAndSwap(word(oldVal), word(newVal)).intValue();
        } while (witness != oldVal);
        // notify waiters (if any)
        if (Build.Target.isLinux()) {
            futex_wake_all(statusPtr);
        } else if (Build.Target.isWasi()) {
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
    static int exitSafePoint(ptr<thread_native> threadNativePtr, int setBits, int clearBits) {
        // XXX WARNING: Thread.currentThread() is INVALID in this method XXX
        // the reason is that GC may relocate it, but this method is not in the captured context so stack walk will miss it

        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        // wait until it's OK to exit the safepoint
        // assert isSafePoint();
        if (Build.Target.isPosix() && ! Build.Target.isLinux() && ! Build.Target.isWasi()) {
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
                if (Build.Target.isLinux()) {
                    // use futex operations to await our desired bit pattern
                    if (! futex_wait_bits(statusPtr, word(STATE_SAFEPOINT_REQUEST), word(0), zero()) && errno != EINTR.intValue()) {
                        // fatal error
                        abort();
                    }
                } else if (Build.Target.isWasi()) {
                    // TODO
                    //  memory.atomic.wait32(statusPtr, state & ~STATE_SAFEPOINT_REQUEST, -1)
                    abort();
                } else if (Build.Target.isPosix()) {
                    if (pthread_cond_wait(addr_of(deref(threadNativePtr).outbound_cond), addr_of(deref(threadNativePtr).mutex)).isNonZero()) abort();
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
        if (Build.Target.isLinux()) {
            futex_wake_all(statusPtr);
        } else if (Build.Target.isWasi()) {
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
    private static void init_thread_list_mutex() {
        // initialize the mutex at early run time
        pthread_mutex_init(addr_of(thread_list_mutex), zero());
    }

    static final class CleanupAction implements Runnable {
        private final ptr<thread_native> nativePtr;

        CleanupAction(final ptr<thread_native> nativePtr) {
            this.nativePtr = nativePtr;
        }

        public void run() {
            freeThreadNative(nativePtr);
        }
    }
}
