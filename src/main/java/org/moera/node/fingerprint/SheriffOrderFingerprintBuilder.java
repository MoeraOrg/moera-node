package org.moera.node.fingerprint;

import java.util.Objects;

import org.moera.lib.node.Fingerprints;
import org.moera.node.model.SheriffOrderDetails;
import org.moera.node.model.SheriffOrderDetailsQ;
import org.moera.node.util.Util;

public class SheriffOrderFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

    public static byte[] build(String nodeName, SheriffOrderDetailsQ sheriffOrderDetails, byte[] entryDigest) {
        return build(LATEST_VERSION, nodeName, sheriffOrderDetails, entryDigest);
    }

    public static byte[] build(
        short version,
        String nodeName,
        SheriffOrderDetailsQ sheriffOrderDetails,
        byte[] entryDigest
    ) {
        return Fingerprints.sheriffOrder(
            sheriffOrderDetails.getSheriffName(),
            nodeName,
            sheriffOrderDetails.getFeedName(),
            entryDigest,
            Objects.toString(sheriffOrderDetails.getCategory(), null),
            Objects.toString(sheriffOrderDetails.getReasonCode(), null),
            sheriffOrderDetails.getReasonDetails(),
            Util.toTimestamp(sheriffOrderDetails.getCreatedAt())
        );
    }

    public static byte[] build(
        short version,
        String nodeName,
        SheriffOrderDetails sheriffOrderDetails,
        byte[] entryDigest
    ) {
        return Fingerprints.sheriffOrder(
            sheriffOrderDetails.getSheriffName(),
            nodeName,
            sheriffOrderDetails.getFeedName(),
            entryDigest,
            Objects.toString(sheriffOrderDetails.getCategory(), null),
            Objects.toString(sheriffOrderDetails.getReasonCode(), null),
            sheriffOrderDetails.getReasonDetails(),
            Util.toTimestamp(sheriffOrderDetails.getCreatedAt())
        );
    }

}
