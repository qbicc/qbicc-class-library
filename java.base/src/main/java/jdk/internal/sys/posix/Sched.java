package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.SafePoint;

/**
 *
 */
@include("<sched.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Sched {
    @SafePoint(setBits = 0)
    public static native c_int sched_yield();
}
