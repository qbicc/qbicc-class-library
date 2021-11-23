package java.lang.invoke;

/**
 * Native stubs for {@link MethodHandleNatives}.
 */
class MethodHandleNatives$_native {
    static void init(MemberName self, Object ref) {
        throw new UnsupportedOperationException();
    }

    static void expand(MemberName self) {
        throw new UnsupportedOperationException();
    }

    static MemberName resolve(MemberName self, Class<?> caller, boolean speculativeResolve) throws LinkageError, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    static int getMembers(Class<?> defc, String matchName, String matchSig, int matchFlags, Class<?> caller, int skip, MemberName[] results) {
        throw new UnsupportedOperationException();
    }

    static long objectFieldOffset(MemberName self) {
        throw new UnsupportedOperationException();
    }

    static long staticFieldOffset(MemberName self) {
        throw new UnsupportedOperationException();
    }

    static Object staticFieldBase(MemberName self) {
        throw new UnsupportedOperationException();
    }

    static Object getMemberVMInfo(MemberName self) {
        throw new UnsupportedOperationException();
    }

    static void setCallSiteTargetNormal(CallSite site, MethodHandle target) {
        throw new UnsupportedOperationException();
    }

    static void setCallSiteTargetVolatile(CallSite site, MethodHandle target) {
        throw new UnsupportedOperationException();
    }

    static void copyOutBootstrapArguments(Class<?> caller, int[] indexInfo, int start, int end, Object[] buf, int pos, boolean resolve, Object ifNotAvailable) {
        throw new UnsupportedOperationException();
    }

    static void clearCallSiteContext(MethodHandleNatives.CallSiteContext context) {
        throw new UnsupportedOperationException();
    }

    static void registerNatives() {
        // no op
    }
}
