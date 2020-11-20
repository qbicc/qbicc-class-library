package sun.nio.ch;

import java.nio.channels.spi.SelectorProvider;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("openjdk/src/java.base/aix/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("openjdk/src/java.base/linux/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("openjdk/src/java.base/macosx/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("openjdk/src/java.base/windows/classes/sun/nio/ch/DefaultSelectorProvider.java")
public class DefaultSelectorProvider {

    /**
     * Prevent instantiation.
     */
    private DefaultSelectorProvider() { }

    /**
     * Returns the default SelectorProvider.
     */
    public static SelectorProvider create() {
        if (Build.Target.isLinux()) {
            return new EPollSelectorProvider();
        } else if (Build.Target.isMacOs()) {
            return new KQueueSelectorProvider();
        } else if (Build.Target.isWindows()) {
            return new WindowsSelectorProvider();
        } else {
            return new PollSelectorProvider();
        }
    }
}
