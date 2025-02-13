package org.moera.node.model;

import org.moera.lib.node.types.ClientReactionInfo;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

public class ClientReactionInfoUtil {

    public static ClientReactionInfo build(Reaction reaction) {
        ClientReactionInfo info = new ClientReactionInfo();
        setEntryId(info, reaction.getEntryRevision().getEntry().getId().toString());
        info.setNegative(reaction.isNegative());
        info.setEmoji(reaction.getEmoji());
        info.setCreatedAt(Util.toEpochSecond(reaction.getCreatedAt()));
        info.setDeadline(Util.toEpochSecond(reaction.getDeadline()));
        return info;
    }

    public static ClientReactionInfo build(OwnReaction reaction) {
        ClientReactionInfo info = new ClientReactionInfo();
        setEntryId(info, reaction.getRemotePostingId());
        info.setNegative(reaction.isNegative());
        info.setEmoji(reaction.getEmoji());
        info.setCreatedAt(Util.toEpochSecond(reaction.getCreatedAt()));
        return info;
    }

    public static String getEntryId(ClientReactionInfo info) {
        return (String) info.getExtra();
    }

    public static void setEntryId(ClientReactionInfo info, String entryId) {
        info.setExtra(entryId);
    }

}
