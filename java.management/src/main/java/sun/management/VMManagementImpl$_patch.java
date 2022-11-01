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
package sun.management;

import jdk.internal.org.qbicc.runtime.FlightRecorder;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

@Tracking("src/java.management/share/native/libmanagement/VMManagementImpl.c")
@PatchClass(sun.management.VMManagementImpl.class)
public class VMManagementImpl$_patch {
    // Alias
    private static boolean compTimeMonitoringSupport;
    private static boolean threadContentionMonitoringSupport;
    private static boolean currentThreadCpuTimeSupport;
    private static boolean otherThreadCpuTimeSupport;
    private static boolean objectMonitorUsageSupport;
    private static boolean synchronizerUsageSupport;
    private static boolean threadAllocatedMemorySupport;
    private static boolean gcNotificationSupport;
    private static boolean remoteDiagnosticCommandsSupport;

    @Replace
    private static String getVersion0() {
        // From hotspot/share/include/jmm.h
        int JMM_VERSION_3   = 0x20030000; // JDK 14
        int JMM_VERSION     = JMM_VERSION_3;
        int major = (JMM_VERSION & 0x0FFF0000) >> 16;
        int minor = (JMM_VERSION & 0xFF00) >> 8;
        return major+"."+minor;
    }

    @Replace
    private static void initOptionalSupportFields() {
        compTimeMonitoringSupport = false;
        threadContentionMonitoringSupport = false;
        currentThreadCpuTimeSupport = false;
        otherThreadCpuTimeSupport = false;
        objectMonitorUsageSupport = false;
        synchronizerUsageSupport = false;
        threadAllocatedMemorySupport = false;
        gcNotificationSupport = false;
        remoteDiagnosticCommandsSupport = false;
    }

    @Replace
    public long getStartupTime() {
        return FlightRecorder.initDoneTime;
    }

}
