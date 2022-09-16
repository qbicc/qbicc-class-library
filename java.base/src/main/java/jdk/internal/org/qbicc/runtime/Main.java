package jdk.internal.org.qbicc.runtime;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.pthread_exit;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.strcmp;

import java.util.ArrayList;
import java.util.Arrays;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NotReachableException;
import org.qbicc.runtime.gc.heap.Heap;

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
    @name(value = "__main_argc_argv", when = Build.Target.IsWasi.class)
    public static c_int main(c_int argc, char_ptr[] argv) {
        Heap.initHeap(argc.intValue(), addr_of(argv[0]).cast());

        // first set up VM
        if (! Heap.checkInit(true)) {
            exit(word(1));
        }

        // next set up the initial thread
        attachNewThread("main", getSystemThreadGroup());
        Thread$_patch mainThread = (Thread$_patch)(Object)Thread.currentThread();
        mainThread.initializeNativeFields();
        mainThread.begin();

        try {
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
            String[] args = new String[argc.intValue() - 1];
            int cnt = 0;
            boolean checkForVmArg = true;
            for (int i = 1; i < argc.intValue(); i++) {
                if (checkForVmArg) {
                    if (strcmp(argv[i].cast(), utf8z("--")).isZero()) {
                        checkForVmArg = false;
                    } else if (Heap.isHeapArgument(argv[i].cast())) {
                        continue;
                    }
                }
                args[cnt++] = utf8zToJavaString(argv[i].cast());
            }
            if (cnt < args.length) {
                args = Arrays.copyOf(args, cnt);
            }
            //todo: string construction
            //String execName = utf8zToJavaString(argv[0].cast());

            userMain(args);
        } catch (Throwable t) {
            Thread.UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
            if (handler != null) {
                try {
                    handler.uncaughtException(Thread.currentThread(), t);
                } catch (Throwable t2) {
                    // exception handler threw an exception... just bail out then
                    fprintf(stderr, utf8z("The uncaught exception handler threw an exception or error\n"));
                    fflush(stderr);
                }
            }
            System.exit(1);
        }
        mainThread.end(); // Will return only if mainThread is not the last non-dameon thread
        if (Build.Target.isPosix()) {
            pthread_exit(zero());
        }
        // todo: windows
        throw new NotReachableException();
    }
}
