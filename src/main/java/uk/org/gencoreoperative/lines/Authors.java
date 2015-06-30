/*
 * MIT License
 *
 * Copyright (c) 2026 GencoreOperative
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package uk.org.gencoreoperative.lines;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Authors {
    private static final String PROGRAM_NAME = "loc-authors";

    @Parameter(names = {"-r", "--repo"}, description = "Path to the git repository", required = true)
    private String repo;

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help = false;

    public static void main(String... args) throws IOException {
        Authors authors = new Authors();
        JCommander commander = new JCommander(authors);
        commander.setProgramName(PROGRAM_NAME);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            commander.usage();
            System.exit(1);
        }
        if (authors.help) {
            commander.usage();
            System.exit(0);
        }

        File sourceFolder = new File(authors.repo);
        Set<String> names = Collections.synchronizedSortedSet(new TreeSet<>());
        Files.walk(sourceFolder.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .parallel()
                .forEach(path -> {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("git", "blame", "--line-porcelain", path.toString());
                        pb.directory(sourceFolder);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                            reader.lines()
                                    .filter(line -> line.startsWith("author "))
                                    .map(line -> line.substring(7))
                                    .forEach(names::add);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        names.forEach(System.out::println);
    }
}
