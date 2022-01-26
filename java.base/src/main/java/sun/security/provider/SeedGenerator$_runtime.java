package sun.security.provider;

import java.io.IOException;
import sun.security.util.Debug;

import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.RunTimeAspect;

@PatchClass(sun.security.provider.SeedGenerator.class)
@RunTimeAspect
class SeedGenerator$_runtime {
    private static SeedGenerator instance;

    // Static initializer to hook in selected or best performing generator
    static {
        String egdSource = SunEntries.getSeedSource();

        /*
         * Try the URL specifying the source (e.g. file:/dev/random)
         *
         * The URLs "file:/dev/random" or "file:/dev/urandom" are used to
         * indicate the SeedGenerator should use OS support, if available.
         *
         * On Windows, this causes the MS CryptoAPI seeder to be used.
         *
         * On Solaris/Linux/MacOS, this is identical to using
         * URLSeedGenerator to read from /dev/[u]random
         */
        if (egdSource.equals(SunEntries.URL_DEV_RANDOM) ||
                egdSource.equals(SunEntries.URL_DEV_URANDOM)) {
            try {
                instance = new NativeSeedGenerator(egdSource);
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println(
                            "Using operating system seed generator" + egdSource);
                }
            } catch (IOException e) {
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Failed to use operating system seed "
                            + "generator: " + e.toString());
                }
            }
        } else if (!egdSource.isEmpty()) {
            try {
                instance = new SeedGenerator.URLSeedGenerator(egdSource);
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Using URL seed generator reading from "
                            + egdSource);
                }
            } catch (IOException e) {
                if (SeedGenerator$_patch.debug != null) {
                    SeedGenerator$_patch.debug.println("Failed to create seed generator with "
                            + egdSource + ": " + e.toString());
                }
            }
        }

        // Fall back to ThreadedSeedGenerator
        if (instance == null) {
            if (SeedGenerator$_patch.debug != null) {
                SeedGenerator$_patch.debug.println("Using default threaded seed generator");
            }
            instance = (SeedGenerator) (Object) new SeedGenerator$_patch.ThreadedSeedGenerator$_patch();
        }
    }
}
