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

package jdk.internal.loader;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;

import sun.net.www.protocol.nativeimage.Handler;

import org.qbicc.runtime.ReflectivelyAccesses;
import org.qbicc.runtime.ReflectivelyAccessedElement;


/**
 * This class provides a mechanism to make selected resources that were available
 * on the build-time classpath also available at runtime.
 *
 * The URLS returned from add resource are of the form: nativeimage:///resource-N
 * where N is the index into the resources for the backing byte[].
 */
 @ReflectivelyAccesses({
    @ReflectivelyAccessedElement(clazz = Handler.class, method = "<init>", params = {}),
    @ReflectivelyAccessedElement(clazz = Handler.class, method = "openConnection", params = {java.net.URL.class})
 })
public class NativeImageResources {
    private static final HashMap<ClassLoader, HashMap<String, URL[]>> mappings = new HashMap<>();
    private static byte[][] resources = new byte[0][0];
    private static final String resourcePrefix = "/resource/";

    // For now, keep this simple and sequential until it proves to be a bottleneck
    static synchronized URL addResource(ClassLoader cl, String resourceName, byte[] resourceBytes) {
        // First de-deuplicate resourceBytes and either reuse an existing entry or create a new one.
        int resourceNumber = -1;
        for (int i=0; i<resources.length; i++) {
            if (Arrays.equals(resources[i], resourceBytes)) {
                resourceNumber = i;
                break;
            }
        }
        if (resourceNumber == -1) {
            resourceNumber = resources.length;
            resources = Arrays.copyOf(resources, resources.length+1);
            resources[resourceNumber] = resourceBytes;
        }

        // Next, make the URL
        try {
            URL url = new URL("nativeimage://qbiccimage" + resourcePrefix + resourceNumber);

            HashMap<String, URL[]> clMap = mappings.get(cl);
            if (clMap == null) {
                clMap = new HashMap<>();
                mappings.put(cl, clMap);
            }
            URL[] current = clMap.get(resourceName);
            if (current == null) {
                clMap.put(resourceName, new URL[]{url});
                return url;
            } else {
                // Deduplicate
                for (int i = 0; i < current.length; i++) {
                    if (current[i].getPath().equals(url.getPath())) {
                        return url;
                    }
                }
                URL[] enhanced = Arrays.copyOf(current, current.length + 1);
                enhanced[current.length] = url;
                clMap.put(resourceName, enhanced);
                return url;
            }
        } catch (MalformedURLException e) {
            throw new InternalError("Failed to create nativeimage URL", e);
        }
    }

    static URL findResource(ClassLoader cl, String resourceName) {
        HashMap<String, URL[]> clMap = mappings.get(cl);
        if (clMap == null) {
            return null;
        }
        URL[] resources = clMap.get(resourceName);
        return resources == null ? null : resources[0];
    }

    static Enumeration<URL> findResources(ClassLoader cl, String resourceName) {
        HashMap<String, URL[]> clMap = mappings.get(cl);
        if (clMap == null) {
            return Collections.emptyEnumeration();
        }
        URL[] resources = clMap.get(resourceName);
        return resources == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(resources));
    }

    public static byte[] getResourceBytes(URL url) {
        if (!url.getProtocol().equals("nativeimage")) {
            return null;
        }
        String path = url.getPath();
        if (!path.startsWith(resourcePrefix)) {
            return null;
        }
        int idx = Integer.parseInt(path.substring(resourcePrefix.length()));
        if (idx < 0 || idx >= resources.length) {
            return null;
        }
        return resources[idx];
    }

}
