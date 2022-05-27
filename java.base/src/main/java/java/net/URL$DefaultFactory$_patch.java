package java.net;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.Replace;

/**
 *
 */
@Patch("java.net.URL$DefaultFactory")
@Tracking("java.base/classes/share/java/net/URL.java")
class URL$DefaultFactory$_patch {
    // alias
    private static String PREFIX;

    @Replace
    public URLStreamHandler createURLStreamHandler(String protocol) {
        // Avoid using reflection during bootstrap
        switch (protocol) {
            case "file":
                return new sun.net.www.protocol.file.Handler();
            case "jar":
                return new sun.net.www.protocol.jar.Handler();
        }
        String name = PREFIX + protocol + ".Handler";
        try {
            Object o = Class.forName(name).getDeclaredConstructor().newInstance();
            return (URLStreamHandler)o;
        } catch (Exception e) {
            // For compatibility, all Exceptions are ignored.
            // any number of exceptions can get thrown here
        }
        return null;
    }
}
