/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 1995, 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Map;

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/native/libjava/ProcessImpl_md.c")
@Tracking("src/java.base/unix/classes/java/lang/ProcessImpl.java")
@Tracking("src/java.base/windows/native/libjava/ProcessImpl_md.c")
@Tracking("src/java.base/windows/classes/java/lang/ProcessImpl.java")
abstract class ProcessImpl extends Process {
    static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();

    static final boolean SUPPORTS_NORMAL_TERMINATION = ! Build.Target.isWindows();

    ProcessImpl() {
    }

    /* ProcessBuilder.start() */
    static Process start(String[] cmdArray,
            Map<String,String> environment,
            String dir,
            ProcessBuilder.Redirect[] redirects,
            boolean redirectErrorStream
    ) throws IOException {
        if (Build.Target.isWindows()) {
            return WindowsProcessImpl.start(cmdArray, environment, dir, redirects, redirectErrorStream);
        } else {
            return PosixProcessImpl.start(cmdArray, environment, dir, redirects, redirectErrorStream);
        }
    }
}

