package jdk.internal.gc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Class.class)
final class ClassAccess {
    long referenceBitMap;
    int modifiers;
    int instanceSize;
    type_id id;
    ClassAccess componentType;
    uint8_t dimension;
}
