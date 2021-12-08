package java.lang.reflect;

import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.CNative;
import org.qbicc.runtime.CNative.*;

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
            type_id typeId = CompilerIntrinsics.getTypeIdFromClass(componentType);
            return CompilerIntrinsics.emitNewReferenceArray(typeId, CNative.word(dimensions), length);
        }
    }
}
