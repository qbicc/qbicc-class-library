package java.io;

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Fcntl.*;

public final class FileOutputStream$_native {
    private FileDescriptor fd;

    private void open0(String name, boolean append) throws FileNotFoundException {
        int flags = O_WRONLY.intValue() | O_CREAT.intValue() | (append ? O_APPEND.intValue() : O_TRUNC.intValue());
        IO_Util.fileOpen(fd, name, word(flags));
    }

    private void write(int b, boolean append) throws IOException {
        IO_Util.writeSingle(fd, (byte)b, append);
    }

    private void writeBytes(byte b[], int off, int len, boolean append) throws IOException {
        IO_Util.writeBytes(fd, b, off, len, append);
    }

    private static void initIDs() {
        // no operation
    }
}
