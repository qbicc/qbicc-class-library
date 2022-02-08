/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * ------
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */
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
