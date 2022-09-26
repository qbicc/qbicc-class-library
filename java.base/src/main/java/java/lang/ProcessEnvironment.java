/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.Math.min;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Unistd.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.SerializeAsZero;

@Tracking("src/java.base/unix/classes/java/lang/ProcessEnvironment.java")
@Tracking("src/java.base/unix/native/libjava/ProcessEnvironment_md.c")
@Tracking("src/java.base/windows/classes/java/lang/ProcessEnvironment.java")
@Tracking("src/java.base/windows/native/libjava/ProcessEnvironment_md.c")
final class ProcessEnvironment {
    private ProcessEnvironment() {}

    @SerializeAsZero
    private static final EnvironmentMap theBuildtimeEnvironment;

    static {
        assert Build.isHost();
        String[] hostEnvironment = getHostEnvironment();
        EnvironmentMap env = new EnvironmentMap();
        for (int i = 0; i < hostEnvironment.length; i += 2) {
            String key = hostEnvironment[i];
            String value = hostEnvironment[i + 1];
            if (isValidEnvString(key) && isValidEnvString(value)) {
                env.put(key, value);
            }
        }
        theBuildtimeEnvironment = env;
    }

    private static Key makeKey(final String str) {
        // this is the only place where key subclasses are constructed
        // thus key instance methods should always be devirtualized completely
        if (str == null) {
            return null;
        } else if (Build.Target.isWindows()) {
            return new WindowsKey(str);
        } else {
            return new PosixKey(str);
        }
    }

    // API used by other components

    /* ProcessBuilder.environment(java.lang.String[]) */
    static final int MIN_NAME_LENGTH = Build.Target.isWindows() ? 1 : 0;

    /* System.getenv(String) */
    static String getenv(String name) {
        if (Build.isHost()) {
            return theBuildtimeEnvironment.get(name);
        } else {
            return ProcessEnvironment$_runtime.theEnvironment.get(name);
        }
    }

    /* System.getenv() */
    static Map<String,String> getenv() {
        // Do not want build-time environment to escape into the initial heap as a Map, so only support this API at runtime.
        return ProcessEnvironment$_runtime.theUnmodifiableEnvironment;
    }

    /* ProcessBuilder.environment() */
    static Map<String, String> environment() {
        // Do not want build-time environment to escape into the initial heap as a Map, so only support this API at runtime.
        return ProcessEnvironment$_runtime.theEnvironment.clone();
    }

    /* Runtime.exec(*) / ProcessBuilder */
    static Map<String,String> emptyEnvironment(int capacity) {
        return new EnvironmentMap(capacity);
    }

    // ProcessImpl.start() (Windows)
    static String toEnvironmentBlock(Map<String,String> map) {
        if (! Build.Target.isWindows()) {
            throw new UnsupportedOperationException();
        } else {
            return makeWindowsEnvironmentBlock(map);
        }
    }

    // ProcessImpl.start() (UNIX)
    static byte[] toEnvironmentBlock(Map<String,String> map, int[] envc) {
        if (Build.Target.isWindows()) {
            throw new UnsupportedOperationException();
        } else {
            return makeUnixEnvironmentBlock(map, envc);
        }
    }

    // Shared

    static boolean isValidEnvString(final String string) {
        return string.indexOf('=') == -1 && string.indexOf('\0') == -1;
    }

    static String validateEnvString(final String string) {
        if (! isValidEnvString(string)) {
            throw new IllegalArgumentException("Invalid environment string");
        }
        return string;
    }

    abstract static class Key implements Comparable<Key> {
        private final String string;
        private int hashCode;

        Key(final String string) {
            validateEnvString(string);
            this.string = string;
        }

        public final boolean equals(final Object other) {
            return other instanceof Key key && equals(key);
        }

        public abstract int compareTo(final Key o);

        abstract boolean equals(final Key other);

        public final int hashCode() {
            int hashCode = this.hashCode;
            if (hashCode == 0) {
                hashCode = computeHashCode();
                if (hashCode == 0) {
                    hashCode |= 1 << 31;
                }
                this.hashCode = hashCode;
            }
            return hashCode;
        }

        abstract int computeHashCode();

        public final String toString() {
            return string;
        }
    }

    /**
     * A read-only map view of an environment, using the target platform's sorting rules.
     */
    static final class EnvironmentMap extends AbstractMap<String, String> {
        private final Map<Key, String> env;
        private EntrySet entrySet;
        private KeySet keySet;

        EnvironmentMap(final Map<Key, String> env) {
            this.env = env;
        }

        EnvironmentMap(final int capacity) {
            this(new HashMap<>(capacity));
        }

        EnvironmentMap() {
            this(new HashMap<>());
        }

        public Set<Entry<String, String>> entrySet() {
            EntrySet entrySet = this.entrySet;
            if (entrySet == null) {
                this.entrySet = entrySet = new EntrySet();
            }
            return entrySet;
        }

        public Set<String> keySet() {
            KeySet keySet = this.keySet;
            if (keySet == null) {
                this.keySet = keySet = new KeySet();
            }
            return keySet;
        }

        public Collection<String> values() {
            return env.values();
        }

        public boolean containsKey(final Object key) {
            Objects.requireNonNull(key, "key");
            return key instanceof String keyStr && env.containsKey(makeKey(keyStr));
        }

        public boolean containsValue(final Object value) {
            Objects.requireNonNull(value, "value");
            return env.containsValue(value);
        }

        public String get(final Object key) {
            Objects.requireNonNull(key, "key");
            return key instanceof String keyStr ? env.get(makeKey(keyStr)) : null;
        }

        public String getOrDefault(final Object key, final String defaultValue) {
            Objects.requireNonNull(key, "key");
            return key instanceof String keyStr ? Objects.requireNonNullElse(env.get(makeKey(keyStr)), defaultValue) : defaultValue;
        }

        public String put(final String key, final String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            return env.put(makeKey(validateEnvString(key)), validateEnvString(value));
        }

        public String putIfAbsent(final String key, final String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            return env.putIfAbsent(makeKey(validateEnvString(key)), validateEnvString(value));
        }

        public String remove(final Object key) {
            Objects.requireNonNull(key, "key");
            return key instanceof String keyStr ? env.remove(makeKey(keyStr)) : null;
        }

        public boolean remove(final Object key, final Object value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            return key instanceof String keyStr && env.remove(makeKey(keyStr), value);
        }

        public String replace(final String key, final String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            return env.replace(makeKey(key), validateEnvString(value));
        }

        public boolean replace(final String key, final String oldValue, final String newValue) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(oldValue, "oldValue");
            Objects.requireNonNull(newValue, "newValue");
            return env.replace(makeKey(key), oldValue, validateEnvString(newValue));
        }

        public void clear() {
            env.clear();
        }

        public EnvironmentMap clone() {
            return new EnvironmentMap(new HashMap<>(env));
        }

        public int size() {
            return env.size();
        }

        final class EntrySet extends AbstractSet<Map.Entry<String, String>> {
            public Iterator<Entry<String, String>> iterator() {
                Iterator<Entry<Key, String>> iterator = env.entrySet().iterator();
                return new Iterator<Entry<String, String>>() {
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Entry<String, String> next() {
                        Entry<Key, String> next = iterator.next();
                        return Map.entry(next.getKey().toString(), next.getValue());
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            public boolean contains(final Object o) {
                return o instanceof Map.Entry me
                    && me.getKey() instanceof String key
                    && me.getValue() instanceof String value
                    && env.entrySet().contains(Map.entry(makeKey(key), value));
            }

            public boolean remove(final Object o) {
                return o instanceof Map.Entry me
                    && me.getKey() instanceof String key
                    && me.getValue() instanceof String value
                    && env.remove(makeKey(key), value);
            }

            public int size() {
                return env.size();
            }
        }

        final class KeySet extends AbstractSet<String> {
            public Iterator<String> iterator() {
                Iterator<Key> iterator = env.keySet().iterator();
                return new Iterator<String>() {
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public String next() {
                        return iterator.next().toString();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            public int size() {
                return env.size();
            }

            public boolean contains(final Object o) {
                return o instanceof String str && env.containsKey(makeKey(str));
            }

            public boolean remove(final Object o) {
                return o instanceof String str && env.remove(makeKey(str)) != null;
            }
        }
    }

    // OS-dependent: Host

    static native String[] getHostEnvironment();

    // OS-dependent: UNIX

    static final class PosixKey extends Key {
        PosixKey(final String string) {
            super(string);
        }

        public int compareTo(final Key o) {
            return toString().compareTo(o.toString());
        }

        boolean equals(final Key other) {
            return other instanceof PosixKey pk && toString().equals(pk.toString());
        }

        int computeHashCode() {
            return toString().hashCode();
        }
    }

    static byte[] makeUnixEnvironmentBlock(final Map<String, String> map, final int[] envCnt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter w = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        int cnt = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) try {
            String key = entry.getKey();
            String value = entry.getValue();
            if (! isValidEnvString(key) || ! isValidEnvString(value)) {
                // skip the key for safety
                continue;
            }
            cnt++;
            w.write(key);
            w.write('\0');
            w.write(value);
            w.write('\0');
            w.flush();
        } catch (IOException e) {
            // impossible
            throw new IllegalStateException();
        }
        envCnt[0] = cnt;
        return baos.toByteArray();
    }

    static void getPosixEnv(final Map<String, String> env) {
        char_ptr_ptr env_ptr = environ;
        StringBuilder b = new StringBuilder();
        char_ptr entry_ptr;
        for (;;) {
            entry_ptr = env_ptr.loadUnshared();
            env_ptr = env_ptr.plus(1);
            String key = makeString(b, entry_ptr);
            if (key == null) {
                return;
            }
            entry_ptr = env_ptr.loadUnshared();
            env_ptr = env_ptr.plus(1);
            String value = makeString(b, entry_ptr);
            if (value == null) {
                return;
            }
            env.put(key, value);
        }
    }

    /**
     * Make a string from a utf8z pointer.
     *
     * @param sb the string builder to (re)use
     * @param ptr the character pointer
     * @return the string, or {@code null} if {@code ptr} is {@code null}
     */
    private static String makeString(StringBuilder sb, char_ptr ptr) {
        if (ptr.isNull()) {
            return null;
        }
        sb.setLength(0);
        for (;;) {
            int a = ptr.loadUnshared().byteValue() & 0xff;
            ptr = ptr.plus(1);
            if (a == 0) {
                // end of string
                return sb.toString();
            }
            if (a < 0x80) {
                sb.appendCodePoint(a);
            } else if (a < 0xC0 || a >= 0xF8) {
                sb.append('�');
            } else {
                // at least two bytes
                int b = ptr.loadUnshared().byteValue() & 0xff;
                ptr = ptr.plus(1);
                if (b == 0) {
                    // end of string after partial
                    sb.append('�');
                    return sb.toString();
                }
                if (b < 0x80 || b >= 0xC0) {
                    sb.append('�').append('�');
                } else if (a < 0xE0) {
                    sb.appendCodePoint((a & 0b11111) << 6 | b & 0b111111);
                } else {
                    // at least three bytes
                    int c = ptr.loadUnshared().byteValue() & 0xff;
                    ptr = ptr.plus(1);
                    if (c == 0) {
                        // end of string after partial
                        sb.append('�').append('�');
                        return sb.toString();
                    }
                    if (c < 0x80 || c >= 0xC0) {
                        sb.append('�').append('�').append('�');
                    } else if (a < 0xF0) {
                        sb.appendCodePoint((a & 0b1111) << 12 | (b & 0b111111) << 6 | c & 0b111111);
                    } else {
                        // at least four bytes
                        int d = ptr.loadUnshared().byteValue() & 0xff;
                        ptr = ptr.plus(1);
                        if (d == 0) {
                            // end of string after partial
                            sb.append('�').append('�').append('�');
                            return sb.toString();
                        }
                        if (d < 0x80 || d >= 0xC0) {
                            sb.append('�').append('�').append('�').append('�');
                        } else {
                            // a < 0xF8
                            sb.appendCodePoint((a & 0b111) << 18 | (b & 0b111111) << 12 | (c & 0b111111) << 6 | d & 0b111111);
                        }
                    }
                }
            }
        }
    }

    // OS-dependent: Windows

    static final class WindowsKey extends Key {
        WindowsKey(final String string) {
            super(string);
        }

        public int compareTo(final Key other) {
            WindowsKey key = (WindowsKey) other;
            String a = toString();
            String b = key.toString();
            int lenA = a.length();
            int lenB = b.length();
            int minLen = min(lenA, lenB);
            int cmp;
            for (int i = 0; i < minLen; i ++) {
                char ac = a.charAt(i);
                char bc = b.charAt(i);
                if (ac != bc) {
                    cmp = Character.compare(Character.toUpperCase(ac), Character.toUpperCase(bc));
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            }
            return Integer.compare(lenA, lenB);
        }

        boolean equals(final Key other) {
            WindowsKey key = (WindowsKey) other;
            String a = toString();
            String b = key.toString();
            int len = a.length();
            if (len != b.length()) {
                return false;
            }
            for (int i = 0; i < len; i ++) {
                char ac = a.charAt(i);
                char bc = b.charAt(i);
                if (ac != bc && Character.toUpperCase(ac) != Character.toUpperCase(bc)) {
                    return false;
                }
            }
            return true;
        }

        int computeHashCode() {
            int hc = 0;
            String str = toString();
            int len = str.length();
            for (int i = 0; i < len; i ++) {
                hc = 31 * hc + Character.toUpperCase(str.charAt(i));
            }
            return hc;
        }
    }

    static native String environmentBlock();

    static int compareWindowsStyle(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        int minLen = min(lenA, lenB);
        int cmp;
        for (int i = 0; i < minLen; i ++) {
            char ac = a.charAt(i);
            char bc = b.charAt(i);
            if (ac != bc) {
                cmp = Character.compare(Character.toUpperCase(ac), Character.toUpperCase(bc));
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        return Integer.compare(lenA, lenB);
    }

    private static String makeWindowsEnvironmentBlock(final Map<String, String> map) {
        List<String> list = new ArrayList<>(map.keySet());
        list.sort(ProcessEnvironment::compareWindowsStyle);

        StringBuilder sb = new StringBuilder(map.size()*30);
        int cmp = -1;

        // Some versions of MSVCRT.DLL require SystemRoot to be set.
        // So, we make sure that it is always set, even if not provided
        // by the caller.
        final String systemRoot = "SystemRoot";

        for (String key : list) {
            String value = map.get(key);
            if (cmp < 0 && (cmp = compareWindowsStyle(key, systemRoot)) > 0) {
                addToEnvIfSet(sb, systemRoot);
            }
            addToEnv(sb, key, value);
        }
        if (cmp < 0) {
            addToEnvIfSet(sb, systemRoot);
        }
        if (sb.length() == 0) {
            // Environment was empty and SystemRoot not set in parent
            sb.append('\u0000');
        }
        // Block is double NUL terminated
        sb.append('\u0000');
        return sb.toString();
    }

    private static void addToEnvIfSet(final StringBuilder sb, final String key) {
        String val = getenv(key);
        if (val != null) {
            addToEnv(sb, key, val);
        }
    }

    private static void addToEnv(final StringBuilder sb, final String key, final String val) {
        sb.append(key).append('=').append(val).appendCodePoint(0);
    }

    static void parseWindowsEnvBlock(String block, Map<String, String> map) {
        if (! Build.Target.isWindows()) {
            throw new UnsupportedOperationException();
        }
        String envblock = environmentBlock();
        int beg, end, eql;
        for (beg = 0;
             ((end = envblock.indexOf('\u0000', beg  )) != -1 &&
              // An initial `=' indicates a magic Windows variable name -- OK
              (eql = envblock.indexOf('='     , beg+1)) != -1);
             beg = end + 1) {
            // Ignore corrupted environment strings.
            if (eql < end)
                map.put(envblock.substring(beg, eql),
                                   envblock.substring(eql+1,end));
        }
    }

}
