package org.moera.node.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

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

public class CommentText {

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

    private UUID repliedToId;

    private byte[] signature;

    private short signatureVersion;

    private Map<String, Principal> operations;

    private Map<String, Principal> reactionOperations;

    private Map<String, Principal> seniorOperations;

    public CommentText() {
    }

    public CommentText(String ownerName, String ownerFullName, String ownerGender, CommentSourceText sourceText,
                       TextConverter textConverter) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        ownerAvatar = sourceText.getOwnerAvatar();
        bodySrc = sourceText.getBodySrc();
        bodySrcFormat = sourceText.getBodySrcFormat() != null ? sourceText.getBodySrcFormat() : SourceFormat.PLAIN_TEXT;
        media = sourceText.getMedia() != null
                ? Arrays.stream(sourceText.getMedia()).map(MediaWithDigest::getId).toArray(UUID[]::new)
                : null;
        createdAt = Util.toEpochSecond(Util.now());
        acceptedReactions = sourceText.getAcceptedReactions();
        repliedToId = sourceText.getRepliedToId();
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
        reactionOperations = sourceText.getReactionOperations();
        seniorOperations = sourceText.getSeniorOperations();
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

    public UUID getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(UUID repliedToId) {
        this.repliedToId = repliedToId;
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

    public Map<String, Principal> getReactionOperations() {
        return reactionOperations;
    }

    public void setReactionOperations(Map<String, Principal> reactionOperations) {
        this.reactionOperations = reactionOperations;
    }

    public Principal getReactionPrincipal(String operationName) {
        return reactionOperations != null ? reactionOperations.get(operationName) : null;
    }

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
    }

    public Principal getSeniorPrincipal(String operationName) {
        return seniorOperations != null ? seniorOperations.get(operationName) : null;
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

        toPrincipal(this::getPrincipal, "view", entry::setViewPrincipal);
        toPrincipal(this::getPrincipal, "viewReactions", entry::setViewReactionsPrincipal);
        toPrincipal(this::getPrincipal, "viewNegativeReactions", entry::setViewNegativeReactionsPrincipal);
        toPrincipal(this::getPrincipal, "viewReactionTotals", entry::setViewReactionTotalsPrincipal);
        toPrincipal(this::getPrincipal, "viewNegativeReactionTotals", entry::setViewNegativeReactionTotalsPrincipal);
        toPrincipal(this::getPrincipal, "viewReactionRatios", entry::setViewReactionRatiosPrincipal);
        toPrincipal(this::getPrincipal, "viewNegativeReactionRatios", entry::setViewNegativeReactionRatiosPrincipal);
        toPrincipal(this::getPrincipal, "addReaction", entry::setAddReactionPrincipal);
        toPrincipal(this::getPrincipal, "addNegativeReaction", entry::setAddNegativeReactionPrincipal);

        ChildOperations ops = entry.getReactionOperations();
        toPrincipal(this::getReactionPrincipal, "view", ops::setView);
        toPrincipal(this::getReactionPrincipal, "delete", ops::setDelete);

        toEntrySenior(entry);
    }

    public void toEntrySenior(Entry entry) {
        toPrincipal(this::getSeniorPrincipal, "view", entry::setParentViewPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewReactions", entry::setParentViewReactionsPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewNegativeReactions", entry::setParentViewNegativeReactionsPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewReactionTotals", entry::setParentViewReactionTotalsPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewNegativeReactionTotals",
                entry::setParentViewNegativeReactionTotalsPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewReactionRatios", entry::setParentViewReactionRatiosPrincipal);
        toPrincipal(this::getSeniorPrincipal, "viewNegativeReactionRatios",
                entry::setParentViewNegativeReactionRatiosPrincipal);
        toPrincipal(this::getSeniorPrincipal, "addReaction", entry::setParentAddReactionPrincipal);
        toPrincipal(this::getSeniorPrincipal, "addNegativeReaction", entry::setParentAddNegativeReactionPrincipal);
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
               && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()))
               && (ownerGender == null || ownerGender.equals(entry.getOwnerGender()))
               && (ownerAvatarMediaFile == null
                    || entry.getOwnerAvatarMediaFile() != null
                        && ownerAvatarMediaFile.getId().equals(entry.getOwnerAvatarMediaFile().getId()))
               && samePrincipalAs("view", entry.getViewPrincipal())
               && samePrincipalAs("viewReactions", entry.getViewReactionsPrincipal())
               && samePrincipalAs("viewNegativeReactions", entry.getViewNegativeReactionsPrincipal())
               && samePrincipalAs("viewReactionTotals", entry.getViewReactionTotalsPrincipal())
               && samePrincipalAs("viewNegativeReactionTotals", entry.getViewNegativeReactionTotalsPrincipal())
               && samePrincipalAs("viewReactionRatios", entry.getViewReactionRatiosPrincipal())
               && samePrincipalAs("viewNegativeReactionRatios", entry.getViewNegativeReactionRatiosPrincipal())
               && samePrincipalAs("addReaction", entry.getAddReactionPrincipal())
               && samePrincipalAs("addNegativeReaction", entry.getAddNegativeReactionPrincipal())
               && sameReactionPrincipalAs("view", entry.getReactionOperations().getView())
               && sameReactionPrincipalAs("delete", entry.getReactionOperations().getDelete())
               && sameSeniorPrincipalAs("view", entry.getParentViewPrincipal())
               && sameSeniorPrincipalAs("viewReactions", entry.getParentViewReactionsPrincipal())
               && sameSeniorPrincipalAs("viewNegativeReactions", entry.getParentViewNegativeReactionsPrincipal())
               && sameSeniorPrincipalAs("viewReactionTotals", entry.getParentViewReactionTotalsPrincipal())
               && sameSeniorPrincipalAs("viewNegativeReactionTotals",
                                        entry.getParentViewNegativeReactionTotalsPrincipal())
               && sameSeniorPrincipalAs("viewReactionRatios", entry.getParentViewReactionRatiosPrincipal())
               && sameSeniorPrincipalAs("viewNegativeReactionRatios",
                                        entry.getParentViewNegativeReactionRatiosPrincipal())
               && sameSeniorPrincipalAs("addReaction", entry.getParentAddReactionPrincipal())
               && sameSeniorPrincipalAs("addNegativeReaction", entry.getParentAddNegativeReactionPrincipal());
    }

    private boolean samePrincipalAs(String operationName, Principal principal) {
        Principal value = getPrincipal(operationName);
        return value == null || Objects.equals(value, principal);
    }

    private boolean sameReactionPrincipalAs(String operationName, Principal principal) {
        Principal value = getReactionPrincipal(operationName);
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

    private boolean sameSeniorPrincipalAs(String operationName, Principal principal) {
        Principal value = getSeniorPrincipal(operationName);
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
                true, revision);
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
