/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2002, 2012, 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/classes/jdk/internal/loader/ClassLoaderHelper.java")
@Tracking("src/java.base/macosx/classes/jdk/internal/loader/ClassLoaderHelper.java")
@Tracking("src/java.base/windows/classes/jdk/internal/loader/ClassLoaderHelper.java")
class ClassLoaderHelper {

    private ClassLoaderHelper() {}

    static final boolean allowsQuotedPathElements = Build.Target.isWindows();

    static File mapAlternativeName(File lib) {
        if (Build.Target.isMacOs()) {
            String name = lib.toString();
            int index = name.lastIndexOf('.');
            if (index < 0) {
                return null;
            }
            return new File(name.substring(0, index) + ".jnilib");
        } else {
            return null;
        }
    }

    static boolean loadLibraryOnlyIfPresent() {
        return !Build.Target.isMacOs(); // Slight simplificaton.  Actual condition is ! (MacOS > 11) (due to dynamic linker cache)
    }
}
