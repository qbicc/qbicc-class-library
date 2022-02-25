package java.lang.invoke;

import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;

/**
 * Patches for method handle objects.
 */
@PatchClass(MethodHandle.class)
abstract class MethodHandle$_patch {

    // alias
    public native MethodType type();

    /**
     * Check an exact call site type at run time.
     *
     * @param callSiteType the call site method type
     * @throws WrongMethodTypeException if the types do not match
     */
    @Add
    @NoReflect
    final void checkType(MethodType callSiteType) {
        Class<?> retType = callSiteType.returnType();
        Class<?>[] argTypes = callSiteType.ptypes();
        MethodType type = type();
        if (retType != void.class && ! retType.isAssignableFrom(type.returnType())) {
            // return type is incompatible
            throw new WrongMethodTypeException();
        }
        // OK so far, the return type is compatible
        int argCnt = argTypes.length;
        int paramCnt = type.parameterCount();
        if (argCnt < paramCnt) {
            // not enough arguments
            throw new WrongMethodTypeException();
        }
        // still OK, now check the parameters (extra arguments are ignored)
        for (int i = 0; i < paramCnt; i ++) {
            Class<?> parameterType = type.parameterType(i);
            Class<?> argType = argTypes[i];
            if (! parameterType.isAssignableFrom(argType)) {
                // we cannot assign the parameter from the argument
                throw new WrongMethodTypeException();
            }
        }
        // all OK
        return;
    }
}
