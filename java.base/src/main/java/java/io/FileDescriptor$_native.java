package java.io;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.Fcntl.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.errno;

import org.qbicc.runtime.Build;

public class FileDescriptor$_native {
    int fd;
    boolean append;

    private static long getHandle(int fd) {
        if (Build.Target.isPosix()) {
            return -1;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean getAppend(int fd) {
        if (Build.Target.isPosix()) {
            c_int res = fcntl(word(fd), F_GETFL);
            return (res.intValue() & O_APPEND.intValue()) != 0;
        } else {
            return false;
        }
    }

    private void close0() throws IOException {
        int fd = this.fd;
        if (fd == -1) {
            return;
        }
        if (Build.Target.isPosix()) {
            if (0 <= fd && fd <= 2) {
                // stdin, stdout, or stderr... redirect to `/dev/null` in the same manner as OpenJDK
                c_int res = open(utf8z("/dev/null"), O_WRONLY);
                if (res.isLt(zero())) {
                    throw new IOException("open /dev/null failed");
                }
                dup2(res, word(fd));
                close(res);
                return;
            }
            this.fd = -1;
            c_int res;
            if (Build.Target.isAix()) {
                do {
                    res = close(word(fd));
                } while (res.intValue() == -1 && errno == EINTR.intValue());
                if (res.intValue() == -1) {
                    // todo: safe strerror...
                    throw new IOException("close failed");
                }
            } else {
                res = close(word(fd));
                if (res.intValue() == -1 && errno != EINTR.intValue()) {
                    // todo: safe strerror...
                    throw new IOException("close failed");
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void initIDs() {
        // no operation
    }

    public void sync() throws SyncFailedException {
        if (Build.Target.isPosix()) {
            c_int res = fsync(word(fd));
            if (res.intValue() == -1) {
                throw new SyncFailedException("sync failed");
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
