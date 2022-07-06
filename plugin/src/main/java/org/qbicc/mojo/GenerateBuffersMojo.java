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

import static java.util.Map.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import build.tools.spp.AbstractSppMojo;
import org.qbicc.rt.annotation.Tracking;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-buffers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/modules/java.base/gensrc/GensrcBuffer.gmk")
public class GenerateBuffersMojo extends AbstractSppMojo {
    // todo: change this to records someday
    static final Map<String, Map<String, String>> argsByType = Map.of(
        "byte", Map.of(
            "x", "b",
            "Type", "Byte",
            "fulltype", "byte",
            "Fulltype", "Byte",
            "category", "integralType",
            "LBPV", "0"
        ),
        "char", Map.of(
            "x", "c",
            "Type", "Char",
            "fulltype", "character",
            "Fulltype", "Character",
            "category", "integralType",
            "streams", "streamableType",
            "streamtype", "int",
            "Streamtype", "Int",
            "LBPV", "1"
        ),
        "short", Map.of(
            "x", "s",
            "Type", "Short",
            "fulltype", "short",
            "Fulltype", "Short",
            "category", "integralType",
            "LBPV", "1"
        ),
        "int", Map.of(
            "a", "an",
            "A", "An",
            "x", "i",
            "Type", "Int",
            "fulltype", "integer",
            "Fulltype", "Integer",
            "category", "integralType",
            "LBPV", "2"
        ),
        "long", Map.of(
            "x", "l",
            "Type", "Long",
            "fulltype", "long",
            "Fulltype", "Long",
            "category", "integralType",
            "LBPV", "3"
        ),
        "float", Map.of(
            "x", "f",
            "Type", "Float",
            "fulltype", "float",
            "Fulltype", "Float",
            "category", "floatingPointType",
            "LBPV", "2"
        ),
        "double", Map.of(
            "x", "d",
            "Type", "Double",
            "fulltype", "double",
            "Fulltype", "Double",
            "category", "floatingPointType",
            "LBPV", "3"
        )
    );

    static final Map<String, String> swapTypes = Map.of(
        "byte",    "byte",
        "char",    "char",
        "short",   "short",
        "int",     "int",
        "long",    "long",
        "float",   "int",
        "double",  "long"
    );

    static final Map<String, String> nBytes = Map.of(
        "char",    "two",
        "short",   "two",
        "int",     "four",
        "long",    "eight",
        "float",   "four",
        "double",  "eight"
    );

    static final Map<String, String> nBytesButOne = Map.of(
        "char",    "one",
        "short",   "one",
        "int",     "three",
        "long",    "seven",
        "float",   "three",
        "double",  "seven"
    );

    @Parameter(defaultValue = "${project.baseDir}/../../openjdk/src/java.base/share/classes")
    File inputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/spp")
    File outputDirectory;

    public void execute() throws MojoFailureException {
        try {
            doBuffers();
            doHeapAllocatedBuffers();
            doDirectByteBuffer();
            doUnswappedViewsOfDirectByteBuffers();
            doSwappedViewsOfDirectByteBuffers();
            doBigEndianViewsOfByteBuffers();
            doLittleEndianViewsOfByteBuffers();
        } catch (IOException e) {
            throw new MojoFailureException("Failed to process one or more file(s)", e);
        }
    }

    Map<String, String> typesAndBits(final String type, final String bo, final String rw) {
        // typesAndBits
        Map<String, String> mappings = new HashMap<>();
        mappings.put("a", "a");
        mappings.put("A", "A");
        mappings.put("type", type);
        Map<String, String> argsForType = argsByType.get(type);
        mappings.putAll(argsForType);
        mappings.put("Swaptype", argsForType.get("Type"));
        mappings.put("memtype", type);
        mappings.put("Memtype", argsForType.get("Type"));
        if (type.equals("float")) {
            mappings.put("memtype", "int");
            mappings.put("Memtype", "Int");
            if (! bo.equals("U")) {
                mappings.put("Swaptype", "Int");
                mappings.put("fromBits", "Float.intBitsToFloat");
                mappings.put("toBits", "Float.floatToRawIntBits");
            }
        }
        if (type.equals("double")) {
            mappings.put("memtype", "long");
            mappings.put("Memtype", "Long");
            if (! bo.equals("U")) {
                mappings.put("Swaptype", "Long");
                mappings.put("fromBits", "Double.longBitsToDouble");
                mappings.put("toBits", "Double.doubleToRawLongBits");
            }
        }
        if (rw.equals("S")) {
            mappings.put("swap", "Bits.swap");
        }
        return mappings;
    }

    void genBinOps(Path sourcePath, Writer output, String target, String type, String bo, String rw) throws IOException {
        try (BufferedReader source = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            Map<String, String> mappings = typesAndBits(type, bo, rw);
            String rwkey = rw.equals("R") ? "ro" : "rw"; // fixRw
            String nBytes = GenerateBuffersMojo.nBytes.get(type);
            String nBytesButOne = GenerateBuffersMojo.nBytesButOne.get(type);
            mappings.put("nbytesButOne", nBytesButOne);
            String lbpv = argsByType.get(type).get("LBPV");
            doSpp(source, output, Set.of(rwkey), Map.ofEntries(
                entry("type", mappings.get("type")),
                entry("Type", mappings.get("Type")),
                entry("fulltype", mappings.get("fulltype")),
                entry("memtype", mappings.get("memtype")),
                entry("Memtype", mappings.get("Memtype")),
                entry("fromBits", mappings.getOrDefault("fromBits", "")),
                entry("toBits", mappings.getOrDefault("toBits", "")),
                entry("LG_BYTES_PER_VALUE", lbpv),
                entry("BYTES_PER_VALUE", "(1 << " + lbpv + ")"),
                entry("nbytes", nBytes),
                entry("nbytesButOne", nBytesButOne),
                entry("RW", rw),
                entry("a", mappings.get("a"))
            ), true, true);
        }
    }

    void doGenBuffer(String name, String templateName, String type, final boolean bin, String rw, String bo) throws IOException {
        // java-specific
        Path baseOutputPath = outputDirectory.toPath().resolve("java").resolve("nio");
        Path baseInputPath = inputDirectory.toPath().resolve("java").resolve("nio");
        Files.createDirectories(baseOutputPath);

        String rwkey = rw.equals("R") ? "ro" : "rw"; // fixRw

        Map<String, String> mappings = typesAndBits(type, bo, rw);

        Path DST = baseOutputPath.resolve(name + ".java");
        Path SRC = baseInputPath.resolve(templateName + ".java.template");
        Path SRC_BIN = baseInputPath.resolve(templateName + "-bin.java.template");

        // we don't use DEP because Maven is boring and runs everything sequentially
        Path OUT;
        if (! bin) {
            OUT = DST;
        } else {
            OUT = baseOutputPath.resolve(name + ".binop.0.java");
        }

        // actually run the action

        String lbpv = argsByType.get(type).get("LBPV");
        Path OUT_tmp = OUT.resolveSibling(OUT.getFileName().toString() + ".tmp");
        doSpp(SRC, OUT_tmp,
            Set.of(
                mappings.get("type"),
                mappings.get("category"),
                mappings.getOrDefault("streams", ""),
                rw.equals("R") ? "ro" : "rw",
                "bo" + bo
            ), Map.ofEntries(
                entry("type", mappings.get("type")),
                entry("Type", mappings.get("Type")),
                entry("fulltype", mappings.get("fulltype")),
                entry("Fulltype", mappings.get("Fulltype")),
                entry("streamtype", mappings.getOrDefault("streamtype", "")),
                entry("Streamtype", mappings.getOrDefault("Streamtype", "")),
                entry("x", mappings.get("x")),
                entry("memtype", mappings.get("memtype")),
                entry("Memtype", mappings.get("Memtype")),
                entry("Swaptype", mappings.get("Swaptype")),
                entry("fromBits", mappings.getOrDefault("fromBits", "")),
                entry("toBits", mappings.getOrDefault("toBits", "")),
                entry("LG_BYTES_PER_VALUE", lbpv),
                entry("BYTES_PER_VALUE", "(1 << " + lbpv + ")"),
                entry("BO", bo),
                entry("RW", rw),
                entry("swap", mappings.getOrDefault("swap", "")),
                entry("a", mappings.get("a")),
                entry("A", mappings.get("A"))
            ), false, true
        );
        Files.move(OUT_tmp, OUT, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        // "extra bin thing"
        Path DST_tmp = DST.resolveSibling(DST.getFileName().toString() + ".tmp");
        if (bin) {
            try (BufferedWriter bw = Files.newBufferedWriter(DST_tmp, StandardCharsets.UTF_8)) {
                // copy all lines up to the one that has `#BIN` from OUT
                try (BufferedReader br = Files.newBufferedReader(OUT, StandardCharsets.UTF_8)) {
                    String line;
                    for (;;) {
                        line = br.readLine();
                        if (line.contains("#BIN")) {
                            break;
                        }
                        bw.write(line);
                        bw.newLine();
                    }
                }
                // Remove OUT
                Files.delete(OUT);
                // Actually process each bin op
                genBinOps(SRC_BIN, bw, name + "_char", "char", bo, rw);
                genBinOps(SRC_BIN, bw, name + "_short", "short", bo, rw);
                genBinOps(SRC_BIN, bw, name + "_int", "int", bo, rw);
                genBinOps(SRC_BIN, bw, name + "_long", "long", bo, rw);
                genBinOps(SRC_BIN, bw, name + "_float", "float", bo, rw);
                genBinOps(SRC_BIN, bw, name + "_double", "double", bo, rw);
                bw.write("}");
                bw.newLine();
            }
            Files.move(DST_tmp, DST, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    void doBuffers() throws IOException {
        doGenBuffer("ByteBuffer",   "X-Buffer", "byte", true, "", "");
        doGenBuffer("CharBuffer",   "X-Buffer", "char", false, "", "");
        doGenBuffer("ShortBuffer",  "X-Buffer", "short", false, "", "");
        doGenBuffer("IntBuffer",    "X-Buffer", "int", false, "", "");
        doGenBuffer("LongBuffer",   "X-Buffer", "long", false, "", "");
        doGenBuffer("FloatBuffer",  "X-Buffer", "float", false, "", "");
        doGenBuffer("DoubleBuffer", "X-Buffer", "double", false, "", "");
    }

    void doHeapAllocatedBuffers() throws IOException {
        doGenBuffer("HeapByteBuffer",    "Heap-X-Buffer", "byte", false, "", "");
        doGenBuffer("HeapByteBufferR",   "Heap-X-Buffer", "byte", false, "R", "");
        doGenBuffer("HeapCharBuffer",    "Heap-X-Buffer", "char", false, "", "");
        doGenBuffer("HeapCharBufferR",   "Heap-X-Buffer", "char", false, "R", "");
        doGenBuffer("HeapShortBuffer",   "Heap-X-Buffer", "short", false, "", "");
        doGenBuffer("HeapShortBufferR",  "Heap-X-Buffer", "short", false, "R", "");
        doGenBuffer("HeapIntBuffer",     "Heap-X-Buffer", "int", false, "", "");
        doGenBuffer("HeapIntBufferR",    "Heap-X-Buffer", "int", false, "R", "");
        doGenBuffer("HeapLongBuffer",    "Heap-X-Buffer", "long", false, "", "");
        doGenBuffer("HeapLongBufferR",   "Heap-X-Buffer", "long", false, "R", "");
        doGenBuffer("HeapFloatBuffer",   "Heap-X-Buffer", "float", false, "", "");
        doGenBuffer("HeapFloatBufferR",  "Heap-X-Buffer", "float", false, "R", "");
        doGenBuffer("HeapDoubleBuffer",  "Heap-X-Buffer", "double", false, "", "");
        doGenBuffer("HeapDoubleBufferR", "Heap-X-Buffer", "double", false, "R", "");
    }

    void doDirectByteBuffer() throws IOException {
        doGenBuffer("DirectByteBuffer",  "Direct-X-Buffer", "byte", true, "", "");
        doGenBuffer("DirectByteBufferR", "Direct-X-Buffer", "byte", true, "R", "");
    }

    void doUnswappedViewsOfDirectByteBuffers() throws IOException {
        doGenBuffer("DirectCharBufferU",    "Direct-X-Buffer", "char", false, "", "U");
        doGenBuffer("DirectCharBufferRU",   "Direct-X-Buffer", "char", false, "R", "U");
        doGenBuffer("DirectShortBufferU",   "Direct-X-Buffer", "short", false, "", "U");
        doGenBuffer("DirectShortBufferRU",  "Direct-X-Buffer", "short", false, "R", "U");
        doGenBuffer("DirectIntBufferU",     "Direct-X-Buffer", "int", false, "", "U");
        doGenBuffer("DirectIntBufferRU",    "Direct-X-Buffer", "int", false, "R", "U");
        doGenBuffer("DirectLongBufferU",    "Direct-X-Buffer", "long", false, "", "U");
        doGenBuffer("DirectLongBufferRU",   "Direct-X-Buffer", "long", false, "R", "U");
        doGenBuffer("DirectFloatBufferU",   "Direct-X-Buffer", "float", false, "", "U");
        doGenBuffer("DirectFloatBufferRU",  "Direct-X-Buffer", "float", false, "R", "U");
        doGenBuffer("DirectDoubleBufferU",  "Direct-X-Buffer", "double", false, "", "U");
        doGenBuffer("DirectDoubleBufferRU", "Direct-X-Buffer", "double", false, "R", "U");
    }

    void doSwappedViewsOfDirectByteBuffers() throws IOException {
        doGenBuffer("DirectCharBufferS",    "Direct-X-Buffer", "char", false, "", "S");
        doGenBuffer("DirectCharBufferRS",   "Direct-X-Buffer", "char", false, "R", "S");
        doGenBuffer("DirectShortBufferS",   "Direct-X-Buffer", "short", false, "", "S");
        doGenBuffer("DirectShortBufferRS",  "Direct-X-Buffer", "short", false, "R", "S");
        doGenBuffer("DirectIntBufferS",     "Direct-X-Buffer", "int", false, "", "S");
        doGenBuffer("DirectIntBufferRS",    "Direct-X-Buffer", "int", false, "R", "S");
        doGenBuffer("DirectLongBufferS",    "Direct-X-Buffer", "long", false, "", "S");
        doGenBuffer("DirectLongBufferRS",   "Direct-X-Buffer", "long", false, "R", "S");
        doGenBuffer("DirectFloatBufferS",   "Direct-X-Buffer", "float", false, "", "S");
        doGenBuffer("DirectFloatBufferRS",  "Direct-X-Buffer", "float", false, "R", "S");
        doGenBuffer("DirectDoubleBufferS",  "Direct-X-Buffer", "double", false, "", "S");
        doGenBuffer("DirectDoubleBufferRS", "Direct-X-Buffer", "double", false, "R", "S");
    }

    void doBigEndianViewsOfByteBuffers() throws IOException {
        doGenBuffer("ByteBufferAsCharBufferB",    "ByteBufferAs-X-Buffer", "char", false, "", "B");
        doGenBuffer("ByteBufferAsCharBufferRB",   "ByteBufferAs-X-Buffer", "char", false, "R", "B");
        doGenBuffer("ByteBufferAsShortBufferB",   "ByteBufferAs-X-Buffer", "short", false, "", "B");
        doGenBuffer("ByteBufferAsShortBufferRB",  "ByteBufferAs-X-Buffer", "short", false, "R", "B");
        doGenBuffer("ByteBufferAsIntBufferB",     "ByteBufferAs-X-Buffer", "int", false, "", "B");
        doGenBuffer("ByteBufferAsIntBufferRB",    "ByteBufferAs-X-Buffer", "int", false, "R", "B");
        doGenBuffer("ByteBufferAsLongBufferB",    "ByteBufferAs-X-Buffer", "long", false, "", "B");
        doGenBuffer("ByteBufferAsLongBufferRB",   "ByteBufferAs-X-Buffer", "long", false, "R", "B");
        doGenBuffer("ByteBufferAsFloatBufferB",   "ByteBufferAs-X-Buffer", "float", false, "", "B");
        doGenBuffer("ByteBufferAsFloatBufferRB",  "ByteBufferAs-X-Buffer", "float", false, "R", "B");
        doGenBuffer("ByteBufferAsDoubleBufferB",  "ByteBufferAs-X-Buffer", "double", false, "", "B");
        doGenBuffer("ByteBufferAsDoubleBufferRB", "ByteBufferAs-X-Buffer", "double", false, "R", "B");
    }

    void doLittleEndianViewsOfByteBuffers() throws IOException {
        doGenBuffer("ByteBufferAsCharBufferL",    "ByteBufferAs-X-Buffer", "char", false, "", "L");
        doGenBuffer("ByteBufferAsCharBufferRL",   "ByteBufferAs-X-Buffer", "char", false, "R", "L");
        doGenBuffer("ByteBufferAsShortBufferL",   "ByteBufferAs-X-Buffer", "short", false, "", "L");
        doGenBuffer("ByteBufferAsShortBufferRL",  "ByteBufferAs-X-Buffer", "short", false, "R", "L");
        doGenBuffer("ByteBufferAsIntBufferL",     "ByteBufferAs-X-Buffer", "int", false, "", "L");
        doGenBuffer("ByteBufferAsIntBufferRL",    "ByteBufferAs-X-Buffer", "int", false, "R", "L");
        doGenBuffer("ByteBufferAsLongBufferL",    "ByteBufferAs-X-Buffer", "long", false, "", "L");
        doGenBuffer("ByteBufferAsLongBufferRL",   "ByteBufferAs-X-Buffer", "long", false, "R", "L");
        doGenBuffer("ByteBufferAsFloatBufferL",   "ByteBufferAs-X-Buffer", "float", false, "", "L");
        doGenBuffer("ByteBufferAsFloatBufferRL",  "ByteBufferAs-X-Buffer", "float", false, "R", "L");
        doGenBuffer("ByteBufferAsDoubleBufferL",  "ByteBufferAs-X-Buffer", "double", false, "", "L");
        doGenBuffer("ByteBufferAsDoubleBufferRL", "ByteBufferAs-X-Buffer", "double", false, "R", "L");
    }
}
