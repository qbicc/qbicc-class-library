/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2008, 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qccrt.annotation.Tracking;

/**
 *
 */
@Tracking("openjdk/src/java.base/aix/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/linux/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/macosx/classes/sun/nio/fs/DefaultFileSystemProvider.java")
@Tracking("openjdk/src/java.base/windows/classes/sun/nio/fs/DefaultFileSystemProvider.java")
public class DefaultFileSystemProvider {
    private static final FileSystemProvider INSTANCE;
    private static final FileSystem THE_FILE_SYSTEM;

    static {
        if (Build.Target.isAix()) {
            AixFileSystemProvider provider = new AixFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isLinux()) {
            LinuxFileSystemProvider provider = new LinuxFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isMacOs()) {
            MacOSXFileSystemProvider provider = new MacOSXFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else if (Build.Target.isWindows()) {
            WindowsFileSystemProvider provider = new WindowsFileSystemProvider();
            INSTANCE = provider;
            THE_FILE_SYSTEM = provider.theFileSystem();
        } else {
            throw new Error();
        }
    }

    private DefaultFileSystemProvider() { }

    /**
     * Returns the platform's default file system provider.
     */
    public static FileSystemProvider instance() {
        return INSTANCE;
    }

    /**
     * Returns the platform's default file system.
     */
    public static FileSystem theFileSystem() {
        return THE_FILE_SYSTEM;
    }

}
