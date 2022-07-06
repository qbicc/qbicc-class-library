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
package java.lang.invoke;

import java.lang.reflect.Constructor;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

import org.qbicc.rt.annotation.Tracking;

// This class supports the backport of OpenJDK 18 MethodHandle based reflection to JDK17 for Qbicc.
//
// It is a hack because patching the <clinit> of MethodHandleImpl to add the needed new methods
// to the anonymous JavaLangInvokeAccess implementation it creates without causing massive
// code duplication is beyond the abilities of our current Patcher infrastructure.
//
// This class should be removed entirely as soon as we move to the next LTS version of Java
// and can ditch the packported reflection code.
@Tracking("src/java.base/share/classes/java/lang/invoke/MethodHandleImpl.java")
public class MethodAccessorBackdoor {

    /**
     * Produces a method handle unreflecting from a {@code Constructor} with
     * the trusted lookup
     */
    public static MethodHandle unreflectConstructor(Constructor<?> ctor) throws IllegalAccessException {
        return IMPL_LOOKUP.unreflectConstructor(ctor);
    }

    /**
     * Produces a method handle of a virtual method with the trusted lookup.
     */
    public static MethodHandle findVirtual(Class<?> defc, String name, MethodType type) throws IllegalAccessException {
        try {
            return IMPL_LOOKUP.findVirtual(defc, name, type);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Produces a method handle of a static method with the trusted lookup.
     */
    public static MethodHandle findStatic(Class<?> defc, String name, MethodType type) throws IllegalAccessException {
        try {
            return IMPL_LOOKUP.findStatic(defc, name, type);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}