package org.qbicc.mojo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * A custom mojo to generate JAR files the standard way.
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE)
public class JarMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    File classesDirectory;

    @Parameter(defaultValue = "${project.build.directory}")
    File outputDirectory;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    String builtByVersion;

    @Parameter(defaultValue = "${project.name}")
    String projectName;

    @Parameter(defaultValue = "${project.version}")
    String projectVersion;

    @Parameter(defaultValue = "${user.name}")
    String builtBy;

    @Parameter(defaultValue = "${project.build.finalName}")
    String finalName;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Optional<ToolProvider> jarProviderOpt = ToolProvider.findFirst("jar");
        ToolProvider jarProvider = jarProviderOpt.orElseThrow(() -> new MojoExecutionException("No provider of the `jar` tool was found"));
        Artifact projectArtifact = project.getArtifact();
        File oldProjectArtifactFile = projectArtifact.getFile();
        if (oldProjectArtifactFile != null && oldProjectArtifactFile.isFile()) {
            throw new MojoExecutionException("A project artifact already exists for this project");
        }
        // TODO: pom.properties?
        // TODO: pom.xml?
        // manifest
        Manifest manifest = new Manifest();
        Attributes main = manifest.getMainAttributes();
        main.putValue("Created-By", "Qbicc RT Plugin " + builtByVersion);
        main.putValue("Built-By", builtBy);
        main.putValue("Build-Jdk", System.getProperty("java.specification.version"));
        Path outputPath = outputDirectory.toPath().resolve(finalName + ".jar");
        Path tmpFile;
        try {
            tmpFile = Files.createTempFile("maven-qbicc-MANIFEST", "MF");
        } catch (IOException e) {
            throw new MojoFailureException("Failed to create temp file", e);
        }
        try {
            try (OutputStream os = Files.newOutputStream(tmpFile)) {
                manifest.write(os);
            } catch (IOException e) {
                throw new MojoFailureException("Failed to write manifest", e);
            }

            List<String> args = new ArrayList<>();

            args.add("--create");

            // add manifest
            args.add("--manifest");
            args.add(tmpFile.toString());

            args.add("--module-version");
            args.add(projectVersion);

            args.add("--file");
            args.add(outputPath.toString());

            args.add("-C");
            args.add(classesDirectory.toString());

            // add all files
            args.add(".");

            // todo: capture output and nicely format it in the log?
            int res = jarProvider.run(System.out, System.err, args.toArray(String[]::new));
            if (res != 0) {
                throw new MojoFailureException("`jar` exited with code: " + res);
            }
            // delete temp manifest file
        } finally {
            safeDelete(tmpFile);
        }

        // attach the resultant JAR
        projectArtifact.setFile(outputPath.toFile());
        // todo: classifier support:
        // projectHelper.attachArtifact(project, "jar", outputPath.toFile());
    }

    private void safeDelete(final Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            getLog().warn("Failed to delete temporary file \"" + path + "\"", e);
        }
    }
}
