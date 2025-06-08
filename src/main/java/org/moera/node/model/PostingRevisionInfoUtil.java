package org.moera.node.model;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.util.Util;

public class PostingRevisionInfoUtil {

    public static PostingRevisionInfo build(
        Posting posting,
        EntryRevision revision,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        String receiverName,
        AccessChecker accessChecker
    ) {
        PostingRevisionInfo info = new PostingRevisionInfo();
        
        info.setId(revision.getId().toString());
        info.setReceiverId(revision.getReceiverRevisionId());
        info.setBodyPreview(new Body(revision.getBodyPreview()));
        info.setBodySrcHash(revision.getReceiverBodySrcHash() != null
            ? revision.getReceiverBodySrcHash()
            : CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc())));
        info.setBodySrcFormat(revision.getBodySrcFormat());
        info.setBody(new Body(revision.getBody()));
        info.setBodyFormat(BodyFormat.forValue(revision.getBodyFormat()));
        info.setMedia(mediaAttachmentsProvider.getMediaAttachments(revision, receiverName));
        info.setHeading(revision.getHeading());
        info.setDescription(revision.getDescription());
        if (!UpdateInfoUtil.isEmpty(revision)) {
            info.setUpdateInfo(UpdateInfoUtil.build(revision));
        }
        info.setCreatedAt(Util.toEpochSecond(revision.getCreatedAt()));
        info.setDeletedAt(Util.toEpochSecond(revision.getDeletedAt()));
        info.setReceiverCreatedAt(Util.toEpochSecond(revision.getReceiverCreatedAt()));
        info.setReceiverDeletedAt(Util.toEpochSecond(revision.getReceiverDeletedAt()));
        info.setDigest(revision.getDigest());
        info.setSignature(revision.getSignature());
        info.setSignatureVersion(revision.getSignatureVersion());
        info.setReactions(ReactionTotalsInfoUtil.build(revision.getReactionTotals(), posting, accessChecker));

        return info;
    }

}
