package org.moera.node.rest.notification;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.global.UniversalContext;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Posting;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.SharingOperations;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingSharingProcessorTest {

    @Mock
    private UniversalContext universalContext;

    @Mock
    private Jobs jobs;

    @Mock
    private MediaManager mediaManager;

    @Mock
    private SharingOperations sharingOperations;

    @Spy
    private Transaction tx = new Transaction();

    @InjectMocks
    private PostingSharingProcessor processor;

    @Test
    void sharedPersistsLocalRecordAndSchedulesAvatarWorkWithoutDownloading() throws Exception {
        UUID nodeId = UUID.randomUUID();
        when(universalContext.nodeId()).thenReturn(nodeId);
        AtomicReference<Object> parameters = new AtomicReference<>();
        doAnswer(invocation -> {
            parameters.set(invocation.getArgument(1));
            return UUID.randomUUID();
        }).when(jobs).runAfterCommit(any(), any(), eq(nodeId));
        PostingSharedNotification notification = notification();
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        EntrySharing sharing = new EntrySharing();
        sharing.setId(UUID.randomUUID());
        sharing.setEntry(posting);
        when(sharingOperations.shared(notification)).thenReturn(sharing);
        // A remote outage must not delay or fail notification processing.
        org.mockito.Mockito.lenient().doThrow(new MoeraNodeException("remote unavailable"))
            .when(mediaManager).downloadAvatar(any(), any());

        assertDoesNotThrow(() -> processor.shared(notification));

        assertNotNull(parameters.get());
        var json = new ObjectMapper().valueToTree(parameters.get());
        assertEquals("remote", json.get("senderNodeName").asString());
        assertEquals(posting.getId().toString(), json.get("postingId").asString());
        assertEquals(sharing.getId().toString(), json.get("sharingId").asString());
        assertEquals("avatar", json.get("senderAvatar").get("mediaId").asString());
        verifyNoInteractions(mediaManager);
    }

    @Test
    void sharedWithoutAvatarStillSchedulesCompletion() {
        UUID nodeId = UUID.randomUUID();
        when(universalContext.nodeId()).thenReturn(nodeId);
        AtomicReference<Object> parameters = new AtomicReference<>();
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            parameters.set(invocation.getArgument(1));
            return UUID.randomUUID();
        }).when(jobs).runAfterCommit(any(), any(), eq(nodeId));
        PostingSharedNotification notification = notification();
        notification.setSenderAvatar(null);
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        EntrySharing sharing = new EntrySharing();
        sharing.setId(UUID.randomUUID());
        sharing.setEntry(posting);
        when(sharingOperations.shared(notification)).thenReturn(sharing);

        processor.shared(notification);

        assertNotNull(parameters.get());
        PostingSharedJob.Parameters scheduled = (PostingSharedJob.Parameters) parameters.get();
        assertEquals(sharing.getId(), scheduled.sharingId());
        assertNull(scheduled.senderAvatar());
    }

    @Test
    void invalidSharingDoesNotScheduleWork() {
        PostingSharedNotification notification = notification();
        notification.setRemoteStoryId(null);

        assertThrows(ValidationFailure.class, () -> processor.shared(notification));

        verifyNoInteractions(jobs, mediaManager, sharingOperations);
    }

    private static PostingSharedNotification notification() {
        PostingSharedNotification notification = new PostingSharedNotification();
        notification.setSenderNodeName("remote");
        notification.setSharingType(SharingType.REPUBLISH);
        notification.setPostingId("posting");
        notification.setRemoteFeedName("timeline");
        notification.setRemoteStoryId("story");
        notification.setRemoteStoryMoment(100L);
        AvatarImage avatar = new AvatarImage();
        avatar.setMediaId("avatar");
        notification.setSenderAvatar(avatar);
        return notification;
    }

}
