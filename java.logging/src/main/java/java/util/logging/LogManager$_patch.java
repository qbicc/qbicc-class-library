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

package java.util.logging;

import jdk.internal.org.qbicc.runtime.Main;
import java.util.concurrent.locks.ReentrantLock;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@PatchClass(LogManager.class)
@Tracking("src/java.logging/share/classes/java/util/LogManager.java")
public class LogManager$_patch {

    // Alias
    private volatile int globalHandlersState;
    // Alias
    private ReentrantLock configurationLock;

    /*
     * LogManager's private (Void) constructor contains all the default initialization
     * code for its instance fields in addition to the call to addShutdownHook that
     * we need to modify. Attempting to replicate the field initialization so we can directly
     * replace the private constructor results in an fairly involved and ugly patch.
     * So instead we replace the otherwise trivial private constructor to inject
     * the call to deferInitAction. In conjunction with making Runtime.addShutdownHook
     * a no-op at build time, this does what we need (but is somewhat fragile...).
     */

    @Replace
    protected LogManager$_patch() {
        this(checkSubclassPermissions());
        if (Build.isHost()) {
            Main.deferInitAction(new LogManager_Runnable1((LogManager)(Object)this));
        }
    }

    // Alias
    private LogManager$_patch(Void checked) {
        checkSubclassPermissions(); // prevent javac from optimizing away constructor
    }
    private static native Void checkSubclassPermissions();

    @Add
    void setStateToShutdown() {
        configurationLock.lock();
        globalHandlersState = 4; /* LogManager.STATE_SHUTDOWN */;
        configurationLock.unlock();
    }
}
