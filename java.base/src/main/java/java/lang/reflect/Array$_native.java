package java.lang.reflect;

import org.qbicc.runtime.main.CompilerIntrinsics;

public final class Array$_native {

    private static Object newArray(Class<?> componentType, int length) throws NegativeArraySizeException {
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
        }  else if (componentType == float.class) {
            return new float[length];
        }  else if (componentType == long.class) {
            return new long[length];
        } else if (componentType == double.class) {
            return new double[length];
        } else if (componentType == void.class) {
            throw new IllegalArgumentException();
        } else {
            if (componentType == null) {
                throw new NullPointerException();
            }
            int dimensions = 1;
            while (componentType.isArray()) {
                dimensions += 1;
                componentType = componentType.getComponentType();
            }
            if (dimensions > 255) {
                throw new IllegalArgumentException();
            }
            return CompilerIntrinsics.emitNewReferenceArray(componentType, dimensions, length);
        }
    }

    private static Object multiNewArray(Class<?> componentType, int[] dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        int dimCount = dimensions.length;
        if (componentType.isPrimitive()) {
            dimCount -= 1;
        } else {
            while (componentType.isArray()) {
                dimCount += 1;
                componentType = componentType.getComponentType();
            }
        }
        if (dimensions.length == 0 || dimCount > 255 || void.class == componentType) {
            throw new IllegalArgumentException();
        }
        return multiNewArrayHelper(componentType, dimCount, 0, dimensions);
    }

    private static Object multiNewArrayHelper(Class<?> leafElemType, int dimCount, int dimIdx, int[] dimensions) throws NegativeArraySizeException {
        if (dimIdx == dimensions.length - 1) {
            if (dimCount > 0) {
                return CompilerIntrinsics.emitNewReferenceArray(leafElemType, dimCount, dimensions[dimIdx]);
            } else {
                return newArray(leafElemType, dimensions[dimIdx]);
            }
        }
        Object[] spine = (Object[])CompilerIntrinsics.emitNewReferenceArray(leafElemType, dimCount, dimensions[dimIdx]);
        for (int i=0; i<dimensions[dimIdx]; i++) {
            spine[i] = multiNewArrayHelper(leafElemType, dimCount-1, dimIdx+1, dimensions);
        }
        return spine;
    }

}
