package java.lang;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.qbicc.runtime.patcher.PatchClass;

/**
 * This patch exposes private fields/methods of System via alises
 * to the other patch classes for System.
 */
@PatchClass(System.class)
final class System$_aliases {
    static Properties props;

    static native PrintStream newPrintStream(FileOutputStream fos, String enc);
}