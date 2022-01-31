package java.io;

import java.util.List;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import org.qbicc.runtime.SerializeAsZero;
import org.qbicc.runtime.SerializeBooleanAs;
import org.qbicc.runtime.SerializeIntegralAs;

@PatchClass(java.io.FileDescriptor.class)
public class FileDescriptor$_patch {

    @Replace
    @SerializeIntegralAs(-1)
    private int fd;

    @Replace
    @SerializeIntegralAs(-1)
    private long handle;

    @Replace
    @SerializeAsZero
    private Closeable parent;

    @Replace
    @SerializeAsZero
    private List<Closeable> otherParents;

    @Replace
    @SerializeBooleanAs(true)
    private boolean closed;
}
