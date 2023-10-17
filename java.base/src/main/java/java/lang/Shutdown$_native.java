package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.NoReturn;

class Shutdown$_native {
    static void beforeHalt() {
        // no operation
    }

    @NoReturn
    static void halt0(int status) {
        if (Build.isHost()) {
            throw new SecurityException("exit() during build");
        }
        // todo: wait for keypress at exit (windows)
        exit(word(status));
    }
}
