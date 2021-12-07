package java.io;

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;

import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.CNative.*;

public final class FileOutputStream$_native {
    private FileDescriptor fd;

    private static final JavaIOFileDescriptorAccess fdAccess =
        SharedSecrets.getJavaIOFileDescriptorAccess();

    @extern
    public static native size_t write(int fd, void_ptr buf, size_t nbyte);

    /**
     * Opens a file, with the specified name, for overwriting or appending.
     * @param name name of file to be opened
     * @param append whether the file is to be opened in append mode
    private void open0(String name, boolean append) throws FileNotFoundException {
    }
    */

    private void write(int b, boolean append) throws IOException {
        byte[] buf = { (byte)b };
        writeBytes(buf, 0, 1, append);
    }

    // TODO: Need a windows implementation; this blindly assumes unix
    // Note: The value of append is ignored because in unix io_util_md.h, IO_Append and IO_Write both go to write.
    private void writeBytes(byte b[], int off, int len, boolean append) throws IOException {
        if (off < 0 || len < 0 || b.length - off < len) {
            throw new IndexOutOfBoundsException();
        }

        while (len > 0) {
            int nfd = fdAccess.get(fd);
            if (nfd == - 1) {
                throw new IOException("Stream Closed");
            }

            size_t nw  = write(nfd, addr_of(b[off]).cast(), word((long)len));
            if (nw.longValue() < 0) {
                // TODO: The JDK native handles EINTR as a non-exception (just try again..).
                throw new IOException();
            }
            off += nw.intValue();
            len -= nw.intValue();
        }
    }
}
