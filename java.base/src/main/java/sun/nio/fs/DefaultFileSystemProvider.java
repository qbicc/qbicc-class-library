package sun.nio.fs;

import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("openjdk/src/java.base/aix/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/linux/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/macosx/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/windows/classes/sun/nio/fs/DefaultFileSystemProvider.java")
public class DefaultFileSystemProvider {
    private static final FileSystemProvider INSTANCE;
    private static final FileSystem THE_FILE_SYSTEM;

    static {
        if (Build.Target.isAix()) {
            AixFileSystemProvider provider = new AixFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isLinux()) {
            LinuxFileSystemProvider provider = new LinuxFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isMacOs()) {
            MacOSXFileSystemProvider provider = new MacOSXFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isWindows()) {
            WindowsFileSystemProvider provider = new WindowsFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else {
            throw new Error();
        }
    }

    private DefaultFileSystemProvider() { }

    /**
     * Returns the platform's default file system provider.
     */
    public static FileSystemProvider instance() {
        return INSTANCE;
    }

    /**
     * Returns the platform's default file system.
     */
    public static FileSystem theFileSystem() {
        return THE_FILE_SYSTEM;
    }

}
