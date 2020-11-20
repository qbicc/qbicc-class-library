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
