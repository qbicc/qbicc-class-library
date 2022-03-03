package sun.nio.fs;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

/**
 *
 */
@PatchClass(UnixFileSystem.class)
@Tracking("src/java.base/unix/classes/java/io/UnixFileSystem.java")
@RunTimeAspect
class UnixFileSystem$_runtime {
    @Add
    static final byte[] RT_DEFAULT_DIR = UnixNativeDispatcher.getcwd();

}
