/*
 * This code is based on the OpenJDK build process defined in `make/gensrc/GensrcBuffer.gmk`, which contains the following
 * copyright notice:
 *
 * #
 * # Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * # DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * #
 * # This code is free software; you can redistribute it and/or modify it
 * # under the terms of the GNU General Public License version 2 only, as
 * # published by the Free Software Foundation.  Oracle designates this
 * # particular file as subject to the "Classpath" exception as provided
 * # by Oracle in the LICENSE file that accompanied this code.
 * #
 * # This code is distributed in the hope that it will be useful, but WITHOUT
 * # ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * # FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * # version 2 for more details (a copy is included in the LICENSE file that
 * # accompanied this code).
 * #
 * # You should have received a copy of the GNU General Public License version
 * # 2 along with this work; if not, write to the Free Software Foundation,
 * # Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * #
 * # Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * # or visit www.oracle.com if you need additional information or have any
 * # questions.
 * #
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */

package org.qbicc.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import build.tools.spp.AbstractSppMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.qbicc.rt.annotation.Tracking;

@Mojo(name = "generate-scoped-memory-access", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/modules/java.base/gensrc/GensrcScopedMemoryAccess.gmk")
public class GenerateScopedMemoryAccessMojo extends AbstractSppMojo {
    static record Item(String type, Set<String> keys) {}

    static final Map<String, Item> items = Map.of(
        "Byte", new Item("byte", Set.of("AtomicAdd", "Bitwise", "ShorterThanInt")),
        "Short", new Item("short", Set.of("Unaligned", "AtomicAdd", "Bitwise", "ShorterThanInt")),
        "Char", new Item("char", Set.of("Unaligned", "AtomicAdd", "Bitwise", "ShorterThanInt")),
        "Int", new Item("int", Set.of("CAS", "Unaligned", "AtomicAdd", "Bitwise")),
        "Long", new Item("long", Set.of("CAS", "Unaligned", "AtomicAdd", "Bitwise")),
        "Float", new Item("float", Set.of("CAS", "floatingPoint", "AtomicAdd")),
        "Double", new Item("double", Set.of("CAS", "floatingPoint", "AtomicAdd"))
    );

    @Parameter(defaultValue = "${project.baseDir}/../../openjdk/src/java.base/share/classes")
    File inputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/spp")
    File outputDirectory;

    public void execute() throws MojoFailureException {
        List<String> scopeMemoryAccessTypes = List.of("Byte", "Short", "Char", "Int", "Long", "Float", "Double");
        try {
            Files.createDirectories(outputDirectory.toPath());
            Path baseInputPath = inputDirectory.toPath().resolve("jdk").resolve("internal").resolve("misc");
            Path scopedMemoryAccessTemplate = baseInputPath.resolve("X-ScopedMemoryAccess.java.template");
            Path scopedMemoryAccessBinTemplate = baseInputPath.resolve("X-ScopedMemoryAccess-bin.java.template");
            Path outputDir = outputDirectory.toPath().resolve("jdk").resolve("internal").resolve("misc");
            Files.createDirectories(outputDir);
            Path output = outputDir.resolve("ScopedMemoryAccess.java");
            try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                try (BufferedReader reader = Files.newBufferedReader(scopedMemoryAccessTemplate, StandardCharsets.UTF_8)) {
                    reader.transferTo(writer);
                }
                for (String type : scopeMemoryAccessTypes) {
                    Item item = items.get(type);
                    Map<String, String> vars = Map.of(
                        "type", item.type,
                        "Type", type
                    );
                    Set<String> keys = new HashSet<>(item.keys);
                    keys.addAll(vars.keySet());

                    try (BufferedReader reader = Files.newBufferedReader(scopedMemoryAccessBinTemplate, StandardCharsets.UTF_8)) {
                        doSpp(reader, writer, keys, vars, false, false);
                    }
                }
                writer.write("}\n");
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to process one or more file(s)", e);
        }
    }
}
