package java.lang;

import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.main.VMHelpers;

import static org.qbicc.runtime.CNative.*;

public final class Class$_native<T> {
    public boolean isAssignableFrom(Class<?> cls) {
        Object pun = this;
        Class<?> thisAsClass = (Class<?>) pun;
        return VMHelpers.isTypeIdAssignableTo(CompilerIntrinsics.getTypeIdFromClass(cls),
                CompilerIntrinsics.getDimensionsFromClass(cls),
                CompilerIntrinsics.getTypeIdFromClass(thisAsClass),
                CompilerIntrinsics.getDimensionsFromClass(thisAsClass));
    }
}
