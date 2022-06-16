/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * ------
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */
package java.io;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.Fcntl.*;
import static org.qbicc.runtime.posix.SysStat.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import java.nio.charset.Charset;
import java.util.Objects;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.host.HostIO;
import org.qbicc.runtime.posix.Fcntl;

@SuppressWarnings({ "SpellCheckingInspection", "ConstantConditions" })
@Tracking("src/java.base/share/native/libjava/io_util.h")
@Tracking("src/java.base/share/native/libjava/io_util.c")
@Tracking("src/java.base/unix/native/libjava/io_util_md.c")
@Tracking("src/java.base/windows/native/libjava/io_util_md.c")
final class IO_Util {
    // this is what OpenJDK presently defines in io_util.c
    private static final int BUF_SIZE = 8192;

    static int readSingle(final FileDescriptor fd) throws IOException {
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.isHost()) {
            int fdes = ((FileDescriptor$_native) (Object) fd).fd;
            return HostIO.readSingle(fdes);
        } else if (Build.Target.isPosix()) {
            c_char ret = auto();
            int cnt = handleRead(fd, addr_of(ret), 1).intValue();
            if (cnt == 0) {
                // eof
                return -1;
            } else if (cnt == -1) {
                // todo: JNU_ThrowIOExceptionWithLastError
                throw new IOException("Read error");
            }
            return ret.byteValue() & 0xff;
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static void writeSingle(final FileDescriptor fd, byte b, boolean append) throws IOException {
        // todo: change to `byte bv = auto(word(b))` after qbicc release
        int8_t bv = auto();
        addr_of(bv).storeUnshared(word(b));
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.isHost()) {
            int fdes = ((FileDescriptor$_native) (Object) fd).fd;
            if (append) {
                HostIO.appendSingle(fdes, b);
            } else {
                HostIO.writeSingle(fdes, b);
            }
        } else if (Build.Target.isPosix()) {
            int cnt = handleWrite(fd, addr_of(bv).cast(), 1).intValue();
            if (cnt == -1) {
                // todo: JNU_ThrowIOExceptionWithLastError
                throw new IOException("Write error");
            }
        } else if (Build.Target.isWindows()) {
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static char_ptr mallocStringChars(final String str) throws OutOfMemoryError {
        byte[] bytes = str.getBytes(Charset.defaultCharset());
        char_ptr ptr = malloc(word(bytes.length + 1));
        if (ptr.isNull()) {
            throw new OutOfMemoryError("malloc failed");
        }
        copy(ptr.cast(), bytes, 0, bytes.length);
        ptr.asArray()[bytes.length] = zero();
        return ptr;
    }

    static long handleOpen(ptr<@c_const c_char> path, c_int flags, c_int mode) {
        if (Build.Target.isPosix()) {
            c_int ofd;
            do {
                ofd = open(path.cast(), flags, mode.cast());
            } while (ofd.intValue() == -1 && errno == EINTR.intValue());
            if (ofd.intValue() == -1) {
                return -1;
            }
            struct_stat buf = auto();
            int result;
            do {
                result = fstat(ofd, addr_of(buf)).intValue();
            } while (result == -1 && errno == EINTR.intValue());
            if (result == -1) {
                close(ofd);
                return -1;
            }
            if ((buf.st_mode.longValue() & S_IFDIR.longValue()) != 0) {
                close(ofd);
                errno = EISDIR.intValue();
                return -1;
            }
            return ofd.longValue();
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static void fileOpen(final FileDescriptor fd, final String name, final c_int flags) throws FileNotFoundException {
        if (Build.isHost()) {
            // convert native flags to host flags
            int nativeFlags = flags.intValue();
            int hostFlags;
            if ((nativeFlags & O_ACCMODE.intValue()) == O_RDONLY.intValue()) {
                hostFlags = HostIO.O_RDONLY;
            } else if ((nativeFlags & O_ACCMODE.intValue()) == O_WRONLY.intValue()) {
                hostFlags = HostIO.O_WRONLY;
            } else if ((nativeFlags & O_ACCMODE.intValue()) == O_RDWR.intValue()) {
                hostFlags = HostIO.O_RDWR;
            } else {
                // impossible
                throw new IllegalStateException();
            }
            if ((nativeFlags & O_APPEND.intValue()) != 0) {
                hostFlags |= HostIO.O_APPEND;
            }
            if ((nativeFlags & O_CREAT.intValue()) != 0) {
                hostFlags |= HostIO.O_CREAT;
            }
            if ((nativeFlags & O_EXCL.intValue()) != 0) {
                hostFlags |= HostIO.O_EXCL;
            }
            if ((nativeFlags & O_TRUNC.intValue()) != 0) {
                hostFlags |= HostIO.O_TRUNC;
            }
            try {
                //noinspection OctalInteger
                ((FileDescriptor$_native) (Object) fd).fd = HostIO.open(name, hostFlags, 0666);
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                FileNotFoundException fnfe = new FileNotFoundException(name);
                fnfe.initCause(e);
                throw fnfe;
            }
        } else if (Build.Target.isPosix()) {
            char_ptr ptr = mallocStringChars(name);
            try {
                if (Build.Target.isLinux() /* todo: || defined(_ALLBSD_SOURCE) */) {
                    char_ptr p = ptr.plus(strlen(ptr.cast()).intValue() - 1);
                    while (p.isGt(ptr) && (p.loadUnshared().byteValue() == (byte)'/')) {
                        p.storeUnshared(zero());
                        p = p.minus(1);
                    }
                }
                //noinspection OctalInteger
                int fdes = (int) handleOpen(ptr, flags, word(0666));
                if (fdes == - 1) {
                    throw new FileNotFoundException(name);
                }
                ((FileDescriptor$_native)(Object)fd).fd = fdes;
                ((FileDescriptor$_native)(Object)fd).append = (flags.intValue() & O_APPEND.intValue()) != 0;
            } finally {
                free(ptr);
            }
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static int readBytes(final FileDescriptor fd, final byte[] b, final int off, final int len) throws IOException {
        Objects.requireNonNull(b, "b");
        if (off < 0 || len < 0 || b.length - off < len) {
            throw new IndexOutOfBoundsException();
        }
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.isHost()) {
            return HostIO.read(((FileDescriptor$_native)(Object)fd).fd, b, off, len);
        }
        int nread;
        //todo: <T, P extends ptr<T>> P alloca(Class<T> type, int count); // and variations
        //then, char_ptr stackBuf = alloca(c_char.class, BUF_SIZE);
        char_ptr stackBuf = alloca(word(sizeof(c_char.class).longValue() * BUF_SIZE));
        char_ptr buf;

        if (len == 0) {
            return 0;
        } else if (len > BUF_SIZE) {
            buf = malloc(word(len));
            if (buf.isNull()) {
                throw new OutOfMemoryError();
            }
        } else {
            buf = stackBuf;
        }
        try {
            nread = handleRead(fd, buf, len).intValue();
            if (nread > 0) {
                copy(b, off, len, buf.cast());
                return nread;
            } else if (nread == -1) {
                // todo: JNU_ThrowIOExceptionWithLastError
                throw new IOException("Read error");
            } else {
                // eof
                return -1;
            }
        } finally {
            if (buf != stackBuf) {
                free(buf);
            }
        }
    }

    static void writeBytes(final FileDescriptor fd, byte[] b, int off, int len, boolean append) throws IOException {
        Objects.requireNonNull(b, "b");
        if (off < 0 || len < 0 || b.length - off < len) {
            throw new IndexOutOfBoundsException();
        }
        if (!fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.isHost()) {
            while (len > 0) {
                int fdNum = ((FileDescriptor$_native) (Object) fd).fd;
                int cnt = append ? HostIO.append(fdNum, b, off, len) : HostIO.write(fdNum, b, off, len);
                len -= cnt;
                off += cnt;
            }
            return;
        }
        while (len > 0) {
            ssize_t nw;
            if (Build.Target.isPosix() || !append) {
                nw = handleWrite(fd, addr_of(b[off]).cast(), len);
            } else {
                nw = handleAppend(fd, addr_of(b[off]).cast(), len);
            }
            len -= nw.intValue();
            off += nw.intValue();
        }
    }

    static long IO_GetLength(final FileDescriptor fd) {
        if (Build.isHost()) {
            try {
                return HostIO.getFileSize(((FileDescriptor$_native)(Object)fd).fd);
            } catch (IOException e) {
                return -1;
            }
        } else if (Build.Target.isWindows()) {
            // todo: GetFileSizeEx();
            throw new UnsupportedOperationException();
        } else if (Build.Target.isPosix()) {
            int fdes = ((FileDescriptor$_native) (Object) fd).fd;
            struct_stat buf = auto();
            int res;
            do {
                res = fstat(word(fdes), addr_of(buf)).intValue();
            } while (res == -1 && errno == EINTR.intValue());
            if (res < 0) {
                return -1;
            }
            // todo: if (Build.Target.isLinux()) .. use ioctl for BLKGETSIZE64
            return buf.st_size.longValue();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static long handleLseek(final FileDescriptor fd, final long offs, final c_int whence) {
        if (Build.isHost()) {
            try {
                if (whence == Fcntl.SEEK_CUR) {
                    return HostIO.seekRelative(((FileDescriptor$_native) (Object) fd).fd, offs);
                } else if (whence == Fcntl.SEEK_SET) {
                    return HostIO.seekAbsolute(((FileDescriptor$_native) (Object) fd).fd, offs);
                } else {
                    return -1;
                }
            } catch (IOException e) {
                return -1;
            }
        }
        if (Build.Target.isPosix()) {
            int fdes = ((FileDescriptor$_native) (Object) fd).fd;
            return lseek(word(fdes), word(offs), whence).longValue();
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static long handleAvailable(final FileDescriptor fd) {
        if (Build.isHost()) {
            long available;
            try {
                available = HostIO.available(((FileDescriptor$_native)(Object)fd).fd);
            } catch (IOException e) {
                return -1;
            }
            return available;
        } else if (Build.Target.isPosix()) {
            // todo: requires Unistd.ioctl(), FIONREAD
            throw new UnsupportedOperationException();
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static ssize_t handleRead(final FileDescriptor fd, final ptr<c_char> buf, final int cnt) throws IOException {
        if (! fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.Target.isPosix()) {
            int fdes = ((FileDescriptor$_native) (Object) fd).fd;
            ssize_t result;
            do {
                result = read(word(fdes), buf.cast(), word(cnt));
            } while (result.longValue() == -1 && errno == EINTR.intValue());
            return result;
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static ssize_t handleWrite(final FileDescriptor fd, final ptr<c_char> buf, final int cnt) throws IOException {
        if (!fd.valid()) {
            throw new IOException("Stream closed");
        }
        if (Build.Target.isPosix()) {
            int fdes = ((FileDescriptor$_native)(Object)fd).fd;
            ssize_t result;
            do {
                result = write(word(fdes), buf.cast(), word(cnt));
            } while (result.longValue() == -1 && errno == EINTR.intValue());
            return result;
        } else if (Build.Target.isWindows()) {
            // todo
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static ssize_t handleAppend(final FileDescriptor fd, final ptr<c_char> buf, final int cnt) throws IOException {
        throw new UnsupportedOperationException();
    }
}
