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

package java.net;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.Patch;
import org.qbicc.runtime.patcher.PatchClass;

@Tracking("java.base/classes/share/java/net/Inet6Address.java")
@Tracking("src/java.base/share/native/libnet/net_util.c")
@PatchClass(Inet6Address.class)
public final class Inet6Address$_patch {

    // alias
    Inet6Address$Inet6AddressHolder$_patch holder6;

    // NET_setInet6Address_ipaddress does approximately this via JNI
    @Add
    void setInet6Address_ipaddress(ptr<uint8_t> address) {
        for (int i = 0; i < 15; i++) {
            holder6.ipaddress[i] = address.get(i).byteValue();
        }
    }

    // NET_setInet6Address_scopeid does approximately this via JNI
    @Add
    void setInet6Address_scopeid(int scopeid) {
        holder6.scope_id = scopeid;
        if (scopeid > 0) {
            holder6.scope_id_set = true;
        }
    }

    // NET_setInet6Address_scope_ifname does approximately this via JNI
    @Add
    void setInet6Address_scope_ifname(NetworkInterface scope_ifname) {
        holder6.scope_ifname = scope_ifname;
    }
}
