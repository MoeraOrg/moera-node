package org.moera.node.userlist;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.SheriffMark;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.UserListItem;
import org.moera.node.data.UserListItemRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.PostingInfoUtil;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SheriffUserListOperationsTest {

    private final UUID nodeId = UUID.randomUUID();
    private final SheriffUserListOperations operations = new SheriffUserListOperations();
    private final UserListItemRepository userListItemRepository = mock(UserListItemRepository.class);
    private final RemoteUserListItemRepository remoteUserListItemRepository = mock(RemoteUserListItemRepository.class);

    @BeforeEach
    void setUp() {
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("local_0");
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "userListItemRepository", userListItemRepository);
        ReflectionTestUtils.setField(operations, "remoteUserListItemRepository", remoteUserListItemRepository);
    }

    @Test
    void targetsWithDifferentSheriffsShareLookupsWithoutSharingMarks() {
        PostingInfo first = posting("author_0", List.of("local_0", "remote_0", "remote_0"));
        PostingInfo second = posting("author_0", List.of("other_0"));
        PostingInfo third = posting("absent_0", List.of("remote_0"));
        UserListItem local = new UserListItem();
        local.setNodeName("author_0");
        RemoteUserListItem marked = new RemoteUserListItem();
        marked.setNodeName("author_0");
        RemoteUserListItem absent = new RemoteUserListItem();
        absent.setNodeName("absent_0");
        absent.setAbsent(true);
        when(userListItemRepository.findByListAndNodeNames(nodeId, UserList.SHERIFF_HIDE, Set.of("author_0")))
            .thenReturn(List.of(local));
        when(remoteUserListItemRepository.findByListAndNodeNames(
            nodeId, "remote_0", UserList.SHERIFF_HIDE, Set.of("author_0", "absent_0")
        )).thenReturn(List.of(marked, absent));

        operations.fillSheriffListMarks(List.of(first, second, third));

        assertEquals(Set.of("local_0", "remote_0"), first.getSheriffMarks().stream()
            .map(SheriffMark::getSheriffName).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, first.getSheriffMarks().size());
        assertNull(second.getSheriffMarks());
        assertNull(third.getSheriffMarks());
        verify(userListItemRepository).findByListAndNodeNames(nodeId, UserList.SHERIFF_HIDE, Set.of("author_0"));
        verify(remoteUserListItemRepository).findByListAndNodeNames(
            nodeId, "remote_0", UserList.SHERIFF_HIDE, Set.of("author_0", "absent_0")
        );
        verify(remoteUserListItemRepository).findByListAndNodeNames(
            nodeId, "other_0", UserList.SHERIFF_HIDE, Set.of("author_0")
        );
        verifyNoMoreInteractions(userListItemRepository, remoteUserListItemRepository);
    }

    @Test
    void unreferencedTargetsAndLocalOwnerDoNotTriggerLookups() {
        PostingInfo unreferenced = posting("author_0", List.of("remote_0"));
        PostingInfoUtil.setSheriffUserListReferred(unreferenced, false);

        operations.fillSheriffListMarks(List.of(unreferenced, posting("local_0", List.of("remote_0"))));

        verifyNoInteractions(userListItemRepository, remoteUserListItemRepository);
    }

    private static PostingInfo posting(String ownerName, List<String> sheriffs) {
        PostingInfo info = new PostingInfo();
        info.setOwnerName(ownerName);
        info.setSheriffs(sheriffs);
        PostingInfoUtil.setSheriffUserListReferred(info, true);
        return info;
    }

}
