package java.lang;

import java.util.Collections;
import java.util.Map;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

/**
 *
 */
@PatchClass(ProcessEnvironment.class)
@RunTimeAspect
final class ProcessEnvironment$_runtime {
    static final ProcessEnvironment.EnvironmentMap theEnvironment;
    static final Map<String, String> theUnmodifiableEnvironment;

    static {
        ProcessEnvironment.EnvironmentMap env = new ProcessEnvironment.EnvironmentMap();
        if (Build.Target.isWindows()) {
            ProcessEnvironment.parseWindowsEnvBlock(ProcessEnvironment.environmentBlock(), env);
        } else if (Build.Target.isPosix()) {
            ProcessEnvironment.getPosixEnv(env);
        } else {
            throw new UnsupportedOperationException();
        }
        theEnvironment = env;
        theUnmodifiableEnvironment = Collections.unmodifiableMap(env);
    }
}
