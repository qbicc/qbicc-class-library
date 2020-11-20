package cc.quarkus.qccrt.mojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import build.tools.cldrconverter.CLDRConverter;
import cc.quarkus.qccrt.annotation.Tracking;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 *
 */
@Mojo(name = "generate-cldr", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcCLDR.gmk")
public class GenerateCldrMojo extends AbstractMojo {
    static final String JAVA = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ? "java.exe" : "java";

    @Parameter(required = true)
    String base;

    @Parameter(required = true)
    String baseLocales;

    @Parameter(required = true)
    File output;

    @Parameter(defaultValue = "false")
    boolean baseModule;

    @Parameter
    File znTemplateFile;

    @Parameter
    File tzDataDir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path java = Path.of(System.getProperty("java.home"), "bin", JAVA);
            if (! (Files.isRegularFile(java) && Files.isExecutable(java))) {
                throw new MojoFailureException("Cannot locate java executable");
            }
            Path outputPath = output.toPath();
            Files.createDirectories(outputPath);
            ProcessBuilder pb = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(java.toString());
            // this is a tricky maneuver to ensure the forked JVM has the same classpath that I do
            command.add("-classpath");
            ClassRealm realm = (ClassRealm) getClass().getClassLoader();
            URL[] urls = realm.getURLs();
            command.add(Arrays.stream(urls).map(Objects::toString).collect(Collectors.joining(File.pathSeparator)));
            // the tool class
            command.add(CLDRConverter.class.getName());

            // args
            command.add("-base");
            command.add(base);
            command.add("-baselocales");
            command.add(baseLocales);
            command.add("-o");
            command.add(output.toString());
            if (baseModule) {
                command.add("-basemodule");
            }
            if (znTemplateFile != null) {
                command.add("-zntempfile");
                command.add(znTemplateFile.toString());
            }
            if (tzDataDir != null) {
                command.add("-tzdatadir");
                command.add(tzDataDir.toString());
            }

            pb.command(command);
            MojoUtil.runAndWaitForProcessNoInput(pb);

        } catch (IOException e) {
            throw new MojoExecutionException("Mojo failed: " + e, e);
        }
    }
}
