package org.moera.node.data;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.fingerprint.PostingFingerprint;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public Posting newRevision() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(getNodeId());
        posting.setEntryId(getEntryId());
        posting.setOwnerName(getOwnerName());
        posting.setOwnerGeneration(getOwnerGeneration());
        posting.setBodyPreviewHtml(getBodyPreviewHtml());
        posting.setBodySrc(getBodySrc());
        posting.setBodySrcFormat(getBodySrcFormat());
        posting.setBodyHtml(getBodyHtml());
        posting.setHeading(getHeading());
        posting.setPublishedAt(getPublishedAt());
        return posting;
    }

    public void sign(ECPrivateKey signingKey) {
        setSignature(CryptoUtil.sign(new PostingFingerprint(this), signingKey));
    }

}
