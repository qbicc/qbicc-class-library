package cc.quarkus.qccrt.mojo;

import static java.util.Map.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

import build.tools.spp.AbstractSppMojo;
import cc.quarkus.qccrt.annotation.Tracking;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-charset-coder", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcCharsetCoder.gmk")
public class GenerateCharsetCoderMojo extends AbstractSppMojo {
    @Parameter(defaultValue = "${project.baseDir}/../../openjdk/src/java.base/share/classes")
    File inputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/spp")
    File outputDirectory;

    public void execute() throws MojoFailureException {
        try {
            Path outputPath = outputDirectory.toPath();
            Path inputPath = inputDirectory.toPath();
            Path pkg = Path.of("java", "nio", "charset");
            Files.createDirectories(outputPath.resolve(pkg));
            // decoder
            Path outFile = outputPath.resolve(pkg).resolve("CharsetDecoder.java");
            Path outFileTmp = outputPath.resolve(pkg).resolve("CharsetDecoder.java.tmp");
            doSpp(
                inputPath.resolve(pkg).resolve("Charset-X-Coder.java.template"),
                outFileTmp,
                Set.of("decoder"),
                Map.ofEntries(
                    entry("A", "A"),
                    entry("a", "a"),
                    entry("Code", "Decode"),
                    entry("code", "decode"),
                    entry("itypesPhrase", "bytes in a specific charset"),
                    entry("otypesPhrase", "sixteen-bit Unicode characters"),
                    entry("itype", "byte"),
                    entry("otype", "character"),
                    entry("Itype", "Byte"),
                    entry("Otype", "Char"),
                    entry("coder", "decoder"),
                    entry("Coder", "Decoder"),
                    entry("coding", "decoding"),
                    entry("OtherCoder", "Encoder"),
                    entry("replTypeName", "string"),
                    entry("defaultRepl", "\"\\uFFFD\""),
                    entry("defaultReplName", "<code>\"&#92;uFFFD\"</code>"),
                    entry("replType", "String"),
                    entry("replFQType", "java.lang.String"),
                    entry("replLength", "length()"),
                    entry("ItypesPerOtype", "CharsPerByte"),
                    entry("notLegal", "not legal for this charset"),
                    entry("otypes-per-itype", "chars-per-byte"),
                    entry("outSequence", "Unicode character")
                ),
                false,
                true
            );
            Files.move(outFileTmp, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            // encoder
            outFile = outputPath.resolve(pkg).resolve("CharsetEncoder.java");
            outFileTmp = outputPath.resolve(pkg).resolve("CharsetEncoder.java.tmp");
            doSpp(
                inputPath.resolve(pkg).resolve("Charset-X-Coder.java.template"),
                outFileTmp,
                Set.of("encoder"),
                Map.ofEntries(
                    entry("A", "A"),
                    entry("a", "a"),
                    entry("Code", "Encode"),
                    entry("code", "encode"),
                    entry("itypesPhrase", "sixteen-bit Unicode characters"),
                    entry("otypesPhrase", "bytes in a specific charset"),
                    entry("itype", "character"),
                    entry("otype", "byte"),
                    entry("Itype", "Char"),
                    entry("Otype", "Byte"),
                    entry("coder", "encoder"),
                    entry("Coder", "Encoder"),
                    entry("coding", "encoding"),
                    entry("OtherCoder", "Decoder"),
                    entry("replTypeName", "byte array"),
                    entry("defaultRepl", "new byte[] { (byte)'\\?' }"),
                    entry("defaultReplName", "<code>{</code>&nbsp;<code>(byte)'?'</code>&nbsp;<code>}</code>"),
                    entry("replType", "byte[]"),
                    entry("replFQType", "byte[]"),
                    entry("replLength", "length"),
                    entry("ItypesPerOtype", "BytesPerChar"),
                    entry("notLegal", "not a legal sixteen-bit Unicode sequence"),
                    entry("otypes-per-itype", "bytes-per-char"),
                    entry("outSequence", "byte sequence in the given charset")
                ),
                false,
                true
            );
            Files.move(outFileTmp, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to process one or more file(s)", e);
        }
    }
}
