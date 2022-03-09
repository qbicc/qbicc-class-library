package sun.nio.fs;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(UnixFileSystem.class)
@Tracking("src/java.base/unix/classes/java/io/UnixFileSystem.java")
class UnixFileSystem$_patch {

    // Alias
    private final UnixFileSystemProvider provider;
    private final byte[] defaultDirectory;

    private final boolean needToResolveAgainstDefaultDirectory;
    private final UnixPath rootDirectory;

    @Replace
    UnixFileSystem$_patch(UnixFileSystemProvider provider, String dir) {
        this.provider = provider;
        // this value is only used at build time
        this.defaultDirectory = Util.toBytes(UnixPath.normalizeAndCheck(dir));
        this.needToResolveAgainstDefaultDirectory = true;
        this.rootDirectory = new UnixPath((UnixFileSystem) (Object) this, "/");
    }

    @Replace
    byte[] defaultDirectory() {
        if (Build.isHost()) {
            return defaultDirectory;
        } else {
            return UnixFileSystem$_runtime.RT_DEFAULT_DIR;
        }
    }
}
