/*
 * This code is based on the OpenJDK build process defined in `make/gensrc/GensrcExceptions.gmk`, which contains the following
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

package cc.quarkus.qccrt.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.quarkus.qccrt.annotation.Tracking;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-exceptions", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcExceptions.gmk")
public class GenerateExceptionsMojo extends AbstractMojo {
    @Parameter(required = true)
    File scriptsDir;

    @Parameter(defaultValue = "genExceptions.sh", required = true)
    String scriptName;

    @Parameter(required = true)
    File basePath;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/exceptions", required = true)
    File outputDirectory;

    public void execute() throws MojoExecutionException {
        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> env = pb.environment();
        env.put("SCRIPTS", scriptsDir.toString());
        env.put("NAWK", MojoUtil.findExec("nawk", "gawk", "awk").toString());
        Path sh = MojoUtil.findExec("sh");
        env.put("SH", sh.toString());
        // add each input file
        Path basePath = this.basePath.toPath();
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        for (String subDir : List.of(".", "charset", "channels")) {
            List<String> command = new ArrayList<>();
            command.add(sh.toString());
            command.add(new File(scriptsDir, scriptName).toString());
            Path exceptionsPath = basePath.resolve(subDir).resolve("exceptions");
            command.add(exceptionsPath.toString());
            Path outputPath = outputDirectory.toPath().resolve(subDir);
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create output directory " + outputPath, e);
            }
            command.add(outputPath.toString());
            pb.command(command);
            MojoUtil.runAndWaitForProcessNoInput(pb);
        }
    }
}
