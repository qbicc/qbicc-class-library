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
import static org.qbicc.runtime.posix.Pwd.*;
import static org.qbicc.runtime.posix.String.*;
import static org.qbicc.runtime.posix.SysResource.*;
import static org.qbicc.runtime.posix.SysTime.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
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
    static void initIDs() {}

    @Add(when = Build.Target.IsMacOs.class)
    private static pid_t getParentPidAndTimings_MacOS(pid_t pid, int64_t_ptr totalTime, int64_t_ptr startTime) {
        pid_t ppid = word(-1);
        struct_kinfo_proc kp = auto();
        size_t bufSize = auto(sizeof(kp));
        c_int[] mib = new c_int[]{ CTL_KERN, KERN_PROC, KERN_PROC_PID, pid.cast()};

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

    @Add
    static pid_t getParentPidAndTimings(pid_t pid, int64_t_ptr totalTime, int64_t_ptr startTime) {
        if (Build.Target.isMacOs()) {
	    return getParentPidAndTimings_MacOS(pid, totalTime, startTime);
	} else {
            throw new UnsupportedOperationException("TODO: getParentPidAndTimings");
        }
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
        c_int[] mib = new c_int[]{ CTL_KERN, KERN_PROC, KERN_PROC_PID, pid.cast()};

        if (sysctl(addr_of(mib[0]), word(4), addr_of(kp), addr_of(bufSize), word(0), word(0)).intValue() == 0) {
            if (bufSize.intValue() > 0 && kp.kp_proc.p_pid == pid) {
                return kp.kp_eproc.e_ucred.cr_uid;
            }
        }
        return word(-1);
    }

    @Add
    private void getCmdlineAndUserInfo(pid_t pid) {
        if (Build.Target.isMacOs()) {

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
                     cp.isLt(argsEnd); cp = cp.plus(1)){
                    if (cp.loadUnshared() != word('\0')) {
                        break;
                    }
                }

                fillArgArray(nargs.intValue(), cp.cast(), argsEnd.cast(), zero());
            } finally {
                free(args);
            }
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
            pid_t ppid = getParentPidAndTimings(pid,  addr_of(totalTime), addr_of(startTime));
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
