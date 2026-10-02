package org.moera.node.rest.notification;

import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Contact;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.operations.SharingOperations;
import org.moera.node.operations.ContactOperations;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.moera.node.util.MediaTransactionTestSupport.assertInsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PostingSharedJobTest {

    @Mock
    private ContactOperations contactOperations;

    @InjectMocks
    private PostingSharedJob job;

    @Test
    void cachedContactAvatarIsAttachedBeforeDownload() throws Exception {
        MediaManager mediaManager = mock(MediaManager.class);
        SharingOperations sharingOperations = mock(SharingOperations.class);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("avatar");
        Contact contact = new Contact();
        contact.setRemoteAvatarMediaFile(mediaFile);
        org.mockito.Mockito.lenient().when(contactOperations.find("remote")).thenReturn(contact);
        doAnswer(invocation -> {
            assertSame(mediaFile, AvatarImageUtil.getMediaFile(invocation.getArgument(1)));
            return null;
        }).when(mediaManager).downloadAvatar(eq("remote"), any());

        restoredJob(mediaManager, sharingOperations).execute();
    }

    @Test
    void restoredJobDownloadsAvatarBeforeCompletingTheExistingSharing() throws Exception {
        AtomicReference<AvatarImage> written = new AtomicReference<>();
        MediaManager mediaManager = mock(MediaManager.class);
        SharingOperations sharingOperations = mock(SharingOperations.class);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("avatar");
        doAnswer(invocation -> {
            assertInsideTransaction();
            assertNull(written.get());
            AvatarImageUtil.setMediaFile(invocation.getArgument(1), mediaFile);
            return null;
        }).when(mediaManager).downloadAvatar(eq("remote"), any());
        doAnswer(invocation -> {
            assertInsideTransaction();
            assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), invocation.getArgument(0));
            assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), invocation.getArgument(1));
            written.set(invocation.getArgument(2));
            return null;
        }).when(sharingOperations).completeShared(any(), any(), any());
        PostingSharedJob job = restoredJob(mediaManager, sharingOperations);

        job.execute();

        AvatarImage avatar = written.get();
        assertEquals("avatar", avatar.getMediaId());
        assertEquals("circle", avatar.getShape());
        assertSame(mediaFile, AvatarImageUtil.getMediaFile(avatar));
    }

    @Test
    void downloadFailurePropagatesToJobWithoutCompletingSharing() throws Exception {
        AtomicReference<AvatarImage> written = new AtomicReference<>();
        MediaManager mediaManager = mock(MediaManager.class);
        SharingOperations sharingOperations = mock(SharingOperations.class);
        MoeraNodeException failure = new MoeraNodeException("remote unavailable");
        doThrow(failure).when(mediaManager).downloadAvatar(eq("remote"), any());
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            written.set(invocation.getArgument(2));
            return null;
        }).when(sharingOperations).completeShared(any(), any(), any());
        PostingSharedJob job = restoredJob(mediaManager, sharingOperations);

        assertSame(failure, assertThrows(MoeraNodeException.class, job::execute));
        assertNull(written.get());
    }

    private PostingSharedJob restoredJob(MediaManager mediaManager, SharingOperations sharingOperations)
        throws Exception {
        AvatarImage avatar = new AvatarImage();
        avatar.setMediaId("avatar");
        avatar.setShape("circle");
        PostingSharedJob.Parameters parameters = new PostingSharedJob.Parameters(
            "remote", avatar, UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002")
        );
        ObjectMapper mapper = new ObjectMapper();
        job.setParameters(mapper.writeValueAsString(parameters), mapper);
        ReflectionTestUtils.setField(job, "mediaManager", mediaManager);
        ReflectionTestUtils.setField(job, "sharingOperations", sharingOperations);
        ReflectionTestUtils.setField(job, "tx", transaction());
        return job;
    }

}
