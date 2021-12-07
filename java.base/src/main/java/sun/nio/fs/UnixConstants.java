/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import static org.qbicc.runtime.CNative.defined;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.posix.Errno;
import org.qbicc.runtime.posix.Fcntl;
import org.qbicc.runtime.posix.SysStat;
import org.qbicc.runtime.posix.Unistd;
import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/unix/classes/sun/nio/fs/UnixConstants.java.template")
final class UnixConstants {
    private UnixConstants() {}

    static final int O_RDONLY = Fcntl.O_RDONLY.intValue();
    static final int O_WRONLY = Fcntl.O_WRONLY.intValue();
    static final int O_RDWR = Fcntl.O_RDWR.intValue();
    static final int O_APPEND = Fcntl.O_APPEND.intValue();
    static final int O_CREAT = Fcntl.O_CREAT.intValue();
    static final int O_EXCL = Fcntl.O_EXCL.intValue();
    static final int O_TRUNC = Fcntl.O_TRUNC.intValue();
    static final int O_SYNC = Fcntl.O_SYNC.intValue();
    static final int O_DSYNC = Fcntl.O_DSYNC.intValue();
    static final int O_NOFOLLOW = Fcntl.O_NOFOLLOW.intValue();
    static final int O_DIRECT = Fcntl.O_DIRECT.intValue();

    static final int S_IRUSR = SysStat.S_IRUSR.intValue();
    static final int S_IWUSR = SysStat.S_IWUSR.intValue();
    static final int S_IXUSR = SysStat.S_IXUSR.intValue();
    static final int S_IRGRP = SysStat.S_IRGRP.intValue();
    static final int S_IWGRP = SysStat.S_IWGRP.intValue();
    static final int S_IXGRP = SysStat.S_IXGRP.intValue();
    static final int S_IROTH = SysStat.S_IROTH.intValue();
    static final int S_IWOTH = SysStat.S_IWOTH.intValue();
    static final int S_IXOTH = SysStat.S_IXOTH.intValue();
    static final int S_IFMT = SysStat.S_IFMT.intValue();
    static final int S_IFREG = SysStat.S_IFREG.intValue();
    static final int S_IFDIR = SysStat.S_IFDIR.intValue();
    static final int S_IFLNK = SysStat.S_IFLNK.intValue();
    static final int S_IFCHR = SysStat.S_IFCHR.intValue();
    static final int S_IFBLK = SysStat.S_IFBLK.intValue();
    static final int S_IFIFO = SysStat.S_IFIFO.intValue();

    static final int S_IAMB = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH;

    static final int R_OK = Unistd.R_OK.intValue();
    static final int W_OK = Unistd.W_OK.intValue();
    static final int X_OK = Unistd.X_OK.intValue();
    static final int F_OK = Unistd.F_OK.intValue();

    static final int ENOENT = Errno.ENOENT.intValue();
    static final int ENXIO = Errno.ENXIO.intValue();
    static final int EACCES = Errno.EACCES.intValue();
    static final int EEXIST = Errno.EEXIST.intValue();
    static final int ENOTDIR = Errno.ENOTDIR.intValue();
    static final int EINVAL = Errno.EINVAL.intValue();
    static final int EXDEV = Errno.EXDEV.intValue();
    static final int EISDIR = Errno.EISDIR.intValue();
    static final int ENOTEMPTY = Errno.ENOTEMPTY.intValue();
    static final int ENOSPC = Errno.ENOSPC.intValue();
    static final int EAGAIN = Errno.EAGAIN.intValue();
    static final int EWOULDBLOCK = Errno.EWOULDBLOCK.intValue();
    static final int ENOSYS = Errno.ENOSYS.intValue();
    static final int ELOOP = Errno.ELOOP.intValue();
    static final int EROFS = Errno.EROFS.intValue();

    static final int ENODATA = defined(Errno.ENODATA) ? Errno.ENODATA.intValue() : 0;

    // todo: ENOATTR when _ALLBSD_SOURCE is set
    // todo: 93 is the value of ENOATTR on Mac OS X
    static final int XATTR_NOT_FOUND = Build.Target.isLinux() ? ENODATA : Build.Target.isMacOs() ? 93 : 0;

    static final int ERANGE = Errno.ERANGE.intValue();
    static final int EMFILE = Errno.EMFILE.intValue();

    static final int AT_SYMLINK_NOFOLLOW = defined(Fcntl.AT_SYMLINK_NOFOLLOW) ? Fcntl.AT_SYMLINK_NOFOLLOW.intValue() : 0;
    static final int AT_REMOVEDIR = defined(Fcntl.AT_REMOVEDIR) ? Fcntl.AT_REMOVEDIR.intValue() : 0;
}
