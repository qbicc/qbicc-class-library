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
package java.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.qbicc.rt.annotation.Tracking;

@Tracking("src/java.base/share/classes/java/util/ServiceLoader.java")
class QbiccServiceLoaderSupport {
    // Each Class[] has the S's Class as its zeroth element and the remaining elements
    // are Class<? extends S>> instances that were computed at compile time from provider configuration files.
    // This field is initialized by qbicc to a non-trivial value as an ADD post-hook.
    static Class<?>[][] providerConfigurationMapping = new Class<?>[0][0];

    // This code is pulled out to simplify overriding it at build-time in the interpreter
    private static Class<?>[] findProviders(Class<?> service) {
        for (Class[] candidate: providerConfigurationMapping) {
            if (service.equals(candidate[0])) {
                return candidate;
            }
        }
        return null;
    }

    static <S> Iterator<ServiceLoader.Provider<S>> newLookupIterator(Class<S> service) {
        Class[] candidate = findProviders(service);
        if (candidate != null) {
            return new ProviderConfigIterator(service, candidate);
        } else {
            return Collections.emptyIterator();
        }
    }

    static class ProviderConfigIterator<S> implements Iterator<ServiceLoader.Provider<S>> {
        final Class<S> service;
        final Class<?>[] candidates;
        int index;

        public boolean hasNext() {
            return index < candidates.length;
        }

        public ServiceLoader.Provider<S> next() {
            Class<?> clazz = candidates[index++];

            if (service.isAssignableFrom(clazz)) {
                Class<? extends S> type = (Class<? extends S>) clazz;
                try {
                    Constructor<? extends S> ctor = type.getConstructor();
                    ProviderImpl<S> p = new ProviderImpl<S>(service, type, ctor);
                    return p;
                } catch (NoSuchMethodException x) {
                    String cn = clazz.getName();
                    ServiceLoader$_patch.fail(service, cn + " Unable to get public no-arg constructor", x);
                    return null; // unreachable
                }
            } else {
                ServiceLoader$_patch.fail(service, clazz.getName() + " not a subtype");
                return null; // unreachable
            }
        }

        ProviderConfigIterator(Class<S> service, Class<?>[] candidates) {
            this.service = service;
            this.candidates = candidates;
            this.index = 1; // candidates[0] is service
        }
    }

    // Replicate private class of ServiceLoader; simplify by removing SecurityManager checks
    private static class ProviderImpl<S> implements ServiceLoader.Provider<S> {
        final Class<S> service;
        final Class<? extends S> type;
        final Method factoryMethod;  // factory method or null
        final Constructor<? extends S> ctor; // public no-args constructor or null

        ProviderImpl(Class<S> service,
                     Class<? extends S> type,
                     Method factoryMethod) {
            this.service = service;
            this.type = type;
            this.factoryMethod = factoryMethod;
            this.ctor = null;
        }

        ProviderImpl(Class<S> service,
                     Class<? extends S> type,
                     Constructor<? extends S> ctor) {
            this.service = service;
            this.type = type;
            this.factoryMethod = null;
            this.ctor = ctor;
        }

        @Override
        public Class<? extends S> type() {
            return type;
        }

        @Override
        public S get() {
            if (factoryMethod != null) {
                return invokeFactoryMethod();
            } else {
                return newInstance();
            }
        }

        private S invokeFactoryMethod() {
            Object result = null;
            try {
                result = factoryMethod.invoke(null);
            } catch (Throwable x) {
                if (x instanceof InvocationTargetException) {
                    x = x.getCause();
                }
                ServiceLoader$_patch.fail(service, factoryMethod + " failed", x);
            }
            if (result == null) {
                ServiceLoader$_patch.fail(service, factoryMethod + " returned null");
            }
            @SuppressWarnings("unchecked")
            S p = (S) result;
            return p;
        }

        private S newInstance() {
            S p = null;
            try {
                p = ctor.newInstance();
            } catch (Throwable x) {
                if (x instanceof  InvocationTargetException) {
                    x = x.getCause();
                }
                String cn = ctor.getDeclaringClass().getName();
                ServiceLoader$_patch.fail(service, "Provider " + cn + " could not be instantiated", x);
            }
            return p;
        }

        @Override
        public int hashCode() {
            return Objects.hash(service, type);
        }

        @Override
        public boolean equals(Object ob) {
            return ob instanceof @SuppressWarnings("unchecked")ProviderImpl<?> that
                    && this.service == that.service
                    && this.type == that.type;
        }
    }
}
