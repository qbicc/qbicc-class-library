/*
 * This code is based on the OpenJDK build process defined in `make/gensrc/GensrcLocaleData.gmk`, which contains the following
 * copyright notice:
 *
 * #
 * # Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static cc.quarkus.qccrt.mojo.MojoUtil.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.quarkus.qccrt.annotation.Tracking;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 */
@Mojo(name = "generate-locale-data", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Tracking("make/gensrc/GensrcLocaleData.gmk")
public class GenerateLocaleDataMojo extends AbstractMojo {

    @Parameter(required = true)
    List<File> sourcePaths;

    @Parameter(required = true)
    List<String> baseLocales;

    @Parameter(required = true)
    List<String> nonBaseLocales;

    @Parameter(required = true)
    File templateFile;

    @Parameter
    File baseLocaleDataFile;

    @Parameter
    File nonBaseLocaleDataFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> baseLocales = new HashSet<>(this.baseLocales);
        HashMap<String, String> baseArgs = new HashMap<>(Map.of(
            "#warn This file is preprocessed before being compiled", "// -- This file was mechanically generated: Do not edit! -- //",
            "#Lang#", "Base",
            "#Package#", "sun.util.locale.provider"
        ));
        HashMap<String, String> nonBaseArgs = new HashMap<>(Map.of(
            "#warn This file is preprocessed before being compiled", "// -- This file was mechanically generated: Do not edit! -- //",
            "#Lang#", "NonBase",
            "#Package#", "sun.util.resources.provider"
        ));
        try {
            List<Path> localeFiles = findFiles(filesToPaths(sourcePaths), stringsToPaths(List.of(
                "FormatData_*.java", "FormatData_*.properties",
                "CollationData_*.java", "CollationData_*.properties",
                "TimeZoneNames_*.java", "TimeZoneNames_*.properties",
                "LocaleNames_*.java", "LocaleNames_*.properties",
                "CurrencyNames_*.java", "CurrencyNames_*.properties",
                "CalendarData_*.java", "CalendarData_*.properties",
                "BreakIteratorInfo_*.java", "BreakIteratorRules_*.java"
            )));
            Set<String> localeResources = localeFiles.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.replaceAll("\\.(properties|java)$", ""))
                .collect(Collectors.toCollection(TreeSet::new));
            Set<String> allBaseLocales = new TreeSet<>();
            Set<String> allNonBaseLocales = new TreeSet<>(nonBaseLocales);
            captureLocale("FormatData", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("CollationData", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("BreakIteratorInfo", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("BreakIteratorRules", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("TimeZoneNames", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("LocaleNames", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("CurrencyNames", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            captureLocale("CalendarData", localeResources, baseLocales, allBaseLocales, allNonBaseLocales);
            baseArgs.put("#AvailableLocales_Locale#", String.join(" ", allBaseLocales));
            nonBaseArgs.put("#AvailableLocales_Locale#", String.join(" ", allNonBaseLocales));
            Path templatePath = templateFile.toPath();
            if (baseLocaleDataFile != null) {
                processFile(templatePath, baseLocaleDataFile.toPath(), baseArgs);
            }
            if (nonBaseLocaleDataFile != null) {
                processFile(templatePath, nonBaseLocaleDataFile.toPath(), nonBaseArgs);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Mojo failed: " + e, e);
        }
    }

    private void processFile(Path template, Path output, Map<String, String> replacements) throws MojoFailureException {
        Pattern pattern = Pattern.compile(replacements.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|")));
        try (BufferedReader br = Files.newBufferedReader(template)) {
            Files.createDirectories(output.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(output)) {
                String line = br.readLine();
                if (line != null) {
                    StringBuilder buf = new StringBuilder();
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
            throw new MojoFailureException("Failed to perform I/O operation: " + e);
        }
    }

    private void captureLocale(String name, Set<String> localeResources, Set<String> baseLocales, Set<String> allBaseLocales, Set<String> allNonBaseLocales) {
        String prefix = name + "_";
        // $1_LOCALES := $$(subst _,-,$$(filter-out $1, $$(subst $1_,,$$(filter $1_%, $(LOCALE_RESOURCES)))))
        Set<String> ourLocales = localeResources.stream()
            .filter(s -> s.startsWith(prefix))
            .map(s -> s.substring(prefix.length()))
            .filter(s -> ! s.equals(name))
            .map(s -> s.replace('_', '-'))
            .collect(Collectors.toCollection(TreeSet::new));
        // $1_BASE_LOCALES := $$(filter $(BASE_LOCALES), $$($1_LOCALES))
        Set<String> ourBaseLocales = ourLocales.stream()
            .filter(baseLocales::contains)
            .collect(Collectors.toCollection(TreeSet::new));
        // $1_NON_BASE_LOCALES := $$(filter-out $(BASE_LOCALES), $$($1_LOCALES))
        Set<String> ourNonBaseLocales = Stream.concat(
            ourLocales.stream()
                .filter(s -> !baseLocales.contains(s))
                // Special handling for Chinese locales to include implicit scripts
                // $1_NON_BASE_LOCALES := $$(subst zh-CN,zh-CN$$(SPACE)zh-Hans-CN, $$($1_NON_BASE_LOCALES))
                .flatMap(s -> s.equals("zh-CN") ? Stream.of(s, "zh-Hans-CN") : Stream.of(s))
                // $1_NON_BASE_LOCALES := $$(subst zh-SG,zh-SG$$(SPACE)zh-Hans-SG, $$($1_NON_BASE_LOCALES))
                .flatMap(s -> s.equals("zh-SG") ? Stream.of(s, "zh-Hans-SG") : Stream.of(s))
                // $1_NON_BASE_LOCALES := $$(subst zh-HK,zh-HK$$(SPACE)zh-Hant-HK, $$($1_NON_BASE_LOCALES))
                .flatMap(s -> s.equals("zh-HK") ? Stream.of(s, "zh-Hant-HK") : Stream.of(s))
                // $1_NON_BASE_LOCALES := $$(subst zh-MO,zh-MO$$(SPACE)zh-Hant-MO, $$($1_NON_BASE_LOCALES))
                .flatMap(s -> s.equals("zh-MO") ? Stream.of(s, "zh-Hant-MO") : Stream.of(s))
                // $1_NON_BASE_LOCALES := $$(subst zh-TW,zh-TW$$(SPACE)zh-Hant-TW, $$($1_NON_BASE_LOCALES))
                .flatMap(s -> s.equals("zh-CN") ? Stream.of(s, "zh-Hant-TW") : Stream.of(s)),
            // Adding implicit locales nb nn-NO and nb-NO
            // $1_NON_BASE_LOCALES += nb  nn-NO  nb-NO
            Stream.of("nb", "nn-NO", "nb-NO")
        ).collect(Collectors.toCollection(TreeSet::new));
        allBaseLocales.addAll(ourBaseLocales);
        allNonBaseLocales.addAll(ourNonBaseLocales);
    }
}
