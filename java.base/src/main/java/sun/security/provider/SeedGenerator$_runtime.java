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

package sun.security.provider;

import java.io.IOException;
import sun.security.util.Debug;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(sun.security.provider.SeedGenerator.class)
@RunTimeAspect
class SeedGenerator$_runtime {
    private static SeedGenerator instance;

    // Static initializer to hook in selected or best performing generator
    static {
        String egdSource = SunEntries.getSeedSource();

        /*
         * Try the URL specifying the source (e.g. file:/dev/random)
         *
         * The URLs "file:/dev/random" or "file:/dev/urandom" are used to
         * indicate the SeedGenerator should use OS support, if available.
         *
         * On Windows, this causes the MS CryptoAPI seeder to be used.
         *
         * On Solaris/Linux/MacOS, this is identical to using
         * URLSeedGenerator to read from /dev/[u]random
         */
        if (egdSource.equals(SunEntries.URL_DEV_RANDOM) ||
                egdSource.equals(SunEntries.URL_DEV_URANDOM)) {
            try {
                instance = new NativeSeedGenerator(egdSource);
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println(
                            "Using operating system seed generator" + egdSource);
                }
            } catch (IOException e) {
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Failed to use operating system seed "
                            + "generator: " + e.toString());
                }
            }
        } else if (!egdSource.isEmpty()) {
            try {
                instance = new SeedGenerator.URLSeedGenerator(egdSource);
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Using URL seed generator reading from "
                            + egdSource);
                }
            } catch (IOException e) {
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Failed to create seed generator with "
                            + egdSource + ": " + e.toString());
                }
            }
        }

        // Fall back to ThreadedSeedGenerator
        if (instance == null) {
            if (SeedGenerator$_patch.debug != null) {
                SeedGenerator$_patch.debug.println("Using default threaded seed generator");
            }
            instance = (SeedGenerator) (Object) new SeedGenerator$_patch.ThreadedSeedGenerator$_patch();
        }
    }
}
