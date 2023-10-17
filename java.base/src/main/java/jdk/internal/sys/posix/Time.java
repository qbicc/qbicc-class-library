package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Time.*;

import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

/**
 *
 */
@include("<time.h>")
public final class Time {

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int clock_gettime(clockid_t clockid, ptr<struct_timespec> tp);

    public static final class clockid_t extends object {}

    public static final clockid_t CLOCK_REALTIME = constant();
    public static final clockid_t CLOCK_MONOTONIC = constant();

    @extern
    public static c_long timezone;

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<struct_tm> localtime_r(ptr<@c_const time_t> timePtr, ptr<struct_tm> result);
}
