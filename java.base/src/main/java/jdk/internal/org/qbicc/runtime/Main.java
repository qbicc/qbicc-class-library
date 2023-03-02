package jdk.internal.org.qbicc.runtime;

import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoThrow;
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
            throw new IllegalStateException("init actions can only be deferred at build time");
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

    static final Thread mainThread = new Thread(getSystemThreadGroup(), "main") {
        public void start() {
            // can only be started by thread_attach
            throw new IllegalThreadStateException();
        }

        public void run() {
            main0();
        }
    };

    private static c_int argc;
    private static ptr<ptr<c_char>> argv;

    @extern
    static native void thread_attach(Thread thread);

    @export
    @Hidden
    public static c_int main(c_int argc, ptr<c_char>[] argv) {
        Heap.initHeap(argc.intValue(), addr_of(argv[0]).cast());

        // first set up VM
        if (! Heap.checkInit(true)) {
            exit(word(1));
        }

        Main.argc = argc;
        Main.argv = addr_of(argv[0]);

        thread_attach(mainThread);
        // should be unreachable...
        return zero();
    }

    @Hidden
    @NoThrow
    private static void main0() {
        try {
            // initialize the JDK
            System$_patch.rtinitPhase1();
            System$_patch.rtinitPhase2();
            System$_patch.rtinitPhase3();

            // Parse the command line arguments and convert from C to Java
            String[] userArgs = processArgs(argc, argv.asArray());

            // TODO: We should process any non-Heap arguments in FlightRecorder.vmArgs here.

            // next execute additional initialization actions deferred from build time
            for (Runnable r: deferredInits) {
                r.run();
            }
            deferredInits = null;

            // Initialization completed
            FlightRecorder.initDoneTime = System.currentTimeMillis();

            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

            // now cause the initial thread to invoke main
            userMain(userArgs);
        } catch (Throwable t) {
            final Thread thread = Thread.currentThread();
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
            System.exit(1);
        }
    }

    // Convert C args to Java Strings and separate the VM and user arguments
    // Return the subset of argv that are arguments for the user main.
    private static String[] processArgs(c_int argc, ptr<c_char>[] argv) {
        String[] userArgs = new String[argc.intValue() - 1];
        String[] vmArgs = new String[0];
        int userCount = 0;
        boolean checkForVmArg = true;
        for (int i=1; i<argc.intValue(); i++) {
            String jarg = utf8zToJavaString(argv[i].cast());
            if (checkForVmArg) {
                if (!jarg.startsWith("--")) {
                    checkForVmArg = false;
                } else if (Heap.isHeapArgument(argv[i].cast())) {
                    vmArgs = Arrays.copyOf(vmArgs, vmArgs.length+1);
                    vmArgs[vmArgs.length - 1] = jarg;
                    continue;
                }
                // TODO: Define additional vmargs (eg for setting properties)
            }
            userArgs[userCount++] = jarg;
        }

        if (userCount < userArgs.length) {
            userArgs = Arrays.copyOf(userArgs, userCount);
        }
        FlightRecorder.vmArgs = vmArgs;

        return userArgs;
    }
}
