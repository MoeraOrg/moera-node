package org.moera.node.model;

import org.moera.node.data.Reaction;

public class ReactionDescription {

    private String ownerName;
    private boolean negative;
    private int emoji;
    private byte[] signature;
    private short signatureVersion;

    public ReactionDescription() {
    }

    public ReactionDescription(String ownerName, ReactionAttributes attributes) {
        this.ownerName = ownerName;
        negative = attributes.isNegative();
        emoji = attributes.getEmoji();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
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

    public void toReaction(Reaction reaction) {
        reaction.setOwnerName(ownerName);
        reaction.setNegative(negative);
        reaction.setEmoji(emoji);
        reaction.setSignature(signature);
        reaction.setSignatureVersion(signatureVersion);
    }

}
