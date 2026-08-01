package org.moera.node.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ToolRunnerTest {

    @Test
    void interruptionIsPropagatedWithoutReinterruptingThread() throws InterruptedException {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread thread = Thread.ofPlatform().start(() -> {
            Thread.currentThread().interrupt();
            try {
                ToolRunner.run(new ProcessBuilder("sleep", "30"), 30);
            } catch (Throwable e) {
                thrown.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        thread.join();

        Assertions.assertInstanceOf(InterruptedException.class, thrown.get());
        Assertions.assertFalse(interrupted.get());
    }

}
