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

package jdk.internal.ref;

import java.lang.ref.Cleaner;
import java.util.concurrent.ThreadFactory;

import jdk.internal.org.qbicc.runtime.Main;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(CleanerImpl.class)
@Tracking("src/java.base/share/classes/jdk/internal/ref/CleanerImpl.java")
public final class CleanerImpl$_patch {

    // Alias
    static native CleanerImpl$_patch getCleanerImpl(Cleaner cleaner);

    @Replace
    public void start(Cleaner cleaner, ThreadFactory threadFactory) {
        if (getCleanerImpl(cleaner) != this) {
            throw new AssertionError("wrong cleaner");
        }
        // schedule a nop cleaning action for the cleaner, so the associated thread
        // will continue to run at least until the cleaner is reclaimable.
        new CleanerImpl.CleanerCleanable(cleaner);

        if (threadFactory == null) {
            threadFactory = CleanerImpl.InnocuousThreadFactory.factory();
        }

        // now that there's at least one cleaning action, for the cleaner,
        // we can start the associated thread, which runs until
        // all cleaning actions have been run.
        if (Build.isHost()) {
            Main.deferInitAction(new CleanerImpl_Runnable1(threadFactory, (CleanerImpl)(Object)this));
        } else {
            Thread thread = threadFactory.newThread((CleanerImpl)(Object)this);
            thread.setDaemon(true);
            thread.start();
        }
    }
}
