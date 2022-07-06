/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodAccessorBackdoor;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.qbicc.rt.annotation.Tracking;

import static java.lang.invoke.MethodType.genericMethodType;
import static java.lang.invoke.MethodType.methodType;

// This is a backport of OpenJDK 18 MethodHandle based reflection to JDK17 for Qbicc.
// It has been simplified by stripping out functionality we don't need.
// It should be removed when we update to the next LTS version of Java.
@Tracking("src/java.base/share/classes/jdk/internal/reflect/MethodHandleAccessorFactory.java")
final class MethodHandleAccessorFactory {
    /**
     * Creates a MethodAccessor for the given reflected method.
     *
     * To simplify the backport, all support for CallerSensitive methods has been dropped.
     */
    static MethodAccessorImpl newMethodAccessor(Method method, boolean callerSensitive) {
        if (callerSensitive) throw new UnsupportedOperationException("Support for callerSensitive methods not backported to qbicc");
        try {
            var dmh = getDirectMethod(method);
            return DirectMethodHandleAccessor.methodAccessor(method, dmh);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Creates a ConstructorAccessor for the given reflected constructor.
     */
    static ConstructorAccessorImpl newConstructorAccessor(Constructor<?> ctor) {
        try {
            MethodHandle mh = MethodAccessorBackdoor.unreflectConstructor(ctor);
            int paramCount = mh.type().parameterCount();
            MethodHandle target = mh.asFixedArity();
            MethodType mtype = specializedMethodTypeForConstructor(paramCount);
            if (paramCount > SPECIALIZED_PARAM_COUNT) {
                // spread the parameters only for the non-specialized case
                target = target.asSpreader(Object[].class, paramCount);
            }
            target = target.asType(mtype);
            return DirectConstructorHandleAccessor.constructorAccessor(ctor, target);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    private static MethodHandle getDirectMethod(Method method) throws IllegalAccessException {
        var mtype = methodType(method.getReturnType(), method.getParameterTypes());
        var isStatic = Modifier.isStatic(method.getModifiers());
        var dmh = isStatic ? MethodAccessorBackdoor.findStatic(method.getDeclaringClass(), method.getName(), mtype)
                : MethodAccessorBackdoor.findVirtual(method.getDeclaringClass(), method.getName(), mtype);
        return makeSpecializedTarget(dmh, isStatic);
    }

    /**
     * Transform the given dmh to a specialized target method handle.
     *
     * If {@code hasCallerParameter} parameter is false, transform the method handle
     * of this method type: {@code (Object, Object[])Object} for the default case.
     *
     * If the number of formal arguments is small, use a method type specialized
     * the number of formal arguments is 0, 1, and 2, for example, the method type
     * of a static method with one argument can be: {@code (Object)Object}
     *
     * If it's a static method, there is no leading Object parameter.
     *
     * @apiNote
     * This implementation avoids using MethodHandles::catchException to help
     * cold startup performance since this combination is very costly to setup.
     *
     * @param dmh DirectMethodHandle
     * @param isStatic whether given dmh represents static method or not
     * @return transformed dmh to be used as a target in direct method accessors
     */
    static MethodHandle makeSpecializedTarget(MethodHandle dmh, boolean isStatic) {
        MethodHandle target = dmh.asFixedArity();

        // number of formal arguments to the original method (not the adapter)
        // If it is a non-static method, it has a leading `this` argument.
        // Also do not count the caller class argument
        int paramCount = dmh.type().parameterCount() - (isStatic ? 0 : 1) ;
        MethodType mtype = specializedMethodType(isStatic, paramCount);
        if (paramCount > SPECIALIZED_PARAM_COUNT) {
            int spreadArgPos = isStatic ? 0 : 1;
            target = target.asSpreader(spreadArgPos, Object[].class, paramCount);
        }
        if (isStatic) {
            // add leading 'this' parameter to static method which is then ignored
            target = MethodHandles.dropArguments(target, 0, Object.class);
        }
        return target.asType(mtype);
    }

    // specialize for number of formal arguments <= 3 to avoid spreader
    static final int SPECIALIZED_PARAM_COUNT = 3;
    static MethodType specializedMethodType(boolean isStatic, int paramCount) {
        return switch (paramCount) {
            case 0 -> genericMethodType(1);
            case 1 -> genericMethodType(2);
            case 2 -> genericMethodType(3);
            case 3 -> genericMethodType(4);
            default -> genericMethodType(1, true);
        };
    }

    static MethodType specializedMethodTypeForConstructor(int paramCount) {
        return switch (paramCount) {
            case 0 ->  genericMethodType(0);
            case 1 ->  genericMethodType(1);
            case 2 ->  genericMethodType(2);
            case 3 ->  genericMethodType(3);
            default -> genericMethodType(0, true);
        };
    }
}
