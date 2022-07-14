package jdk.internal.ref;

import java.lang.ref.Cleaner;
import java.util.concurrent.ThreadFactory;

import jdk.internal.misc.InnocuousThread;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

/**
 *
 */
@PatchClass(CleanerFactory.class)
@RunTimeAspect
class CleanerFactory$_patch {
    private static final java.lang.ref.Cleaner commonCleaner = Cleaner.create(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return InnocuousThread.newSystemThread("Common-Cleaner", r, Thread.MAX_PRIORITY - 2);
        }
    });
}
