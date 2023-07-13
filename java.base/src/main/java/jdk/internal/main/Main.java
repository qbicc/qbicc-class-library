package jdk.internal.main;

import static jdk.internal.sys.stdc.Stdio.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import java.util.ArrayList;

import jdk.internal.gc.Gc;
import jdk.internal.thread.ThreadNative;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoThrow;

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

    static final Thread mainThread = new Thread(ThreadNative.getSystemThreadGroup(), "main") {
        public void start() {
            // can only be started by thread_attach
            throw new IllegalThreadStateException();
        }

        public void run() {
            main0();
        }
    };

    private static ptr<@c_const c_char> exeName;
    private static c_int argc;
    private static ptr<ptr<@c_const c_char>> argv;
    private static c_int sysArgc;
    private static ptr<ptr<@c_const c_char>> sysArgv;

    /**
     * @see ThreadNative#thread_attach
     */
    @extern
    static native void thread_attach(Thread thread);

    @export
    @Hidden
    public static c_int main(c_int argc, ptr<c_char>[] argv) {
        if (strcmp(argv[0], utf8z("jspawnhelper")).isZero()) {
            // todo: jspawnhelper impl
            abort();
        }
        exeName = argv[0];

        Gc.qbicc_initialize_page_size();

        // initial argument processing
        ptr<ptr<c_char>> sysArgv = calloc(argc.cast(), sizeof(ptr.class));
        ptr<ptr<c_char>> userArgv = calloc(argc.cast(), sizeof(ptr.class));
        if (userArgv.isNull()) {
            // not enough memory
            fprintf(stderr, utf8z("Not enough memory to create VM\n"));
            exit(word(1));
        }

        int sysArgc = 0;
        int userArgc = 0;
        long minHeap = -1;
        long maxHeap = -1;
        for (int i = 1; i < argc.intValue(); i ++) {
            if (strncmp(argv[i], utf8z("-X"), word(2)).isZero()) {
                // it's a "JVM" option
                if (strncmp(argv[i].plus(2), utf8z("ms"), word(2)).isZero()) {
                    // min heap size
                    minHeap = parseMemorySize(argv[i].plus(4));
                    if (minHeap < Gc.getPageSize()) {
                        minHeap = Gc.getPageSize();
                    }
                } else if (strncmp(argv[i].plus(2), utf8z("mx"), word(2)).isZero()) {
                    // max heap size
                    maxHeap = parseMemorySize(argv[i].plus(4));
                    if (maxHeap < Gc.getPageSize()) {
                        maxHeap = Gc.getPageSize();
                    }
                } else {
                    fprintf(stderr, utf8z("Unknown or unsupported VM argument: %s\n"), argv[i]);
                    exit(word(1));
                }
                // add to system arguments list
                sysArgv.asArray()[sysArgc++] = argv[i];
            } else {
                // end of VM arguments or first user argument
                int j;
                if (strcmp(argv[i], utf8z("-X-")).isZero()) {
                    j = i + 1;
                } else {
                    j = i;
                }
                for (; j < argc.intValue(); j ++) {
                    userArgv.asArray()[userArgc++] = argv[j];
                }
                break;
            }
        }
        long pageMask = Gc.getPageSize() - 1;
        if (maxHeap == -1 && minHeap == -1) {
            maxHeap = Gc.getConfiguredMaxHeapSize();
            minHeap = Gc.getConfiguredMinHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                maxHeap = minHeap;
            }
        } else if (maxHeap == -1) {
            maxHeap = Gc.getConfiguredMaxHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                // minHeap takes precedence because maxHeap was not given
                maxHeap = minHeap;
            }
        } else if (minHeap == -1) {
            minHeap = Gc.getConfiguredMinHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                // maxHeap takes precedence because minHeap was not given
                minHeap = maxHeap;
            }
        } else {
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                maxHeap = minHeap;
            }
        }

        Main.argc = word(userArgc);
        Main.argv = userArgv;
        Main.sysArgc = word(sysArgc);
        Main.sysArgv = sysArgv;

        Gc.qbicc_initialize_heap(minHeap, maxHeap);

        thread_attach(mainThread);
        // should be unreachable...
        return zero();
    }

    @export(withScope = ExportScope.LOCAL)
    private static long parseMemorySize(final ptr<@c_const c_char> arg) {
        ptr<c_char> endPtr = auto();
        long num = strtoll(arg, addr_of(endPtr), word(10)).longValue();
        char ch = endPtr.loadUnshared(uint8_t.class).charValue();
        if (ch == 0) {
            // just bytes
        } else if (ch == 'T' || ch == 't') {
            // terabytes
            num *= 1L << 40;
            endPtr = endPtr.plus(1);
        } else if (ch == 'G' || ch == 'g') {
            // gigabytes
            num *= 1L << 30;
            endPtr = endPtr.plus(1);
        } else if (ch == 'M' || ch == 'm') {
            // megabytes
            num *= 1L << 20;
            endPtr = endPtr.plus(1);
        } else if (ch == 'K' || ch == 'k') {
            // kilobytes
            num *= 1L << 10;
            endPtr = endPtr.plus(1);
        }
        ch = endPtr.loadUnshared(uint8_t.class).charValue();
        if (ch != 0) {
            fprintf(stderr, utf8z("Invalid memory size: %s\n"), arg);
            exit(word(1));
        }
        return num;
    }

    @Hidden
    @NoThrow
    private static void main0() {
        try {
            // start GC
            Gc.start();
            // initialize the JDK
            System$_patch.rtinitPhase1();
            System$_patch.rtinitPhase2();
            System$_patch.rtinitPhase3();

            // Parse the command line arguments and convert from C to Java
            String[] userArgs = makeJavaStringArray(argv.asArray(), argc.intValue());
            FlightRecorder.vmArgs = makeJavaStringArray(sysArgv.asArray(), sysArgc.intValue());

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

    private static String[] makeJavaStringArray(final ptr<c_char>[] args, final int count) {
        String[] strings = new String[count];
        for (int i = 0; i < count; i ++) {
            strings[i] = utf8zToJavaString(args[i].cast());
        }
        return strings;
    }
}
