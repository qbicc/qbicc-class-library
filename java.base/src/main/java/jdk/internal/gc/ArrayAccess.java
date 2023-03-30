package jdk.internal.gc;

import org.qbicc.runtime.patcher.Patch;

@Patch("[")
final class ArrayAccess {
    int length;
}
