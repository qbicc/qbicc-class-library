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
package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.main.VMHelpers;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * Native methods for java.lang.Class; declared in the same order as in Class.java.
 */
@Tracking("src/java.base/share/classes/java/lang/Class.java")
@Tracking("src/java.base/share/native/libjava/Class.c")
@Tracking("src/hotspot/share/classfile/systemDictionary.cpp")
public final class Class$_native<T> {

    private static void registerNatives() {
        // no-op
    }

    // loosely following load_* in systemDictionary.cpp, but simplified because at runtime
    // qbicc's module suystem is alwasys fully booted and we can't load a new non-array class
    private static Class<?> forName0(String name, boolean initialize, ClassLoader loader, Class<?> caller) throws ClassNotFoundException {
        if (name.startsWith("[")) {
            // TODO: Deal with array case
            // Strip down to element type; get the Class object; call getArrayClass() to build back up again.
            throw new ClassNotFoundException("TODO: forName0 on arrays ");
        } else {
            Class<?> cls;
            if (loader == null) {
                cls = VMHelpers.findLoadedClass(name, null);
            } else {
                cls = loader.loadClass(name);
                if (cls != null && !cls.getName().equals(name)) {
                    cls = null; // user-defined classloader misbehaved; override
                }
            }
            if (cls == null) {
                throw new ClassNotFoundException(name);
            }
            return cls;
        }
    }

    public boolean isInstance(Object obj) {
        return VMHelpers.instanceofClass(obj, (Class<?>)(Object)this);
    }

    public boolean isAssignableFrom(Class<?> cls) {
        return VMHelpers.isTypeIdAssignableTo(CompilerIntrinsics.getTypeIdFromClass(cls),
                CompilerIntrinsics.getDimensionsFromClass(cls),
                CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this),
                CompilerIntrinsics.getDimensionsFromClass((Class<?>)(Object)this));
    }

    public boolean isInterface() {
        return CompilerIntrinsics.isInterface(CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this));
    }

    public boolean isArray() {
        type_id typeId = CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this);
        uint8_t dims = CompilerIntrinsics.getDimensionsFromClass((Class<?>)(Object)this);
        return dims.intValue() > 0 || CompilerIntrinsics.isPrimArray(typeId);
    }

    public boolean isPrimitive() {
        return CompilerIntrinsics.isPrimitive(CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this));
    }

    public Class<? super T> getSuperclass() {
        type_id typeId = CompilerIntrinsics.getTypeIdFromClass((Class<?>)(Object)this);
        if (CompilerIntrinsics.isJavaLangObject(typeId) || CompilerIntrinsics.isPrimitive(typeId) || CompilerIntrinsics.isInterface(typeId)) {
            return null;
        }
        type_id superTypeId = CompilerIntrinsics.getSuperClassTypeId(typeId);
        uint8_t dims = word(0);
        return (Class<? super T>)CompilerIntrinsics.getClassFromTypeId(superTypeId, dims);
    }

    public int getModifiers() {
        return ((Class$_patch<T>) (Object) this).modifiers & 0xffff;
    }

    public boolean isHidden() {
        int I_ACC_HIDDEN = 1 << 18; // TODO: get this from a set of constants shared with the runtime
        return (((Class$_patch<T>) (Object) this).modifiers & I_ACC_HIDDEN) == I_ACC_HIDDEN;
    }

    private java.security.ProtectionDomain getProtectionDomain0() {
        return null; // TODO: add instance field and store for real??
    }

    private Class<?>[] getPermittedSubclasses0() {
        // TODO: For now, just return null (means not sealed).
        //       Eventually we need to extend qbicc to extract the information from the classfile.
        return null;
    }

    private Field[] getDeclaredFields0(boolean publicOnly) {
        if (Build.isTarget()) {
            throw new UnsupportedOperationException("Must register "+this+" for runtime reflection at compile time");
        } else {
            throw new IllegalStateException("Class.getDeclaredFields0() should have been intercepted in the interpreter!");
        }
    }

    private Method[] getDeclaredMethods0(boolean publicOnly) {
        if (Build.isTarget()) {
            throw new UnsupportedOperationException("Must register "+this+" for runtime reflection at compile time");
        } else {
            throw new IllegalStateException("Class.getDeclaredMethods0() should have been intercepted in the interpreter!");
        }
    }

    private Constructor<T>[] getDeclaredConstructors0(boolean publicOnly) {
        if (Build.isTarget()) {
            throw new UnsupportedOperationException("Must register "+this+" for runtime reflection at compile time");
        } else {
            throw new IllegalStateException("Class.getDeclaredConstructors0() should have been intercepted in the interpreter!");
        }
    }

    byte[] getRawAnnotations() {
        if (Build.isTarget()) {
            throw new UnsupportedOperationException("Must register "+this+" for runtime reflection at compile time");
        } else {
            throw new IllegalStateException("Class.getRawAnnotations() should have been intercepted in the interpreter!");
        }
    }

    byte[] getRawTypeAnnotations() {
        if (Build.isTarget()) {
            throw new UnsupportedOperationException("Must register "+this+" for runtime reflection at compile time");
        } else {
            throw new IllegalStateException("Class.getRawTypeAnnotations() should have been intercepted in the interpreter!");
        }
    }

    String getGenericSignature0() {
        return ((Class$_patch) (Object) this).genericSignature;
    }
}
