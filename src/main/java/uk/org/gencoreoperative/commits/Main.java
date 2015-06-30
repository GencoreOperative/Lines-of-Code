/*
 * MIT License
 *
 * Copyright (c) 2015 GencoreOperative
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 *
 */
public class Main {
    public static final String PROGRAM_NAME = "loc-by-user";
    @Parameter (names = {"-u", "--url"}, description = "SVN repository URL", required = false)
    private String url;
    @Parameter (names ={"-n", "--name"}, description = "The user to generate stats for", required = true)
    private String user;
    @Parameter (names = {"-t", "--timeout"}, description = "How long to wait (in minutes) for git diff results", required = false)
    private int timeout = 10;
    @Parameter (names = {"-h", "--help"}, help = true)
    private boolean help = false;

    private static final Git git = new Git();


    public static void main(String... args) {
        Main main = new Main();
        JCommander commander = new JCommander(main);
        commander.setProgramName(PROGRAM_NAME);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            help(commander);
        }

        if (main.help) {
            help(commander);
        }

        List<Result> results = git.getAllResults(main.user);

        results.sort(Comparator.comparing(Result::getDate));

        Map<String, Long> addedByMonth = new LinkedHashMap<>();
        Map<String, Long> deletedByMonth = new LinkedHashMap<>();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM");

        // Fill in the blanks
        Date end = results.get(results.size()-1).getDate();
        Date cursor = results.get(0).getDate();
        Calendar cal = Calendar.getInstance();
        while (cursor.before(end)) {
            cal.setTime(cursor);
            String key = format.format(cursor);
            if (!addedByMonth.containsKey(key)) {
                addedByMonth.put(key, 0l);
                deletedByMonth.put(key, 0l);
            }
            cal.add(Calendar.MONTH, 1);
            cursor.setTime(cal.getTimeInMillis());
        }

        // Collect up results
        for (Result r : results) {
            String key = format.format(r.getDate());
            addToMap(addedByMonth, key, r.getAdded());
            addToMap(deletedByMonth, key, r.getDeleted());
        }

        // Format for display
        System.out.println("Month\tAdded\tDeleted");
        List<String> strings = new ArrayList<>(addedByMonth.keySet());
        for (String key : strings) {
            System.out.println(key + "\t" + addedByMonth.get(key) + "\t" + deletedByMonth.get(key));
        }

        System.exit(0);
    }

    private static void help(JCommander commander) {
        commander.usage();
        System.exit(0);
    }

    private static void addToMap(Map<String, Long> map, String key, long n) {
        long t = 0;
        if (map.containsKey(key)) {
            t = map.get(key);
        }
        map.put(key, t + n);
    }
}