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

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Hidden;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.qbicc.rt.annotation.Tracking;

import static java.lang.invoke.MethodType.genericMethodType;
import static jdk.internal.reflect.MethodHandleAccessorFactory.SPECIALIZED_PARAM_COUNT;

// This is a backport of OpenJDK 18 MethodHandle based reflection to JDK17 for Qbicc.
// It has been simplified by stripping out functionality we don't need.
// It should be removed when we update to the next LTS version of Java.
@Tracking("src/java.base/share/classes/jdk/internal/reflect/DirectMethodHandleAccessor.java")
class DirectMethodHandleAccessor extends MethodAccessorImpl {
    /**
     * Creates a MethodAccessorImpl for a non-native method.
     */
    static MethodAccessorImpl methodAccessor(Method method, MethodHandle target) {
        assert !Modifier.isNative(method.getModifiers());
        return new DirectMethodHandleAccessor(method, target);
    }

    private static final int PARAM_COUNT_MASK = 0x00FF;
    private static final int IS_STATIC_BIT = 0x0200;
    private static final int NONZERO_BIT = 0x8000_0000;

    private final Class<?> declaringClass;
    private final int paramCount;
    private final int flags;
    private final MethodHandle target;

    DirectMethodHandleAccessor(Method method, MethodHandle target) {
        this.declaringClass = method.getDeclaringClass();
        this.paramCount = method.getParameterCount();
        this.flags = (Modifier.isStatic(method.getModifiers()) ? IS_STATIC_BIT : 0);
        this.target = target;
    }

    @Override
    @ForceInline
    public Object invoke(Object obj, Object[] args) throws InvocationTargetException {
        if (!isStatic()) {
            checkReceiver(obj);
        }
        checkArgumentCount(paramCount, args);
        try {
            return invokeImpl(obj, args);
        } catch (ClassCastException | WrongMethodTypeException e) {
            if (isIllegalArgument(e)) {
                // No cause in IAE to be consistent with the old behavior
                throw new IllegalArgumentException("argument type mismatch");
            } else {
                throw new InvocationTargetException(e);
            }
        } catch (NullPointerException e) {
            if (isIllegalArgument(e)) {
                throw new IllegalArgumentException(e);
            } else {
                throw new InvocationTargetException(e);
            }
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    @Hidden
    @ForceInline
    private Object invokeImpl(Object obj, Object[] args) throws Throwable {
        return switch (paramCount) {
            case 0 -> target.invokeExact(obj);
            case 1 -> target.invokeExact(obj, args[0]);
            case 2 -> target.invokeExact(obj, args[0], args[1]);
            case 3 -> target.invokeExact(obj, args[0], args[1], args[2]);
            default -> target.invokeExact(obj, args);
        };
    }

    private boolean isStatic() {
        return (flags & IS_STATIC_BIT) == IS_STATIC_BIT;
    }

    private boolean isIllegalArgument(RuntimeException ex) {
        return AccessorUtils.isIllegalArgument(DirectMethodHandleAccessor.class, ex);
    }

    private void checkReceiver(Object o) {
        // NOTE: will throw NullPointerException, as specified, if o is null
        if (!declaringClass.isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException("object is not an instance of declaring class");
        }
    }

    private static void checkArgumentCount(int paramCount, Object[] args) {
        // only check argument count for specialized forms
        if (paramCount > SPECIALIZED_PARAM_COUNT) return;

        int argc = args != null ? args.length : 0;
        if (argc != paramCount) {
            throw new IllegalArgumentException("wrong number of arguments: " + argc + " expected: " + paramCount);
        }
    }
}
