/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * ------
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */

package java.lang;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.invoke.StringConcatFactory;
import java.util.Map;
import java.util.Properties;

import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.module.ModuleBootstrap;
import jdk.internal.util.StaticProperty;
import jdk.internal.util.SystemProps;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Annotate;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import org.qbicc.runtime.SerializeAsZero;

@PatchClass(System.class)
@Tracking("src/java.base/share/classes/java/lang/System.java")
public final class System$_patch {
    // Alias
    private static int allowSecurityManager;
    // Alias
    static ModuleLayer bootLayer;
    // Alias
    private static String lineSeparator;
    // Alias
    private static Properties props;

    @Annotate
    @SerializeAsZero
    public static InputStream in;
    @Annotate
    @SerializeAsZero
    public static PrintStream err;
    @Annotate
    @SerializeAsZero
    public static PrintStream out;

    // Alias
    private static native Properties createProperties(Map<String, String> initialProps);
    // Alias
    private static native void logInitException(boolean printToStderr, boolean printStackTrace, String msg, Throwable e);
    // Alias
    static native PrintStream newPrintStream(FileOutputStream fos, String enc);
    // Alias
    static native void setIn0(InputStream in);
    // Alias
    static native void setOut0(PrintStream out);
    // Alias
    static native void setErr0(PrintStream err);
    // Alias
    private static native void setJavaLangAccess();



    @Replace
    public static SecurityManager getSecurityManager() {
        return null;
    }

    @Replace
    public static void loadLibrary(String libname) {
        switch (libname) {
            case "extnet":
            case "management":
            case "net":
            case "nio":
            case "prefs":
            case "zip":
            case "jimage":
                return;
            default:
                throw new UnsatisfiedLinkError("Can't load " + libname);
        }
    }


    /**********************
     * JDK Initialization
     **********************/

    @Replace
    private static void initPhase1() {
        // register the shared secrets - do this first, since SystemProps.initProperties
        // might initialize CharsetDecoders that rely on it
        setJavaLangAccess();

        // VM might invoke JNU_NewStringPlatform() to set those encoding
        // sensitive properties (user.home, user.name, boot.class.path, etc.)
        // during "props" initialization.
        // The charset is initialized in System.c and does not depend on the Properties.
        Map<String, String> tempProps = SystemProps.initProperties();
        VersionProps.init(tempProps);

        // There are certain system configurations that may be controlled by
        // VM options such as the maximum amount of direct memory and
        // Integer cache size used to support the object identity semantics
        // of autoboxing.  Typically, the library will obtain these values
        // from the properties set by the VM.  If the properties are for
        // internal implementation use only, these properties should be
        // masked from the system properties.
        //
        // Save a private copy of the system properties object that
        // can only be accessed by the internal implementation.
        VM.saveProperties(tempProps);
        props = createProperties(tempProps);

        StaticProperty.javaHome();          // Load StaticProperty to cache the property values

        lineSeparator = props.getProperty("line.separator");

        /* BEGIN replicated in rtinitPhase1 */
        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
        setIn0(new BufferedInputStream(fdIn));
        // sun.stdout/err.encoding are set when the VM is associated with the terminal,
        // thus they are equivalent to Console.charset(), otherwise the encoding
        // defaults to Charset.defaultCharset()
        setOut0(newPrintStream(fdOut, props.getProperty("sun.stdout.encoding")));
        setErr0(newPrintStream(fdErr, props.getProperty("sun.stderr.encoding")));
        /* END replicated in rtinitPhase1 */

        /*
         * MOVED TO rtinitPhase1
        // Setup Java signal handlers for HUP, TERM, and INT (where available).
        Terminator.setup();
        */

        /*
         * MOVED to  rtinitPhase1
        // Initialize any miscellaneous operating system settings that need to be
        // set for the class libraries. Currently this is no-op everywhere except
        // for Windows where the process-wide error mode is set before the java.io
        // classes are used.
        VM.initializeOSEnvironment();
        */

        /*
         * MOVED to  rtinitPhase1
        // The main thread is not added to its thread group in the same
        // way as other threads; we must do it ourselves here.
        Thread current = Thread.currentThread();
        current.getThreadGroup().add(current);
        */

        // Subsystems that are invoked during initialization can invoke
        // VM.isBooted() in order to avoid doing things that should
        // wait until the VM is fully initialized. The initialization level
        // is incremented from 0 to 1 here to indicate the first phase of
        // initialization has completed.
        // IMPORTANT: Ensure that this remains the last initialization action!
        VM.initLevel(1);
    }

    @Replace
    private static int initPhase2(boolean printToStderr, boolean printStackTrace) {
        // For qbicc, we get better error reporting if we simply allow any exception
        // raised by ModuleBootstrap.boot() to propagate back to VmImpl.initialize()
        // and be caught and reported there.
        bootLayer = ModuleBootstrap.boot();

        // module system initialized
        VM.initLevel(2);

        return 0; // JNI_OK
    }

    @Replace
    private static void initPhase3() {
        // Initialize the StringConcatFactory eagerly to avoid potential
        // bootstrap circularity issues that could be caused by a custom
        // SecurityManager
        Unsafe.getUnsafe().ensureClassInitialized(StringConcatFactory.class);

        // Simplify by unconditionally removing deprecated security manager support for qbicc
        allowSecurityManager = 1 /* System.NEVER */;

        // initializing the system class loader
        VM.initLevel(3);

        // system class loader initialized
        ClassLoader scl = ClassLoader.initSystemClassLoader();

        // set TCCL
        Thread.currentThread().setContextClassLoader(scl);

        // system is fully initialized
        VM.initLevel(4);
    }


    // The portions of System.initPhase1 that need to be (re-)executed at runtime
    @Add
    public static void rtinitPhase1() {
        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
        setIn0(new BufferedInputStream(fdIn));
        // sun.stdout/err.encoding are set when the VM is associated with the terminal,
        // thus they are equivalent to Console.charset(), otherwise the encoding
        // defaults to Charset.defaultCharset()
        setOut0(newPrintStream(fdOut, props.getProperty("sun.stdout.encoding")));
        setErr0(newPrintStream(fdErr, props.getProperty("sun.stderr.encoding")));

        // Setup Java signal handlers for HUP, TERM, and INT (where available).
        Terminator.setup();

        // Initialize any miscellaneous operating system settings that need to be
        // set for the class libraries. Currently this is no-op everywhere except
        // for Windows where the process-wide error mode is set before the java.io
        // classes are used.
        VM.initializeOSEnvironment();

        // The main thread is not added to its thread group in the same
        // way as other threads; we must do it ourselves here.
        Thread current = Thread.currentThread();
        current.getThreadGroup().add(current);
    }

    // The portions of System.initPhase2 that need to be  (re-)executed at runtime
    @Add
    public static void rtinitPhase2() {
    }

    // The portions of System.initPhase3 that need to be  (re-)executed at runtime
    @Add
    public static void rtinitPhase3() {
        /*
        TODO (?)
        // set TCCL
        Thread.currentThread().setContextClassLoader(scl);
        */
    }
}
