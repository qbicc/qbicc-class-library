package cc.quarkus.qccrt.mojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import build.tools.generatecharacter.GenerateCharacter;
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
@Mojo(name = "generate-character-data", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcCharacterData.gmk")
public class GenerateCharacterDataMojo extends AbstractMojo {
    static final String JAVA = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ? "java.exe" : "java";

    @Parameter(required = true)
    File characterData;

    @Parameter(required = true)
    File unicodeData;

    @Parameter(required = true)
    File output;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path javaLangTarget = output.toPath().resolve("java").resolve("lang");
            Files.createDirectories(javaLangTarget);
            generateCharacterData("CharacterDataLatin1", false, -1, true, 8);
            generateCharacterData("CharacterData00", true, 0, false, 11, 4, 1);
            generateCharacterData("CharacterData01", true, 1, false, 11, 4, 1);
            generateCharacterData("CharacterData02", true, 2, false, 11, 4, 1);
            generateCharacterData("CharacterData0E", true, 14, false, 11, 4, 1);
            Files.copy(characterData.toPath().resolve("CharacterDataUndefined.java.template"), javaLangTarget.resolve("CharacterDataUndefined.java"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(characterData.toPath().resolve("CharacterDataPrivateUse.java.template"), javaLangTarget.resolve("CharacterDataPrivateUse.java"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoExecutionException("Mojo failed: " + e, e);
        }
    }

    void generateCharacterData(String name, boolean string, int plane, boolean latin1, int... bits) throws MojoFailureException, MojoExecutionException {
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
            command.add(GenerateCharacter.class.getName());

            // args
            if (latin1) {
                command.add("-latin1");
            }
            if (string) {
                command.add("-string");
            }
            if (plane >= 0) {
                command.add("-plane");
                command.add(Integer.toString(plane));
            }

            command.add("-template");
            command.add(characterData.toPath().resolve(name + ".java.template").toString());

            command.add("-spec");
            command.add(unicodeData.toPath().resolve("UnicodeData.txt").toString());

            command.add("-specialcasing");
            command.add(unicodeData.toPath().resolve("SpecialCasing.txt").toString());

            command.add("-proplist");
            command.add(unicodeData.toPath().resolve("PropList.txt").toString());

            command.add("-o");
            command.add(output.toPath().resolve("java").resolve("lang").resolve(name + ".java").toString());

            command.add("-usecharforbyte");

            for (int bit : bits) {
                command.add(Integer.toString(bit));
            }

            pb.command(command);
            MojoUtil.runAndWaitForProcessNoInput(pb);

        } catch (IOException e) {
            throw new MojoExecutionException("Mojo failed: " + e, e);
        }
    }
}
