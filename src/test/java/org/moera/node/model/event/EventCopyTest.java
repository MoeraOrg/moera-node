package org.moera.node.model.event;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SharingInfo;
import org.moera.lib.node.types.SheriffComplaintGroupInfo;
import org.moera.lib.node.types.SheriffComplaintInfo;
import org.moera.lib.node.types.principal.Principal;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class EventCopyTest {

    @ParameterizedTest
    @MethodSource("eventsWithDifferentConstructorParameterTypes")
    void copiesPayloadUsingItsPropertiesInsteadOfDomainConstructorTypes(Event event) {
        ObjectMapper objectMapper = new ObjectMapper();
        var originalPayload = objectMapper.valueToTree(event);

        Event copy = event.copy();

        assertNotSame(event, copy);
        assertEquals(originalPayload, objectMapper.valueToTree(copy));
    }

    private static Stream<Event> eventsWithDifferentConstructorParameterTypes() {
        UUID postingId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sharingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        SharingInfo sharing = new SharingInfo();
        sharing.setId(sharingId.toString());

        NodeNameChangedEvent nodeName = new NodeNameChangedEvent();
        nodeName.setAvatar(new AvatarImage());

        SheriffComplaintInfo complaint = new SheriffComplaintInfo();
        complaint.setId(sharingId.toString());
        SheriffComplaintAddedEvent complaintAdded = new SheriffComplaintAddedEvent();
        complaintAdded.setComplaint(complaint);
        complaintAdded.setGroupId(postingId.toString());

        SheriffComplaintGroupInfo group = new SheriffComplaintGroupInfo();
        group.setId(postingId.toString());
        SheriffComplaintGroupAddedEvent groupAdded = new SheriffComplaintGroupAddedEvent();
        groupAdded.setGroup(group);
        SheriffComplaintGroupUpdatedEvent groupUpdated = new SheriffComplaintGroupUpdatedEvent();
        groupUpdated.setGroup(group);

        return Stream.of(
            new PostingSharingAddedEvent(postingId, sharing, Principal.PUBLIC),
            new PostingSharingDeletedEvent(postingId, sharingId, Principal.PUBLIC),
            nodeName,
            complaintAdded,
            groupAdded,
            groupUpdated
        );
    }

}
