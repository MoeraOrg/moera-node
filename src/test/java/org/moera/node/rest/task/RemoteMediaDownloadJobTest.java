package org.moera.node.rest.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.MediaManager;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.RemoteMediaCacheOperations;
import org.moera.node.media.TemporaryFile;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.moera.node.util.MediaTransactionTestSupport.assertOutsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoteMediaDownloadJobTest {

    @TempDir
    Path directory;

    @Test
    void downloadingFileDoesNotHoldTransactionOrConnection() throws Exception {
        Transaction tx = transaction();
        UUID nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("local");
        NodeApi nodeApi = mock(NodeApi.class);
        MoeraNode remote = mock(MoeraNode.class);
        when(nodeApi.at(eq("remote"), any())).thenReturn(remote);
        MediaFileOwnerRepository owners = mock(MediaFileOwnerRepository.class);
        when(owners.findByFile(nodeId, "hash")).thenReturn(List.of());
        MediaOperations operations = mock(MediaOperations.class);
        Path temporary = directory.resolve("download");
        when(operations.tmpFile()).thenReturn(new TemporaryFile(temporary, Files.newOutputStream(temporary)));
        MediaManager manager = new MediaManager();
        ReflectionTestUtils.setField(manager, "tx", tx);
        ReflectionTestUtils.setField(manager, "universalContext", context);
        ReflectionTestUtils.setField(manager, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(manager, "mediaFileOwnerRepository", owners);
        ReflectionTestUtils.setField(manager, "mediaOperations", operations);
        ReflectionTestUtils.setField(manager, "remoteMediaCacheRepository", mock(RemoteMediaCacheRepository.class));
        ReflectionTestUtils.setField(manager, "remoteMediaCacheOperations", mock(RemoteMediaCacheOperations.class));
        MoeraNodeException failure = new MoeraNodeException("Download interrupted");
        doAnswer(invocation -> {
            assertOutsideTransaction();
            throw failure;
        }).when(remote).getPrivateMedia(eq("media"), isNull(), isNull(), isNull(), isNull(), any());

        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        info.setId("media");
        info.setHash("hash");
        RemoteMediaDownloadJob job = new RemoteMediaDownloadJob() {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        RemoteMediaDownloadJob.State state = new RemoteMediaDownloadJob.State();
        state.setMediaInfo(info);
        ReflectionTestUtils.setField(job, "state", state);
        ReflectionTestUtils.setField(job, "parameters", new RemoteMediaDownloadJob.Parameters("remote", "media", null));
        ReflectionTestUtils.setField(job, "tx", tx);
        ReflectionTestUtils.setField(job, "mediaManager", manager);
        ReflectionTestUtils.setField(job, "universalContext", context);

        assertSame(failure, assertThrows(MoeraNodeException.class, job::execute));
        assertOutsideTransaction();
    }

}
