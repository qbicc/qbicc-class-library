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

import org.qbicc.rt.annotation.Tracking;

/*
 * This class exists because defining an anonymous Runnable in a patch class
 * like LogManager$_patch is not currently supported by our Patcher infrastructure.
 */
class LogManager_Runnable1 implements Runnable {
    final LogManager lm;

    LogManager_Runnable1(LogManager lm) {
        this.lm = lm;
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Cleaner(lm));
    }

    @Tracking("src/java.logging/share/classes/java/util/LogManager.java")
    static class Cleaner extends Thread {
        final LogManager lm;
        private Cleaner(LogManager lm) {
            super(null, null, "Logging-Cleaner", 0, false);
            this.setContextClassLoader(null);
            this.lm = lm;
        }

        @Override
        public void run() {
            ((LogManager$_patch)(Object)lm).setStateToShutdown();
            lm.reset();
        }
    }
}
