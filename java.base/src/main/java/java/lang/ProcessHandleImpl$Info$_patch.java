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
package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.bsd.SysProc.*;
import static org.qbicc.runtime.bsd.SysSysctl.*;
import static org.qbicc.runtime.linux.Unistd.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.Fcntl.*;
import static org.qbicc.runtime.posix.Limits.*;
import static org.qbicc.runtime.posix.Pwd.*;
import static org.qbicc.runtime.posix.String.*;
import static org.qbicc.runtime.posix.SysResource.*;
import static org.qbicc.runtime.posix.SysStat.*;
import static org.qbicc.runtime.posix.SysTime.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Replace;

@Patch("java.lang.ProcessHandleImpl$Info")
@Tracking("src/java.base/unix/native/libjava/ProcessHandleImpl_unix.c")
@Tracking("src/java.base/windows/native/libjava/ProcessHandleImpl_win.c")
class ProcessHandleImpl$Info$_patch {
    // alias
    String command;
    String commandLine;
    String[] arguments;
    long startTime;
    long totalTime;
    String user;

    @Replace
    static void initIDs() {
    }

    @Add(when = Build.Target.IsMacOs.class)
    private static pid_t getParentPidAndTimings_MacOs(pid_t pid, int64_t_ptr totalTime, int64_t_ptr startTime) {
        pid_t ppid = word(-1);
        struct_kinfo_proc kp = auto();
        size_t bufSize = auto(sizeof(kp));
        c_int[] mib = new c_int[]{CTL_KERN, KERN_PROC, KERN_PROC_PID, pid.cast()};

        if (sysctl(addr_of(mib[0]), word(4), addr_of(kp), addr_of(bufSize), word(0), word(0)).intValue() < 0) {
            throw new RuntimeException("sysctl failed");
        }

        if (bufSize.intValue() > 0 && kp.kp_proc.p_pid == pid) {
            struct_timeval tv = addr_of(kp.kp_proc.p_un).cast(struct_timeval_ptr.class).loadUnshared();
            long st = tv.tv_sec.longValue() * 1000 + tv.tv_usec.longValue() / 1000;
            startTime.storeUnshared(word(st));
            ppid = kp.kp_eproc.e_ppid;
        }

        // Get cputime if for current process
        if (pid == getpid()) {
            struct_rusage usage = auto();
            if (getrusage(RUSAGE_SELF, addr_of(usage)).intValue() == 0) {
                long microsecs = usage.ru_utime.tv_sec.longValue() * 1000 * 1000 + usage.ru_utime.tv_usec.longValue() +
                        usage.ru_stime.tv_sec.longValue() * 1000 * 1000 + usage.ru_stime.tv_usec.longValue();
                totalTime.storeUnshared(word(microsecs * 1000));
            }
        }
        return ppid;
    }

    /**
     * Read /proc/<pid>/stat and return the ppid, total cputime and start time.
     * -1 is fail;  >=  0 is parent pid
     * 'total' will contain the running time of 'pid' in nanoseconds.
     * 'start' will contain the start time of 'pid' in milliseconds since epoch.
     */
    @Add(when = Build.Target.IsLinux.class)
    private static pid_t getParentPidAndTimings_Linux(pid_t pid, int64_t_ptr totalTime, int64_t_ptr startTime) {
        c_char[] buffer = new c_char[2048];
        c_char[] fn = new c_char[32];
        pid_t parentPid = auto();
        uint64_t utime = auto();
        uint64_t stime = auto();
        uint64_t start = auto();

        /*
         * Try to stat and then open /proc/%d/stat
         */
        snprintf(addr_of(fn[0]), sizeof(fn), utf8z("/proc/%d/stat"), pid);

        FILE_ptr fp = fopen(addr_of(fn[0]), utf8z("r"));
        if (fp.isNull()) {
            return word(-1);              // fail, no such /proc/pid/stat
        }

        /*
         * The format is: pid (command) state ppid ...
         * As the command could be anything we must find the right most
         * ")" and then skip the white spaces that follow it.
         */
        int statlen = fread(addr_of(buffer[0]).cast(), word(1), word(sizeof(buffer).intValue() - 1), fp).intValue();
        fclose(fp);
        if (statlen < 0) {
            return word(-1);               // parent pid is not available
        }

        buffer[statlen] = word('\0');
        ptr<c_char> s = strchr(addr_of(buffer[0]), word('('));
        if (s.isNull()) {
            return word(-1);               // parent pid is not available
        }
        // Found start of command, skip to end
        s = s.plus(1);
        s = strrchr(s.cast(), word(')'));
        if (s.isNull()) {
            return word(-1);               // parent pid is not available
        }
        s = s.plus(1);

        // Scan the needed fields from status, retaining only ppid(4),
        // utime (14), stime(15), starttime(22)
        if (4 != sscanf(s.cast(), utf8z(" %*c %d %*d %*d %*d %*d %*d %*u %*u %*u %*u %lu %lu %*d %*d %*d %*d %*d %*d %llu"),
                addr_of(parentPid), addr_of(utime), addr_of(stime), addr_of(start)).intValue()) {
            return word(0);              // not all values parsed; return error
        }

        totalTime.storeUnshared(word((utime.longValue() + stime.longValue()) * (1000000000L / ProcessHandleImpl$Info$_runtime.clock_ticks_per_second)));
        startTime.storeUnshared(word(ProcessHandleImpl$Info$_runtime.bootTime_ms + ((start.longValue() * 1000) / ProcessHandleImpl$Info$_runtime.clock_ticks_per_second)));

        return parentPid;
    }

    @Add
    private static size_t getpw_buf_size() {
        size_t buf_size = sysconf(_SC_GETPW_R_SIZE_MAX).cast();
        return buf_size.intValue() == -1 ? word(1024) : buf_size;
    }

    @Add
    private void getUserInfo(uid_t uid) {
        size_t buf_size = getpw_buf_size();
        char_ptr pwbuf = malloc(buf_size);
        if (pwbuf.isNull()) {
            throw new OutOfMemoryError("Unable to open getpwent");
        }
        struct_passwd pwent = auto();
        struct_passwd_ptr p = auto(word(0));
        c_int result = word(0);
        do {
            result = getpwuid_r(uid, addr_of(pwent), pwbuf, buf_size, addr_of(p).cast());
        } while (result.intValue() == -1 && errno == EINTR.intValue());

        if (result.intValue() == 0 && !p.isNull() && !p.sel().pw_name.isNull() &&
                p.sel().pw_name.get(0) != word('\0')) {
            this.user = utf8zToJavaString(p.sel().pw_name.cast());
        }
        free(pwbuf);
    }

    @Add
    private void fillArgArray(int nargs, const_char_ptr cp, const_char_ptr argsEnd, const_char_ptr cmdline) {
        if (nargs >= 1) {
            String[] argsArray = new String[nargs - 1];

            for (int i = 0; i < nargs - 1; i++) {
                cp = cp.plus(strlen(cp).intValue()).plus(1);
                if (cp.isGt(argsEnd) || cp.loadUnshared() == word('\0')) {
                    return;  // Off the end pointer or an empty argument is an error
                }
                argsArray[i] = utf8zToJavaString(cp.cast());
            }
            this.arguments = argsArray;
        }

        if (!cmdline.isNull()) {
            this.commandLine = utf8zToJavaString(cmdline.cast());
        }
    }

    @Add(when = Build.Target.IsMacOs.class)
    private static uid_t getUID(pid_t pid) {
        struct_kinfo_proc kp = auto();
        size_t bufSize = auto(sizeof(kp));
        c_int[] mib = new c_int[]{CTL_KERN, KERN_PROC, KERN_PROC_PID, pid.cast()};

        if (sysctl(addr_of(mib[0]), word(4), addr_of(kp), addr_of(bufSize), word(0), word(0)).intValue() == 0) {
            if (bufSize.intValue() > 0 && kp.kp_proc.p_pid == pid) {
                return kp.kp_eproc.e_ucred.cr_uid;
            }
        }
        return word(-1);
    }

    @Add(when = Build.Target.IsMacOs.class)
    private void getCmdlineAndUserInfo_MacOs(pid_t pid) {
        getUserInfo(getUID(pid));

        // Get the maximum size of the arguments
        c_int[] mib = new c_int[3];
        mib[0] = CTL_KERN;
        mib[1] = KERN_ARGMAX;
        c_int maxargs = auto();
        size_t size = auto(sizeof(maxargs));
        if (sysctl(addr_of(mib[0]), word(2), addr_of(maxargs), addr_of(size), word(0), word(0)).intValue() == -1) {
            throw new RuntimeException("sysctl failed");
        }

        char_ptr args = malloc(maxargs.cast());
        if (args.isNull()) {
            throw new OutOfMemoryError("malloc failed");
        }
        try {
            // Get the actual arguments
            mib[0] = CTL_KERN;
            mib[1] = KERN_PROCARGS2;
            mib[2] = pid.cast();
            size = maxargs.cast();
            if (sysctl(addr_of(mib[0]), word(3), args, addr_of(size), word(0), word(0)).intValue() == -1) {
                throw new RuntimeException("sysctl failed");
            }

            c_int nargs = args.cast(int_ptr.class).loadUnshared();
            char_ptr cp = args.plus(sizeof(nargs).intValue());
            char_ptr argsEnd = args.plus(maxargs.intValue());

            // Store the command executable path
            this.command = utf8zToJavaString(cp.cast());

            // Skip trailing nulls after the executable path
            for (cp = cp.plus(strnlen(cp.cast(), word(maxargs.intValue() - sizeof(nargs).intValue())).intValue());
                 cp.isLt(argsEnd); cp = cp.plus(1)) {
                if (cp.loadUnshared() != word('\0')) {
                    break;
                }
            }

            fillArgArray(nargs.intValue(), cp.cast(), argsEnd.cast(), zero());
        } finally {
            free(args);
        }
    }

    @Add(when = Build.Target.IsLinux.class)
    private void getCmdlineAndUserInfo_Linux(pid_t pid) {
        char_ptr cmdline = zero();
        char_ptr cmdEnd = zero();
        char_ptr args = zero();
        c_char[] fn = new c_char[32];
        struct_stat stat_buf = auto();

        /*
         * Stat /proc/<pid> to get the user id
         */
        snprintf(addr_of(fn[0]).cast(), sizeof(fn), utf8z("/proc/%d"), pid);
        if (stat(addr_of(fn[0]).cast(), addr_of(stat_buf)).isZero()) {
            getUserInfo(stat_buf.st_uid);
        }

        /*
         * Try to open /proc/<pid>/cmdline
         */
        strncat(addr_of(fn[0]).cast(), utf8z("/cmdline").cast(), word(sizeof(fn).intValue() - strnlen(addr_of(fn[0]).cast(), sizeof(fn)).intValue() - 1));
        c_int fd = open(addr_of(fn[0]).cast(), O_RDONLY);
        if (fd.intValue() < 0) {
            return;
        }

        try {
            int i = 0;
            boolean truncated = false;
            int count;
            int pageSize = ProcessHandleImpl$Info$_runtime.pageSize;

            /*
             * The path name read by readlink() is limited to PATH_MAX characters.
             * The content of /proc/<pid>/cmdline is limited to PAGE_SIZE characters.
             */
            cmdline = malloc(word((PATH_MAX.intValue() > pageSize ? PATH_MAX.intValue() : pageSize) + 1 ));
            if (cmdline.isNull()) {
                return;
            }

            /*
             * On Linux, the full path to the executable command is the link in
             * /proc/<pid>/exe. But it is only readable for processes we own.
             */
            snprintf(addr_of(fn[0]), sizeof(fn), utf8z("/proc/%d/exe").cast(), pid);
            int cmdlen = readlink(addr_of(fn[0]), cmdline, PATH_MAX).intValue();
            if (cmdlen > 0) {
                // null terminate and create String to store for command
                cmdline.set(cmdlen, word('\0'));
                this.command = utf8zToJavaString(cmdline.cast());
            }

            /*
             * The command-line arguments appear as a set of strings separated by
             * null bytes ('\0'), with a further null byte after the last
             * string. The last string is only null terminated if the whole command
             * line is not exceeding (PAGE_SIZE - 1) characters.
             */
            cmdlen = 0;
            char_ptr s = cmdline;
            while ((count = read(fd, s.cast(), word(pageSize - cmdlen)).intValue()) > 0) {
                cmdlen += count;
                s = s.plus(count);
            }
            if (count < 0) {
                return;
            }
            // We have to null-terminate because the process may have changed argv[]
            // or because the content in /proc/<pid>/cmdline is truncated.
            cmdline.set(cmdlen, word('\0'));
            if (cmdlen == pageSize && !cmdline.get(pageSize - 1).isZero()) {
                truncated = true;
            } else if (cmdlen == 0) {
                // /proc/<pid>/cmdline was empty. This usually happens for kernel processes
                // like '[kthreadd]'. We could try to read /proc/<pid>/comm in the future.
            }
            if (cmdlen > 0 && (this.command == null || truncated)) {
                // We have no exact command or the arguments are truncated.
                // In this case we save the command line from /proc/<pid>/cmdline.
                args = malloc(word(pageSize + 1));
                if (!args.isNull()) {
                    memcpy(args.cast(), cmdline.cast(), word(cmdlen + 1));
                    for (i = 0; i < cmdlen; i++) {
                        if (args.get(i).isZero()) {
                            args.set(i, word(' '));
                        }
                    }
                }
            }
            i = 0;
            if (!truncated) {
                // Count the arguments
                cmdEnd = cmdline.plus(cmdlen);
                for (s = cmdline; s.loadUnshared() != word('\0') && (s.isLt(cmdEnd)); i++) {
                    s = s.plus(strnlen(s.cast(), cmdEnd.minus(s).cast()).intValue() + 1);
                }
            }
            fillArgArray(i, cmdline.cast(), cmdEnd.cast(), args.cast());
        } finally {
            free(cmdline);
            free(args);
            if (fd.intValue() > 0) {
                close(fd);
            }
        }
    }

    @Add
    static pid_t getParentPidAndTimings(pid_t pid, int64_t_ptr totalTime, int64_t_ptr startTime) {
        if (Build.Target.isMacOs()) {
            return getParentPidAndTimings_MacOs(pid, totalTime, startTime);
        } else if (Build.Target.isLinux()) {
            return getParentPidAndTimings_Linux(pid, totalTime, startTime);
        } else {
            throw new UnsupportedOperationException("TODO: getParentPidAndTimings");
        }
    }

    @Add
    private void getCmdlineAndUserInfo(pid_t pid) {
        if (Build.Target.isMacOs()) {
            getCmdlineAndUserInfo_MacOs(pid);
        } else if (Build.Target.isLinux()){
            getCmdlineAndUserInfo_Linux(pid);
        } else {
            throw new UnsupportedOperationException("TODO: getCmdlineAndUserInfo");
        }
    }

    @Replace
    private void info0(long jpid) {
        if (Build.Target.isPosix()) {
            pid_t pid = word(jpid);
            long totalTime = auto(-1L);
            long startTime = auto(-1L);
            pid_t ppid = getParentPidAndTimings(pid, addr_of(totalTime), addr_of(startTime));
            if (ppid.intValue() >= 0) {
                this.totalTime = totalTime;
                this.startTime = startTime;
            }
            getCmdlineAndUserInfo(pid);
        } else {
            throw new UnsupportedOperationException("Unsupported platform");
        }
    }
}
