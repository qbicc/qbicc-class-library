/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 1994, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.qbicc.runtime.linux.Futex.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.posix.Time.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;

import java.security.AccessControlContext;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 * Patches for {@code java.lang.Thread}.
 */
@PatchClass(Thread.class)
public class Thread$_patch {
    // alias fields
    String name;
    ThreadGroup group;
    boolean daemon;
    int priority;
    ClassLoader contextClassLoader;
    AccessControlContext inheritedAccessControlContext;
    Runnable target;
    ThreadLocal.ThreadLocalMap inheritableThreadLocals;
    long stackSize;
    long tid;
    int threadStatus;

    // alias methods
    native void setPriority(final int priority);
    static native long nextThreadID();

    // used only by non-Linux
    // TODO: predicate class cannot be loaded before java.lang.Thread is loaded
    // @Add(unless = Build.Target.IsLinux.class)
    pthread_mutex_t mutex;
    // @Add(unless = Build.Target.IsLinux.class)
    pthread_cond_t cond;
    // used by Linux & POSIX
    @Add
    uint32_t parkFlag;

    @Replace
    Thread$_patch(ThreadGroup g, Runnable target, String name,
                   long stackSize, AccessControlContext acc,
                   boolean inheritThreadLocals) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        this.name = name;
        Thread parent = Thread.currentThread();
        if (g == null) {
            g = parent.getThreadGroup();
        }
        g.addUnstarted();
        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        this.contextClassLoader = parent.getContextClassLoader();
        this.inheritedAccessControlContext = null;
        this.target = target;
        setPriority(priority);
        if (inheritThreadLocals && parent.inheritableThreadLocals != null) {
            this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        }
        this.stackSize = stackSize;
        this.tid = nextThreadID();
        // initialize native fields
        parkFlag = zero();
        if (! Build.Target.isLinux()) {
            // mutex type does not matter
            c_int res = pthread_mutex_init(addr_of(refToPtr(this).sel().mutex), zero());
            if (res.isNonNull()) {
                throw new InternalError("Failed to initialize thread park mutex");
            }
            // cond type does not matter either
            res = pthread_cond_init(addr_of(refToPtr(this).sel().cond), zero());
            if (res.isNonNull()) {
                throw new InternalError("Failed to initialize thread park condition");
            }
        }
    }

    @Replace
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    private static final int STATE_ALIVE = 1 << 0;
    private static final int STATE_TERMINATED = 1 << 1;
    private static final int STATE_RUNNABLE = 1 << 2;
    private static final int STATE_WAITING = 1 << 4;
    private static final int STATE_WAITING_WITH_TIMEOUT = 1 << 5;
    private static final int STATE_BLOCKED = 1 << 10;

    @Add
    void blocked() {
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseOrOpaque(word(STATE_BLOCKED));
    }

    @Add
    void unblocked() {
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseAndOpaque(word(~STATE_BLOCKED));
    }

    @SuppressWarnings("ConstantConditions")
    @Add
    static void park(boolean isAbsolute, long time) {
        Thread thread = Thread.currentThread();
        Thread$_patch patchThread = (Thread$_patch) (Object) thread;
        uint32_t_ptr ptr = addr_of(refToPtr(patchThread).sel().parkFlag);
        // if we have a pending unpark, the wait value will be 1 and we will not block.
        if (ptr.compareAndSet(word(1), zero())) {
            return;
        }
        // indicate that we are waiting
        int flag = time == 0 && ! isAbsolute ? STATE_WAITING : STATE_WAITING_WITH_TIMEOUT;
        addr_of(refToPtr(patchThread).sel().threadStatus).getAndBitwiseOrOpaque(word(flag));
        try {
            if (Build.Target.isLinux()) {
                // block via futex.
                struct_timespec timespec = auto();
                // no pending unpark that we've detected
                if (isAbsolute) {
                    // time is in milliseconds since epoch
                    timespec.tv_sec = word(time / 1_000L);
                    timespec.tv_nsec = word(time * 1_000_000L);
                    futex_wait_absolute(ptr, word(0), addr_of(timespec));
                } else if (time == 0) {
                    // relative time of zero means wait indefinitely
                    futex_wait(ptr, word(0), zero());
                } else {
                    // time is in relative nanoseconds
                    timespec.tv_sec = word(time / 1_000_000_000L);
                    timespec.tv_nsec = word(time % 1_000_000_000L);
                    futex_wait(ptr, word(0), addr_of(timespec));
                }
                ptr.storeRelease(zero());
            } else if (Build.Target.isPosix()) {
                // block via condition.
                struct_timespec timespec = auto();
                pthread_mutex_t_ptr mutexPtr = addr_of(refToPtr(patchThread).sel().mutex);
                if (pthread_mutex_lock(mutexPtr).isNonZero()) {
                    throw new InternalError("mutex operation failed");
                }
                try {
                    pthread_cond_t_ptr condPtr = addr_of(refToPtr(patchThread).sel().cond);
                    if (isAbsolute) {
                        // time is in milliseconds since epoch
                        timespec.tv_sec = word(time / 1_000L);
                        timespec.tv_nsec = word(time * 1_000_000L);
                        pthread_cond_timedwait(condPtr, mutexPtr, addr_of(timespec));
                    } else if (time == 0) {
                        // relative time of zero means wait indefinitely
                        pthread_cond_wait(condPtr, mutexPtr);
                    } else {
                        // time is in relative nanoseconds; we have to add it to the wall clock
                        clock_gettime(CLOCK_REALTIME, addr_of(timespec));
                        long sec = timespec.tv_sec.longValue() + time / 1_000_000_000L;
                        long nsec = timespec.tv_nsec.longValue() + time % 1_000_000_000L;
                        if (nsec >= 1_000_000_000L) {
                            // plus one second
                            sec++;
                            nsec -= 1_000_000_000L;
                        }
                        timespec.tv_sec = word(sec);
                        timespec.tv_nsec = word(nsec);
                        pthread_cond_timedwait(condPtr, mutexPtr, addr_of(timespec));
                    }
                } finally {
                    pthread_mutex_unlock(mutexPtr);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } finally {
            addr_of(refToPtr(patchThread).sel().threadStatus).getAndBitwiseAndOpaque(word(~flag));
        }
    }

    @Add
    void unpark() {
        uint32_t_ptr ptr = addr_of(refToPtr(this).sel().parkFlag);
        if (Build.Target.isLinux()) {
            if (ptr.compareAndSet(zero(), word(1))) {
                // nothing we can do about errors really, other than panic
                futex_wake_all(ptr);
            }
        } else if (Build.Target.isPosix()) {
            if (ptr.compareAndSet(zero(), word(1))) {
                // wake
                pthread_mutex_t_ptr mutexPtr = addr_of(refToPtr(this).sel().mutex);
                if (pthread_mutex_lock(mutexPtr).isNonZero()) {
                    throw new InternalError("mutex operation failed");
                }
                try {
                    pthread_cond_t_ptr condPtr = addr_of(refToPtr(this).sel().cond);
                    if (pthread_cond_broadcast(condPtr).isNonZero()) {
                        throw new InternalError("mutex condition operation failed");
                    }
                } finally {
                    pthread_mutex_unlock(mutexPtr);
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
