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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native bindings for {@link String}.
 */
public final class String$_native {
    private static final ReferenceQueue<String> REF_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Key, Ref> INTERNED = new ConcurrentHashMap<>();

    public String intern() {
        String self = (String) (Object) this;
        byte coder = self.coder();
        byte[] bytes = self.value();
        int hc = self.hashCode();
        Key key = new Key(bytes, coder, hc);
        for (;;) {
            Ref strRef = INTERNED.get(key);
            String existing = strRef.get();
            if (existing != null) {
                cleanQueue();
                return existing;
            }
            if (INTERNED.replace(key, strRef, new Ref(key, self, REF_QUEUE))) {
                cleanQueue();
                return self;
            }
        }
    }

    private void cleanQueue() {
        Ref item;
        while ((item = (Ref) REF_QUEUE.poll()) != null) {
            INTERNED.remove(item.key, item);
        }
    }

    static final class Ref extends WeakReference<String> {
        final Key key;

        Ref(final Key key, final String referent, final ReferenceQueue<? super String> q) {
            super(referent, q);
            this.key = key;
        }
    }

    static final class Key {
        private final byte[] bytes;
        private final byte coder;
        private final int hashCode;

        Key(final byte[] bytes, final byte coder, final int hashCode) {
            this.bytes = bytes;
            this.coder = coder;
            this.hashCode = hashCode;
        }

        public boolean equals(final Object other) {
            return other instanceof Key key && equals(key);
        }

        boolean equals(final Key other) {
            return this == other || other != null && hashCode == other.hashCode && coder == other.coder && Arrays.equals(bytes, other.bytes);
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
