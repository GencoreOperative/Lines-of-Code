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

import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Main {
    private static final String PROGRAM_NAME = "loc-by-blame";

    @Parameter(names = {"-n", "--name"}, description = "Author name to count lines for", required = true)
    private String user;

    @Parameter(names = {"-r", "--repo"}, description = "Path to the git repository", required = true)
    private String repo;

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help = false;

    public static void main(String... args) throws IOException {
        Main main = new Main();
        JCommander commander = new JCommander(main);
        commander.setProgramName(PROGRAM_NAME);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            commander.usage();
            System.exit(1);
        }
        if (main.help) {
            commander.usage();
            System.exit(0);
        }

        AtomicInteger totalLines = new AtomicInteger(0);
        AtomicInteger userLines = new AtomicInteger(0);
        File sourceFolder = new File(main.repo);
        String user = main.user;
        Files.walk(sourceFolder.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .parallel()
                .forEach(path -> {
                    try {
                        Map<String, Integer> map = countNames(sourceFolder, path.toString());

                        // Compute the total lines from the count
                        int fileTotal = total(map.values());
                        totalLines.getAndAdd(fileTotal);

                        // If the user had lines in this file, add and log
                        if (map.containsKey(user)) {
                            userLines.addAndGet(map.get(user));
                            System.out.println(format("{0}\t{1}\t{2}",
                                    Integer.toString(map.get(user)),
                                    Integer.toString(fileTotal),
                                    path));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        System.out.println(format("User: {0}\nTotal: {1}", userLines, totalLines));
    }

    private static int total(Collection<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    private static Map<String, Integer> countNames(File folder, String fileName) throws IOException {
        Map<String, Integer> counts = new HashMap<>();

        // How to set working directory?
        ProcessBuilder pb = new ProcessBuilder("git", "blame", "--line-porcelain", fileName);
        pb.directory(folder);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            reader.lines()
                    .filter(line -> line.startsWith("author "))
                    .map(line -> line.substring(7))
                    .forEach(name -> counts.put(name, counts.getOrDefault(name, 0) + 1));
        }
        return counts;
    }
}
