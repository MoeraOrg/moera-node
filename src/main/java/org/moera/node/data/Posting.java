package org.moera.node.data;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.fingerprint.PostingFingerprint;
import org.moera.node.util.Util;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public static Posting newPosting(PostingRepository postingRepository, UUID nodeId,
                                     String ownerName, int ownerGeneration) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setOwnerName(ownerName);
        posting.setOwnerGeneration(ownerGeneration);

        return postingRepository.save(posting);
    }

    public void newRevision(EntryRevisionRepository entryRevisionRepository) {
        EntryRevision revision;
        if (getTotalRevisions() == 0) {
            revision = EntryRevision.newRevision(entryRevisionRepository, this);
            setTotalRevisions(1);
        } else {
            revision = getCurrentRevision().newRevision(entryRevisionRepository);
            getCurrentRevision().setDeletedAt(Util.now());
            setTotalRevisions(getTotalRevisions() + 1);
        }
        getRevisions().add(revision);
        setCurrentRevision(revision);
    }

    public void sign(ECPrivateKey signingKey) {
        getCurrentRevision().setSignature(
                CryptoUtil.sign(new PostingFingerprint(this, getCurrentRevision()), signingKey));
    }

}
