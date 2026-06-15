/*
 * MIT License
 *
 * Copyright (c) 2017 GencoreOperative
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
package uk.org.gencoreoperative.commits;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Git {
    private static final String GIT = "/usr/bin/git";
    private final RunProcess run;

    public Git(File repoDir) {
        this.run = new RunProcess(repoDir);
    }


    private List<String> getAllCommits(String user) {
        return run.run(GIT, "log", "--no-merges", "--oneline", "--author=" + user)
                .map(l -> l.split("\\s+")[0])
                .collect(Collectors.toList());
    }

    private Result getResult(String revision) {
        // Process Date
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        Optional<Date> date = run.run(GIT, "show", revision)
                .filter(l -> l.startsWith("Date:"))
                .map(l -> l.substring("Date:".length()))
                .map(String::trim)
                .map(l -> parse(format, l))
                .findAny();
        if (!date.isPresent()) throw new IllegalStateException();

        // Process Additions
        long additions = run.run(GIT, "show", revision)
                .map(String::trim)
                .filter(l -> l.startsWith("+"))
                .count();

        // Process Deletions
        long deletions = run.run(GIT, "show", revision)
                .map(String::trim)
                .filter(l -> l.startsWith("-"))
                .count();
        return new Result(date.get(), additions, deletions);
    }

    public List<Result> getAllResults(String user) {
        List<String> allCommits = getAllCommits(user);
        return allCommits.stream().map(this::getResult).collect(Collectors.toList());
    }

    private static Date parse(SimpleDateFormat format, String s) {
        try {
            return format.parse(s);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }
}
