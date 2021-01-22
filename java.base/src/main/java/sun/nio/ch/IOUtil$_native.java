package sun.nio.ch;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.Fcntl.*;
import static cc.quarkus.qcc.runtime.posix.Unistd.*;
import static cc.quarkus.qcc.runtime.stdc.Limits.*;

import java.io.IOException;

import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("src/java.base/unix/native/libnio/ch/IOUtil.c")
@Tracking("src/java.base/windows/native/libnio/ch/IOUtil.c")
final class IOUtil$_native {
    private IOUtil$_native() {}

    static void initIDs() {
    }

    static c_int configureBlocking(c_int fd, boolean blocking) {
        c_int flags = fcntl(fd, F_GETFL);
        c_int newFlags = word(blocking ? (flags.intValue() & ~ O_NONBLOCK.intValue()) : (flags.intValue() | O_NONBLOCK.intValue()));
        return (flags == newFlags) ? zero() : fcntl(fd, F_SETFL, newFlags);
    }

    static c_long makePipe(boolean blocking) throws IOException {
        // only called on POSIX.
        c_int[] fd = new c_int[2];
        if (pipe(fd).intValue() < 0) {
            // todo: strerror
            throw new IOException("Pipe failed");
        }
        if (! blocking) {
            if (configureBlocking(fd[0], false).intValue() < 0
                 || configureBlocking(fd[1], false).intValue() < 0) {
                close(fd[0]);
                close(fd[1]);
                // todo: strerror
                throw new IOException("Configure blocking failed");
            }
        }
        return word(fd[0].longValue() << 32 | fd[1].longValue());
    }

    static int iovMax() {
        if (defined(IOV_MAX)) {
            // the original version uses sysconf to get the run time value, but we're statically compiled.
            return IOV_MAX.intValue();
        } else {
            // windows perhaps?
            return 16;
        }
    }
}
