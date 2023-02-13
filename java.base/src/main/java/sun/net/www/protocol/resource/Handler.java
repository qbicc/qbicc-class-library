/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 1994, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.resource;

import java.net.URLConnection;
import java.net.URL;
import java.net.Proxy;
import java.net.URLStreamHandler;
import java.io.InputStream;
import java.io.IOException;

import jdk.internal.loader.NativeImageResources;

/**
 * Open a resource input stream for a URL
 */
public class Handler extends URLStreamHandler {

    public synchronized URLConnection openConnection(URL u) throws IOException {
        return openConnection(u, null);
    }

    public synchronized URLConnection openConnection(URL u, Proxy p) throws IOException {
        byte[] backingBytes = NativeImageResources.getResourceBytes(u);
        if (backingBytes == null) {
            throw new IOException("Unable to connect to: " + u.toExternalForm());
        }
        return new ResourceURLConnection(u, backingBytes);
    }
}
