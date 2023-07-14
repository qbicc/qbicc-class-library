package jdk.internal.sys.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;

/**
 *
 */
@include("<string.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class String {
    @name(value = "__xpg_strerror_r", when = Build.Target.IsGLibCLike.class)
    public static native c_int strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);

    public static native size_t strnlen(ptr<@c_const c_char> s, size_t maxlen);

    // GLIBC only
    @Deprecated
    public static native c_int __xpg_strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);
}
