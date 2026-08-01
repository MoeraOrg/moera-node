package org.moera.node.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ToolRunner {

    public record ToolResult(int exitCode, String stdout, String stderr, boolean timeout) {
    }

    public static ToolResult run(
        ProcessBuilder processBuilder,
        int timeoutSec
    ) throws IOException, InterruptedException {
        Process process = processBuilder.start();

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
                boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);

                if (!finished) {
                    process.destroy();

                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                        process.waitFor();
                    }
                }

                return new ToolResult(process.exitValue(), getOutput(stdout), getOutput(stderr), !finished);
            } catch (InterruptedException e) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                throw e;
            }
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

}
