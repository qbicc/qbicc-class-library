package java.lang;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@RunTimeAspect
@PatchClass(System.class)
public final class System$_runtime {
    public static final InputStream in;
    public static final PrintStream out;
    public static final PrintStream err;

    static {
        // Snippet adapted from System.initPhase1
        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
        in = new BufferedInputStream(fdIn);
        // sun.stdout/err.encoding are set when the VM is associated with the terminal,
        // thus they are equivalent to Console.charset(), otherwise the encoding
        // defaults to Charset.defaultCharset()
        out = System$_aliases.newPrintStream(fdOut, System$_aliases.props.getProperty("sun.stdout.encoding"));
        err = System$_aliases.newPrintStream(fdErr, System$_aliases.props.getProperty("sun.stderr.encoding"));
    }
}
