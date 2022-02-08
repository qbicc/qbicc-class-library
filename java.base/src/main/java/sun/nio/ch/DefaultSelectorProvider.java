/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2002, 2011, 2015, 2017, 2021 Oracle and/or its affiliates. All rights reserved.
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
package sun.nio.ch;

import org.qbicc.runtime.Build;
import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/aix/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("src/java.base/linux/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("src/java.base/macosx/classes/sun/nio/ch/DefaultSelectorProvider.java")
@Tracking("src/java.base/windows/classes/sun/nio/ch/DefaultSelectorProvider.java")
public class DefaultSelectorProvider {

    private static final SelectorProviderImpl INSTANCE = create();

    /**
     * Prevent instantiation.
     */
    private DefaultSelectorProvider() { }

    /**
     * Returns the default SelectorProvider.
     */
    private static SelectorProviderImpl create() {
        if (Build.Target.isLinux()) {
            return new EPollSelectorProvider();
        } else if (Build.Target.isMacOs()) {
            return new KQueueSelectorProvider();
        } else if (Build.Target.isWindows()) {
            return new WindowsSelectorProvider();
        } else {
            return new PollSelectorProvider();
        }
    }

    public static SelectorProviderImpl get() {
        return INSTANCE;
    }
}
