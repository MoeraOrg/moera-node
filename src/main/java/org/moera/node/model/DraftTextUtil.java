package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.DraftText;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Draft;
import org.moera.node.data.MediaFile;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.MediaExtractor;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class DraftTextUtil {

    private static final Logger log = LoggerFactory.getLogger(DraftTextUtil.class);

    public static void toDraft(DraftText draftText, Draft draft, TextConverter textConverter) {
        draft.setDraftType(draftText.getDraftType());
        draft.setEditedAt(Util.now());
        draft.setRepliedToId(draftText.getRepliedToId());
        if (draftText.getOwnerFullName() != null) {
            draft.setOwnerFullName(draftText.getOwnerFullName());
        }
        if (draftText.getOwnerAvatar() != null) {
            MediaFile ownerAvatarMediaFile = AvatarDescriptionUtil.getMediaFile(draftText.getOwnerAvatar());
            if (ownerAvatarMediaFile != null) {
                draft.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (draftText.getOwnerAvatar().getShape() != null) {
                draft.setOwnerAvatarShape(draftText.getOwnerAvatar().getShape());
            }
        }

        if (draftText.getRejectedReactions() != null) {
            if (draftText.getRejectedReactions().getPositive() != null) {
                draft.setRejectedReactionsPositive(draftText.getRejectedReactions().getPositive());
            }
            if (draftText.getRejectedReactions().getNegative() != null) {
                draft.setRejectedReactionsNegative(draftText.getRejectedReactions().getNegative());
            }
        }
        if (draftText.getCommentRejectedReactions() != null) {
            if (draftText.getCommentRejectedReactions().getPositive() != null) {
                draft.setChildRejectedReactionsPositive(draftText.getCommentRejectedReactions().getPositive());
            }
            if (draftText.getCommentRejectedReactions().getNegative() != null) {
                draft.setChildRejectedReactionsNegative(draftText.getCommentRejectedReactions().getNegative());
            }
        }
        if (draftText.getBodySrcFormat() != null) {
            draft.setBodySrcFormat(draftText.getBodySrcFormat());
        }

        if (!ObjectUtils.isEmpty(draftText.getBodySrc())) {
            if (draft.getBodySrcFormat() != SourceFormat.APPLICATION) {
                draft.setBodySrc(draftText.getBodySrc().getEncoded());
                Body body = textConverter.toHtml(draft.getBodySrcFormat(), draftText.getBodySrc());
                draft.setBody(body.getEncoded());
                draft.setBodyFormat(BodyFormat.MESSAGE.getValue());
                String heading = HeadingExtractor.extractHeading(body, null, true);
                if (ObjectUtils.isEmpty(heading)) {
                    boolean hasGallery = hasAttachedGallery(body, draftText.getMedia());
                    if (hasGallery) {
                        heading = HeadingExtractor.EMOJI_PICTURE;
                    }
                }
                draft.setHeading(heading);
            } else {
                draft.setBodySrc(Body.EMPTY);
                draft.setBody(draftText.getBodySrc().getEncoded());
                draft.setBodyFormat(BodyFormat.APPLICATION.getValue());
            }
        }

        if (draftText.getPublishAt() != null) {
            draft.setPublishAt(Util.toTimestamp(draftText.getPublishAt()));
        }

        if (draftText.getUpdateInfo() != null) {
            if (draftText.getUpdateInfo().getImportant() != null) {
                draft.setUpdateImportant(draftText.getUpdateInfo().getImportant());
            }
            if (draftText.getUpdateInfo().getDescription() != null) {
                draft.setUpdateDescription(draftText.getUpdateInfo().getDescription());
            }
        }

        if (draftText.getOperations() != null) {
            try {
                draft.setOperations(new ObjectMapper().writeValueAsString(draftText.getOperations()));
            } catch (JacksonException e) {
                log.error("Error serializing DraftText.operations", e);
            }
        }

        if (draftText.getCommentOperations() != null) {
            try {
                draft.setChildOperations(new ObjectMapper().writeValueAsString(draftText.getCommentOperations()));
            } catch (JacksonException e) {
                log.error("Error serializing DraftText.commentOperations", e);
            }
        }
    }

    private static boolean hasAttachedGallery(Body body, List<RemoteMedia> media) {
        if (ObjectUtils.isEmpty(media)) {
            return false;
        }
        int embeddedCount = MediaExtractor.extractMediaFileIds(body).size();
        return media.size() > embeddedCount;
    }

}
