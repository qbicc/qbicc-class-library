package java.lang;

import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Integer.class)
class Integer$_patch {

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int bitCount(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int compare(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int compareUnsigned(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int divideUnsigned(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int hashCode(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int highestOneBit(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int lowestOneBit(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int max(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int min(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int numberOfLeadingZeros(int a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int numberOfTrailingZeros(int a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int remainderUnsigned(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int reverse(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int reverseBytes(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int rotateLeft(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int rotateRight(int a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int signum(int i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long toUnsignedLong(int a);
}
