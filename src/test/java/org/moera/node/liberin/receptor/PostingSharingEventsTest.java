package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.EntrySharingRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.instant.PostingSharingInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingSharingAddedLiberin;
import org.moera.node.liberin.model.PostingSharingDeletedLiberin;
import org.moera.node.model.event.Event;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostingSharingEventsTest {

    @Test
    void addedEventCarriesSharingAndPostingIdWithoutPostingPayload() {
        Fixture fixture = new Fixture();
        when(fixture.postingRepository.findByNodeIdAndIdForUpdate(null, fixture.posting.getId()))
            .thenReturn(Optional.of(fixture.posting));
        when(fixture.entrySharingRepository.findByEntryIdAndId(fixture.posting.getId(), fixture.sharing.getId()))
            .thenReturn(Optional.of(fixture.sharing));

        fixture.receptor.added(new PostingSharingAddedLiberin(fixture.posting, fixture.sharing));

        assertEquals(1, fixture.receptor.events.size());
        Event event = fixture.receptor.events.getFirst();
        JsonNode json = new ObjectMapper().valueToTree(event);
        assertEquals("POSTING_SHARING_ADDED", json.get("type").asString());
        assertEquals(fixture.posting.getId().toString(), json.get("postingId").asString());
        assertEquals(fixture.sharing.getId().toString(), json.get("sharing").get("id").asString());
        assertEquals("republish", json.get("sharing").get("sharingType").asString());
        assertEquals("remote", json.get("sharing").get("remoteNodeName").asString());
        assertEquals("story", json.get("sharing").get("remoteStoryId").asString());
        assertEquals(fixture.sharing.getMoment(), json.get("sharing").get("moment").asLong());
        assertEquals(3, json.size());
        assertFalse(json.has("posting"));
        assertViewingPermissions(event);
        verify(fixture.instants).added(fixture.posting, fixture.sharing);
    }

    @Test
    void deletedEventCarriesOnlyPostingAndSharingIds() {
        Fixture fixture = new Fixture();
        List<UUID> substoryIds = List.of(UUID.randomUUID());

        fixture.receptor.deleted(new PostingSharingDeletedLiberin(fixture.posting, fixture.sharing, substoryIds));

        assertEquals(1, fixture.receptor.events.size());
        Event event = fixture.receptor.events.getFirst();
        JsonNode json = new ObjectMapper().valueToTree(event);
        assertEquals("POSTING_SHARING_DELETED", json.get("type").asString());
        assertEquals(fixture.posting.getId().toString(), json.get("postingId").asString());
        assertEquals(fixture.sharing.getId().toString(), json.get("sharingId").asString());
        assertEquals(3, json.size());
        assertViewingPermissions(event);
        verify(fixture.instants).deleted(fixture.posting.getId(), substoryIds);
    }

    @Test
    void delayedAdditionDoesNotAnnounceAnAlreadyDeletedSharing() {
        Fixture fixture = new Fixture();
        when(fixture.postingRepository.findByNodeIdAndIdForUpdate(null, fixture.posting.getId()))
            .thenReturn(Optional.of(fixture.posting));
        when(fixture.entrySharingRepository.findByEntryIdAndId(fixture.posting.getId(), fixture.sharing.getId()))
            .thenReturn(Optional.empty());

        fixture.receptor.added(new PostingSharingAddedLiberin(fixture.posting, fixture.sharing));

        assertTrue(fixture.receptor.events.isEmpty());
    }

    private static void assertViewingPermissions(Event event) {
        assertTrue(event.getFilter().includes(false, "viewer", true, new String[0]));
        assertFalse(event.getFilter().includes(false, "viewer", false, new String[0]));
        assertFalse(event.getFilter().includes(false, "other", true, new String[0]));
        assertFalse(event.getFilter().includes(false, null, true, new String[0]));
    }

    private static class Fixture {

        private final TestReceptor receptor = new TestReceptor();
        private final PostingRepository postingRepository = mock(PostingRepository.class);
        private final EntrySharingRepository entrySharingRepository = mock(EntrySharingRepository.class);
        private final PostingSharingInstants instants = mock(PostingSharingInstants.class);
        private final Posting posting = new Posting();
        private final EntrySharing sharing = new EntrySharing();

        private Fixture() {
            posting.setId(UUID.randomUUID());
            posting.setViewPrincipal(Principal.ofNode("viewer"));
            posting.setViewSharingsPrincipal(Principal.SUBSCRIBED);
            sharing.setId(UUID.randomUUID());
            sharing.setEntry(posting);
            sharing.setSharingType(SharingType.REPUBLISH);
            sharing.setRemoteNodeName("remote");
            sharing.setRemoteStoryId("story");
            sharing.setMoment(123L);
            ReflectionTestUtils.setField(receptor, "postingRepository", postingRepository);
            ReflectionTestUtils.setField(receptor, "entrySharingRepository", entrySharingRepository);
            ReflectionTestUtils.setField(receptor, "postingSharingInstants", instants);
        }

    }

    private static class TestReceptor extends PostingSharingReceptor {

        private final List<Event> events = new ArrayList<>();

        @Override
        protected void send(Liberin liberin, Event event) {
            events.add(event);
        }

    }

}
