package sun.nio.ch;

import java.nio.channels.spi.AsynchronousChannelProvider;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qccrt.annotation.Tracking;

@Tracking("java.base/aix/classes/sun/nio/ch/DefaultAsynchronousChannelProvider.java")
@Tracking("java.base/linux/classes/sun/nio/ch/DefaultAsynchronousChannelProvider.java")
@Tracking("java.base/macosx/classes/sun/nio/ch/DefaultAsynchronousChannelProvider.java")
@Tracking("java.base/windows/classes/sun/nio/ch/DefaultAsynchronousChannelProvider.java")
public class DefaultAsynchronousChannelProvider {
    /**
     * Prevent instantiation.
     */
    private DefaultAsynchronousChannelProvider() { }

    /**
     * Returns the default AsynchronousChannelProvider.
     */
    public static AsynchronousChannelProvider create() {
        if (Build.Target.isAix()) {
            return new AixAsynchronousChannelProvider();
        } else if (Build.Target.isLinux()) {
            return new LinuxAsynchronousChannelProvider();
        } else if (Build.Target.isMacOs()) {
            return new BsdAsynchronousChannelProvider();
        } else if (Build.Target.isWindows()) {
            return new WindowsAsynchronousChannelProvider();
        } else {
            throw new Error();
        }
    }
}
