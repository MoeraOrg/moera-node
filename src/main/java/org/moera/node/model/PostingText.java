package org.moera.node.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.sanitizer.HtmlSanitizer;
import org.moera.node.text.TextConverter;
import org.moera.node.text.shorten.Shortener;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class PostingText {

    @Size(max = 96)
    private String ownerFullName;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private UUID[] media;

    @Valid
    private AcceptedReactions acceptedReactions;

    private Boolean reactionsVisible;

    private Boolean reactionTotalsVisible;

    private List<StoryAttributes> publications;

    @Valid
    private UpdateInfo updateInfo;

    public PostingText() {
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

    public UUID[] getMedia() {
        return media;
    }

    public void setMedia(UUID[] media) {
        this.media = media;
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
    }

    public boolean sameAsEntry(Entry entry) {
        return (acceptedReactions == null
                || (acceptedReactions.getPositive() == null
                        || acceptedReactions.getPositive().equals(entry.getAcceptedReactionsPositive()))
                    && (acceptedReactions.getNegative() == null
                        || acceptedReactions.getNegative().equals(entry.getAcceptedReactionsNegative())))
               && (reactionsVisible == null || reactionsVisible.equals(entry.isReactionsVisible()))
               && (reactionTotalsVisible == null || reactionTotalsVisible.equals(entry.isReactionTotalsVisible()))
               && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()))
               && (ownerAvatarMediaFile == null
                    || entry.getOwnerAvatarMediaFile() != null
                        && ownerAvatarMediaFile.getId().equals(entry.getOwnerAvatarMediaFile().getId()));
    }

    public void toEntryRevision(EntryRevision revision, TextConverter textConverter, List<MediaFileOwner> media) {
        if (bodySrcFormat != null) {
            revision.setBodySrcFormat(bodySrcFormat);
        }

        if (!ObjectUtils.isEmpty(bodySrc)) {
            if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
                revision.setBodySrc(bodySrc);
                Body body = textConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
                revision.setBody(body.getEncoded());
                revision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false, media));
                revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
                Body bodyPreview = Shortener.shorten(body);
                if (bodyPreview != null) {
                    revision.setBodyPreview(bodyPreview.getEncoded());
                    revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(bodyPreview, true, media));
                } else {
                    revision.setBodyPreview(Body.EMPTY);
                    revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(body, true, media));
                }
                revision.setHeading(HeadingExtractor.extractHeading(body));
                revision.setDescription(HeadingExtractor.extractDescription(body));
            } else {
                revision.setBodySrc(Body.EMPTY);
                revision.setBody(bodySrc);
                revision.setBodyFormat(BodyFormat.APPLICATION.getValue());
            }
        }

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
                && (updateInfo != null ? updateInfo.getImportant() : false) == revision.isUpdateImportant()
                && Objects.equals(
                        updateInfo != null && updateInfo.getDescription() != null ? updateInfo.getDescription() : "",
                        revision.getUpdateDescription());
    }

}
