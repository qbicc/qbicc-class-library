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

import static java.io.FileSystem.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.SysStat.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import java.nio.charset.StandardCharsets;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.host.HostIO;

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
        if (Build.isHost()) {
            try {
                return HostIO.getBooleanAttributes(f.toString());
            } catch (IOException ignored) {
                return 0;
            }
        }
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
        if (Build.isHost()) {
            try {
                HostIO.checkAccess(f.toString());
                return true;
            } catch (IOException ignored) {
                return false;
            }
        }
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

    public long getLength(File f) {
        if (Build.isHost()) {
            try {
                return HostIO.stat(f.toString(), true).size();
            } catch (IOException e) {
                return 0;
            }
        }
        final char_ptr pathPtr = mallocPath(f);
        struct_stat sb = auto();
        long rv = 0;
        if (stat(pathPtr.cast(), addr_of(sb)).isZero()) {
            rv = sb.st_size.longValue();
        }
        free(pathPtr);
        return rv;
    }

    public boolean setPermission(File f, int access, boolean enable, boolean owneronly) {
        if (Build.isHost()) {
            return true; // Ignore permissions when simulating with HostIO
        }
        int amode = switch (access) {
            case FileSystem.ACCESS_READ ->
                    owneronly ? S_IRUSR.intValue() : S_IRUSR.intValue() | S_IRGRP.intValue() | S_IROTH.intValue();
            case FileSystem.ACCESS_WRITE ->
                    owneronly ? S_IWUSR.intValue() : S_IWUSR.intValue() | S_IWGRP.intValue() | S_IWOTH.intValue();
            case FileSystem.ACCESS_EXECUTE ->
                    owneronly ? S_IXUSR.intValue() : S_IXUSR.intValue() | S_IXGRP.intValue() | S_IXOTH.intValue();
            default ->
                throw new IllegalArgumentException("Unrecognized access mode "+access);
        };

        final char_ptr pathPtr = mallocPath(f);
        try {
            struct_stat sb = auto();
            if (!stat(pathPtr.cast(), addr_of(sb)).isZero()) {
                return false;
            }
            int mode = sb.st_mode.intValue();
            if (enable) {
                mode |= amode;
            } else {
                mode &= ~amode;
            }
            while (true) {
                c_int res = chmod(pathPtr.cast(), word(mode));
                if (res.isZero()) {
                    return true;
                } else if (errno != EINTR.intValue()) {
                    return false;
                }
            }
        } finally {
            free(pathPtr);
        }
    }

    public boolean createDirectory(File f) {
        final char_ptr pathPtr = mallocPath(f);
        c_int mkdirRes = mkdir(pathPtr.cast(), word(0777));
        free(pathPtr);
        return mkdirRes.intValue() == 0;
    }

    private static void initIDs() {
        // no operation
    }
}

