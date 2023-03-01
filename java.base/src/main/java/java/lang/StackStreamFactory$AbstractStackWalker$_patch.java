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

import java.lang.invoke.MemberName$_patch;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;
import org.qbicc.runtime.stackwalk.JavaStackWalker;
import org.qbicc.runtime.stackwalk.StackWalker;

import static org.qbicc.runtime.CNative.*;

@Tracking("src/java.base/share/native/libjava/StackStreamFactory.c")
@Tracking("src/java.base/share/classes/java/lang/StackStreamFactory.java")
@PatchClass(java.lang.StackStreamFactory.AbstractStackWalker.class)
abstract class StackStreamFactory$AbstractStackWalker$_patch<R,T> {

    // Alias constants of StackStreamFactory.java
    private static final int DEFAULT_MODE              = 0x0;
    private static final int FILL_CLASS_REFS_ONLY      = 0x2;
    private static final int GET_CALLER_CLASS          = 0x4;
    private static final int SHOW_HIDDEN_FRAMES        = 0x20;  // LambdaForms are hidden by the VM

    // alias
    private native Object doStackWalk(long anchor, int skipFrames, int batchSize,
                                      int bufStartIndex, int bufEndIndex);

    /**
     * Begins stack walking.  This method anchors this frame and invokes
     * AbstractStackWalker::doStackWalk after fetching the first batch of stack frames.
     *
     * @param mode        mode of stack walking
     * @param skipframes  number of frames to be skipped before filling the frame buffer.
     * @param batchSize   the batch size, max. number of elements to be filled in the frame buffers.
     * @param startIndex  start index of the frame buffers to be filled.
     * @param frames      Either a Class<?> array, if mode is {@link #FILL_CLASS_REFS_ONLY}
     *                    or a {@link StackFrameInfo} (or derivative) array otherwise.
     * @return            Result of AbstractStackWalker::doStackWalk
     */
    @Replace
    private R callStackWalk(long mode, int skipframes,
                            int batchSize, int startIndex,
                            T[] frames) {
        boolean skipHidden = (mode & GET_CALLER_CLASS) !=0 || (mode & SHOW_HIDDEN_FRAMES) == 0;
        StackWalker sw = new StackWalker();
        JavaStackWalker jsw = new JavaStackWalker(skipHidden);
        boolean ok = jsw.next(sw); // Start
        ok = ok && jsw.next(sw);   // Skip me.

        for (int i=0; i<skipframes; i++) {
            ok = ok && jsw.next(sw);
        }

        int numFilled = 0;
        if (ok) {
            numFilled = fillFramesInternal(jsw, sw, mode, batchSize, startIndex, frames);
        }

        // TODO: We really need to pass both jsw and sw to make the current qbicc impl
        //       of stackwalking work, but the JDK only gives us one long we can use.
        //       I think the right change is to change JavaStackWalkker to allow the StackWalker
        //       to be embedded in the JSW instead of being passed as a param to next().
        long anchor = refToPtr(sw).longValue();
        return (R)doStackWalk(anchor, skipframes, batchSize, startIndex, startIndex+numFilled);
    }

    /**
     * Fetch the next batch of stack frames.
     *
     * @param mode        mode of stack walking
     * @param anchor
     * @param batchSize   the batch size, max. number of elements to be filled in the frame buffers.
     * @param startIndex  start index of the frame buffers to be filled.
     * @param frames      Either a Class<?> array, if mode is {@link #FILL_CLASS_REFS_ONLY}
     *                    or a {@link StackFrameInfo} (or derivative) array otherwise.
     *
     * @return the end index to the frame buffers
     */
    @Replace
    private int fetchStackFrames(long mode, long anchor,
                                 int batchSize, int startIndex,
                                 T[] frames) {

        // anchor is a ptr to a StackWalker created in callStackWalk

        throw new UnsupportedOperationException("TODO!");
    }

    @Add
    private int fillFramesInternal(JavaStackWalker javaStackWalker, StackWalker sw,
                                   long mode, int batchSize, int startIndex, T[] frames) {
        boolean skipHidden = (mode & GET_CALLER_CLASS) !=0 || (mode & SHOW_HIDDEN_FRAMES) == 0;
        int numFilled = 0;
        while (numFilled < batchSize) {
            if ((mode & FILL_CLASS_REFS_ONLY) != 0) {
                Class<?> clazz = javaStackWalker.getFrameClass();
                frames[startIndex + numFilled] = (T)clazz;
                numFilled += 1;
            } else {
                Class<?> clazz = javaStackWalker.getFrameClass();
                String method = javaStackWalker.getFrameMethodName();
                int bci = javaStackWalker.getFrameBytecodeIndex();
                StackFrameInfo$_patch sfi = (StackFrameInfo$_patch)frames[startIndex + numFilled];
                sfi.bci = bci;
                ((MemberName$_patch)(Object)(sfi.memberName)).setClass(clazz);
                ((MemberName$_patch)(Object)(sfi.memberName)).setName(method);
            }
            boolean ok = javaStackWalker.next(sw);
            if (!ok) {
                break;
            }
        }
        return numFilled;
    }
}
