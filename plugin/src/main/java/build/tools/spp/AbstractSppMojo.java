package build.tools.spp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;

/**
 *
 */
public abstract class AbstractSppMojo extends AbstractMojo {

    public void doSpp(Path source, Path target, final Set<String> keys, final Map<String, String> vars, final boolean be, final boolean el) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                doSpp(reader, writer, keys, vars, be, el);
            }
        }
    }

    public void doSpp(Reader source, Writer target, final Set<String> keys, final Map<String, String> vars, final boolean be, final boolean el) throws IOException {
        StringBuffer buf = new StringBuffer();
        new build.tools.spp.Spp().spp(new Scanner(source), buf, "", keys, vars, be, el, false);
        target.append(buf);
    }
}
