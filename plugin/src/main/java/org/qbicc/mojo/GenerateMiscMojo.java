/*
 * This code is based on the OpenJDK build process defined in `make/gensrc/GensrcMisc.gmk`, which contains the following
 * copyright notice:
 *
 * #
 * # Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.qbicc.rt.annotation.Tracking;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-misc", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcMisc.gmk")
public class GenerateMiscMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.baseDir}/../../openjdk/src/java.base/share/classes")
    File inputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/misc")
    File outputDirectory;

    @Parameter(defaultValue = "qbicc", required = true)
    String launcherName;

    @Parameter(defaultValue = "qbicc runtime environment", required = true)
    String runtimeName;

    @Parameter(defaultValue = "${project.version}", required = true)
    String versionShort;

    @Parameter(defaultValue = "${project.version}", required = true)
    String versionString;

    @Parameter(defaultValue = "${project.version}", required = true)
    String versionNumber;

    @Parameter
    String versionPre;

    @Parameter(defaultValue = "0", required = true)
    String versionBuild;

    @Parameter
    String versionOpt;

    @Parameter
    String versionDate;

    @Parameter
    String vendorVersionString;

    @Parameter(required = true)
    String vendor;

    @Parameter(required = true)
    String vendorUrl;

    @Parameter(required = true)
    String vendorUrlBug;

    @Parameter(required = true)
    String vendorUrlVmBug;


    public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            Path outputPath = outputDirectory.toPath();
            Path inputPath = inputDirectory.toPath();

            // VersionProps.java
            Path pkg = Path.of("java", "lang");
            Files.createDirectories(outputPath.resolve(pkg));
            processTextFile(
                inputPath.resolve(pkg).resolve("VersionProps.java.template"),
                outputPath.resolve(pkg).resolve("VersionProps.java"),
                Map.ofEntries(
                    entry("@@LAUNCHER_NAME@@", launcherName),
                    entry("@@RUNTIME_NAME@@", runtimeName),
                    entry("@@VERSION_SHORT@@", versionShort),
                    entry("@@VERSION_STRING@@", versionString),
                    entry("@@VERSION_NUMBER@@", versionNumber),
                    entry("@@VERSION_PRE@@", versionPre == null ? "" : versionPre),
                    entry("@@VERSION_BUILD@@", versionBuild),
                    entry("@@VERSION_OPT@@", versionOpt == null ? "" : versionOpt),
                    entry("@@VERSION_DATE@@", versionDate == null ? LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) : versionDate),
                    entry("@@VENDOR_VERSION_STRING@@", vendorVersionString == null ? "" : vendorVersionString),
                    entry("@@VENDOR@@", vendor),
                    entry("@@VENDOR_URL@@", vendorUrl),
                    entry("@@VENDOR_URL_BUG@@", vendorUrlBug),
                    entry("@@VENDOR_URL_VM_BUG@@", vendorUrlVmBug)
                )
            );

            // JceSecurity.java
            pkg = Path.of("javax", "crypto");
            Files.createDirectories(outputPath.resolve(pkg));
            processTextFile(
                inputPath.resolve(pkg).resolve("JceSecurity.java.template"),
                outputPath.resolve(pkg).resolve("JceSecurity.java"),
                Map.of(
                    "@@JCE_DEFAULT_POLICY@@", "unlimited"
                )
            );

        } catch (IOException e) {
            throw new MojoFailureException("Failed to process one or more file(s)", e);
        }
    }

    void processTextFile(Path sourceFile, Path outputFile, Map<String, String> replacements) throws MojoExecutionException {
        // the original TextFileProcessing is just plain bananas, so we'll implement it the simple way
        Pattern pattern = Pattern.compile(replacements.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|")));
        StringBuilder buf = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            try (BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                String line = br.readLine();
                if (line != null) {
                    Matcher matcher = pattern.matcher(line);
                    for (;;) {
                        while (matcher.find()) {
                            matcher.appendReplacement(buf, replacements.get(matcher.group()));
                        }
                        matcher.appendTail(buf);
                        bw.append(buf);
                        bw.newLine();
                        buf.setLength(0);
                        line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        matcher.reset(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process " + sourceFile + " into " + outputFile, e);
        }
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }
}
