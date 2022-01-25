package jdk.internal.ref;

import jdk.internal.misc.InnocuousThread;

import java.lang.ref.Cleaner;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.ReplaceInit;

@PatchClass(CleanerFactory.class)
@ReplaceInit
public final class CleanerFactory$_patch {

    // Disable at buildtime; moved to <rtinit>
    private static final Cleaner commonCleaner = null;
}
