package jdk.internal.misc;

import static org.qbicc.runtime.posix.Unistd.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

/**
 * Runtime-initialized Unsafe constants.
 */
@RunTimeAspect
@PatchClass(Unsafe.class)
final class Unsafe$_runtime {
    @Add static final int PAGE_SIZE;

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
