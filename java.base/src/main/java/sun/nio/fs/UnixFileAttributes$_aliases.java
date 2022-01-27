package sun.nio.fs;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(UnixFileAttributes.class)
class UnixFileAttributes$_aliases {
    int st_mode;
    long st_ino;
    long st_dev;
    long st_rdev;
    int st_nlink;
    int st_uid;
    int st_gid;
    long st_size;
    long st_atime_sec;
    long st_atime_nsec;
    long st_mtime_sec;
    long st_mtime_nsec;
    long st_ctime_sec;
    long st_ctime_nsec;
    long st_birthtime_sec;
}
