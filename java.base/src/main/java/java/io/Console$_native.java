package java.io;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/native/libjava/Console_md.c")
@Tracking("src/java.base/windows/native/libjava/Console_md.c")
public class Console$_native {

    private static String encoding() {
        if (Build.Target.isUnix()) {
            return null;
        } else if (Build.Target.isWindows()) {
            throw new UnsupportedOperationException();
            /*
            int cp = GetConsoleCP();
            if (cp >= 874 && cp <= 950) {
                return "ms"+cp;
            } else if (cp == 65001) {
                return "UTF-8";
            } else {
                return "cp"+cp;
            }
             */
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
