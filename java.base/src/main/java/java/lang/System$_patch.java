package java.lang;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(System.class)
public final class System$_patch {
    // Alias
    static native void setIn0(InputStream in);
    // Alias
    static native void setOut0(PrintStream out);
    // Alias
    static native void setErr0(PrintStream err);
    // Alias
    private static Properties props;
    // Alias
    static native PrintStream newPrintStream(FileOutputStream fos, String enc);

    @Replace
    public static SecurityManager getSecurityManager() {
        return null;
    }

    @Replace
    public static void loadLibrary(String libname) {
        switch (libname) {
            case "extnet":
            case "net":
            case "nio":
            case "prefs":
            case "zip":
                return;
            default:
                throw new UnsatisfiedLinkError("Can't load " + libname);
        }
    }

    // The portions of System.initPhase1 that need to be re-executed at runtime
    @Add
    public static void rtinitPhase1() {
        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
        setIn0(new BufferedInputStream(fdIn));
        // sun.stdout/err.encoding are set when the VM is associated with the terminal,
        // thus they are equivalent to Console.charset(), otherwise the encoding
        // defaults to Charset.defaultCharset()
        setOut0(newPrintStream(fdOut, props.getProperty("sun.stdout.encoding")));
        setErr0(newPrintStream(fdErr, props.getProperty("sun.stderr.encoding")));

        // Setup Java signal handlers for HUP, TERM, and INT (where available).
        Terminator.setup();
    }

    // The portions of System.initPhase2 that need to be re-executed at runtime
    @Add
    public static void rtinitPhase2() {
    }

    // The portions of System.initPhase3 that need to be re-executed at runtime
    @Add
    public static void rtinitPhase3() {
    }
}
