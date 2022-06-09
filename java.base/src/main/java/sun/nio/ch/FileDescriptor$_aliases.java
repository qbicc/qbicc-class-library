package sun.nio.ch;

import java.io.FileDescriptor;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(FileDescriptor.class)
class FileDescriptor$_aliases {
    // alias
    int fd;
}
