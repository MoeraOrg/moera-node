package org.moera.node.data.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingFingerprint extends Fingerprint {

    public String ownerName;
    public int ownerGeneration;
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String bodyHtml;
    public long created;

    public PostingFingerprint(Posting posting) {
        super(0);
        ownerName = posting.getOwnerName();
        ownerGeneration = posting.getOwnerGeneration();
        bodySrc.setValue(posting.getBodySrc());
        bodySrcFormat = posting.getBodySrcFormat().getValue();
        bodyHtml = posting.getBodyHtml();
        created = Util.toEpochSecond(posting.getCreated());
    }

}
