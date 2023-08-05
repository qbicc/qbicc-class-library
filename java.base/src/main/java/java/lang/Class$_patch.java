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

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 * Build-time patches for {@link Class}.
 */
@PatchClass(Class.class)
@Tracking("src/java.base/share/classes/java/lang/Class.java")
final class Class$_patch<T> {

    /**
     * The class name (aliased from {@code Class}).
     */
    String name;

    /**
     * Class data object.
     */
    @Replace
    private transient final Object classData;

    /**
     * The module (aliased from {@code Class}).
     */
    private transient final Module module;

    /**
     * The JDK spec says this field is invisible to reflection.
     */
    @Annotate
    @NoReflect
    final ClassLoader classLoader;

    /**
     * ReflectionData for subset of members that have been annotated as available for runtime reflection
     */
    @Add
    @NoReflect
    private final transient Class$ReflectionData$_patch<T> qbiccReflectionData;

    /**
     * The type ID of (leaf) instances of this class.
     */
    @Add
    @NoReflect
    final type_id id;

    /**
     * The number of reference array dimensions to add to the type ID.
     */
    @Add
    @NoReflect
    final uint8_t dimension;

    /**
     * The lazily-populated array class of this class.
     */
    @Add
    @NoReflect
    volatile Class<?> arrayClass;

    /**
     * The size of instances of this class.
     */
    @Add
    @NoReflect
    final int instanceSize;

    /**
     * The minimum alignment of instances of this class.
     */
    @Add
    @NoReflect
    final byte instanceAlign;

    /**
     * The nest host of this class.
     */
    @Add
    @NoReflect
    final Class<?> nestHost;

    /**
     * The loaded nest members of this class, or {@code null} if the class is not a nest host or it hosts only itself.
     */
    @Add
    @NoReflect
    volatile Class<?>[] nestMembers;

    /**
     * The bit map of reference fields on instances of this class. Each bit corresponds to an aligned reference-sized
     * word; a {@code 1} indicates that the slot contains a reference. If a reference field appears beyond the 64th
     * reference-sized slot in instance memory, the class will have the {@code I_ACC_EXTENDED_BITMAP} modifier and this
     * field will hold a pointer to the table whose size (in 32-bit {@code int}s) is large enough to accommodate
     * {@link #instanceSize} bytes worth of reference fields.
     */
    @Add
    @NoReflect
    final long referenceBitMap;

    /**
     * The full set of class modifiers.
     */
    @Add
    @NoReflect
    final int modifiers;

    /**
     * The string to be returned from getGenericSignature0
     */
    @Add
    @NoReflect
    final String genericSignature;

    /**
     * Injected constructor for "normal" class objects.
     *
     * @param id the instance ID
     * @param classLoader the class loader (may be {@code null})
     * @param name the friendly class name (must not be {@code null})
     * @param classData the class data object reference
     * @param module the module (must not be {@code null})
     * @param instanceSize the size of an object instance of this (leaf) type
     * @param instanceAlign the alignment of an object instance of this type
     * @param nestHost the nest host of this class, or {@code null} if none
     * @param referenceBitMap the reference bit map or pointer
     * @param modifiers the class modifiers
     */
    @SuppressWarnings({ "unchecked", "ConstantConditions" })
    @Add
    private Class$_patch(
        final type_id id,
        final ClassLoader classLoader,
        final String name,
        final Object classData,
        final Module module,
        final int instanceSize,
        final byte instanceAlign,
        final Class<?> nestHost,
        final long referenceBitMap,
        final int modifiers
    ) {
        this.classLoader = classLoader;
        this.name = name;
        this.id = id;
        this.classData = classData;
        this.module = module;
        this.instanceSize = instanceSize;
        this.instanceAlign = instanceAlign;
        Class<?> actualNestHost = nestHost == null ? ((Class<T>) (Object) this) : nestHost.getNestHost();
        this.nestHost = actualNestHost;
        if (actualNestHost != (Object)this) {
            ((Class$_patch<?>)(Object)actualNestHost).addNestMember((Class<?>)(Object)this);
        }
        this.referenceBitMap = referenceBitMap;
        this.modifiers = modifiers;
        this.dimension = zero();
        this.qbiccReflectionData = new Class$ReflectionData$_patch<T>();
        this.genericSignature = null;
    }

    @Add
    @Deprecated(forRemoval = true)
    private Class$_patch(final Class$_patch<?> elementClass, final Class$_patch<?> ignored) {
        this(elementClass);
    }

    /**
     * Injected constructor for reference array class objects.
     *
     * @param elementClass the array's element class (must not be {@code null})
     */
    @SuppressWarnings({ "unchecked", "ConstantConditions", "CopyConstructorMissesField" })
    @Add
    private Class$_patch(final Class$_patch<?> elementClass) {
        int elemDims = elementClass.dimension.intValue();
        if (elemDims > 0 || CompilerIntrinsics.isPrimArray(elementClass.id)) {
            this.name = ('[' + elementClass.name).intern();
        } else {
            this.name = ('[' + ('L' + elementClass.name) + ';').intern();
        }
        final Class$_patch<?> refArrayClass = (Class$_patch<?>) (Object) CompilerIntrinsics.getClassFromTypeIdSimple(CompilerIntrinsics.getReferenceArrayTypeId());
        this.classLoader = elementClass.classLoader;
        this.id = elementClass.id;
        this.classData = null;
        this.module = elementClass.module;
        this.instanceSize = refArrayClass.instanceSize;
        this.instanceAlign = refArrayClass.instanceAlign;
        this.dimension = uword(elemDims + 1);
        this.nestHost = (Class<T>)(Object)this;
        this.nestMembers = null;
        this.modifiers = Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.FINAL | refArrayClass.modifiers & 0xffff_0000;
        this.referenceBitMap = refArrayClass.referenceBitMap;
        this.qbiccReflectionData = new Class$ReflectionData$_patch<T>();
        this.genericSignature = null;
    }

    @SuppressWarnings("ConstantConditions")
    @Replace
    public Class<?>[] getNestMembers() {
        if (nestMembers != null) {
            return nestMembers.clone();
        } else if (nestHost != (Object) this) {
            return nestHost.getNestMembers();
        } else {
            return new Class<?>[] { ((Class<?>) (Object) this) };
        }
    }

    void addNestMember(Class<?> member) {
        Class<?>[] oldVal = this.nestMembers;
        Class<?>[] newVal, witness;
        for (;;) {
            if (oldVal == null) {
                newVal = new Class<?>[] { (Class<?>)(Object)this, member };
            } else {
                int oldLen = oldVal.length;
                newVal = Arrays.copyOf(oldVal, oldLen + 1);
                newVal[oldLen] = member;
            }
            witness = compareAndSwapNestMembers(oldVal, newVal);
            if (witness == oldVal) {
                return;
            }
            oldVal = witness;
        }
    }

    private Class<?>[] compareAndSwapNestMembers(Class<?>[] expect, Class<?>[] update) {
        return addr_of(refToPtr(this).sel().nestMembers).compareAndSwap(expect, update);
    }

    @Replace
    public Class<?> getNestHost() {
        return nestHost;
    }

    @Replace
    private Class$ReflectionData$_patch<T> reflectionData() {
        return qbiccReflectionData;
    }

    @Replace
    public boolean isPrimitive() {
        return (modifiers & (1 << 17)) != 0;
    }

    // todo: @Replace public Class<?> arrayType() { ... }
    // todo: @Replace public Class<?> componentType() { ... }
    // todo: @Remove private final Class<?> componentType;
    // todo: @Remove private final SoftReference<ReflectionData<T>> reflectionData
}
