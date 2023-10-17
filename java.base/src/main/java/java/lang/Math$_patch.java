package java.lang;

import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Math.class)
class Math$_patch {

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native double abs(double a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native float abs(float a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int abs(int a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long abs(long a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native double max(double a, double b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native float max(float a, float b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int max(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long max(long a, long b);
}
