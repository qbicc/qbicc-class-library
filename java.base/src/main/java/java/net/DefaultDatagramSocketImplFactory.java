/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Properties;
import sun.security.action.GetPropertyAction;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

/**
 * This class defines a factory for creating DatagramSocketImpls. It defaults
 * to creating plain DatagramSocketImpls, but may create other DatagramSocketImpls
 * by setting the impl.prefix system property.
 *
 * @author Chris Hegarty
 */

@Tracking("src/java.base/unix/classes/java/net/DefaultDatagramSocketImplFactory.java")
@Tracking("src/java.base/windows/classes/java/net/DefaultDatagramSocketImplFactory.java")
class DefaultDatagramSocketImplFactory {
    static Class<?> prefixImplClass = null;

    /* True if exclusive binding is on for Windows */
    private static boolean exclusiveBind;

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();

        if (Build.Target.isWindows()) {
            String exclBindProp = props.getProperty("sun.net.useExclusiveBind", "");
            exclusiveBind = (exclBindProp.isEmpty())
                    ? true
                    : Boolean.parseBoolean(exclBindProp);
        }

        String prefix = null;
        try {
            prefix = props.getProperty("impl.prefix");
            if (prefix != null)
                prefixImplClass = Class.forName("java.net."+prefix+"DatagramSocketImpl");
        } catch (Exception e) {
            System.err.println("Can't find class: java.net." +
                    prefix +
                    "DatagramSocketImpl: check impl.prefix property");
        }
    }

    /**
     * Creates a new <code>DatagramSocketImpl</code> instance.
     *
     * @param   isMulticast     true if this impl if for a MutlicastSocket
     * @return  a new instance of a <code>DatagramSocketImpl</code>.
     */
    static DatagramSocketImpl createDatagramSocketImpl(boolean isMulticast /*unused on unix*/)
            throws SocketException {
        if (prefixImplClass != null) {
            try {
                @SuppressWarnings("deprecation")
                DatagramSocketImpl result = (DatagramSocketImpl) prefixImplClass.newInstance();
                return result;
            } catch (Exception e) {
                throw new SocketException("can't instantiate DatagramSocketImpl");
            }
        } else {
            if (Build.Target.isWindows()) {
                // Always use TwoStacksPlainDatagramSocketImpl since we need
                // to support multicasting at DatagramSocket level
                return new TwoStacksPlainDatagramSocketImpl(exclusiveBind && !isMulticast, isMulticast);
            } else {
                return new java.net.PlainDatagramSocketImpl(isMulticast);
            }
        }
    }
}
