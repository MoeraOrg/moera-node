package org.moera.node.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.body.Body;
import org.moera.node.text.TextConverter;
import org.moera.node.text.shorten.Shortener;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class PostingText {

    @Size(max = 63)
    private String ownerName;

    @Size(max = 96)
    private String ownerFullName;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    private String bodyPreview;

    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private String body;

    @Size(max = 75)
    private String bodyFormat;

    private UUID[] media;

    private Long createdAt;

    @Valid
    private AcceptedReactions acceptedReactions;

    private Boolean reactionsVisible;

    private Boolean reactionTotalsVisible;

    private List<StoryAttributes> publications;

    @Valid
    private UpdateInfo updateInfo;

    private byte[] signature;

    private short signatureVersion;

    private Map<String, Principal> operations;

    public PostingText() {
    }

    public PostingText(String ownerName, String ownerFullName, PostingSourceText sourceText,
                       TextConverter textConverter) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        ownerAvatar = sourceText.getOwnerAvatar();
        bodySrc = sourceText.getBodySrc();
        bodySrcFormat = sourceText.getBodySrcFormat() != null ? sourceText.getBodySrcFormat() : SourceFormat.PLAIN_TEXT;
        media = sourceText.getMedia() != null
                ? Arrays.stream(sourceText.getMedia()).map(MediaWithDigest::getId).toArray(UUID[]::new)
                : null;
        createdAt = Util.toEpochSecond(Util.now());
        acceptedReactions = sourceText.getAcceptedReactions();
        if (bodySrcFormat != SourceFormat.APPLICATION) {
            Body decodedBody = textConverter.toHtml(bodySrcFormat, new Body(bodySrc));
            body = decodedBody.getEncoded();
            bodyFormat = BodyFormat.MESSAGE.getValue();
            Body decodedBodyPreview = Shortener.shorten(decodedBody, false);
            if (decodedBodyPreview == null) {
                decodedBodyPreview = new Body(Body.EMPTY);
            }
            bodyPreview = decodedBodyPreview.getEncoded();
        } else {
            body = new Body(bodySrc).getEncoded();
            bodyFormat = BodyFormat.APPLICATION.getValue();
        }
        operations = sourceText.getOperations();
    }

    public void initAcceptedReactionsDefaults() {
        if (acceptedReactions == null) {
            acceptedReactions = new AcceptedReactions();
        }
        if (acceptedReactions.getPositive() == null) {
            acceptedReactions.setPositive("");
        }
        if (acceptedReactions.getNegative() == null) {
            acceptedReactions.setNegative("");
        }
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public AvatarDescription getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarDescription ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public MediaFile getOwnerAvatarMediaFile() {
        return ownerAvatarMediaFile;
    }

    public void setOwnerAvatarMediaFile(MediaFile ownerAvatarMediaFile) {
        this.ownerAvatarMediaFile = ownerAvatarMediaFile;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(String bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public UUID[] getMedia() {
        return media;
    }

    public void setMedia(UUID[] media) {
        this.media = media;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
    }

    public Boolean getReactionsVisible() {
        return reactionsVisible;
    }

    public void setReactionsVisible(Boolean reactionsVisible) {
        this.reactionsVisible = reactionsVisible;
    }

    public Boolean getReactionTotalsVisible() {
        return reactionTotalsVisible;
    }

    public void setReactionTotalsVisible(Boolean reactionTotalsVisible) {
        this.reactionTotalsVisible = reactionTotalsVisible;
    }

    public List<StoryAttributes> getPublications() {
        return publications;
    }

    public void setPublications(List<StoryAttributes> publications) {
        this.publications = publications;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public void toEntry(Entry entry) {
        if (sameAsEntry(entry)) {
            return;
        }

        entry.setEditedAt(Util.now());
        if (acceptedReactions != null) {
            if (acceptedReactions.getPositive() != null) {
                entry.setAcceptedReactionsPositive(acceptedReactions.getPositive());
            }
            if (acceptedReactions.getNegative() != null) {
                entry.setAcceptedReactionsNegative(acceptedReactions.getNegative());
            }
        }
        if (reactionsVisible != null) {
            entry.setReactionsVisible(reactionsVisible);
        }
        if (reactionTotalsVisible != null) {
            entry.setReactionTotalsVisible(reactionTotalsVisible);
        }
        if (ownerName != null) {
            entry.setOwnerName(ownerName);
        }
        if (ownerFullName != null) {
            entry.setOwnerFullName(ownerFullName);
        }
        if (ownerAvatar != null) {
            if (ownerAvatarMediaFile != null) {
                entry.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (ownerAvatar.getShape() != null) {
                entry.setOwnerAvatarShape(ownerAvatar.getShape());
            }
        }
        if (getPrincipal("view") != null) {
            entry.setViewPrincipal(getPrincipal("view"));
        }
        if (getPrincipal("viewComments") != null) {
            entry.setViewCommentsPrincipal(getPrincipal("viewComments"));
        }
        if (getPrincipal("addComment") != null) {
            entry.setAddCommentPrincipal(getPrincipal("addComment"));
        }
    }

    public boolean sameAsEntry(Entry entry) {
        return (acceptedReactions == null
                || (acceptedReactions.getPositive() == null
                        || acceptedReactions.getPositive().equals(entry.getAcceptedReactionsPositive()))
                    && (acceptedReactions.getNegative() == null
                        || acceptedReactions.getNegative().equals(entry.getAcceptedReactionsNegative())))
               && (reactionsVisible == null || reactionsVisible.equals(entry.isReactionsVisible()))
               && (reactionTotalsVisible == null || reactionTotalsVisible.equals(entry.isReactionTotalsVisible()))
               && (ownerName == null || ownerName.equals(entry.getOwnerName()))
               && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()))
               && (ownerAvatarMediaFile == null
                    || entry.getOwnerAvatarMediaFile() != null
                        && ownerAvatarMediaFile.getId().equals(entry.getOwnerAvatarMediaFile().getId()))
               && Objects.equals(getPrincipal("view"), entry.getViewPrincipal())
               && Objects.equals(getPrincipal("viewComments"), entry.getViewCommentsPrincipal())
               && Objects.equals(getPrincipal("addComment"), entry.getAddCommentPrincipal());
    }

    public void toEntryRevision(EntryRevision revision, byte[] digest, TextConverter textConverter,
                                List<MediaFileOwner> media) {
        if (createdAt != null) {
            revision.setCreatedAt(Util.toTimestamp(createdAt));
        }
        if (bodySrcFormat != null) {
            revision.setBodySrcFormat(bodySrcFormat);
        }
        revision.setSignature(signature);
        revision.setSignatureVersion(signatureVersion);
        revision.setDigest(digest);
        textConverter.toRevision(bodySrc, body, bodyFormat, bodyPreview, signature != null, media,
                false, revision);

        if (updateInfo != null) {
            if (updateInfo.getImportant() != null) {
                revision.setUpdateImportant(updateInfo.getImportant());
            }
            if (updateInfo.getDescription() != null) {
                revision.setUpdateDescription(updateInfo.getDescription());
            }
        }
    }

    public boolean sameAsRevision(EntryRevision revision) {
        return (ObjectUtils.isEmpty(bodySrcFormat) || bodySrcFormat == revision.getBodySrcFormat())
                && (ObjectUtils.isEmpty(bodySrc)
                    || (revision.getBodySrcFormat() != SourceFormat.APPLICATION
                        ? bodySrc.equals(revision.getBodySrc()) : bodySrc.equals(revision.getBody())))
                && Arrays.equals(
                        media != null ? media : new UUID[0],
                        revision.getAttachments().stream().map(EntryAttachment::getMediaFileOwner).toArray())
                && (revision.getSignature() != null || signature == null)
                && (updateInfo != null ? updateInfo.getImportant() : false) == revision.isUpdateImportant()
                && Objects.equals(
                        updateInfo != null && updateInfo.getDescription() != null ? updateInfo.getDescription() : "",
                        revision.getUpdateDescription());
    }

}
