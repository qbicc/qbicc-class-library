package org.qbicc.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;

final class MojoUtil {
    private MojoUtil() {}

    static Path findExec(String... possibleNames) throws MojoExecutionException {
        String pathEnv = System.getenv("PATH");
        String[] segments = pathEnv.split(Pattern.quote(File.pathSeparator));
        int len = segments.length;
        Path[] paths = new Path[len];
        for (int i = 0; i < len; i ++) {
            paths[i] = Path.of(segments[i]);
        }
        for (String possibleName : possibleNames) {
            Path matchedPath = null;
            for (Path path : paths) {
                Path candidate = path.resolve(possibleName);
                if (Files.exists(candidate)) {
                    // found on this path
                    matchedPath = candidate;
                    break;
                }
            }
            if (matchedPath != null) {
                if (Files.isRegularFile(matchedPath) && Files.isExecutable(matchedPath)) {
                    return matchedPath;
                }
            }
        }
        throw new MojoExecutionException("Cannot find any executable matches for " + Arrays.asList(possibleNames));
    }

    public static void runAndWaitForProcessNoInput(final ProcessBuilder pb) throws MojoExecutionException {
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to execute subprocess", e);
        }
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        boolean intr = false;
        try {
            for (;;) try {
                if (process.waitFor() > 0) {
                    throw new MojoExecutionException("Process execution failed");
                }
                break;
            } catch (InterruptedException e) {
                intr = true;
            }
        } finally {
            if (intr) Thread.currentThread().interrupt();
        }
    }

    public static List<Path> findFiles(List<Path> searchPaths, List<Path> globs) throws IOException {
        List<Path> result = new ArrayList<>();
        Matcher searchMatcher = Pattern.compile(
            globs.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.replaceAll("\\*", ".*"))
                .collect(Collectors.joining("|"))).matcher("");
        for (Path searchPath : searchPaths) {
            findFiles(result, searchPath, searchMatcher);
        }
        return result;
    }

    private static void findFiles(final List<Path> result, final Path searchPath, final Matcher searchMatcher) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(searchPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    findFiles(result, path, searchMatcher);
                } else {
                    searchMatcher.reset(path.getFileName().toString());
                    if (searchMatcher.matches()) {
                        result.add(path);
                    }
                }
            }
        }
    }

    public static <I, O> List<O> map(List<I> in, Function<I, O> function) {
        return in.stream().map(function).collect(Collectors.toList());
    }

    public static List<Path> filesToPaths(List<File> files) {
        return map(files, File::toPath);
    }

    public static List<Path> stringsToPaths(List<String> strings) {
        return map(strings, Path::of);
    }
}
