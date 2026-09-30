package org.moera.node.rest.task;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.types.FeedSliceInfo;
import org.moera.node.api.node.NodeApi;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteFeedFetchJobTest {

    @Test
    void initialFeedFetchRequestsFiveStories() throws Exception {
        NodeApi nodeApi = mock(NodeApi.class);
        MoeraNode remote = mock(MoeraNode.class);
        when(nodeApi.at("remote")).thenReturn(remote);
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setStories(List.of());
        when(remote.getFeedSlice(eq("timeline"), isNull(), isNull(), anyInt())).thenReturn(sliceInfo);

        RemoteFeedFetchJob job = new RemoteFeedFetchJob();
        ReflectionTestUtils.setField(job, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(
            job, "parameters", new RemoteFeedFetchJob.Parameters("news", "remote", "timeline")
        );

        job.execute();

        verify(remote).getFeedSlice("timeline", null, null, 5);
    }

}
