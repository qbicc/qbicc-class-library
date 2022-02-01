package java.io;

import static java.io.FileSystem.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysStat.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import java.nio.charset.StandardCharsets;

import org.qbicc.rt.annotation.Tracking;

/**
 *
 */
@Tracking("src/java.base/unix/classes/java/io/UnixFileSystem.java")
@Tracking("src/java.base/unix/native/libjava/UnixFileSystem_md.c")
class UnixFileSystem$_native {
    /**
     * Canonicalize the given path.  Removes all {@code .} and {@code ..} segments from the path.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    private String canonicalize0(String path) {
        final int length = path.length();
        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;
        if (length == 0) {
            return path;
        }
        final char[] targetBuf = new char[length];
        // string segment end exclusive
        int e = length;
        // string cursor position
        int i = length;
        // buffer cursor position
        int a = length - 1;
        // number of segments to skip
        int skip = 0;
        loop:
        while (--i >= 0) {
            char c = path.charAt(i);
            outer:
            switch (c) {
                case '/': {
                    inner:
                    switch (state) {
                        case 0:
                        case 1:
                            state = 3;
                            e = i;
                            break outer;
                        case 2:
                            state = 3;
                            e = i;
                            skip++;
                            break outer;
                        case 3:
                            e = i;
                            break outer;
                        default:
                            throw new IllegalStateException();
                    }
                    // not reached!
                }
                case '.': {
                    inner:
                    switch (state) {
                        case 0:
                        case 3:
                            state = 1;
                            break outer;
                        case 1:
                            state = 2;
                            break outer;
                        case 2:
                            break inner; // emit!
                        default:
                            throw new IllegalStateException();
                    }
                    // fall thru
                }
                default: {
                    final int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;
                    final int segmentLength = e - newE - 1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        if (state == 3) {
                            targetBuf[a--] = '/';
                        }
                        path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        if (state == 3) {
            targetBuf[a--] = '/';
        }
        return new String(targetBuf, a + 1, length - a - 1);
    }

    private static char_ptr mallocPath(File f) {
        byte[] bytes = f.getPath().getBytes(StandardCharsets.UTF_8);
        int len = bytes.length + 1;
        char_ptr ptr = malloc(uword(len + 1));
        if (ptr.isNull()) {
            throw new OutOfMemoryError("malloc");
        }
        copy(ptr.cast(), bytes, 0, len);
        ptr.asArray()[len] = zero();
        return ptr;
    }

    public int getBooleanAttributes0(File f) {
        final struct_stat statBuf = auto();
        final char_ptr pathPtr = mallocPath(f);
        c_int statResult = stat(pathPtr.cast(), addr_of(statBuf));
        free(pathPtr);
        int res = 0;
        if (statResult.isZero()) {
            mode_t mode = addr_of(statBuf.st_mode).loadUnshared();
            mode_t fmt = wordAnd(mode, S_IFMT);
            res = BA_EXISTS;
            if (fmt == S_IFREG) {
                res |= BA_REGULAR;
            } else if (fmt == S_IFDIR) {
                res |= BA_DIRECTORY;
            }
        }
        return res;
    }

    public boolean checkAccess(File f, int chkAccess) {
        final c_int mode = switch (chkAccess) {
            case ACCESS_READ -> R_OK;
            case ACCESS_WRITE -> W_OK;
            case ACCESS_EXECUTE -> X_OK;
            default -> throw new IllegalArgumentException();
        };
        final char_ptr pathPtr = mallocPath(f);
        c_int accessRes = access(pathPtr.cast(), mode);
        free(pathPtr);
        return accessRes.isNonZero();
    }

    private static void initIDs() {
        // no operation
    }
}

