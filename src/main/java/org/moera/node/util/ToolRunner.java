package org.moera.node.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolRunner.class);

    private static final Duration DESTROY_GRACE_PERIOD = Duration.ofSeconds(2);
    private static final int MAX_OUTPUT_LENGTH = 64 * 1024;
    private static final int MAX_ERROR_LOG_LENGTH = 1024;

    public enum StopReason {
        NONE,
        NO_PROGRESS,
        DEADLINE
    }

    public record ToolResult(int exitCode, String stdout, String stderr, StopReason stopReason) {

        public boolean exitedWithError() {
            return stopReason == StopReason.NONE && exitCode != 0;
        }

    }

    public static ToolResult run(
        ProcessBuilder processBuilder,
        Duration timeout
    ) throws IOException, InterruptedException {
        log.info("Running tool: {}", formatCommand(processBuilder));
        Process process = processBuilder.start();
        long startedAt = Util.milliTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() -> {
                try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            });

            Future<String> stderr = executor.submit(() -> {
                try (BufferedReader reader = process.errorReader(StandardCharsets.UTF_8)) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            });

            try {
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!finished) {
                    terminate(process, DESTROY_GRACE_PERIOD);
                }
                long finishedAt = Util.milliTime();
                StopReason stopReason = finished ? StopReason.NONE : StopReason.DEADLINE;
                ToolResult result = new ToolResult(
                    process.exitValue(), getOutput(stdout), getOutput(stderr), stopReason
                );
                logResult(result, startedAt, finishedAt);
                return result;
            } catch (InterruptedException e) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                throw e;
            }
        }
    }

    public static ToolResult runWithProgress(
        ProcessBuilder processBuilder,
        Duration noProgressTimeout,
        Duration totalTimeout
    ) throws IOException, InterruptedException {
        return runWithProgress(
            processBuilder,
            noProgressTimeout,
            totalTimeout,
            DESTROY_GRACE_PERIOD,
            MAX_OUTPUT_LENGTH
        );
    }

    static ToolResult runWithProgress(
        ProcessBuilder processBuilder,
        Duration noProgressTimeout,
        Duration totalTimeout,
        Duration destroyGracePeriod,
        int maxOutputLength
    ) throws IOException, InterruptedException {
        log.info("Running tool: {}", formatCommand(processBuilder));
        Process process = processBuilder.start();
        long startedAt = Util.milliTime();
        AtomicLong lastProgressAt = new AtomicLong(startedAt);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() -> readProgressOutput(
                process, lastProgressAt, maxOutputLength
            ));
            Future<String> stderr = executor.submit(() -> readBoundedOutput(
                process.getErrorStream(), maxOutputLength
            ));

            StopReason stopReason = StopReason.NONE;
            try {
                long pollMillis = Math.clamp(
                    Math.min(noProgressTimeout.toMillis(), totalTimeout.toMillis()) / 4,
                    1,
                    1000
                );
                while (!process.waitFor(pollMillis, TimeUnit.MILLISECONDS)) {
                    long now = Util.milliTime();
                    if (now - lastProgressAt.get() > noProgressTimeout.toMillis()) {
                        stopReason = StopReason.NO_PROGRESS;
                        terminate(process, destroyGracePeriod);
                        break;
                    }
                    if (now - startedAt > totalTimeout.toMillis()) {
                        stopReason = StopReason.DEADLINE;
                        terminate(process, destroyGracePeriod);
                        break;
                    }
                }

                long finishedAt = Util.milliTime();
                ToolResult result = new ToolResult(
                    process.exitValue(), getOutput(stdout), getOutput(stderr), stopReason
                );
                logResult(result, startedAt, finishedAt);
                return result;
            } catch (InterruptedException e) {
                terminate(process, destroyGracePeriod);
                throw e;
            }
        }
    }

    private static String readProgressOutput(Process process, AtomicLong lastProgressAt, int maxOutputLength) {
        StringBuilder output = new StringBuilder();
        try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendBounded(output, line + "\n", maxOutputLength);
                if (line.startsWith("progress=") && line.length() > "progress=".length()) {
                    lastProgressAt.set(Util.milliTime());
                }
            }
        } catch (IOException e) {
            // Destroying a process may close its pipes before the process has fully terminated.
        }
        return output.toString();
    }

    private static String readBoundedOutput(InputStream input, int maxOutputLength) {
        StringBuilder output = new StringBuilder();
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int length;
            while ((length = reader.read(buffer)) >= 0) {
                appendBounded(output, new String(buffer, 0, length), maxOutputLength);
            }
        } catch (IOException e) {
            // Destroying a process may close its pipes before the process has fully terminated.
        }
        return output.toString();
    }

    private static void appendBounded(StringBuilder output, String value, int maxOutputLength) {
        output.append(value);
        if (output.length() > maxOutputLength) {
            output.delete(0, output.length() - maxOutputLength);
        }
    }

    private static void terminate(Process process, Duration gracePeriod) throws InterruptedException {
        if (!process.isAlive()) {
            return;
        }
        process.destroy();
        if (!process.waitFor(gracePeriod.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            process.waitFor();
        }
    }

    private static String getOutput(Future<String> output) throws IOException, InterruptedException {
        try {
            return output.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UncheckedIOException cause) {
                throw cause.getCause();
            }
            if (e.getCause() instanceof IOException cause) {
                throw cause;
            }
            throw new IOException("Could not read tool output", e.getCause());
        }
    }

    private static String formatCommand(ProcessBuilder processBuilder) {
        return processBuilder.command().stream()
            .map(ToolRunner::quoteArgument)
            .collect(Collectors.joining(" "));
    }

    private static String quoteArgument(String argument) {
        if (argument.matches("[A-Za-z0-9_./:=+@%-]+")) {
            return argument;
        }

        return "'" + argument.replace("'", "'\\''") + "'";
    }

    private static void logResult(ToolResult result, long startedAt, long finishedAt) {
        double elapsedSeconds = (finishedAt - startedAt) / 1000.0;
        log.info(
            "Tool finished in {} seconds: {}",
            String.format(Locale.ROOT, "%.3f", elapsedSeconds), stopReason(result)
        );

        if (result.stopReason() != StopReason.NONE || result.exitedWithError()) {
            String stderr = result.stderr();
            log.error("Tool stderr: {}", stderr.substring(Math.max(0, stderr.length() - MAX_ERROR_LOG_LENGTH)));
        }
    }

    private static String stopReason(ToolResult result) {
        return switch (result.stopReason()) {
            case NONE -> "exit code " + result.exitCode();
            case NO_PROGRESS -> "no progress timeout";
            case DEADLINE -> "execution timeout";
        };
    }

}
