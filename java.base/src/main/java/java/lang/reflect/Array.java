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
public final class Array {
    private Array() {}

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

    private static Object multiNewArrayHelper(Class<?> leafType, int dimCount, Class<?> lastLeafType, int dimIdx, int[] dimensions) throws NegativeArraySizeException {
        if (dimIdx == dimensions.length - 1) {
            if (dimCount > 0) {
                return CompilerIntrinsics.emitNewReferenceArray(leafType, dimCount, dimensions[dimIdx]);
            } else {
                if (lastLeafType.isPrimitive()) {
                    if (lastLeafType == byte.class) {
                        return new byte[dimensions[dimIdx]];
                    } else if (lastLeafType == boolean.class) {
                        return new boolean[dimensions[dimIdx]];
                    } else if (lastLeafType == short.class) {
                        return new short[dimensions[dimIdx]];
                    } else if (lastLeafType == char.class) {
                        return new char[dimensions[dimIdx]];
                    } else if (lastLeafType == int.class) {
                        return new int[dimensions[dimIdx]];
                    } else if (lastLeafType == float.class) {
                        return new float[dimensions[dimIdx]];
                    } else if (lastLeafType == long.class) {
                        return new long[dimensions[dimIdx]];
                    } else if (lastLeafType == double.class) {
                        return new double[dimensions[dimIdx]];
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    int componentDim = CompilerIntrinsics.getDimensionsFromClass(lastLeafType).intValue();
                    if (componentDim == 0) {
                        return CompilerIntrinsics.emitNewReferenceArray(lastLeafType, 1, dimensions[dimIdx]);
                    } else if (componentDim > 254) {
                        throw new IllegalArgumentException();
                    } else {
                        type_id leafTypeId = CompilerIntrinsics.getTypeIdFromClass(lastLeafType);
                        Class<?> leafClass = CompilerIntrinsics.getClassFromTypeIdSimple(leafTypeId);
                        return CompilerIntrinsics.emitNewReferenceArray(leafClass, componentDim + 1, dimensions[dimIdx]);
                    }
                }
            }
        }
        Object[] spine = (Object[])CompilerIntrinsics.emitNewReferenceArray(leafType, dimCount, dimensions[dimIdx]);
        for (int i=0; i<dimensions[dimIdx]; i++) {
            spine[i] = multiNewArrayHelper(leafType, dimCount-1, lastLeafType, dimIdx+1, dimensions);
        }
        return spine;
    }

    public static Object newInstance(Class<?> componentType, int length) throws NegativeArraySizeException {
        return newArray(componentType, length);
    }

    private static Object newArray(final Class<?> componentType, final int length) throws NegativeArraySizeException {
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

    public static Object newInstance(Class<?> componentType, int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
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

    public static Object get(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            return a[index];
        } else if (array instanceof byte[] a) {
            return Byte.valueOf(a[index]);
        } else if (array instanceof boolean[] a) {
            return Boolean.valueOf(a[index]);
        } else if (array instanceof short[] a) {
            return Short.valueOf(a[index]);
        } else if (array instanceof char[] a) {
            return Character.valueOf(a[index]);
        } else if (array instanceof int[] a) {
            return Integer.valueOf(a[index]);
        } else if (array instanceof long[] a) {
            return Long.valueOf(a[index]);
        } else if (array instanceof float[] a) {
            return Float.valueOf(a[index]);
        } else if (array instanceof double[] a) {
            return Double.valueOf(a[index]);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static boolean getBoolean(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof boolean[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static byte getByte(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static char getChar(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof char[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static short getShort(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else if (array instanceof short[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static int getInt(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else if (array instanceof char[] a) {
            return a[index];
        } else if (array instanceof short[] a) {
            return a[index];
        } else if (array instanceof int[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static long getLong(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else if (array instanceof char[] a) {
            return a[index];
        } else if (array instanceof short[] a) {
            return a[index];
        } else if (array instanceof int[] a) {
            return a[index];
        } else if (array instanceof long[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static float getFloat(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else if (array instanceof char[] a) {
            return a[index];
        } else if (array instanceof short[] a) {
            return a[index];
        } else if (array instanceof int[] a) {
            return a[index];
        } else if (array instanceof float[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static double getDouble(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[] a) {
            return a[index];
        } else if (array instanceof char[] a) {
            return a[index];
        } else if (array instanceof short[] a) {
            return a[index];
        } else if (array instanceof int[] a) {
            return a[index];
        } else if (array instanceof long[] a) {
            return a[index];
        } else if (array instanceof float[] a) {
            return a[index];
        } else if (array instanceof double[] a) {
            return a[index];
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void set(Object array, int index, Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = value;
        } else if (array instanceof byte[] a) {
            if (value instanceof Byte v) {
                a[index] = v.byteValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof boolean[] a) {
            if (value instanceof Boolean v) {
                a[index] = v.booleanValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof short[] a) {
            if (value instanceof Byte v) {
                a[index] = v.shortValue();
            } else if (value instanceof Short v) {
                a[index] = v.shortValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof char[] a) {
            if (value instanceof Character v) {
                a[index] = v.charValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof int[] a) {
            if (value instanceof Byte v) {
                a[index] = v.intValue();
            } else if (value instanceof Character v) {
                a[index] = v.charValue();
            } else if (value instanceof Short v) {
                a[index] = v.intValue();
            } else if (value instanceof Integer v) {
                a[index] = v.intValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof long[] a) {
            if (value instanceof Byte v) {
                a[index] = v.longValue();
            } else if (value instanceof Character v) {
                a[index] = v.charValue();
            } else if (value instanceof Short v) {
                a[index] = v.longValue();
            } else if (value instanceof Integer v) {
                a[index] = v.longValue();
            } else if (value instanceof Long v) {
                a[index] = v.longValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof float[] a) {
            if (value instanceof Byte v) {
                a[index] = v.floatValue();
            } else if (value instanceof Character v) {
                a[index] = v.charValue();
            } else if (value instanceof Short v) {
                a[index] = v.floatValue();
            } else if (value instanceof Integer v) {
                a[index] = v.floatValue();
            } else if (value instanceof Float v) {
                a[index] = v.floatValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (array instanceof double[] a) {
            if (value instanceof Byte v) {
                a[index] = v.doubleValue();
            } else if (value instanceof Character v) {
                a[index] = v.charValue();
            } else if (value instanceof Short v) {
                a[index] = v.doubleValue();
            } else if (value instanceof Integer v) {
                a[index] = v.doubleValue();
            } else if (value instanceof Float v) {
                a[index] = v.doubleValue();
            } else if (value instanceof Long v) {
                a[index] = v.doubleValue();
            } else if (value instanceof Double v) {
                a[index] = v.doubleValue();
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setBoolean(Object array, int index, boolean v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Boolean.valueOf(v);
        } else if (array instanceof boolean[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setByte(Object array, int index, byte v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Byte.valueOf(v);
        } else if (array instanceof byte[] a) {
            a[index] = v;
        } else if (array instanceof short[] a) {
            a[index] = v;
        } else if (array instanceof int[] a) {
            a[index] = v;
        } else if (array instanceof long[] a) {
            a[index] = v;
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setChar(Object array, int index, char v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Character.valueOf(v);
        } else if (array instanceof char[] a) {
            a[index] = v;
        } else if (array instanceof int[] a) {
            a[index] = v;
        } else if (array instanceof long[] a) {
            a[index] = v;
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setShort(Object array, int index, short v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Short.valueOf(v);
        } else if (array instanceof short[] a) {
            a[index] = v;
        } else if (array instanceof int[] a) {
            a[index] = v;
        } else if (array instanceof long[] a) {
            a[index] = v;
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setInt(Object array, int index, int v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Integer.valueOf(v);
        } else if (array instanceof int[] a) {
            a[index] = v;
        } else if (array instanceof long[] a) {
            a[index] = v;
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setLong(Object array, int index, long v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Long.valueOf(v);
        } else if (array instanceof long[] a) {
            a[index] = v;
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setFloat(Object array, int index, float v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Float.valueOf(v);
        } else if (array instanceof float[] a) {
            a[index] = v;
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void setDouble(Object array, int index, double v) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof Object[] a) {
            a[index] = Double.valueOf(v);
        } else if (array instanceof double[] a) {
            a[index] = v;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
