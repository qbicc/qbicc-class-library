package jdk.internal.misc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.Build;

class Signal$_native {

    private static int findSignal0(String sigName) {
        if (Build.Target.isPosix()) {
            return switch(sigName) {
                case "ABRT" -> org.qbicc.runtime.stdc.Signal.SIGABRT.intValue();
                case "ALRM" -> org.qbicc.runtime.posix.Signal.SIGALRM.intValue();
                case "FPE" -> org.qbicc.runtime.stdc.Signal.SIGFPE.intValue();
                case "HUP" -> org.qbicc.runtime.posix.Signal.SIGHUP.intValue();
                case "ILL" -> org.qbicc.runtime.stdc.Signal.SIGILL.intValue();
                case "INT" -> org.qbicc.runtime.stdc.Signal.SIGINT.intValue();
                case "KILL" -> org.qbicc.runtime.posix.Signal.SIGKILL.intValue();
                case "PIPE" -> org.qbicc.runtime.posix.Signal.SIGPIPE.intValue();
                case "QUIT" -> org.qbicc.runtime.posix.Signal.SIGQUIT.intValue();
                case "SEGV" -> org.qbicc.runtime.stdc.Signal.SIGSEGV.intValue();
                case "TERM" ->  org.qbicc.runtime.stdc.Signal.SIGTERM.intValue();
                default -> -1;
            };
        } else if (Build.Target.isWindows()) {
            return switch(sigName) {
                case "ABRT" -> org.qbicc.runtime.stdc.Signal.SIGABRT.intValue();
                case "BREAK" -> 21; //  Proper symbol + c-compiler lookup for this on windows.
                case "FPE" -> org.qbicc.runtime.stdc.Signal.SIGFPE.intValue();
                case "ILL" -> org.qbicc.runtime.stdc.Signal.SIGILL.intValue();
                case "INT" -> org.qbicc.runtime.stdc.Signal.SIGINT.intValue();
                case "SEGV" -> org.qbicc.runtime.stdc.Signal.SIGSEGV.intValue();
                case "TERM" -> org.qbicc.runtime.stdc.Signal.SIGTERM.intValue();
                default -> -1;
            };
        } else {
            return -1;
        }
    }

    private static long handle0(int sig, long nativeH) {
        // TODO: Real implementation that actually updates a data structure that the hypothetical master signal handler uses.
        return 0; // pretend old handler was default handler
    }
}
