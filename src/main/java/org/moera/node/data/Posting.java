package org.moera.node.data;

import java.security.interfaces.ECPrivateKey;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.fingerprint.PostingFingerprint;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public void sign(ECPrivateKey signingKey) {
        getCurrentRevision().setSignature(
                CryptoUtil.sign(new PostingFingerprint(this, getCurrentRevision()), signingKey));
    }

}
