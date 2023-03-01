package java.lang;

import static java.lang.Throwable$_patch$Constants.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import org.qbicc.runtime.stackwalk.JavaStackWalker;
import org.qbicc.runtime.stackwalk.StackWalker;

/**
 *
 */
@PatchClass(Throwable.class)
final class Throwable$_patch {
    private transient Object backtrace;
    private transient int depth;

    @Replace
    private Throwable fillInStackTrace(int ignored) {
        if (! Build.Target.isWasm()) {
            StackWalker sw = new StackWalker();
            JavaStackWalker jsw = new JavaStackWalker(true);
            final unw_cursor_t cursor = auto();
            // get a copy of the initial cursor
            sw.getCursor(addr_of(cursor));
            int depth = 0;
            while (jsw.next(sw)) {
                if (++depth == MAX_STACK) {
                    break;
                }
            }
            // now, create the backtrace
            sw.setCursor(addr_of(cursor));
            jsw.reset();

            // todo: configurable max stack depth
            try {
                int[] bt = new int[depth];
                for (int i = 0; i < depth; i ++) {
                    jsw.next(sw);
                    bt[i] = jsw.getSourceIndex();
                }
                this.backtrace = bt;
                this.depth = depth;
                return (Throwable) (Object) this;
            } catch (OutOfMemoryError ignored1) {
            }
        }
        this.backtrace = NO_STACK;
        this.depth = 0;
        return (Throwable) (Object) this;
    }
}

final class Throwable$_patch$Constants {
    static final int[] NO_STACK = new int[0];
    static final int MAX_STACK = 2000;
}
