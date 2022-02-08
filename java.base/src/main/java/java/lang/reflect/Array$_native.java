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
package java.lang.reflect;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.main.CompilerIntrinsics;

import static org.qbicc.runtime.CNative.*;

@Tracking("src/java.base/share/classes/java/lang/reflect/Array.java")
public final class Array$_native {

    public static int getLength(Object array) throws IllegalArgumentException {
        if (array == null) {
            throw new NullPointerException();
        }
        type_id typeId = CompilerIntrinsics.typeIdOf(array);
        if (CompilerIntrinsics.isReferenceArray(typeId) || CompilerIntrinsics.isPrimArray(typeId)) {
            return CompilerIntrinsics.lengthOf(array);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Object newArray(Class<?> componentType, int length) throws NegativeArraySizeException {
        if (componentType.isPrimitive()) {
            if (componentType == byte.class) {
                return new byte[length];
            } else if (componentType == boolean.class) {
                return new boolean[length];
            } else if (componentType == short.class) {
                return new short[length];
            } else if (componentType == char.class) {
                return new char[length];
            } else if (componentType == int.class) {
                return new int[length];
            } else if (componentType == float.class) {
                return new float[length];
            } else if (componentType == long.class) {
                return new long[length];
            } else if (componentType == double.class) {
                return new double[length];
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            int componentDim = CompilerIntrinsics.getDimensionsFromClass(componentType).intValue();
            if (componentDim == 0) {
                return CompilerIntrinsics.emitNewReferenceArray(componentType, 1, length);
            } else if (componentDim > 254) {
                throw new IllegalArgumentException();
            } else {
                type_id leafTypeId = CompilerIntrinsics.getTypeIdFromClass(componentType);
                Class<?> leafClass = CompilerIntrinsics.getClassFromTypeIdSimple(leafTypeId);
                return CompilerIntrinsics.emitNewReferenceArray(leafClass, componentDim + 1, length);
            }
        }
    }

    private static Object multiNewArray(Class<?> componentType, int[] dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        int dimCount = dimensions.length;
        Class<?> lastLeafType;
        Class<?> leafType;
        if (componentType.isPrimitive()) {
            if (componentType == void.class) {
                throw new IllegalArgumentException();
            }
            dimCount -= 1;
            lastLeafType = componentType;
            leafType = CompilerIntrinsics.getArrayClassOf(componentType);
        } else {
            int componentDim = CompilerIntrinsics.getDimensionsFromClass(componentType).intValue();
            if (componentDim == 0) {
                leafType = lastLeafType = componentType;
            } else {
                dimCount += componentDim;
                type_id leafTypeId = CompilerIntrinsics.getTypeIdFromClass(componentType);
                leafType = lastLeafType = CompilerIntrinsics.getClassFromTypeIdSimple(leafTypeId);
            }
        }

        if (dimensions.length == 0 || dimCount > 255) {
            throw new IllegalArgumentException();
        }
        return multiNewArrayHelper(leafType, dimCount, lastLeafType, 0, dimensions);
    }

    private static Object multiNewArrayHelper(Class<?> leafType, int dimCount, Class<?> lastLeafType, int dimIdx, int[] dimensions) throws NegativeArraySizeException {
        if (dimIdx == dimensions.length - 1) {
            if (dimCount > 0) {
                return CompilerIntrinsics.emitNewReferenceArray(leafType, dimCount, dimensions[dimIdx]);
            } else {
                return newArray(lastLeafType, dimensions[dimIdx]);
            }
        }
        Object[] spine = (Object[])CompilerIntrinsics.emitNewReferenceArray(leafType, dimCount, dimensions[dimIdx]);
        for (int i=0; i<dimensions[dimIdx]; i++) {
            spine[i] = multiNewArrayHelper(leafType, dimCount-1, lastLeafType, dimIdx+1, dimensions);
        }
        return spine;
    }

}
