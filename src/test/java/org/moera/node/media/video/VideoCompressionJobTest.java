package org.moera.node.media.video;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.data.MediaFile;
import org.moera.node.data.PendingJob;
import org.moera.node.task.JobRetryPolicy;
import org.springframework.test.util.ReflectionTestUtils;

public class VideoCompressionJobTest {

    private static class TestVideoCompressionJob extends VideoCompressionJob {

        JobRetryPolicy retryPolicy() {
            return getRetryPolicy();
        }

    }

    @Test
    void retryPolicyAllowsInitialAttemptAndFiveRetriesAtThirtyMinutes() {
        var job = new TestVideoCompressionJob();
        JobRetryPolicy policy = job.retryPolicy();

        Assertions.assertEquals(6, ReflectionTestUtils.getField(policy, "maxCount"));
        Assertions.assertEquals(Duration.ofMinutes(30), policy.waitTime());
    }

    @Test
    void executionDeadlineIncludesFixedAndDurationBasedParts() {
        Assertions.assertEquals(Duration.ofSeconds(900), VideoCompressionJob.executionDeadline(0));
        Assertions.assertEquals(Duration.ofSeconds(1200), VideoCompressionJob.executionDeadline(20));
        Assertions.assertEquals(Duration.ofSeconds(901), VideoCompressionJob.executionDeadline(0.01f));
    }

    @Test
    void mediaFileIsCurrentOnlyForThisJobAndWithoutPublishedResult() {
        UUID jobId = UUID.randomUUID();
        var job = new TestVideoCompressionJob();
        ReflectionTestUtils.setField(job, "id", jobId);

        MediaFile mediaFile = new MediaFile();
        PendingJob pendingJob = new PendingJob();
        pendingJob.setId(jobId);
        mediaFile.setCompressionJob(pendingJob);

        Assertions.assertTrue(current(job, mediaFile));

        pendingJob.setId(UUID.randomUUID());
        Assertions.assertFalse(current(job, mediaFile));

        pendingJob.setId(jobId);
        MediaFile compressed = new MediaFile();
        mediaFile.setCompressedFile(compressed);
        Assertions.assertFalse(current(job, mediaFile));

        Assertions.assertFalse(current(job, null));
    }

    private static boolean current(VideoCompressionJob job, MediaFile mediaFile) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(job, "isCurrent", mediaFile));
    }

}
