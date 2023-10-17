package java.lang;

import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Long.class)
class Long$_patch {

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int bitCount(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int compare(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int compareUnsigned(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long divideUnsigned(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int hashCode(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long highestOneBit(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long lowestOneBit(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long max(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long min(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int numberOfLeadingZeros(long a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int numberOfTrailingZeros(long a);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native int remainderUnsigned(long a, long b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long reverse(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long reverseBytes(long i);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long rotateLeft(long a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long rotateRight(long a, int b);

    @Annotate
    @SafePoint(SafePointBehavior.ALLOWED)
    static native long signum(long i);
}
