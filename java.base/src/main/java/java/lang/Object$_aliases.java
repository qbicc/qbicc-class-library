package java.lang;

import org.qbicc.runtime.patcher.PatchClass;

/**
 *
 */
@PatchClass(Object.class)
class Object$_aliases {
    native boolean holdsLock();
}
