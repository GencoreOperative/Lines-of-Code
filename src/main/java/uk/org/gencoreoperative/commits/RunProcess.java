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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simplified approach to executing operating system commands.
 *
 * {@link RunProcess} will manage the complexity of invoking a system process
 * and provide the response in a convenient format.
 */
public class RunProcess {

    private final ExecutorService service = Executors.newCachedThreadPool();
    private final File workingDir;

    public RunProcess(File workingDir) {
        this.workingDir = workingDir;
        Runtime.getRuntime().addShutdownHook(new Thread(service::shutdown));
    }
    /**
     * This operation defers to {@link RunProcess#run(boolean, String...)} with
     * merge set to false.
     *
     * @param args Non null arguments starting with the command to execute.
     * @return A non null but possibly empty list of the lines of output.
     */
    public Stream<String> run(String... args) {
        return run(false, args);
    }

    /**
     * Invokes the requested command, passing each argument to the command in order.
     *
     * Note: Whilst this implementation indicates it returns a Stream, it actually collects
     * the result as a list first before streaming. One day this will be upgraded to the
     * superior behaviour of streaming...
     *
     * @param mergeStdoutError If true, stdout will be merged with stderr.
     * @param args The first argument must be the executable, all subsequent arguments are
     *             parameters to pass to the executable.
     * @return Non null Stream of the lines of output, possibly empty.
     */
    public Stream<String> run(boolean mergeStdoutError, String... args) {
        final Process p;
        try {
            p = new ProcessBuilder(args).directory(workingDir).redirectErrorStream(mergeStdoutError).start();
        } catch (IOException e) {
            throw new ProcessException("Failed to create process", e);
        }

        try {
            FutureTask<List<String>> stdout = getTask(p.getInputStream());
            FutureTask<List<String>> stderr = getTask(p.getErrorStream());
            service.execute(stdout);
            service.execute(stderr);

            p.waitFor();
            List<String> errors = stderr.get();
            if (!mergeStdoutError && !errors.isEmpty()) {
                throw new ExecutionException(errors.stream().collect(Collectors.joining("\n")), null);
            }
            return stdout.get().stream();
        } catch (InterruptedException|ExecutionException e) {
            throw new ProcessException("Error whilst waiting", e);
        }
    }

    /**
     * A {@link java.util.concurrent.Future} implementation that will read the provided input stream.
     * @param inputStream Non null stream.
     * @return A {@link FutureTask} which can be executed in an {@link ExecutorService}.
     */
    private FutureTask<List<String>> getTask(final InputStream inputStream) {
        return new FutureTask<>(() -> {
            List<String> results = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
                String data = "";
                while (data != null) {
                    data = reader.readLine();
                    if (data != null) results.add(data);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return results;
        });
    }

    public static class ProcessException extends RuntimeException {
        public ProcessException(String s, Exception e) {
            super(s, e);
        }
    }
}
