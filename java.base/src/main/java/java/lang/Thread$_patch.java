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
import static org.qbicc.runtime.InlineCondition.*;
import static org.qbicc.runtime.linux.Futex.*;
import static org.qbicc.runtime.llvm.LLVM.*;
import static org.qbicc.runtime.posix.Errno.EAGAIN;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.posix.Time.*;
import static org.qbicc.runtime.stdc.Errno.errno;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;

import java.security.AccessControlContext;
import java.util.concurrent.locks.LockSupport;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.SerializeAsZero;
import org.qbicc.runtime.main.VMHelpers;
import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 * Patches for {@code java.lang.Thread}.
 */
@PatchClass(Thread.class)
@Tracking("src/java.base/share/classes/java/lang/Thread.java")
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
    @Annotate
    @SerializeAsZero
    ThreadLocal.ThreadLocalMap threadLocals;
    long stackSize;
    long tid;
    int threadStatus;

    // alias methods
    native void setPriority(final int priority);
    static native long nextThreadID();

    @Add
    pthread_t thread;

    // used only by non-Linux
    @Add(unless = Build.Target.IsLinux.class)
    pthread_mutex_t mutex;
    @Add(unless = Build.Target.IsLinux.class)
    pthread_cond_t cond;

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

        // fields that contain pointers to OS/native resources are initialized in start0()
        // This allows Thread instances to be created (but not started) at build time.
        // One typical use case is <clinit> methods that register shutdownhooks.
    }

    @Add
    @Hidden
    public void initializeNativeFields() {
        if (Build.isTarget() && ! Build.Target.isLinux()) {
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

    @Add
    private static final long MAX_NANOS_PER_MS_TIME = (Long.MAX_VALUE / 1_000_000L) - 1L;

    @Replace
    public static void sleep(long millis, int nanos) throws InterruptedException {
        long start, end;
        if (millis < 0 || millis == 0 && nanos <= 0) {
            return;
        }
        while (nanos > 1_000_000) {
            if (millis < Long.MAX_VALUE) {
                millis ++;
            } else {
                // max possible time
                nanos = 999_999;
                break;
            }
            nanos -= 1_000_000;
        }
        end = System.nanoTime();
        int32_t_ptr statusPtr = addr_of(refToPtr((Thread$_patch) (Object) Thread.currentThread()).sel().threadStatus).cast();
        statusPtr.getAndBitwiseOrOpaque(word(STATE_SLEEPING));
        try {
            for (;;) {
                start = end;
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                // we can only fit MAX_NANOS_PER_MS_TIME milliseconds into a long as nanos; so that's the max we can wait
                LockSupport.parkNanos(Math.min(millis, MAX_NANOS_PER_MS_TIME) * 1_000_000L + nanos);
                end = System.nanoTime();
                // subtract the elapsed milliseconds
                millis -= Long.divideUnsigned(end - start, 1_000_000L);
                // subtract the elapsed nanoseconds
                nanos -= (int) Long.remainderUnsigned(end - start, 1_000_000L);
                while (nanos < 0) {
                    millis -= 1;
                    nanos += 1_000_000L;
                }
                if (millis < 0 || millis == 0 && nanos <= 0) {
                    return;
                }
            }
        } finally {
            statusPtr.getAndBitwiseAndOpaque(word(~STATE_SLEEPING));
        }
    }

    // stick with the JVMTI flag definitions

    static final int STATE_ALIVE = 1 << 0; // thread, cond, and mutex are valid only when this bit is set
    static final int STATE_TERMINATED = 1 << 1;
    static final int STATE_RUNNABLE = 1 << 2;
    static final int STATE_WAITING_INDEFINITELY = 1 << 4;
    static final int STATE_WAITING_WITH_TIMEOUT = 1 << 5;
    static final int STATE_SLEEPING = 1 << 6;
    static final int STATE_WAITING = 1 << 7;
    static final int STATE_IN_OBJECT_WAIT = 1 << 8;
    static final int STATE_PARKED = 1 << 9;
    static final int STATE_BLOCKED = 1 << 10; // on monitor enter
    // bits 9 - 20 unused
    static final int STATE_INTERRUPTED = 1 << 21;
    static final int STATE_IN_NATIVE = 1 << 22;
    // bits 23 - 27 unused
    static final int STATE_UNPARK = 1 << 28; // pending unpark (aka vendor 1)
    static final int STATE_VENDOR_2 = 1 << 29;
    static final int STATE_VENDOR_3 = 1 << 30;

    @Add
    void blocked() {
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseOrOpaque(word(STATE_BLOCKED));
    }

    @Add
    void unblocked() {
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseAndOpaque(word(~STATE_BLOCKED));
    }

    /**
     * The number of non-daemon threads; when this reaches zero, we exit.
     */
    @Add
    private static volatile int nonDaemonThreadCount;

    @Replace
    @Hidden
    @NoReflect
    private void start0() {
        // execute defered initialization of native fields
        initializeNativeFields();

        addr_of(refToPtr(this).sel().threadStatus).storeSingleRelease(word(STATE_ALIVE));
        void_ptr_unaryoperator_function_ptr threadWrapper = CompilerIntrinsics.nativeFunctionPointer("org.qbicc.runtime.main.VMHelpers", "pthreadCreateWrapper");
        ptr<Thread> thisPtr = refToPtr(this).cast();
        ptr<pthread_t> pthreadPtr = addr_of(refToPtr(this).sel().thread);
        int result = pthread_create(pthreadPtr.cast(), zero(), threadWrapper.cast(), thisPtr.cast()).intValue();
        if (result != 0) {
            // terminated - clear ALIVE and set TERMINATED in one swap
            addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseXor(word(STATE_ALIVE | STATE_TERMINATED));
            if (errno == EAGAIN.intValue()) {
                throw new OutOfMemoryError("Native thread");
            } else {
                throw new InternalError("Native thread");
            }
        }
    }

    /**
     * Actually run the thread body.
     */
    @Add
    @Hidden
    @NoReflect
    @SuppressWarnings("CallToThreadRun")
    void run0() {
        Thread self = (Thread) (Object) this;
        begin();
        try {
            self.run();
        } finally {
            end();
        }
    }

    @Add
    @Hidden
    @NoReflect
    private void begin() {
        Thread self = (Thread) (Object) this;
        if (! self.isDaemon()) {
            addr_of(nonDaemonThreadCount).getAndAdd(word(1));
        }
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseOr(word(STATE_RUNNABLE));
    }

    @Add
    @Hidden
    @NoReflect
    private void end() {
        Thread self = (Thread) (Object) this;
        if (! self.isDaemon()) {
            int cnt = addr_of(nonDaemonThreadCount).getAndAdd(word(- 1)).intValue();
            if (cnt == 1) {
                // it was the last non-daemon thread
                // TODO: process exit
            }
        }
        // terminated - clear ALIVE and RUNNABLE and set TERMINATED in one swap
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseXor(word(STATE_ALIVE | STATE_RUNNABLE | STATE_TERMINATED));
    }

    @Replace
    private void interrupt0() {
        addr_of(refToPtr(this).sel().threadStatus).getAndBitwiseOr(word(STATE_INTERRUPTED));
        // unpark the thread so it can observe the interruption
        unpark();
    }

    @Replace
    @Inline(ALWAYS)
    private static void clearInterruptEvent() {
        addr_of(refToPtr((Thread$_patch) (Object) Thread.currentThread()).sel().threadStatus).getAndBitwiseAnd(word(~STATE_INTERRUPTED));
    }

    @SuppressWarnings("ConstantConditions")
    @Add
    static void park(boolean isAbsolute, long time) {
        Thread thread = Thread.currentThread();
        Thread$_patch patchThread = (Thread$_patch) (Object) thread;
        int32_t_ptr ptr = addr_of(refToPtr(patchThread).sel().threadStatus).cast();
        int oldVal, newVal, witness, setFlags;
        oldVal = ptr.loadAcquire().intValue();
        setFlags = STATE_PARKED | STATE_WAITING | (time == 0 && ! isAbsolute ? STATE_WAITING_INDEFINITELY : STATE_WAITING_WITH_TIMEOUT);
        for (;;) {
            if ((oldVal & STATE_UNPARK) != 0) {
                // clear pending unpark
                newVal = oldVal & ~STATE_UNPARK;
                // todo: singleRelease
                witness = ptr.compareAndSwapRelease(word(oldVal), word(newVal)).intValue();
                if (witness == oldVal) {
                    // updated successfully; exit
                    return;
                }
            } else {
                newVal = oldVal | setFlags;
                witness = ptr.compareAndSwap(word(oldVal), word(newVal)).intValue();
                if (witness == oldVal) {
                    // updated successfully; break out to park
                    break;
                }
            }
            // retry CAS
            oldVal = witness;
        }
        // now park
        try {
            if (Build.Target.isLinux()) {
                // block via futex.
                struct_timespec timespec = auto();
                // no pending unpark that we've detected
                if (isAbsolute) {
                    // time is in milliseconds since epoch
                    timespec.tv_sec = word(time / 1_000L);
                    timespec.tv_nsec = word(time * 1_000_000L);
                    futex_wait_absolute(ptr.cast(), word(newVal), addr_of(timespec));
                } else if (time == 0) {
                    // relative time of zero means wait indefinitely
                    futex_wait(ptr.cast(), word(newVal), zero());
                } else {
                    // time is in relative nanoseconds
                    timespec.tv_sec = word(time / 1_000_000_000L);
                    timespec.tv_nsec = word(time % 1_000_000_000L);
                    futex_wait(ptr.cast(), word(newVal), addr_of(timespec));
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
            // reset flags on exit
            ptr.getAndBitwiseAndOpaque(word(~(setFlags | STATE_PARKED)));
        }
    }

    @Add
    void unpark() {
        int32_t_ptr ptr = addr_of(refToPtr(this).sel().threadStatus).cast();
        int oldVal, newVal, witness;
        oldVal = ptr.loadSingleAcquire().intValue();
        for (;;) {
            if ((oldVal & (STATE_UNPARK | STATE_TERMINATED)) != 0) {
                // no op necessary; unpark already pending or the thread is gone
                return;
            }
            newVal = oldVal | STATE_UNPARK;
            // todo: single release
            witness = ptr.compareAndSwapRelease(word(oldVal), word(newVal)).intValue();
            if (witness == oldVal) {
                // done; now we just have to signal waiters (well just one waiter really) (well, maybe zero actually)
                if ((oldVal & STATE_ALIVE) == 0) {
                    // there isn't actually anyone to wake
                    return;
                }
                if (Build.Target.isLinux()) {
                    // nothing we can do about errors really, other than panic
                    futex_wake_all(ptr.cast());
                } else if (Build.Target.isPosix()) {
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
                } else {
                    throw new UnsupportedOperationException();
                }
                return;
            }
            // retry
            oldVal = witness;
        }

    }

    @Replace
    public static void onSpinWait() {
        if (Build.isHost()) {
            return;
        } else if (Build.Target.isAmd64()) {
            asm(c_void.class, "pause", "", ASM_FLAG_SIDE_EFFECT);
        } else if (Build.Target.isAarch64()) {
            asm(c_void.class, "yield", "", ASM_FLAG_SIDE_EFFECT);
        } else {
            // no operation
            return;
        }
    }
}
