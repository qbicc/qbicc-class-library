package jdk.internal.gc;

import org.qbicc.runtime.CNative.reference;
import org.qbicc.runtime.patcher.Patch;

@Patch("[L")
final class ObjectArrayAccess {
    reference<?>[] content;
}
