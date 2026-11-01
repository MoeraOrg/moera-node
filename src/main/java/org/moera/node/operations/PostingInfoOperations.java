package org.moera.node.operations;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.model.ClientReactionInfoUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.RemotePostingOrNodeUtil;
import org.moera.node.userlist.SheriffUserListOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class PostingInfoOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private LinkedEntryRepository linkedEntryRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private EntryOperations entryOperations;

    @Inject
    private DirectServeOperations directServeOperations;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private BlockedByUserOperations blockedByUserOperations;

    @Inject
    private SheriffUserListOperations sheriffUserListOperations;

    public PostingInfo withLinkedPostings(PostingInfo postingInfo) {
        fillLinkedPostings(List.of(postingInfo));
        return postingInfo;
    }

    public void fillLinkedPostings(Collection<PostingInfo> postings) {
        if (postings.isEmpty()) {
            return;
        }

        var revisionIds = postings.stream()
            .map(PostingInfo::getRevisionId)
            .map(UUID::fromString)
            .collect(Collectors.toSet());

        Map<UUID, Set<UUID>> byRevision = new LinkedHashMap<>();
        Set<UUID> loadedIds = new HashSet<>();
        List<Posting> visible = new ArrayList<>();
        while (!revisionIds.isEmpty()) {
            revisionIds.forEach(id -> byRevision.put(id, new LinkedHashSet<>()));
            var links = linkedEntryRepository.findResolvedByRevisionIds(requestContext.nodeId(), revisionIds);
            for (var link : links) {
                byRevision.get(link.getEntryRevision().getId()).add(link.getLinkedEntry().getId());
            }
            var targetIds = links.stream()
                .map(link -> link.getLinkedEntry().getId())
                .filter(loadedIds::add)
                .collect(Collectors.toSet());
            List<Posting> next = targetIds.isEmpty()
                ? List.of()
                : postingRepository
                    .findFullByNodeIdAndIds(requestContext.nodeId(), targetIds).stream()
                    .filter(posting -> posting.getCurrentRevision() != null)
                    .filter(posting -> requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT))
                    .toList();
            visible.addAll(next);
            revisionIds = next.stream()
                .filter(posting -> !isMediaPosting(posting))
                .map(Posting::getCurrentRevision)
                .map(EntryRevision::getId)
                .filter(id -> !byRevision.containsKey(id))
                .collect(Collectors.toSet());
        }

        Map<UUID, PostingInfo> targets = buildLinkedPostings(visible);
        var mediaIds = visible.stream()
            .filter(PostingInfoOperations::isMediaPosting)
            .map(Posting::getId)
            .map(UUID::toString)
            .collect(Collectors.toSet());
        for (PostingInfo posting : postings) {
            expandLinkedPostings(posting, byRevision, targets, mediaIds);
        }
    }

    private static boolean isMediaPosting(Posting posting) {
        return posting.getParentMedia() != null || posting.getParentRemoteMedia() != null
            || posting.getParentMediaEntry() != null;
    }

    private Map<UUID, PostingInfo> buildLinkedPostings(List<Posting> visible) {
        Map<UUID, PostingInfo> targets = new LinkedHashMap<>();
        if (visible.isEmpty()) {
            return targets;
        }
        var stories = storyRepository.findByEntryIds(
            requestContext.nodeId(), visible.stream().map(Posting::getId).toList()
        ).stream().collect(Collectors.groupingBy(story -> story.getEntry().getId()));
        var attachments = entryOperations.prepareMediaAttachments(
            visible.stream().map(Posting::getCurrentRevision).toList()
        );
        for (Posting posting : visible) {
            targets.put(
                posting.getId(),
                PostingInfoUtil.build(
                    posting,
                    stories.getOrDefault(posting.getId(), List.of()),
                    attachments,
                    requestContext,
                    requestContext.getOptions(),
                    directServeOperations
                )
            );
        }
        fillClientReactions(targets.values());
        fillBlockings(targets.values());
        sheriffUserListOperations.fillSheriffListMarks(targets.values());
        return targets;
    }

    private void expandLinkedPostings(
        PostingInfo root,
        Map<UUID, Set<UUID>> byRevision,
        Map<UUID, PostingInfo> targets,
        Set<String> mediaIds
    ) {
        if (mediaIds.contains(root.getId())) {
            root.setLinkedPostings(null);
            return;
        }

        Set<String> ancestors = new HashSet<>();
        var stack = new ArrayDeque<LinkedPostingsFrame>();
        ancestors.add(root.getId());
        root.setLinkedPostings(new ArrayList<>());
        stack.push(new LinkedPostingsFrame(
            root, byRevision.getOrDefault(UUID.fromString(root.getRevisionId()), Set.of()).iterator()
        ));
        while (!stack.isEmpty()) {
            LinkedPostingsFrame frame = stack.peek();
            if (!frame.targetIds().hasNext()) {
                ancestors.remove(frame.posting().getId());
                stack.pop();
                continue;
            }
            PostingInfo target = targets.get(frame.targetIds().next());
            if (target == null || ancestors.contains(target.getId())) {
                continue;
            }
            // Each occurrence has its own descendants; cached metadata is complete and no longer mutated.
            PostingInfo linked = target.clone();
            frame.posting().getLinkedPostings().add(linked);
            if (mediaIds.contains(linked.getId())) {
                continue;
            }
            ancestors.add(linked.getId());
            linked.setLinkedPostings(new ArrayList<>());
            stack.push(new LinkedPostingsFrame(
                linked, byRevision.getOrDefault(UUID.fromString(linked.getRevisionId()), Set.of()).iterator()
            ));
        }
    }

    public PostingInfo withClientReaction(PostingInfo postingInfo) {
        fillClientReactions(List.of(postingInfo));
        return postingInfo;
    }

    private void fillClientReactions(Collection<PostingInfo> postings) {
        String clientName = requestContext.isOwner()
            ? requestContext.nodeName()
            : requestContext.getClientName(Scope.IDENTIFY);
        if (ObjectUtils.isEmpty(clientName)) {
            return;
        }

        boolean viewContent = requestContext.isOwner()
            ? requestContext.isAdmin(Scope.VIEW_CONTENT)
            : requestContext.hasClientScope(Scope.VIEW_CONTENT);

        var originals = postings.stream()
            .filter(PostingInfoUtil::isOriginal)
            .collect(Collectors.toMap(posting -> UUID.fromString(posting.getId()), Function.identity()));
        if (!originals.isEmpty()) {
            reactionRepository.findByEntryIdsAndOwner(originals.keySet(), clientName).stream()
                .filter(reaction -> reaction.getViewE().isPublic() || viewContent)
                .forEach(reaction ->
                    originals.get(reaction.getEntryRevision().getEntry().getId())
                        .setClientReaction(ClientReactionInfoUtil.build(reaction))
                );
        }

        var remote = postings.stream().filter(posting -> !PostingInfoUtil.isOriginal(posting)).toList();
        if (!remote.isEmpty() && requestContext.isAdmin(Scope.VIEW_CONTENT)) {
            // OwnReaction does not store the view principal, so only the administrator can see it.
            var reactions = ownReactionRepository.findAllByRemotePostingIds(
                requestContext.nodeId(), remote.stream().map(PostingInfo::getReceiverPostingId).distinct().toList()
            ).stream().collect(Collectors.toMap(
                reaction -> new RemotePosting(reaction.getRemoteNodeName(), reaction.getRemotePostingId()),
                Function.identity()
            ));
            for (PostingInfo posting : remote) {
                OwnReaction reaction = reactions.get(new RemotePosting(
                    posting.getReceiverName(), posting.getReceiverPostingId()
                ));
                posting.setClientReaction(reaction != null ? ClientReactionInfoUtil.build(reaction) : null);
            }
        }
    }

    public PostingInfo withBlockings(PostingInfo postingInfo) {
        fillBlockings(List.of(postingInfo));
        return postingInfo;
    }

    private void fillBlockings(Collection<PostingInfo> postings) {
        String clientName = requestContext.getClientName(Scope.IDENTIFY);
        if (!requestContext.isOwner() && !ObjectUtils.isEmpty(clientName)) {
            var originals = postings.stream().filter(PostingInfoUtil::isOriginal).collect(Collectors.toMap(
                posting -> UUID.fromString(posting.getId()), Function.identity()
            ));
            if (!originals.isEmpty()) {
                var blockings = blockedUserOperations.search(
                    requestContext.nodeId(),
                    List.of(BlockedOperation.COMMENT, BlockedOperation.REACTION),
                    clientName,
                    originals.keySet(),
                    null,
                    null,
                    false
                );
                for (var blocking : blockings) {
                    if (blocking.isGlobal()) {
                        originals.values().forEach(posting ->
                            PostingInfoUtil.putBlockedOperation(posting, blocking.getBlockedOperation())
                        );
                    } else {
                        PostingInfo posting = originals.get(blocking.getEntry().getId());
                        if (posting != null) {
                            PostingInfoUtil.putBlockedOperation(posting, blocking.getBlockedOperation());
                        }
                    }
                }
            }
        }

        if (requestContext.isOwner() && requestContext.isAdmin(Scope.VIEW_PEOPLE)) {
            var remote = postings.stream().filter(posting -> !PostingInfoUtil.isOriginal(posting)).toList();
            if (remote.isEmpty()) {
                return;
            }
            var blockings = blockedByUserOperations.search(
                requestContext.nodeId(),
                List.of(BlockedOperation.COMMENT, BlockedOperation.REACTION),
                remote.stream().map(posting -> RemotePostingOrNodeUtil.build(
                    posting.getReceiverName(), posting.getReceiverPostingId()
                )).toList(),
                false
            );
            for (var blocking : blockings) {
                remote.stream()
                    .filter(posting -> Objects.equals(blocking.getRemoteNodeName(), posting.getReceiverName()))
                    .filter(posting -> blocking.isGlobal()
                        || Objects.equals(blocking.getRemotePostingId(), posting.getReceiverPostingId()))
                    .forEach(posting -> PostingInfoUtil.putBlockedOperation(posting, blocking.getBlockedOperation()));
            }
        }
    }

    public PostingInfo withClientInfo(PostingInfo postingInfo) {
        fillClientReactions(List.of(postingInfo));
        fillBlockings(List.of(postingInfo));
        sheriffUserListOperations.fillSheriffListMarks(postingInfo);
        return postingInfo;
    }

    private record RemotePosting(String nodeName, String postingId) {
    }

    private record LinkedPostingsFrame(PostingInfo posting, Iterator<UUID> targetIds) {
    }

}
