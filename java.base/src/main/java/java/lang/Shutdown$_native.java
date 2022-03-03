package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import org.qbicc.runtime.Build;

class Shutdown$_native {
    static void beforeHalt() {
        // no operation
    }

    static void halt0(int status) {
        if (Build.isHost()) {
            throw new SecurityException("exit() during build");
        }
        // todo: safepoint?
        // todo: wait for keypress at exit (windows)
        exit(word(status));
    }
}
