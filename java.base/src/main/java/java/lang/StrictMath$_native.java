package java.lang;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.llvm.LLVM;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
public class StrictMath$_native {
    public static double sin(double val) {
        if (Build.Target.isLlvm()) {
            return LLVM.sin(val);
        } else {
            return org.qbicc.runtime.stdc.Math.sin(val);
        }
    }

    public static double cos(double val) {
        if (Build.Target.isLlvm()) {
            return LLVM.cos(val);
        } else {
            return org.qbicc.runtime.stdc.Math.cos(val);
        }
    }

    public static double tan(double val) {
        return org.qbicc.runtime.stdc.Math.tan(val);
    }

    public static double asin(double val) {
        return org.qbicc.runtime.stdc.Math.asin(val);
    }

    public static double acos(double val) {
        return org.qbicc.runtime.stdc.Math.acos(val);
    }

    public static double atan(double val) {
        return org.qbicc.runtime.stdc.Math.atan(val);
    }

    public static double log(double val) {
        if (Build.Target.isLlvm()) {
            return LLVM.log(val);
        } else {
            return org.qbicc.runtime.stdc.Math.log(val);
        }
    }

    public static double log10(double val) {
        if (Build.Target.isLlvm()) {
            return LLVM.log10(val);
        } else {
            return org.qbicc.runtime.stdc.Math.log10(val);
        }
    }

    public static double sqrt(double val) {
        return org.qbicc.runtime.stdc.Math.sqrt(val);
    }

    public static double IEEEremainder(double a, double b) {
        return org.qbicc.runtime.stdc.Math.remainder(a, b);
    }

    public static double atan2(double a, double b) {
        return org.qbicc.runtime.stdc.Math.atan2(a, b);
    }

    public static double sinh(double val) {
        return org.qbicc.runtime.stdc.Math.sinh(val);
    }

    public static double cosh(double val) {
        return org.qbicc.runtime.stdc.Math.cosh(val);
    }

    public static double tanh(double val) {
        return org.qbicc.runtime.stdc.Math.tanh(val);
    }

    public static double expm1(double val) {
        return org.qbicc.runtime.stdc.Math.expm1(val);
    }

    public static double log1p(double val) {
        return org.qbicc.runtime.stdc.Math.log1p(val);
    }
}
