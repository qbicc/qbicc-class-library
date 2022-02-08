package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

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
     * The JDK spec says this field is invisible to reflection.
     */
    @Annotate
    @NoReflect
    final ClassLoader classLoader;

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
     * The nest host of this class, or {@code null} if it is its own host.
     */
    @Add
    @NoReflect
    final Class<?> nestHost;

    /**
     * The nest members of this class, or {@code null} if the class is not a nest host or it hosts only itself.
     */
    @Add
    @NoReflect
    final Class<?>[] nestMembers;

    /**
     * Injected constructor for "normal" class objects.
     * @param id the instance ID
     * @param classLoader the class loader (may be {@code null})
     * @param name the friendly class name (must not be {@code null})
     * @param instanceSize the size of an object instance of this (leaf) type
     * @param instanceAlign the alignment of an object instance of this type
     * @param nestHost the nest host of this class, or {@code null} if none
     * @param nestMembers the nest members of this class, or {@code null} if none
     */
    @Add
    private Class$_patch(final type_id id, final ClassLoader classLoader, final String name, final int instanceSize, final byte instanceAlign, final Class<?> nestHost, final Class<?>[] nestMembers) {
        this.classLoader = classLoader;
        this.name = name;
        this.id = id;
        this.instanceSize = instanceSize;
        this.instanceAlign = instanceAlign;
        this.nestHost = nestHost;
        this.nestMembers = nestMembers;
        this.dimension = zero();
    }

    /**
     * Injected constructor for reference array class objects.
     *
     * @param elementClass the array's element class (must not be {@code null})
     * @param refArrayClass the reference array class (must not be {@code null})
     */
    @Add
    private Class$_patch(final Class$_patch<?> elementClass, final Class$_patch<?> refArrayClass) {
        int elemDims = elementClass.dimension.intValue();
        if (elemDims > 0 || CompilerIntrinsics.isPrimArray(elementClass.id)) {
            this.name = '[' + elementClass.name;
        } else {
            this.name = '[' + ('L' + elementClass.name) + ';';
        }
        this.classLoader = elementClass.classLoader;
        this.id = elementClass.id;
        this.instanceSize = refArrayClass.instanceSize;
        this.instanceAlign = refArrayClass.instanceAlign;
        this.dimension = uword(elemDims + 1);
        this.nestHost = null;
        this.nestMembers = null;
    }

    @SuppressWarnings("ConstantConditions")
    @Replace
    public Class<?>[] getNestMembers() {
        if (nestMembers != null) {
            return nestMembers.clone();
        } else {
            return new Class<?>[] { ((Class<?>) (Object) this) };
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Replace
    public Class<?> getNestHost() {
        if (nestHost != null) {
            return nestHost;
        } else {
            return ((Class<?>) (Object) this);
        }
    }
}
