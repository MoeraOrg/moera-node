package org.moera.node.liberin.receptor;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.FavorType;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.operations.FavorOperations;
import org.moera.node.text.MentionsExtractor;
import org.moera.node.text.PostingReferencesExtractor.Reference;
import org.springframework.util.StringUtils;

abstract class EntryReceptor extends LiberinReceptorBase {

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    protected Set<String> mentions(EntryRevision revision) {
        if (revision == null || !StringUtils.hasLength(revision.getBody())) {
            return Set.of();
        }
        return MentionsExtractor.extract(new Body(revision.getBody()));
    }

    protected void updateMentionFavors(Entry entry, Set<String> names, Set<String> previousNames) {
        if (names.equals(previousNames)) {
            return;
        }
        String ownerName = entry.getOwnerName();
        names.stream()
            .filter(name -> !name.equals(ownerName) && !previousNames.contains(name))
            .forEach(name -> favorOperations.addFavor(entry.getNodeId(), name, FavorType.MENTION));
        previousNames.stream()
            .filter(name -> !name.equals(ownerName) && !names.contains(name))
            .forEach(name -> favorOperations.addFavor(entry.getNodeId(), name, FavorType.UNMENTION));
    }

    protected void updateReferenceFavors(UUID nodeId, Set<Reference> current, Set<Reference> previous) {
        current.stream()
            .filter(reference -> !previous.contains(reference))
            .forEach(reference -> favorOperations.addFavor(nodeId, reference.nodeName(), FavorType.REFERENCE));
        previous.stream()
            .filter(reference -> !current.contains(reference))
            .forEach(reference -> favorOperations.addFavor(nodeId, reference.nodeName(), FavorType.UNREFERENCE));
    }

    protected Set<String> mentionedRecipients(Set<String> mentions, String ownerName, Principal view) {
        return mentions.stream()
            .filter(name -> !name.equals(ownerName))
            .filter(name -> canView(name, view))
            .collect(Collectors.toSet());
    }

    protected boolean canView(String nodeName, Principal view) {
        return view.includes(
            false, nodeName, () -> subscribedCache.isSubscribed(nodeName), () -> friendCache.getClientGroupIds(nodeName)
        );
    }

}
