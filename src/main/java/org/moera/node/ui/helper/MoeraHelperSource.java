package org.moera.node.ui.helper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.lib.UniversalLocation;
import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.AvatarInfo;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.ReactionTotalInfo;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgentOs;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

@HelperSource
public class MoeraHelperSource {

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

    public CharSequence nodename(String nodeName, String fullName, Options options) {
        boolean linked = HelperUtil.boolArg(options.hash("linked", "true"));

        StringBuilder buf = new StringBuilder();
        NodeName registeredName = NodeName.parse(nodeName);
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            String nodeUrl = namingCache.getFast(nodeName).getNodeUri();

            String tag = linked ? "a" : "span";
            buf.append('<');
            buf.append(tag);
            if (tag.equals("a")) {
                HelperUtil.appendAttr(buf, "href", UniversalLocation.redirectTo(nodeName, nodeUrl));
            }
            HelperUtil.appendAttr(buf, "class", "node-name");
            if (!ObjectUtils.isEmpty(fullName)) {
                HelperUtil.appendAttr(buf, "title", "@" + registeredName.toShortString());
            }
            buf.append('>');

            if (!ObjectUtils.isEmpty(fullName)) {
                HelperUtil.safeAppend(buf, fullName);
            } else {
                HelperUtil.safeAppend(buf, registeredName.getName());

                if (registeredName.getGeneration() != 0) {
                    buf.append("<sub class=\"generation\">");
                    buf.append(registeredName.getGeneration());
                    buf.append("</sub>");
                }
            }

            buf.append("</%s>".formatted(tag));
        }
        return new SafeString(buf);
    }

    public CharSequence fullName(String nodeName, String fullName) {
        return !ObjectUtils.isEmpty(fullName) ? fullName : NodeName.shorten(nodeName);
    }

    public CharSequence shortName(String nodeName) {
        return ObjectUtils.isEmpty(nodeName) ? "" : NodeName.shorten(nodeName);
    }

    public CharSequence shortGender(String gender) {
        if (ObjectUtils.isEmpty(gender)) {
            return "";
        }
        return switch (gender.toLowerCase()) {
            case "male" -> "m.";
            case "female" -> "f.";
            default -> gender;
        };
    }

    public int nameAngle(String ownerName) {
        if (ownerName == null) {
            return 0;
        }

        int angle = 0;
        for (int i = 0; i < ownerName.length(); i++) {
            angle = (angle + ownerName.charAt(i)) % 12;
        }

        return angle * 360 / 12;
    }

    public CharSequence avatar(Object avatar, Object size, Options options) {
        long sz = HelperUtil.intArg(1, size);
        String nodeName = options.hash("nodeName");

        StringBuilder buf = new StringBuilder();
        NodeName registeredName = NodeName.parse(nodeName);
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            String nodeUrl = namingCache.getFast(nodeName).getNodeUri();
            buf.append("<a");
            HelperUtil.appendAttr(
                buf, "href", UniversalLocation.redirectTo(nodeName, nodeUrl, "/", null, null)
            );
            HelperUtil.appendAttr(buf, "title", "Profile");
            HelperUtil.appendAttr(buf, "class", "avatar-link");
            buf.append('>');
        }
        if (avatar == null) {
            buf.append("<div");
            HelperUtil.appendAttr(buf, "title", "Avatar placeholder");
            HelperUtil.appendAttr(buf, "class", "avatar avatar-circle avatar-placeholder");
            String style =
                "width: %dpx; height: %dpx; font-size: %dpx; filter: hue-rotate(%ddeg)"
                .formatted(sz, sz, sz / 3, nameAngle(nodeName));
            HelperUtil.appendAttr(buf, "style", style);
            buf.append('>');
            String shortName = registeredName.toShortString();
            if (shortName == null) {
                shortName = "XX";
            }
            buf.append(shortName.length() > 2 ? shortName.substring(0, 2).toUpperCase() : shortName.toUpperCase());
            buf.append("</div>");
        } else {
            AvatarImage avatarImage = avatar instanceof AvatarInfo
                ? AvatarImageUtil.build((AvatarInfo) avatar) : (AvatarImage) avatar;

            buf.append("<img");
            HelperUtil.appendAttr(buf, "src", "/moera/media/" + avatarImage.getPath());
            HelperUtil.appendAttr(buf, "alt", "Avatar");
            HelperUtil.appendAttr(buf, "class", "avatar avatar-" + avatarImage.getShape());
            HelperUtil.appendAttr(buf, "width", sz);
            HelperUtil.appendAttr(buf, "height", sz);
            buf.append('>');
        }
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            buf.append("</a>");
        }
        return new SafeString(buf);
    }

    public CharSequence reactions(Object postingOrCommentInfo) {
        ReactionTotalsInfo totalsInfo = null;
        boolean totalsVisible = false;
        boolean negativeTotalsVisible = false;
        if (postingOrCommentInfo instanceof PostingInfo info) {
            totalsInfo = info.getReactions();
            totalsVisible =
                PostingOperations.getViewReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                || PostingOperations.getViewReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic();
            negativeTotalsVisible =
                PostingOperations.getViewReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                    && PostingOperations.getViewNegativeReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                || PostingOperations.getViewReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic()
                    && PostingOperations.getViewNegativeReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic();
        }
        if (postingOrCommentInfo instanceof CommentInfo info) {
            totalsInfo = info.getReactions();
            totalsVisible =
                CommentOperations.getViewReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                || CommentOperations.getViewReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic();
            negativeTotalsVisible =
                CommentOperations.getViewReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                    && CommentOperations.getViewNegativeReactions(info.getOperations(), Principal.PUBLIC).isPublic()
                || CommentOperations.getViewReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic()
                    && CommentOperations.getViewNegativeReactionTotals(info.getOperations(), Principal.PUBLIC).isPublic();
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"reactions\">");
        if (totalsInfo != null && !totalsInfo.getPositive().isEmpty()) {
            buf.append("<span class=\"positive\">");
            appendEmojis(buf, totalsInfo.getPositive());
            if (totalsVisible) {
                buf.append(sum(totalsInfo.getPositive()));
            }
            buf.append("</span>");
        }
        if (totalsInfo != null && !totalsInfo.getNegative().isEmpty()) {
            buf.append("<span class=\"negative\">");
            appendEmojis(buf, totalsInfo.getNegative());
            if (totalsVisible && negativeTotalsVisible) {
                buf.append(sum(totalsInfo.getNegative()));
            }
            buf.append("</span>");
        }
        buf.append("</div>");
        return new SafeString(buf);
    }

    private long sum(List<ReactionTotalInfo> totals) {
        return totals.stream()
                .map(ReactionTotalInfo::getTotal)
                .filter(Objects::nonNull)
                .collect(Collectors.summarizingInt(Integer::intValue))
                .getSum();
    }

    private void appendEmojis(StringBuilder buf, List<ReactionTotalInfo> totals) {
        totals = totals.stream()
                .filter(rt -> rt.getTotal() != null)
                .sorted(Comparator.comparingInt(ReactionTotalInfo::getTotal).reversed())
                .collect(Collectors.toList());
        buf.append("<span class=\"emojis\">");
        for (int i = 0; i < 3 && i < totals.size(); i++) {
            appendEmoji(buf, totals.get(i).getEmoji());
        }
        buf.append("</span>");
    }

    private void appendEmoji(StringBuilder buf, int emoji) {
        switch (emoji) {
            case 0x1f4a1:
                emoji = 0x1f914;
                break;
            case 0x1f620:
                emoji = 0x1f92c;
                break;
            case 0x1f643:
                emoji = 0x1f921;
        }
        buf.append("&#%d;".formatted(emoji));
    }

    public CharSequence invitation() {
        StringBuilder buf = new StringBuilder();
        buf.append("Try all <a href=\"https://moera.org/\">Moera</a> features — ");
        if (requestContext.getUserAgentOs() == UserAgentOs.ANDROID) {
            buf.append(
                "<br><a href=\"https://play.google.com/store/apps/details"
                + "?id=org.moera.web.twa&pcampaignid=invitation-node\" class=\"btn btn-light btn-sm\">"
                + "Get Moera App</a> or "
            );
        }
        buf.append("<a class=\"btn btn-primary btn-sm\" href=\"");
        buf.append(Util.he(requestContext.getRedirectorUrl()));
        buf.append("\">View in Web Client</a>");
        return new SafeString(buf);
    }

}
