package org.moera.node.model;

import java.util.Map;
import java.util.UUID;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryType;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

public class ReactionInfoUtil {

    public static ReactionInfo build(Reaction reaction, AccessChecker accessChecker) {
        ReactionInfo reactionInfo = new ReactionInfo();
        
        reactionInfo.setOwnerName(reaction.getOwnerName());
        reactionInfo.setOwnerFullName(reaction.getOwnerFullName());
        reactionInfo.setOwnerGender(reaction.getOwnerGender());
        
        if (reaction.getOwnerAvatarMediaFile() != null) {
            reactionInfo.setOwnerAvatar(AvatarImageUtil.build(reaction.getOwnerAvatarMediaFile(), 
                    reaction.getOwnerAvatarShape()));
        }

        EntryRevision entryRevision = reaction.getEntryRevision();
        Entry entry = entryRevision.getEntry();
        if (entry.getEntryType() == EntryType.POSTING) {
            reactionInfo.setPostingId(entry.getId().toString());
            reactionInfo.setPostingRevisionId(entryRevision.getId().toString());
        } else {
            reactionInfo.setCommentId(entry.getId().toString());
            reactionInfo.setCommentRevisionId(entryRevision.getId().toString());
            reactionInfo.setPostingId(entry.getParent().getId().toString());
        }

        reactionInfo.setNegative(reaction.isNegative());
        reactionInfo.setEmoji(reaction.getEmoji());
        reactionInfo.setMoment(reaction.getMoment());
        reactionInfo.setCreatedAt(Util.toEpochSecond(reaction.getCreatedAt()));
        reactionInfo.setDeadline(Util.toEpochSecond(reaction.getDeadline()));
        reactionInfo.setSignature(reaction.getSignature());
        reactionInfo.setSignatureVersion(reaction.getSignatureVersion());

        ReactionOperations operations = new ReactionOperations();
        operations.setView(reaction.getViewCompound(), Principal.PUBLIC);
        operations.setDelete(reaction.getDeleteCompound(), Principal.PRIVATE);
        reactionInfo.setOperations(operations);

        if (accessChecker.isPrincipal(reaction.getViewOperationsE(), Scope.VIEW_CONTENT)) {
            ReactionOperations ownerOperations = new ReactionOperations();
            ownerOperations.setView(reaction.getViewPrincipal(), Principal.PUBLIC);
            ownerOperations.setDelete(reaction.getDeletePrincipal(), Principal.PRIVATE);
            reactionInfo.setOwnerOperations(ownerOperations);

            if (reaction.getEntryRevision().getEntry().getParent() == null) {
                ReactionOperations seniorOperations = new ReactionOperations();
                seniorOperations.setView(reaction.getPostingViewPrincipal(), Principal.UNSET);
                seniorOperations.setDelete(reaction.getPostingViewPrincipal(), Principal.UNSET);
                reactionInfo.setSeniorOperations(seniorOperations);
            } else {
                ReactionOperations seniorOperations = new ReactionOperations();
                seniorOperations.setView(reaction.getCommentViewPrincipal(), Principal.UNSET);
                seniorOperations.setDelete(reaction.getCommentDeletePrincipal(), Principal.UNSET);
                reactionInfo.setSeniorOperations(seniorOperations);

                ReactionOperations majorOperations = new ReactionOperations();
                majorOperations.setView(reaction.getPostingViewPrincipal(), Principal.UNSET);
                majorOperations.setDelete(reaction.getPostingDeletePrincipal(), Principal.UNSET);
                reactionInfo.setMajorOperations(majorOperations);
            }
        }

        return reactionInfo;
    }

    public static ReactionInfo ofPosting(UUID postingId) {
        ReactionInfo reactionInfo = new ReactionInfo();
        reactionInfo.setPostingId(postingId.toString());
        return reactionInfo;
    }

    public static ReactionInfo ofComment(UUID commentId) {
        ReactionInfo reactionInfo = new ReactionInfo();
        reactionInfo.setCommentId(commentId.toString());
        return reactionInfo;
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public static void toOwnReaction(ReactionInfo info, OwnReaction ownReaction) {
        ownReaction.setRemotePostingId(info.getPostingId());
        ownReaction.setNegative(Boolean.TRUE.equals(info.getNegative()));
        ownReaction.setEmoji(info.getEmoji());
        ownReaction.setCreatedAt(Util.now());
    }

}
