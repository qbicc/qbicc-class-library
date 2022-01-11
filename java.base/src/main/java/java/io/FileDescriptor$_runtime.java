package java.io;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(FileDescriptor.class)
@RunTimeAspect
public class FileDescriptor$_runtime {
    public static final FileDescriptor$_runtime in = new FileDescriptor$_runtime(0);
    public static final FileDescriptor$_runtime out = new FileDescriptor$_runtime(1);
    public static final FileDescriptor$_runtime err = new FileDescriptor$_runtime(2);

    public FileDescriptor$_runtime(final int fd) {
        // alias
    }
}
