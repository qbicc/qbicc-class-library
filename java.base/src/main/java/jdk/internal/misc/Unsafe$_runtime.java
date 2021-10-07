package jdk.internal.misc;

import static org.qbicc.runtime.posix.Unistd.*;

import org.qbicc.runtime.Build;

/**
 * Runtime-initialized Unsafe constants.
 */
public final class Unsafe$_runtime {
    static final int PAGE_SIZE;

    static {
        int pageSize;
        if (Build.Target.isPosix()) {
            pageSize = sysconf(_SC_PAGE_SIZE).intValue();
            if (pageSize == -1) {
                throw new InternalError("Can't read page size");
            }
        } else {
            // won't appear in the native image unless it's actually not supported
            throw new UnsupportedOperationException("page_size");
        }
        PAGE_SIZE = pageSize;
    }
}
