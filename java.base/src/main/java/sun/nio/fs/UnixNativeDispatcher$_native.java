package sun.nio.fs;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.SysStat.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.F_OK;
import static org.qbicc.runtime.stdc.Errno.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.posix.Fcntl;
import org.qbicc.runtime.posix.SysStat;
import org.qbicc.runtime.posix.Unistd;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Tracking("src/java.base/unix/native/libnio/fs/UnixNativeDispatcher.c")
class UnixNativeDispatcher$_native {
    static byte[] getcwd() {
        return new byte[0];
    }

    static int dup(int filedes) throws UnixException {
        int res;
        do {
            res = Unistd.dup(word(filedes)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
        return res;
    }

    static int open0(long pathAddress, int flags, int mode) throws UnixException {
        int fd;
        do {
            fd = Fcntl.open(word(pathAddress), word(flags), word(mode)).intValue();
        } while (fd == -1 && errno == EINTR.intValue());
        if (fd == -1) {
            throw new UnixException(errno);
        }
        throw new UnixException(errno);
    }

    static int openat0(int dfd, long pathAddress, int flags, int mode) throws UnixException {
        int fd;
        do {
            fd = Fcntl.openat(word(dfd), word(pathAddress), word(flags), word(mode)).intValue();
        } while (fd == -1 && errno == EINTR.intValue());
        if (fd == -1) {
            throw new UnixException(errno);
        }
        return fd;
    }

    // NOTE: the JDK does not declare this exception but the impl can still throw it.
    // Reported upstream: JDK-
    static void close0(int fd) throws UnixException {
        int res;
        do {
            res = Unistd.close(word(fd)).intValue();
        } while (Build.Target.isAix() && res == -1 && errno == EINTR.intValue());
        if (res == -1 && errno != EINTR.intValue()) {
            throw new UnixException(errno);
        }
    }

    static void rewind(long stream) throws UnixException {
        // todo: requires Stdio.rewind()
        throw new UnsupportedOperationException();
    }

    static int getlinelen(long stream) throws UnixException {
        // todo: requires Stdio.getline(), Stdio.feof()
        throw new UnsupportedOperationException();
    }

    static void link0(long existingAddress, long newAddress) throws UnixException {
        // todo: requires Unistd.link()
        throw new UnsupportedOperationException();
    }

    static void unlink0(long pathAddress) throws UnixException {
        // not restartable (no EINTR)
        if (Unistd.unlink(word(pathAddress)) == word(-1)) {
            throw new UnixException(errno);
        }
    }

    static void unlinkat0(int dfd, long pathAddress, int flag) throws UnixException {
        // todo: requires Unistd.unlinkat()
        throw new UnsupportedOperationException();
    }

    static void mknod0(long pathAddress, int mode, long dev) throws UnixException {
        // todo: requires SysStat.mknod()
        throw new UnsupportedOperationException();
    }

    static void rename0(long fromAddress, long toAddress) throws UnixException {
        // todo: requires Stdio.rename()
        throw new UnsupportedOperationException();
    }

    static void renameat0(int fromfd, long fromAddress, int tofd, long toAddress) throws UnixException {
        // todo: requires Stdio.renameat()
        throw new UnsupportedOperationException();
    }

    static void mkdir0(long pathAddress, int mode) throws UnixException {
        // todo: requires SysStat.mkdir()
        throw new UnsupportedOperationException();
    }

    static void rmdir0(long pathAddress) throws UnixException {
        // todo: requires Unistd.rmdir()
        throw new UnsupportedOperationException();
    }

    static byte[] readlink0(long pathAddress) throws UnixException {
        // todo: we have Unistd.readlink() but we do not have Limits.PATH_MAX
        throw new UnsupportedOperationException();
    }

    static byte[] realpath0(long pathAddress) throws UnixException {
        // todo: requires Unistd.realpath() and Limits.PATH_MAX
        throw new UnsupportedOperationException();
    }

    static void symlink0(long name1, long name2) throws UnixException {
        // todo: requires Unistd.symlink()
        throw new UnsupportedOperationException();
    }

    static void stat0(long pathAddress, UnixFileAttributes$_aliases attrs) throws UnixException {
        struct_stat buf = auto();
        int res;
        do {
            res = SysStat.stat(word(pathAddress), addr_of(buf)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
        prepAttributes(addr_of(buf), attrs);
    }

    static int stat1(long pathAddress) {
        struct_stat buf = auto();
        int res;
        do {
            res = SysStat.stat(word(pathAddress), addr_of(buf)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            return 0;
        } else {
            return buf.st_mode.intValue();
        }
    }

    static void lstat0(long pathAddress, UnixFileAttributes$_aliases attrs) throws UnixException {
        struct_stat buf = auto();
        int res;
        do {
            res = SysStat.lstat(word(pathAddress), addr_of(buf)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
        prepAttributes(addr_of(buf), attrs);
   }

    static void fstat(int fd, UnixFileAttributes$_aliases attrs) throws UnixException {
        struct_stat buf = auto();
        int res;
        do {
            res = SysStat.fstat(word(fd), addr_of(buf)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
        prepAttributes(addr_of(buf), attrs);
    }

    static void fstatat0(int dfd, long pathAddress, int flag, UnixFileAttributes$_aliases attrs) throws UnixException {
        struct_stat buf = auto();
        int res;
        do {
            res = SysStat.fstatat(word(dfd), word(pathAddress), addr_of(buf), word(flag)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
        prepAttributes(addr_of(buf), attrs);
    }

    static void chown0(long pathAddress, int uid, int gid) throws UnixException {
        // todo: requires Unistd.chown()
        throw new UnsupportedOperationException();
    }

    static void lchown0(long pathAddress, int uid, int gid) throws UnixException {
        // todo: requires Unistd.lchown()
        throw new UnsupportedOperationException();
    }

    static void fchown(int fd, int uid, int gid) throws UnixException {
        // todo: requires Unistd.fchown()
        throw new UnsupportedOperationException();
    }

    static void chmod0(long pathAddress, int mode) throws UnixException {
        // todo: requires SysStat.chmod()
        throw new UnsupportedOperationException();
    }

    static void fchmod(int fd, int mode) throws UnixException {
        // todo: requires SysStat.fchmod()
        throw new UnsupportedOperationException();
    }

    static void utimes0(long pathAddress, long times0, long times1) throws UnixException {
        // todo: requires SysTime.utimes
        throw new UnsupportedOperationException();
    }

    static void futimes(int fd, long times0, long times1) throws UnixException {
        // todo: requires ???.futimes() (BSD)
        throw new UnsupportedOperationException();
    }

    static void futimens(int fd, long times0, long times1) throws UnixException {
        // todo: requires SysStat.futimens()
        throw new UnsupportedOperationException();
    }

    static void lutimes0(long pathAddress, long times0, long times1) throws UnixException {
        // todo: requires ???.lutimes (BSD)
        throw new UnsupportedOperationException();
    }

    static long opendir0(long pathAddress) throws UnixException {
        // todo: requires Dirent.opendir()
        throw new UnsupportedOperationException();
    }

    static long fdopendir(int dfd) throws UnixException {
        // todo: requires Dirent.fdopendir()
        throw new UnsupportedOperationException();
    }

    static void closedir(long dir) throws UnixException {
        // todo: requires Dirent.closedir()
        throw new UnsupportedOperationException();
    }

    static byte[] readdir(long dir) throws UnixException {
        // todo: requires Dirent.readdir()
        throw new UnsupportedOperationException();
    }

    static int read(int fildes, long buf, int nbyte) throws UnixException {
        ssize_t n;
        do {
            n = Unistd.read(word(fildes), word(buf), word(nbyte));
        } while (n.longValue() == -1 && errno == EINTR.intValue());
        if (n.longValue() == -1) {
            throw new UnixException(errno);
        }
        return n.intValue();
    }

    static int write(int fildes, long buf, int nbyte) throws UnixException {
        ssize_t n;
        do {
            n = Unistd.write(word(fildes), word(buf), word(nbyte));
        } while (n.longValue() == -1 && errno == EINTR.intValue());
        if (n.longValue() == -1) {
            throw new UnixException(errno);
        }
        return n.intValue();
    }

    static void access0(long pathAddress, int amode) throws UnixException {
        int res;
        do {
            res = Unistd.access(word(pathAddress), word(amode)).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        if (res == -1) {
            throw new UnixException(errno);
        }
    }

    static boolean exists0(long pathAddress) {
        int res;
        do {
            res = Unistd.access(word(pathAddress), F_OK).intValue();
        } while (res == -1 && errno == EINTR.intValue());
        return res == 0;
    }

    static byte[] getpwuid(int uid) throws UnixException {
        // todo: requires SysTypes.getpwuid_r, _SC_GETPW_R_SIZE_MAX, ENT_BUF_SIZE, etc.
        throw new UnsupportedOperationException();
    }

    static byte[] getgrgid(int gid) throws UnixException {
        // todo: requires SysTypes.getgrgid_r, etc.
        throw new UnsupportedOperationException();
    }

    static int getpwnam0(long nameAddress) throws UnixException {
        // todo: requires SysTypes.getpwnam_r, etc.
        throw new UnsupportedOperationException();
    }

    static int getgrnam0(long nameAddress) throws UnixException {
        // todo: requires SysTypes.getgrnam_r, etc.
        throw new UnsupportedOperationException();
    }

    static void statvfs0(long pathAddress, UnixFileStoreAttributes attrs) throws UnixException {
        // todo: requires SysStat.statvfs() / statfs() on Mac OS
        throw new UnsupportedOperationException();
    }

    static byte[] strerror(int errnum) {
        // todo: requires String.strerror_r (the POSIX version, not the GNU version)
        throw new UnsupportedOperationException();
    }

    static int fgetxattr0(int filedes, long nameAddress, long valueAddress, int valueLen) throws UnixException {
        // todo: requires SysXattr.fgetxattr()
        throw new UnsupportedOperationException();
    }

    static void fsetxattr0(int filedes, long nameAddress, long valueAddress, int valueLen) throws UnixException {
        // todo: requires SysXattr.fsetxattr()
        throw new UnsupportedOperationException();
    }

    static void fremovexattr0(int filedes, long nameAddress) throws UnixException {
        // todo: requires SysXattr.fremovexattr()
        throw new UnsupportedOperationException();
    }

    static int flistxattr(int filedes, long listAddress, int size) throws UnixException {
        // todo: requires SysXattr.flistxattr()
        throw new UnsupportedOperationException();
    }

    private static void prepAttributes(final ptr<struct_stat> statBuf, final UnixFileAttributes$_aliases attrs) {
        attrs.st_mode = addr_of(statBuf.sel().st_mode).loadUnshared().intValue();
        attrs.st_ino = addr_of(statBuf.sel().st_ino).loadUnshared().longValue();
        attrs.st_dev = addr_of(statBuf.sel().st_dev).loadUnshared().longValue();
        attrs.st_rdev = addr_of(statBuf.sel().st_rdev).loadUnshared().longValue();
        attrs.st_nlink = addr_of(statBuf.sel().st_nlink).loadUnshared().intValue();
        attrs.st_uid = addr_of(statBuf.sel().st_uid).loadUnshared().intValue();
        attrs.st_gid = addr_of(statBuf.sel().st_gid).loadUnshared().intValue();
        attrs.st_size = addr_of(statBuf.sel().st_size).loadUnshared().longValue();
        attrs.st_atime_sec = addr_of(statBuf.sel().st_atime).loadUnshared().longValue();
        attrs.st_mtime_sec = addr_of(statBuf.sel().st_mtime).loadUnshared().longValue();
        attrs.st_ctime_sec = addr_of(statBuf.sel().st_ctime).loadUnshared().longValue();
        // todo: if (defined(_DARWIN_FEATURE_64_BIT_INODE)) {
        // todo:     attrs.st_birthtime = addr_of(statBuf.sel().st_birthtime).loadUnshared().longValue();
        // todo: }
        // --
        // todo: missing platform-dependent members
        // if (Build.Target.isMacOs()) {
        //     attrs.st_atime_nsec = addr_of(statBuf.sel().st_atim.tv_nsec).loadUnshared().longValue();
        //     attrs.st_mtime_nsec = addr_of(statBuf.sel().st_mtim.tv_nsec).loadUnshared().longValue();
        //     attrs.st_ctime_nsec = addr_of(statBuf.sel().st_ctim.tv_nsec).loadUnshared().longValue();
        // } else {
        //     attrs.st_atime_nsec = addr_of(statBuf.sel().st_atimespec.tv_nsec).loadUnshared().longValue();
        //     attrs.st_mtime_nsec = addr_of(statBuf.sel().st_mtimespec.tv_nsec).loadUnshared().longValue();
        //     attrs.st_ctime_nsec = addr_of(statBuf.sel().st_ctimespec.tv_nsec).loadUnshared().longValue();
        // }
    }

    private static final int SUPPORTS_OPENAT        = 1 << 1;  // syscalls
    private static final int SUPPORTS_FUTIMES       = 1 << 2;
    private static final int SUPPORTS_FUTIMENS      = 1 << 3;
    private static final int SUPPORTS_LUTIMES       = 1 << 4;
    private static final int SUPPORTS_XATTR         = 1 << 5;
    private static final int SUPPORTS_BIRTHTIME     = 1 << 16; // other features

    static int init() {
        // todo: add other flags based on OS and whether the missing features are implemented
        return 0
            | SUPPORTS_OPENAT
            ;
    }
}
