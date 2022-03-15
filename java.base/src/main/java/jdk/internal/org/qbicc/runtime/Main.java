package jdk.internal.org.qbicc.runtime;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.pthread_exit;

import java.util.ArrayList;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NotReachableException;

/**
 * Holds the native image main entry point.
 */
public final class Main {
    /**
     * A list of initialization actions deferred from build time to runtime.
     * These will be executed in the order they were enqueued by #deferInitAction
     * after the core JDK is fully booted but before the application main method is invoked.
     */
    private static ArrayList<Runnable> deferredInits = new ArrayList<>();

    private Main() {
    }

    /**
     * Enqueue an initialization action at build time that will be performed
     * at runtime before the application-level main method is invoked.
     */
    public static void deferInitAction(Runnable r) {
        if (!Build.isHost()) {
            throw new IllegalStateException("init actions can only be deferrred at build time");
        }
        synchronized (deferredInits) {
            deferredInits.add(r);
        }
    }

    /**
     * This is a stub that gets redirected to the real main method by the main method plugin.
     *
     * @param args the arguments to pass to the main program
     */
    static native void userMain(String[] args);

    static native ThreadGroup getSystemThreadGroup();

    @export
    @Hidden
    public static c_int main(c_int argc, char_ptr[] argv) {

        // first set up VM
        // ...
        // next set up the initial thread
        attachNewThread("main", getSystemThreadGroup());
        ((Thread$_patch)(Object)Thread.currentThread()).initializeNativeFields();

        // next initialize the JDK
        System$_patch.rtinitPhase1();
        System$_patch.rtinitPhase2();
        System$_patch.rtinitPhase3();

        // next execute additional initialization actions deferred from build time
        /*
         * TODO: Have to sort out threading story first so that deamon threads created by
         *       deferredInits don't prevent the process from exiting.
        for (Runnable r: deferredInits) {
            r.run();
        }
        */

        // now cause the initial thread to invoke main
        final String[] args = new String[argc.intValue() - 1];
        for (int i = 1; i < argc.intValue(); i++) {
            args[i - 1] = utf8zToJavaString(argv[i].cast());
        }
        //todo: string construction
        //String execName = utf8zToJavaString(argv[0].cast());
        try {
            userMain(args);
        } catch (Throwable t) {
            Thread.UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
            if (handler != null) {
                handler.uncaughtException(Thread.currentThread(), t);
            }
        }
        if (Build.Target.isPosix()) {
            pthread_exit(zero());
        }
        // todo: windows
        throw new NotReachableException();
    }
}
