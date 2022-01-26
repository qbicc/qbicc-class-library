package jdk.internal.ref;

import jdk.internal.misc.InnocuousThread;

import java.lang.ref.Cleaner;
import java.util.concurrent.ThreadFactory;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(CleanerFactory.class)
@RunTimeAspect
public final class CleanerFactory$_runtime {
    private static final Cleaner commonCleaner = Cleaner.create(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return InnocuousThread.newSystemThread("Common-Cleaner",
                    r, Thread.MAX_PRIORITY - 2);
        }
    });
}
