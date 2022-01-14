package java.lang;

import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.main.VMHelpers;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * Native methods for java.lang.Class; declared in the same order as in Class.java.
 */
public final class Class$_native<T> {

    private static void registerNatives() {
        // no-op
    }

    private static Class<?> forName0(String name, boolean initialize, ClassLoader loader, Class<?> caller) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    public boolean isInstance(Object obj) {
        return VMHelpers.instanceofClass(obj, (Class<?>)(Object)this);
    }

    public boolean isAssignableFrom(Class<?> cls) {
        return VMHelpers.isTypeIdAssignableTo(CompilerIntrinsics.getTypeIdFromClass(cls),
                CompilerIntrinsics.getDimensionsFromClass(cls),
                CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this),
                CompilerIntrinsics.getDimensionsFromClass((Class<?>)(Object)this));
    }

    public boolean isInterface() {
        return CompilerIntrinsics.isInterface(CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this));
    }

    public boolean isArray() {
        type_id typeId = CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this);
        uint8_t dims = CompilerIntrinsics.getDimensionsFromClass((Class<?>)(Object)this);
        return dims.intValue() > 0 || CompilerIntrinsics.isPrimArray(typeId);
    }

    public boolean isPrimitive() {
        return CompilerIntrinsics.isPrimitive(CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this));
    }

    public Class<? super T> getSuperclass() {
        type_id typeId = CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this);
        if (CompilerIntrinsics.isJavaLangObject(typeId) || CompilerIntrinsics.isPrimitive(typeId) || CompilerIntrinsics.isInterface(typeId)) {
            return null;
        }
        type_id superTypeId = CompilerIntrinsics.getSuperClassTypeId(typeId);
        uint8_t dims = word(0);
        return (Class<? super T>)CompilerIntrinsics.getClassFromTypeId(superTypeId, dims);
    }
}
