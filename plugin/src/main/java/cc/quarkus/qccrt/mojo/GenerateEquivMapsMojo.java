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

import build.tools.generatelsrequivmaps.EquivMapsGenerator;
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
@Mojo(name = "generate-locale-equiv-maps", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/Gensrc-java.base.gmk")
public class GenerateEquivMapsMojo extends AbstractMojo {
    static final String JAVA = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ? "java.exe" : "java";

    @Parameter(required = true)
    File languageSubtagRegistry;

    @Parameter
    File outputFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path java = Path.of(System.getProperty("java.home"), "bin", JAVA);
            if (! (Files.isRegularFile(java) && Files.isExecutable(java))) {
                throw new MojoFailureException("Cannot locate java executable");
            }
            Path outputPath = outputFile.toPath();
            Files.createDirectories(outputPath.getParent());
            ProcessBuilder pb = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(java.toString());
            // this is a tricky maneuver to ensure the forked JVM has the same classpath that I do
            command.add("-classpath");
            ClassRealm realm = (ClassRealm) getClass().getClassLoader();
            URL[] urls = realm.getURLs();
            command.add(Arrays.stream(urls).map(Objects::toString).collect(Collectors.joining(File.pathSeparator)));
            // the tool class
            command.add(EquivMapsGenerator.class.getName());

            // args
            command.add(languageSubtagRegistry.toString());
            command.add(outputFile.toString());

            pb.command(command);
            MojoUtil.runAndWaitForProcessNoInput(pb);

        } catch (IOException e) {
            throw new MojoExecutionException("Mojo failed: " + e, e);
        }
    }
}
