package java.lang.ref;

import jdk.internal.access.JavaLangRefAccess;
import jdk.internal.access.SharedSecrets;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(Reference.class)
@ReplaceInit
public abstract class Reference$_patch<T> {
    // Alias & preserve original <clinit>
    private static final Object processPendingLock = new Object();
    // Alias & preserve orginal <clinit>
    private static boolean processPendingActive = false;

    // Alias
    private static native boolean waitForReferenceProcessing() throws InterruptedException;

    static {
        /*
         * EXCLUDE from build-time <clinit> (eventually need to do at runtime).
         *
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent());
        Thread handler = new ReferenceHandler(tg, "Reference Handler");
        /* If there were a special system-only priority greater than
         * MAX_PRIORITY, it would be used here
         * /
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);
        handler.start();
        */

        // provide access in SharedSecrets
        SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
            @Override
            public boolean waitForReferenceProcessing()
                    throws InterruptedException
            {
                return Reference$_patch.waitForReferenceProcessing();
            }

            @Override
            public void runFinalization() {
                Finalizer.runFinalization();
            }
        });
    }
}
