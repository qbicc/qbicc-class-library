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
package java.lang.ref;

import jdk.internal.access.JavaLangRefAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.main.Main;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(Reference.class)
@ReplaceInit
@Tracking("src/java.base/share/classes/java/lang/ref/Reference.java")
public abstract class Reference$_patch<T> {
    // Alias & preserve original <clinit>
    private static final Object processPendingLock = new Object();
    // Alias & preserve orginal <clinit>
    private static boolean processPendingActive = false;

    @Add
    private static Object pendingReferenceListLock = new Object();
    @Add
    private static Reference pendingReferenceList = null;

    // Alias
    private static native boolean waitForReferenceProcessing() throws InterruptedException;

    static {
        Main.deferInitAction(new ReferenceDeferredInitAction());

        // provide access in SharedSecrets
        SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
            @Override
            public boolean waitForReferenceProcessing()
                    throws InterruptedException
            {
                return Reference$_patch.waitForReferenceProcessing();
            }

            @Override
            public void runFinalization() {
                Finalizer.runFinalization();
            }
        });
    }

    // Alias
    private Object referent;

    @Replace
    private void clear0() {
        referent = null;
    }

    // Alias -- make accessible to ReferenceDeferredInitAction.ReferenceHandler
    static native void processPendingReferences();

    @Replace
    private static Reference<?> getAndClearReferencePendingList() {
        Reference<?> prior = null;
        synchronized(pendingReferenceListLock) {
            prior = pendingReferenceList;
            pendingReferenceList = null;
        }
        return prior;
    }

    @Replace
    private static boolean hasReferencePendingList() {
        synchronized(pendingReferenceListLock) {
            return pendingReferenceList != null;
        }
    }

    @Replace
    private static void waitForReferencePendingList() {
        synchronized (pendingReferenceListLock) {
            try {
                pendingReferenceListLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
