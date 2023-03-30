package jdk.internal.gc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(Object.class)
final class ObjectAccess {
    header_type header;
    type_id typeId;
}
