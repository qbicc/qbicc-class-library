package java.lang;

import static org.qbicc.runtime.posix.Sched.sched_yield;

import java.util.concurrent.locks.LockSupport;

import org.qbicc.runtime.Build;

class Thread$_native {
    private static void registerNatives() {
        // no operation
    }

    public final boolean isAlive() {
        return (((Thread$_patch) (Object) this).threadStatus & Thread$_patch.STATE_ALIVE) != 0;
    }

    public static void yield() {
        if (Build.Target.isPosix()) {
            sched_yield();
        }
        // else no operation
    }

    public static void sleep(long remaining) throws InterruptedException {
        // see Thread$_patch#sleep(long,int)
        Thread.sleep(remaining, 0);
    }

    // TODO: private native void start0();

    public static boolean holdsLock(Object obj) {
        return ((Object$_aliases) obj).holdsLock();
    }

    // TODO: private static native StackTraceElement[][] dumpThreads(Thread[] threads);
    // TODO: private static native Thread[] getThreads();

    private void setPriority0(int newPriority) {
        // no operation
    }

    private void stop0(Object o) {
        throw new UnsupportedOperationException();
    }

    private void suspend0() {
        throw new UnsupportedOperationException();
    }

    private void resume0() {
        throw new UnsupportedOperationException();
    }

    private void interrupt0() {
        // unpark the thread so it can observe the interruption
        LockSupport.unpark((Thread) (Object) this);
    }

    private static void clearInterruptEvent() {
        // no operation
    }

    private void setNativeName(String name) {
        // no operation
    }
}
