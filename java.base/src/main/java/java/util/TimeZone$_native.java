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

import static java.lang.System.getenv;
import static org.qbicc.runtime.CNative.*;
import static jdk.internal.sys.posix.Time.*;
import static org.qbicc.runtime.stdc.Time.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/share/native/libjava/TimeZone.c")
@Tracking("src/java.base/unix/native/libjava/TimeZone_md.c")
@Tracking("src/java.base/windows/native/libjava/TimeZone_md.c")
public class TimeZone$_native {

    private static String getSystemTimeZoneID(String javaHome) {
        String tz = getenv("TZ");
        if (tz == null || tz.isEmpty()) {
            tz = getPlatformTimeZoneID();
        }
        if (tz == null) {
            return null;
        }
        if (tz.startsWith(":")) {
            tz = tz.substring(1);
        }
        if (Build.Target.isLinux() && tz.startsWith("posix/")) {
            tz = tz.substring(6);
        }
        if (Build.Target.isAix()) {
            // todo: map platform time zone to Java
            throw new UnsupportedOperationException();
        } else {
            return tz;
        }
    }

    private static String getPlatformTimeZoneID() {
        // complex platform-dependent action
        // first try /etc/timezone
        if (Build.Target.isLinux()) {
            try (BufferedReader br = Files.newBufferedReader(Path.of("/etc/timezone"), StandardCharsets.UTF_8)) {
                String line = br.readLine();
                if (line != null) {
                    return line.trim();
                }
            } catch (IOException ignored) {}
        }
        // next try /etc/localtime
        Path localTimePath = Path.of("/etc/localtime");
        if (Files.isSymbolicLink(localTimePath)) {
            try {
                Path linkPath = Files.readSymbolicLink(localTimePath);
                String str = linkPath.toString();
                int idx = str.indexOf("zoneinfo/");
                if (idx != -1) {
                    return str.substring(idx);
                }
            } catch (IOException ignored) {}
        }
        // worst case: find a zone info file that matches the content of /etc/localtime
        try {
            byte[] bytes = Files.readAllBytes(localTimePath);
            Path zoneInfo = Path.of("/usr/share/zoneinfo");
            return searchZoneInfo(bytes, zoneInfo);
        } catch (IOException ignored) {}
        // no idea
        return null;
    }

    private static String searchZoneInfo(final byte[] bytes, final Path zoneInfo) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(zoneInfo)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    if (path.endsWith(".") || path.endsWith("..")) {
                        // ignore
                        continue;
                    }
                    String result = searchZoneInfo(bytes, path);
                    if (result != null) {
                        return result;
                    }
                } else {
                    String fnStr = path.getFileName().toString();
                    if (fnStr.equals("ROC") || fnStr.equals("posixrules") || fnStr.equals("localtime")) {
                        // ignore
                        continue;
                    }
                    try {
                        byte[] fileBytes = Files.readAllBytes(path);
                        if (Arrays.equals(bytes, fileBytes)) {
                            String str = path.toString();
                            int idx = str.indexOf("zoneinfo/");
                            if (idx != -1) {
                                return str.substring(idx);
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    /**
     * Gets the custom time zone ID based on the GMT offset of the
     * platform. (e.g., "GMT+08:00")
     */
    private static String getSystemGMTOffsetID() {
        time_t offset;
        char sign;
        int h;
        int m;
        if (Build.Target.isMacOs()) {
            struct_tm local_tm = auto();

            time_t clock = auto(time(zero()));
            if (localtime_r(addr_of(clock), addr_of(local_tm)).isNull()) {
                return "GMT";
            }
            offset = addr_of(local_tm.tm_gmtoff).loadUnshared().cast();
            if (offset.isZero()) {
                return "GMT";
            } else if (offset.isGt(zero())) {
                sign = '+';
            } else {
                offset = word(-offset.longValue());
                sign = '-';
            }
        } else if (Build.Target.isPosix()) {
            offset = timezone.cast();
            if (offset.isZero()) {
                return "GMT";
            } else if (offset.isGt(zero())) {
                // Note the sign is opposite of above
                sign = '-';
            } else {
                offset = word(-offset.longValue());
                sign = '+';
            }
        } else {
            // todo: windows
            throw new UnsupportedOperationException();
        }
        h = offset.intValue() / 3600;
        m = offset.intValue() % 3600 / 60;
        StringBuilder sb = new StringBuilder(9);
        sb.append("GMT");
        sb.append(sign);
        if (h < 10) {
            sb.append('0');
        }
        sb.append(h);
        sb.append(':');
        if (m < 10) {
            sb.append('0');
        }
        sb.append(m);
        return sb.toString();
    }
}
