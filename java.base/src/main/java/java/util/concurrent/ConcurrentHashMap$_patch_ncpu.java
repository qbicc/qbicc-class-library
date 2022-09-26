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

package java.util.concurrent;

import jdk.internal.misc.Unsafe;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.patcher.PatchClass;
import org.qbicc.runtime.patcher.Replace;

/*
 * The entire purpose of this patch is to deal with the references to
 * NCPU (which is a runtime initialized field). There are no other
 * real changes, but the diff is fairly large just to get this code to
 * compile in the context of a patch class.
 */
@PatchClass(ConcurrentHashMap.class)
@Tracking("src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java")
public class ConcurrentHashMap$_patch_ncpu<K,V> {
    // alias static fields
    static int MIN_TRANSFER_STRIDE;
    static int RESIZE_STAMP_SHIFT;
    static long SIZECTL;
    static long TRANSFERINDEX;
    static Unsafe U;

    // alias instance fields
    private transient volatile ConcurrentHashMap.Node<K,V>[] nextTable;
    private transient volatile int sizeCtl;
    transient volatile ConcurrentHashMap.Node<K,V>[] table;
    private transient volatile int transferIndex;

    // alias methods
    static native int resizeStamp(int n);
    static native <K,V> ConcurrentHashMap.Node<K,V> tabAt(ConcurrentHashMap.Node<K,V>[] tab, int i);
    static native <K,V> boolean casTabAt(ConcurrentHashMap.Node<K,V>[] tab, int i,
                                         ConcurrentHashMap.Node<K,V> c, ConcurrentHashMap.Node<K,V> v);
    static native <K,V> void setTabAt(ConcurrentHashMap.Node<K,V>[] tab, int i, ConcurrentHashMap.Node<K,V> v);
    static native <K,V> ConcurrentHashMap.Node<K,V> untreeify(ConcurrentHashMap.Node<K,V> b);

    @Replace
    private final void transfer(ConcurrentHashMap.Node<K,V>[] tab, ConcurrentHashMap.Node<K,V>[] nextTab) {
        int NCPU = Build.isHost() ? 1 : ConcurrentHashMap.NCPU;
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap.Node<K,V>[] nt = (ConcurrentHashMap.Node<K,V>[])new ConcurrentHashMap.Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        ConcurrentHashMap.ForwardingNode<K,V> fwd = new ConcurrentHashMap.ForwardingNode<K,V>(nextTab);
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0;;) {
            ConcurrentHashMap.Node<K,V> f; int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                else if (U.compareAndSetInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSetInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            else if ((fh = f.hash) == ConcurrentHashMap.MOVED)
                advance = true; // already processed
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        ConcurrentHashMap.Node<K,V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            ConcurrentHashMap.Node<K,V> lastRun = f;
                            for (ConcurrentHashMap.Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (ConcurrentHashMap.Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new ConcurrentHashMap.Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new ConcurrentHashMap.Node<K,V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        else if (f instanceof ConcurrentHashMap.TreeBin) {
                            ConcurrentHashMap.TreeBin<K,V> t = (ConcurrentHashMap.TreeBin<K,V>)f;
                            ConcurrentHashMap.TreeNode<K,V> lo = null, loTail = null;
                            ConcurrentHashMap.TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (ConcurrentHashMap.Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                ConcurrentHashMap.TreeNode<K,V> p = new ConcurrentHashMap.TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= ConcurrentHashMap.UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new ConcurrentHashMap.TreeBin<K,V>(lo) : t;
                            hn = (hc <= ConcurrentHashMap.UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new ConcurrentHashMap.TreeBin<K,V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        else if (f instanceof ConcurrentHashMap.ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
            }
        }
    }
}
