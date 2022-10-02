package org.moera.node.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BodyFormat;
import org.moera.node.data.ChildOperations;
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

    @Size(max = 31)
    private String ownerGender;

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

    private List<StoryAttributes> publications;

    @Valid
    private UpdateInfo updateInfo;

    private byte[] signature;

    private short signatureVersion;

    private Map<String, Principal> operations;

    private Map<String, Principal> commentOperations;

    private Map<String, Principal> reactionOperations;

    private Map<String, Principal> commentReactionOperations;

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
        commentOperations = sourceText.getCommentOperations();
        reactionOperations = sourceText.getReactionOperations();
        commentReactionOperations = sourceText.getCommentReactionOperations();
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

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
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

    public Map<String, Principal> getCommentOperations() {
        return commentOperations;
    }

    public void setCommentOperations(Map<String, Principal> commentOperations) {
        this.commentOperations = commentOperations;
    }

    public Principal getCommentPrincipal(String operationName) {
        return commentOperations != null ? commentOperations.get(operationName) : null;
    }

    public Map<String, Principal> getReactionOperations() {
        return reactionOperations;
    }

    public void setReactionOperations(Map<String, Principal> reactionOperations) {
        this.reactionOperations = reactionOperations;
    }

    public Principal getReactionPrincipal(String operationName) {
        return reactionOperations != null ? reactionOperations.get(operationName) : null;
    }

    public Map<String, Principal> getCommentReactionOperations() {
        return commentReactionOperations;
    }

    public void setCommentReactionOperations(Map<String, Principal> commentReactionOperations) {
        this.commentReactionOperations = commentReactionOperations;
    }

    public Principal getCommentReactionPrincipal(String operationName) {
        return commentReactionOperations != null ? commentReactionOperations.get(operationName) : null;
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
        if (ownerName != null) {
            entry.setOwnerName(ownerName);
        }
        if (ownerFullName != null) {
            entry.setOwnerFullName(ownerFullName);
        }
        if (ownerGender != null) {
            entry.setOwnerGender(ownerGender);
        }
        if (ownerAvatar != null) {
            if (ownerAvatarMediaFile != null) {
                entry.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (ownerAvatar.getShape() != null) {
                entry.setOwnerAvatarShape(ownerAvatar.getShape());
            }
        }
        if (entry.getParentMedia() == null) {
            toPrincipal(this::getPrincipal, "view", entry::setViewPrincipal);
            toPrincipal(this::getPrincipal, "viewComments", entry::setViewCommentsPrincipal);
            toPrincipal(this::getPrincipal, "addComment", entry::setAddCommentPrincipal);
            toPrincipal(this::getPrincipal, "viewReactions", entry::setViewReactionsPrincipal);
            toPrincipal(this::getPrincipal, "viewNegativeReactions", entry::setViewNegativeReactionsPrincipal);
            toPrincipal(this::getPrincipal, "viewReactionTotals", entry::setViewReactionTotalsPrincipal);
            toPrincipal(this::getPrincipal, "viewNegativeReactionTotals", entry::setViewNegativeReactionTotalsPrincipal);
            toPrincipal(this::getPrincipal, "viewReactionRatios", entry::setViewReactionRatiosPrincipal);
            toPrincipal(this::getPrincipal, "viewNegativeReactionRatios", entry::setViewNegativeReactionRatiosPrincipal);
            toPrincipal(this::getPrincipal, "addReaction", entry::setAddReactionPrincipal);
            toPrincipal(this::getPrincipal, "addNegativeReaction", entry::setAddNegativeReactionPrincipal);

            ChildOperations ops = entry.getChildOperations();
            toPrincipal(this::getCommentPrincipal, "view", ops::setView);
            toPrincipal(this::getCommentPrincipal, "edit", ops::setEdit);
            toPrincipal(this::getCommentPrincipal, "delete", ops::setDelete);
            toPrincipal(this::getCommentPrincipal, "viewReactions", ops::setViewReactions);
            toPrincipal(this::getCommentPrincipal, "viewNegativeReactions", ops::setViewNegativeReactions);
            toPrincipal(this::getCommentPrincipal, "viewReactionTotals", ops::setViewReactionTotals);
            toPrincipal(this::getCommentPrincipal, "viewNegativeReactionTotals", ops::setViewNegativeReactionTotals);
            toPrincipal(this::getCommentPrincipal, "viewReactionRatios", ops::setViewReactionRatios);
            toPrincipal(this::getCommentPrincipal, "viewNegativeReactionRatios", ops::setViewNegativeReactionRatios);
            toPrincipal(this::getCommentPrincipal, "addReaction", ops::setAddReaction);
            toPrincipal(this::getCommentPrincipal, "addNegativeReaction", ops::setAddNegativeReaction);
            toPrincipal(this::getCommentPrincipal, "overrideReaction", ops::setOverrideReaction);

            ops = entry.getReactionOperations();
            toPrincipal(this::getReactionPrincipal, "view", ops::setView);
            toPrincipal(this::getReactionPrincipal, "delete", ops::setDelete);

            ops = entry.getChildReactionOperations();
            toPrincipal(this::getCommentReactionPrincipal, "view", ops::setView);
            toPrincipal(this::getCommentReactionPrincipal, "delete", ops::setDelete);
        }
    }

    private void toPrincipal(Function<String, Principal> getPrincipal, String operationName,
                             Consumer<Principal> setPrincipal) {
        Principal value = getPrincipal.apply(operationName);
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

    public boolean sameAsEntry(Entry entry) {
        return (acceptedReactions == null
                || (acceptedReactions.getPositive() == null
                        || acceptedReactions.getPositive().equals(entry.getAcceptedReactionsPositive()))
                    && (acceptedReactions.getNegative() == null
                        || acceptedReactions.getNegative().equals(entry.getAcceptedReactionsNegative())))
               && (ownerName == null || ownerName.equals(entry.getOwnerName()))
               && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()))
               && (ownerGender == null || ownerGender.equals(entry.getOwnerGender()))
               && (ownerAvatarMediaFile == null
                    || entry.getOwnerAvatarMediaFile() != null
                        && ownerAvatarMediaFile.getId().equals(entry.getOwnerAvatarMediaFile().getId()))
               && samePrincipalAs("view", entry.getViewPrincipal())
               && samePrincipalAs("viewComments", entry.getViewCommentsPrincipal())
               && samePrincipalAs("addComment", entry.getAddCommentPrincipal())
               && samePrincipalAs("viewReactions", entry.getViewReactionsPrincipal())
               && samePrincipalAs("viewNegativeReactions", entry.getViewNegativeReactionsPrincipal())
               && samePrincipalAs("viewReactionTotals", entry.getViewReactionTotalsPrincipal())
               && samePrincipalAs("viewNegativeReactionTotals", entry.getViewNegativeReactionTotalsPrincipal())
               && samePrincipalAs("viewReactionRatios", entry.getViewReactionRatiosPrincipal())
               && samePrincipalAs("viewNegativeReactionRatios", entry.getViewNegativeReactionRatiosPrincipal())
               && samePrincipalAs("addReaction", entry.getAddReactionPrincipal())
               && samePrincipalAs("addNegativeReaction", entry.getAddNegativeReactionPrincipal())
               && sameCommentPrincipalAs("view", entry.getChildOperations().getView())
               && sameCommentPrincipalAs("edit", entry.getChildOperations().getEdit())
               && sameCommentPrincipalAs("delete", entry.getChildOperations().getDelete())
               && sameCommentPrincipalAs("viewReactions", entry.getChildOperations().getViewReactions())
               && sameCommentPrincipalAs("viewNegativeReactions", entry.getChildOperations().getViewNegativeReactions())
               && sameCommentPrincipalAs("viewReactionTotals", entry.getChildOperations().getViewReactionTotals())
               && sameCommentPrincipalAs("viewNegativeReactionTotals",
                                         entry.getChildOperations().getViewNegativeReactionTotals())
               && sameCommentPrincipalAs("viewReactionRatios", entry.getChildOperations().getViewReactionRatios())
               && sameCommentPrincipalAs("viewNegativeReactionRatios",
                                         entry.getChildOperations().getViewNegativeReactionRatios())
               && sameCommentPrincipalAs("addReaction", entry.getChildOperations().getAddReaction())
               && sameCommentPrincipalAs("addNegativeReaction", entry.getChildOperations().getAddNegativeReaction())
               && sameCommentPrincipalAs("overrideReaction", entry.getChildOperations().getOverrideReaction())
               && sameReactionPrincipalAs("view", entry.getReactionOperations().getView())
               && sameReactionPrincipalAs("delete", entry.getReactionOperations().getDelete())
               && sameCommentReactionPrincipalAs("view", entry.getChildReactionOperations().getView())
               && sameCommentReactionPrincipalAs("delete", entry.getChildReactionOperations().getDelete());
    }

    public boolean sameViewComments(Entry entry) {
        return samePrincipalAs("view", entry.getViewPrincipal())
                && samePrincipalAs("viewComments", entry.getViewCommentsPrincipal());
    }

    private boolean samePrincipalAs(String operationName, Principal principal) {
        Principal value = getPrincipal(operationName);
        return value == null || Objects.equals(value, principal);
    }

    private boolean sameCommentPrincipalAs(String operationName, Principal principal) {
        Principal value = getCommentPrincipal(operationName);
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

    private boolean sameReactionPrincipalAs(String operationName, Principal principal) {
        Principal value = getReactionPrincipal(operationName);
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

    private boolean sameCommentReactionPrincipalAs(String operationName, Principal principal) {
        Principal value = getCommentReactionPrincipal(operationName);
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
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
                && (media == null
                    || Arrays.equals(
                        media,
                        revision.getAttachments().stream().map(EntryAttachment::getMediaFileOwner).toArray()))
                && !(revision.getSignature() == null && signature != null);
    }

}
