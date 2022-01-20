package sun.nio.ch;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysSocket.*;

import java.io.IOError;
import java.io.IOException;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;
import org.qbicc.runtime.posix.Unistd;

@PatchClass(FileDispatcherImpl.class)
@RunTimeAspect
class FileDispatcherImpl$_runtime {
    @Add
    private static final c_int preCloseFd;

    static {
        if (Build.Target.isPosix()) {
            c_int[] sp = new c_int[2];
            if ((socketpair(AF_UNIX, SOCK_STREAM, zero(), sp).isNegative())) {
                throw new IOError(new IOException("socketpair failed"));
            }
            preCloseFd = sp[0];
            Unistd.close(sp[1]);
        } else {
            // not used on Windows
            preCloseFd = zero();
        }
    }
}
