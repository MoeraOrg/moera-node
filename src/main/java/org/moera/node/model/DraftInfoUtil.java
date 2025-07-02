package org.moera.node.model;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.DraftInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryAttachment;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DraftInfoUtil {

    private static final Logger log = LoggerFactory.getLogger(DraftInfoUtil.class);

    public static DraftInfo build(Draft draft) {
        DraftInfo draftInfo = new DraftInfo();
        
        draftInfo.setId(draft.getId().toString());
        draftInfo.setDraftType(draft.getDraftType());
        draftInfo.setReceiverName(draft.getReceiverName());
        draftInfo.setReceiverPostingId(draft.getReceiverPostingId());
        draftInfo.setReceiverCommentId(draft.getReceiverCommentId());
        draftInfo.setRepliedToId(draft.getRepliedToId());
        draftInfo.setCreatedAt(Util.toEpochSecond(draft.getCreatedAt()));
        draftInfo.setEditedAt(Util.toEpochSecond(draft.getEditedAt()));
        draftInfo.setDeadline(Util.toEpochSecond(draft.getDeadline()));
        draftInfo.setOwnerFullName(draft.getOwnerFullName());

        if (draft.getOwnerAvatarMediaFile() != null) {
            draftInfo.setOwnerAvatar(
                AvatarImageUtil.build(draft.getOwnerAvatarMediaFile(), draft.getOwnerAvatarShape())
            );
        }

        draftInfo.setRejectedReactions(
            RejectedReactionsUtil.build(draft.getRejectedReactionsPositive(), draft.getRejectedReactionsNegative())
        );
        draftInfo.setCommentRejectedReactions(
            RejectedReactionsUtil.build(
                draft.getChildRejectedReactionsPositive(),
                draft.getChildRejectedReactionsNegative()
            )
        );

        draftInfo.setBodySrc(new Body(draft.getBodySrc()));
        draftInfo.setBodySrcFormat(draft.getBodySrcFormat());
        draftInfo.setBody(new Body(draft.getBody()));
        draftInfo.setBodyFormat(BodyFormat.forValue(draft.getBodyFormat()));

        draftInfo.setMedia(
            draft.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, null))
                .collect(Collectors.toList())
        );

        draftInfo.setHeading(draft.getHeading());
        draftInfo.setPublishAt(Util.toEpochSecond(draft.getPublishAt()));

        if (!UpdateInfoUtil.isEmpty(draft)) {
            draftInfo.setUpdateInfo(UpdateInfoUtil.build(draft));
        }

        try {
            draftInfo.setOperations(new ObjectMapper().readValue(draft.getOperations(), PostingOperations.class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Draft.operations", e);
        }

        try {
            draftInfo.setCommentOperations(
                new ObjectMapper().readValue(draft.getChildOperations(), CommentOperations.class)
            );
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Draft.childOperations", e);
        }

        return draftInfo;
    }

}
