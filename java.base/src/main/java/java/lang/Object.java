/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 1994, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;

import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.main.Monitor;

public class Object {

    @NoReflect
    private Monitor monitor;

    public Object() {}

    public final native Class<?> getClass();

    public int hashCode() {
        return System.identityHashCode(this);
    }

    public boolean equals(Object other) {
        return this == other;
    }

    public final void notify() {
        getMonitor().signal();
    }

    public final void notifyAll() {
        getMonitor().signalAll();
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public final void wait() throws InterruptedException {
        getMonitor().await();
    }

    public final void wait(long millis) throws InterruptedException {
        getMonitor().await(millis);
    }

    public final void wait(long millis, int nanos) throws InterruptedException {
        getMonitor().await(millis, nanos);
    }

    @NoReflect
    @Hidden
    private void monitorEnter() {
        getMonitor().enter();
    }

    @NoReflect
    @Hidden
    private void monitorExit() {
        getMonitor().exit();
    }

    @NoReflect
    @Hidden
    private boolean holdsLock() {
        Monitor monitor = addr_of(refToPtr(this).sel().monitor).loadSingleAcquire();
        return monitor != null && monitor.isHeldByCurrentThread();
    }

    @NoReflect
    private Monitor getMonitor() {
        Monitor monitor = addr_of(refToPtr(this).sel().monitor).loadSingleAcquire();
        if (monitor == null) {
            monitor = new Monitor();
            Monitor appearing = addr_of(refToPtr(this).sel().monitor).compareAndSwapRelease(null, monitor);
            if (appearing != null) {
                monitor = appearing;
            }
        }
        return monitor;
    }

    protected Object clone() throws CloneNotSupportedException {
        Class<?> clazz = this.getClass();
        if (clazz.isArray()) {
            Class<?> elemType = clazz.getComponentType();
            int length = Array.getLength(this);
            Object cloned = Array.newInstance(elemType, length);
            System.arraycopy(this, 0, cloned, 0, length);
            return cloned;
        } else {
            if (!Cloneable.class.isAssignableFrom(clazz)) {
                throw new CloneNotSupportedException();
            }
            Object cloned = CompilerIntrinsics.emitNew(clazz);
            CompilerIntrinsics.copyInstanceFields(clazz, this, cloned);
            cloned.monitor = null;
            return cloned;
        }
    }

    @Deprecated
    protected void finalize() throws Throwable {}
}
