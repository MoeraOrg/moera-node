package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.TypeDef;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalType;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.Util;

@Entity
@Table(name = "media_file_owners")
@TypeDef(name = "Principal", typeClass = PrincipalType.class, defaultForType = Principal.class)
public class MediaFileOwner {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String ownerName;

    @ManyToOne
    @NotNull
    private MediaFile mediaFile;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentMedia")
    private Set<Posting> postings = new HashSet<>();

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    @NotNull
    @Column(insertable = false, updatable = false)
    private Timestamp usageUpdatedAt = Util.now();

    @Column(insertable = false, updatable = false)
    private Timestamp deadline;

    private Principal viewPrincipal = Principal.PRIVATE;

    @NotNull
    private Timestamp permissionsUpdatedAt = Util.now();

    @Size(max = 32)
    private String nonce;

    @Size(max = 32)
    private String prevNonce;

    private Timestamp nonceDeadline;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return MimeUtils.fileName(id.toString(), getMediaFile().getMimeType());
    }

    public String getDirectFileName() {
        return nonce != null
                ? MimeUtils.fileName(id.toString() + '_' + nonce, getMediaFile().getMimeType())
                : null;
    }

    public String getPrevDirectFileName() {
        return prevNonce != null
                ? MimeUtils.fileName(id.toString() + '_' + prevNonce, getMediaFile().getMimeType())
                : null;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public Set<Posting> getPostings() {
        return postings;
    }

    public void setPostings(Set<Posting> postings) {
        this.postings = postings;
    }

    public Posting getPosting(String receiverName) {
        return postings.stream()
                .filter(p -> Objects.equals(p.getReceiverName(), receiverName))
                .findFirst()
                .orElse(null);
    }

    public void addPosting(Posting posting) {
        postings.add(posting);
        posting.setParentMedia(this);
    }

    public void removePosting(Posting posting) {
        postings.removeIf(sr -> sr.getId().equals(posting.getId()));
        posting.setParentMedia(null);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Timestamp getUsageUpdatedAt() {
        return usageUpdatedAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public Principal getViewE(String nodeName) {
        return getViewPrincipal().withOwner(getOwnerName() != null ? getOwnerName() : nodeName);
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Timestamp getPermissionsUpdatedAt() {
        return permissionsUpdatedAt;
    }

    public void setPermissionsUpdatedAt(Timestamp permissionsUpdatedAt) {
        this.permissionsUpdatedAt = permissionsUpdatedAt;
    }

    public static String generateNonce() {
        return CryptoUtil.token().substring(0, 32);
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getPrevNonce() {
        return prevNonce;
    }

    public void setPrevNonce(String prevNonce) {
        this.prevNonce = prevNonce;
    }

    public Timestamp getNonceDeadline() {
        return nonceDeadline;
    }

    public void setNonceDeadline(Timestamp nonceDeadline) {
        this.nonceDeadline = nonceDeadline;
    }

}
