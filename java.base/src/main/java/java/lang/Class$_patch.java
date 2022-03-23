package java.lang;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import java.lang.reflect.Modifier;

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
        this.nestHost = nestHost == null ? ((Class<T>)(Object)this) : nestHost;
        this.referenceBitMap = referenceBitMap;
        this.modifiers = modifiers;
        this.dimension = zero();
    }

    /**
     * Injected constructor for reference array class objects.
     *
     * @param elementClass the array's element class (must not be {@code null})
     * @param refArrayClass the reference array class (must not be {@code null})
     */
    @SuppressWarnings({ "unchecked", "ConstantConditions" })
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
        this.classData = null;
        this.module = elementClass.module;
        this.instanceSize = refArrayClass.instanceSize;
        this.instanceAlign = refArrayClass.instanceAlign;
        this.dimension = uword(elemDims + 1);
        this.nestHost = (Class<T>)(Object)this;
        this.nestMembers = null;
        this.modifiers = Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.FINAL | refArrayClass.modifiers & 0xffff_0000;
        this.referenceBitMap = refArrayClass.referenceBitMap;
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

    @Replace
    public Class<?> getNestHost() {
        return nestHost;
    }

    // todo: @Replace public Class<?> arrayType() { ... }
    // todo: @Replace public Class<?> componentType() { ... }
    // todo: @Remove private final Class<?> componentType;
}
