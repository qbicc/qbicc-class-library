package java.lang.invoke;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/**
 *
 */
@PatchClass(InnerClassLambdaMetafactory.class)
class InnerClassLambdaMetafactory$_patch {

    @Replace
    private static String lambdaClassName(Class<?> targetClass) {
        String name = targetClass.getName();
        if (targetClass.isHidden()) {
            // use the original class name
            name = name.replace('/', '_');
        }
        // the class is hidden, so we don't need to put a numerical suffix on it
        return name.replace('.', '/') + "$$Lambda";
    }
}
