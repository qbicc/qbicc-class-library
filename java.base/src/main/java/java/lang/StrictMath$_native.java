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

package java.lang;

import org.qbicc.rt.annotation.Tracking;

@SuppressWarnings("SpellCheckingInspection")
@Tracking("src/java.base/share/native/libjava/StrictMath.c")
@Tracking("src/java.base/share/classes/java/lang/StrictMath.java")
public class StrictMath$_native {
    public static double sin(double val) {
        return org.qbicc.runtime.stdc.Math.sin(val);
    }

    public static double cos(double val) {
        return org.qbicc.runtime.stdc.Math.cos(val);
    }

    public static double tan(double val) {
        return org.qbicc.runtime.stdc.Math.tan(val);
    }

    public static double asin(double val) {
        return org.qbicc.runtime.stdc.Math.asin(val);
    }

    public static double acos(double val) {
        return org.qbicc.runtime.stdc.Math.acos(val);
    }

    public static double atan(double val) {
        return org.qbicc.runtime.stdc.Math.atan(val);
    }

    public static double log(double val) {
        return org.qbicc.runtime.stdc.Math.log(val);
    }

    public static double log10(double val) {
        return org.qbicc.runtime.stdc.Math.log10(val);
    }

    public static double sqrt(double val) {
        return org.qbicc.runtime.stdc.Math.sqrt(val);
    }

    public static double IEEEremainder(double a, double b) {
        return org.qbicc.runtime.stdc.Math.remainder(a, b);
    }

    public static double atan2(double a, double b) {
        return org.qbicc.runtime.stdc.Math.atan2(a, b);
    }

    public static double sinh(double val) {
        return org.qbicc.runtime.stdc.Math.sinh(val);
    }

    public static double cosh(double val) {
        return org.qbicc.runtime.stdc.Math.cosh(val);
    }

    public static double tanh(double val) {
        return org.qbicc.runtime.stdc.Math.tanh(val);
    }

    public static double expm1(double val) {
        return org.qbicc.runtime.stdc.Math.expm1(val);
    }

    public static double log1p(double val) {
        return org.qbicc.runtime.stdc.Math.log1p(val);
    }
}
