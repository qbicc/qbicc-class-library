package sun.nio.fs;

import static sun.nio.fs.UnixConstants.*;
import static sun.nio.fs.UnixNativeDispatcher.copyToNativeBuffer;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.host.HostIO;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 *
 */
@PatchClass(UnixNativeDispatcher.class)
class UnixNativeDispatcher$_patch {
    @Replace
    static int stat(UnixPath path) {
        if (Build.isHost()) {
            // we have to reconstruct the UNIX st_mode...
            int mode = 0;
            //    public static native HostBasicFileAttributes stat(String pathName, boolean followLinks) throws IOException;
            BasicFileAttributes attr;
            try {
                attr = HostIO.stat(path.toString(), true);
            } catch (IOException e) {
                return 0;
            }
            if (attr.isDirectory()) {
                mode |= S_IFDIR;
                // todo: UNIX permissions...
                //noinspection OctalInteger
                mode |= 0755;
            }
            if (attr.isRegularFile()) {
                mode |= S_IFREG;
                // todo: UNIX permissions...
                //noinspection OctalInteger
                mode |= 0644;
            }
            return mode;
        }
        try (NativeBuffer buffer = copyToNativeBuffer(path)) {
            return stat1(buffer.address());
        }
    }

    // alias
    private static native int stat1(long pathAddress);
}
