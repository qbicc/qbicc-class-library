package java.lang;

import static jdk.internal.sys.posix.Limits.*;
import static jdk.internal.sys.posix.PThread.*;
import static jdk.internal.sys.posix.Sched.*;

import static jdk.internal.sys.posix.Time.CLOCK_MONOTONIC;
import static jdk.internal.thread.ThreadNative.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.llvm.LLVM.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import java.lang.module.ModuleDescriptor;
import java.lang.ref.Reference;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.ref.CleanerFactory;
import jdk.internal.thread.ThreadNative;
import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.stackwalk.JavaStackWalker;
import org.qbicc.runtime.stackwalk.StackWalker;
import sun.nio.ch.Interruptible;

/**
 * The qbicc implementation of the {@code Thread} class.
 */
@Tracking("src/java.base/share/classes/java/lang/Thread.java")
public class Thread implements Runnable {

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

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    // ==================================
    //  Public API
    // ==================================

    public static native Thread currentThread();

    public static void yield() {
        if (Build.Target.isPosix()) {
            sched_yield();
        }
        // else no operation
    }

    // Sleep implementation

    public static void sleep(long millis) throws InterruptedException {
        sleep(millis, 0);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (millis == 0 && nanos == 0) {
            return;
        }
        if (millis < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        if (0 < nanos || nanos > 999999) {
            throw new IllegalArgumentException("Invalid nanoseconds");
        }
        sleep0(millis / 1000L, nanos + 1000 * (int)(millis % 1000));
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    @SafePoint(setBits = STATE_WAITING | STATE_SLEEPING, clearBits = STATE_RUNNABLE)
    private static void sleep0(final long seconds, final int nanos) {
        ThreadNative.awaitThreadInboundTimed(seconds, nanos, STATE_INTERRUPTED);
    }

    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
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
        pthread_condattr_t cond_attr = auto();
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
                if (pthread_condattr_init(addr_of(cond_attr)).isNonZero()) abort();
                if (! Build.Target.isMacOs()) {
                    if (pthread_condattr_setclock(addr_of(cond_attr), CLOCK_MONOTONIC).isNonZero()) abort();
                }
                if (pthread_mutex_init(addr_of(deref(threadNativePtr).mutex), zero()).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).inbound_cond), addr_of(cond_attr)).isNonZero()) abort();
                if (pthread_cond_init(addr_of(deref(threadNativePtr).outbound_cond), addr_of(cond_attr)).isNonZero()) abort();
                if (pthread_condattr_destroy(addr_of(cond_attr)).isNonZero()) abort();
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

        // now, the tricky part: we have to add this thread to the big linked list
        // acquire lock
        ThreadNative.lockThreadList();
        try {
            // we hold the big list lock; do ye olde linked list insertion
            deref(thread_list_terminus.prev).next = threadNativePtr;
            deref(threadNativePtr).prev = thread_list_terminus.prev;
            thread_list_terminus.prev = threadNativePtr;
            deref(threadNativePtr).next = addr_of(thread_list_terminus);
        } finally {
            // release lock
            ThreadNative.unlockThreadList();
        }

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
            notifyThreadOutbound();
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

    @SafePoint(SafePointBehavior.NONE)
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
        final ptr<function<pthread_run>> run_fn = addr_of(function.of(ThreadNative::runThreadBody));
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
                    notifyThreadOutbound();
                }
                return;
            }
            oldVal = witness;
        }
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public static boolean interrupted() {
        final ptr<thread_native> threadNativePtr = Thread.currentThread().threadNativePtr;
        // assert threadNativePtr.isNonNull();
        final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
        final int oldVal = statusPtr.getAndBitwiseAnd(word(~ STATE_INTERRUPTED)).intValue();
        return (oldVal & STATE_INTERRUPTED) != 0;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    public boolean isInterrupted() {
        return (getStatus() & STATE_INTERRUPTED) != 0;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
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
                // XXX - unsafe, incorrect
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
        Thread[] threads = getAllThreads();
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

    @SafePoint(SafePointBehavior.NONE)
    private static Thread[] getAllThreads() {
        Thread[] array = new Thread[getSystemThreadGroup().activeCount() + 64];
        ThreadNative.lockThreadList();
        int cnt;
        try {
            cnt = getAllThreadsInternal(array);
        } finally {
            ThreadNative.unlockThreadList();
        }
        return Arrays.copyOf(array, cnt);
    }

    // ensure that no safepoints are possible while lock is held
    @SafePoint(SafePointBehavior.FORBIDDEN)
    @NoThrow
    private static int getAllThreadsInternal(final Thread[] array) {
        int i = 0;
        ptr<thread_native> current = deref(addr_of(thread_list_terminus)).next;
        while (current != addr_of(thread_list_terminus)) {
            array[i++] = deref(current).ref.toObject();
            if (i == array.length) {
                break;
            }
            current = deref(current).next;
        }
        return i;
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

    @NoReturn
    @SafePoint(SafePointBehavior.NONE)
    void end() {
        Thread self = this;
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        // allocate this early
        CleanupAction action = new CleanupAction(threadNativePtr);
        // terminated - clear ALIVE and RUNNABLE and set TERMINATED in one swap
        // remove this thread from the thread list (and set termination flag)
        end2(threadNativePtr, action, self);
    }

    @NoReturn
    @SafePoint(setBits = STATE_TERMINATED, clearBits = STATE_ALIVE | STATE_RUNNABLE)
    private void end2(final ptr<thread_native> threadNativePtr, final CleanupAction action, final Thread self) {
        ThreadNative.lockThreadList_sp();
        try {
            deref(deref(threadNativePtr).prev).next = deref(threadNativePtr).next;
            deref(deref(threadNativePtr).next).prev = deref(threadNativePtr).prev;
            deref(threadNativePtr).next = zero();
            deref(threadNativePtr).prev = zero();
        } finally {
            ThreadNative.unlockThreadList();
        }
        end3(threadNativePtr, action, self);
    }

    @NoReturn
    @SafePoint(SafePointBehavior.EXIT)
    private void end3(final ptr<thread_native> threadNativePtr, final CleanupAction action, final Thread self) {
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
        } else {
            abort();
        }
    }

    @SuppressWarnings("unused") // called by `bind`
    @Hidden
    @NoThrow
    @NoReturn
    @SafePoint(value = SafePointBehavior.EXIT, setBits = STATE_RUNNABLE)
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

    @SafePoint(SafePointBehavior.ALLOWED)
    private int getStatus() {
        final ptr<thread_native> threadNativePtr = this.threadNativePtr;
        return threadNativePtr.isNull() ? 0 : addr_of(deref(threadNativePtr).state).loadSingleAcquire().intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    private int compareAndSwapConfig(int expect, int update) {
        return addr_of(deref(refToPtr(this)).config).compareAndSwap(word(expect), word(update)).intValue();
    }

    /**
     * Set the blocker field; invoked via jdk.internal.access.SharedSecrets from java.nio code
     */
    @SafePoint(SafePointBehavior.NONE)
    static void blockedOn(Interruptible b) {
        Thread.currentThread().blocker = b;
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
