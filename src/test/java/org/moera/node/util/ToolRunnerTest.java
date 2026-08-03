package org.moera.node.util;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ToolRunnerTest {

    @Test
    void progressHeartbeatsKeepProcessAlive() throws Exception {
        var result = ToolRunner.runWithProgress(
            shell("i=0; while [ $i -lt 5 ]; do echo progress=continue; i=$((i + 1)); sleep 0.04; done"),
            Duration.ofMillis(100),
            Duration.ofSeconds(2),
            Duration.ofMillis(30),
            1024
        );

        Assertions.assertEquals(ToolRunner.StopReason.NONE, result.stopReason());
        Assertions.assertEquals(0, result.exitCode());
        Assertions.assertTrue(result.stdout().contains("progress=continue"));
    }

    @Test
    void processWithoutProgressIsStopped() throws Exception {
        var result = ToolRunner.runWithProgress(
            shell("while :; do :; done"),
            Duration.ofMillis(80),
            Duration.ofSeconds(5),
            Duration.ofMillis(30),
            1024
        );

        Assertions.assertEquals(ToolRunner.StopReason.NO_PROGRESS, result.stopReason());
        Assertions.assertNotEquals(0, result.exitCode());
    }

    @Test
    void deadlineForcesProcessThatIgnoresNormalTermination() throws Exception {
        var result = ToolRunner.runWithProgress(
            shell("trap '' TERM; while :; do echo progress=continue; done"),
            Duration.ofSeconds(2),
            Duration.ofMillis(100),
            Duration.ofMillis(30),
            1024
        );

        Assertions.assertEquals(ToolRunner.StopReason.DEADLINE, result.stopReason());
        Assertions.assertNotEquals(0, result.exitCode());
    }

    @Test
    void diagnosticsAreBoundedToTheirTail() throws Exception {
        var result = ToolRunner.runWithProgress(
            shell("printf 1234567890; printf abcdefghij >&2"),
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            Duration.ofMillis(30),
            5
        );

        Assertions.assertTrue(result.stdout().length() <= 5);
        Assertions.assertTrue(result.stdout().endsWith("7890\n"));
        Assertions.assertEquals("fghij", result.stderr());
    }

    @Test
    void interruptionIsPropagatedWithoutReinterruptingThread() throws InterruptedException {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread thread = Thread.ofPlatform().start(() -> {
            Thread.currentThread().interrupt();
            try {
                ToolRunner.run(new ProcessBuilder("sleep", "30"), Duration.ofSeconds(30));
            } catch (Throwable e) {
                thrown.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        thread.join();

        Assertions.assertInstanceOf(InterruptedException.class, thrown.get());
        Assertions.assertFalse(interrupted.get());
    }

    @Test
    void progressRunnerInterruptionIsPropagatedWithoutReinterruptingThread() throws InterruptedException {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread thread = Thread.ofPlatform().start(() -> {
            Thread.currentThread().interrupt();
            try {
                ToolRunner.runWithProgress(
                    shell("while :; do :; done"),
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(30),
                    Duration.ofMillis(30),
                    1024
                );
            } catch (Throwable e) {
                thrown.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        thread.join();

        Assertions.assertInstanceOf(InterruptedException.class, thrown.get());
        Assertions.assertFalse(interrupted.get());
    }

    private static ProcessBuilder shell(String command) {
        return new ProcessBuilder("sh", "-c", command);
    }

}
