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
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libnet/net_util.c")
@Tracking("src/java.base/unix/native/libnet/net_util_md.c")
@Tracking("src/java.base/unix/native/libnet/SocketImpl.c")
@Tracking("src/java.base/windows/native/libnet/net_util_md.c")
@Tracking("src/java.base/windows/native/libnet/SocketImpl.c")
class NetUtil {
    private static boolean IPV4Available;
    private static boolean IPV4AvailableComputed;

    private static boolean IPV6Available;
    private static boolean IPV6AvailableComputed;

    static boolean reuseport_supported() {
        if (Build.Target.isWindows()) {
            return false;
        } else if (Build.Target.isMacOs() || Build.Target.isLinux()) {
            return true;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static boolean ipv4_available() {
        if (IPV4AvailableComputed) {
            return IPV4Available;
        }

        if (Build.Target.isPosix()) {
            c_int fd = socket(AF_INET, SOCK_STREAM, word(0)) ;
            if (fd.intValue() < 0) {
                IPV4Available = false;
            } else {
                IPV4Available = true;
                close(fd);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        IPV4AvailableComputed = true;
        return IPV4Available;
    }

    static boolean ipv6_available() {
        if (IPV6AvailableComputed) {
            return IPV6Available;
        }

        boolean v6Avail = false;
        // TODO: Here is where we would execute code ported from ipv6_available() in net_util.md
        //       and compute a real platform-dependent value for v6Avail;

        IPV6Available = v6Avail && !Boolean.getBoolean("java.net.preferIPv4Stack");
        IPV6AvailableComputed = true;
        return IPV6Available;
    }


    static boolean setInet6Address_ipaddress(Inet6Address iaObj, char_ptr address) {
        // TODO: port the code below -- needed for IPv6
        /*
        jobject holder;
        jbyteArray addr;

        holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
        CHECK_NULL_RETURN(holder, JNI_FALSE);
        addr = (jbyteArray)(*env)->GetObjectField(env, holder, ia6_ipaddressID);
        if (addr == NULL) {
            addr = (*env)->NewByteArray(env, 16);
            CHECK_NULL_RETURN(addr, JNI_FALSE);
            (*env)->SetObjectField(env, holder, ia6_ipaddressID, addr);
        }
        (*env)->SetByteArrayRegion(env, addr, 0, 16, (jbyte *)address);
        (*env)->DeleteLocalRef(env, addr);
        (*env)->DeleteLocalRef(env, holder);
        return JNI_TRUE;
         */
        return true;
    }

    static boolean setInet6Address_scopeid(Inet6Address iaObj, int scopeid) {
        /*
        // TODO: port the code below -- needed for IPv6


            jobject holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, JNI_FALSE);
    (*env)->SetIntField(env, holder, ia6_scopeidID, scopeid);
    if (scopeid > 0) {
        (*env)->SetBooleanField(env, holder, ia6_scopeidsetID, JNI_TRUE);
    }
    (*env)->DeleteLocalRef(env, holder);
    return JNI_TRUE;
         */
        return true;
    }

    static boolean setInet6Address_scopeifname(Inet6Address iaObj, NetworkInterface scopeifname) {
                /*
        // TODO: port the code below -- needed for IPv6
        jobject holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
        CHECK_NULL_RETURN(holder, JNI_FALSE);
        (*env)->SetObjectField(env, holder, ia6_scopeifnameID, scopeifname);
        (*env)->DeleteLocalRef(env, holder);
        return JNI_TRUE;
                 */
        return true;
    }

}
