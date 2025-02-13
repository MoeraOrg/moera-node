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
import org.moera.lib.node.types.ReactionTotalInfo;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgentOs;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.ReactionsInfo;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

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

            buf.append(String.format("</%s>", tag));
        }
        return new SafeString(buf);
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

    public CharSequence avatar(Object avatar, Object size, Options options) {
        long sz = HelperUtil.intArg(1, size);
        String nodeName = options.hash("nodeName");

        StringBuilder buf = new StringBuilder();
        NodeName registeredName = NodeName.parse(nodeName);
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            String nodeUrl = namingCache.getFast(nodeName).getNodeUri();
            buf.append("<a");
            HelperUtil.appendAttr(buf, "href",
                    UniversalLocation.redirectTo(nodeName, nodeUrl, "/", null, null));
            HelperUtil.appendAttr(buf, "title", "Profile");
            buf.append('>');
        }
        buf.append("<img");
        if (avatar == null) {
            HelperUtil.appendAttr(buf, "src", "/pics/avatar.png");
            HelperUtil.appendAttr(buf, "alt", "Avatar placeholder");
            HelperUtil.appendAttr(buf, "class", "avatar avatar-circle");
        } else {
            AvatarImage avatarImage = avatar instanceof AvatarInfo
                    ? AvatarImageUtil.build((AvatarInfo) avatar) : (AvatarImage) avatar;

            HelperUtil.appendAttr(buf, "src", "/moera/media/" + avatarImage.getPath());
            HelperUtil.appendAttr(buf, "alt", "Avatar");
            HelperUtil.appendAttr(buf, "class", "avatar avatar-" + avatarImage.getShape());
        }
        HelperUtil.appendAttr(buf, "width", sz);
        HelperUtil.appendAttr(buf, "height", sz);
        buf.append('>');
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            buf.append("</a>");
        }
        return new SafeString(buf);
    }

    public CharSequence reactions(ReactionsInfo reactionsInfo) {
        ReactionTotalsInfo totalsInfo = reactionsInfo.getReactions();
        boolean totalsVisible = reactionsInfo.getPrincipal("viewReactions", Principal.PUBLIC).isPublic()
                || reactionsInfo.getPrincipal("viewReactionTotals", Principal.PUBLIC).isPublic();
        boolean negativeTotalsVisible = reactionsInfo.getPrincipal("viewReactions", Principal.PUBLIC).isPublic()
                && reactionsInfo.getPrincipal("viewNegativeReactions", Principal.PUBLIC).isPublic()
                || reactionsInfo.getPrincipal("viewNegativeReactionTotals", Principal.PUBLIC).isPublic();

        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"reactions\">");
        if (!totalsInfo.getPositive().isEmpty()) {
            buf.append("<span class=\"positive\">");
            appendEmojis(buf, totalsInfo.getPositive());
            if (totalsVisible) {
                buf.append(sum(totalsInfo.getPositive()));
            }
            buf.append("</span>");
        }
        if (!totalsInfo.getNegative().isEmpty()) {
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
            buf.append(String.format("&#%d;", totals.get(i).getEmoji()));
        }
        buf.append("</span>");
    }

    public CharSequence invitation() {
        StringBuilder buf = new StringBuilder();
        buf.append("This site participates in <a href=\"http://moera.org/\">Moera</a> Network. ");
        buf.append("To unlock all features, ");
        if (requestContext.getUserAgentOs() == UserAgentOs.ANDROID) {
            buf.append("<a href=\"https://play.google.com/store/apps/details"
                    + "?id=org.moera.web.twa&pcampaignid=invitation-node\">"
                    + "get Moera app on Google Play</a> or ");
        }
        buf.append("<a class=\"btn btn-success btn-sm\" href=\"");
        buf.append(Util.he(requestContext.getRedirectorUrl()));
        buf.append("\">View in Web Client</a>");
        return new SafeString(buf);
    }

    public CharSequence commentInvitation() {
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"alert alert-info mt-3\">To react or comment&nbsp; ");
        buf.append("<a class=\"btn btn-success btn-sm\" href=\"");
        buf.append(Util.he(requestContext.getRedirectorUrl()));
        buf.append("\">View in Web Client</a></div>");
        return new SafeString(buf);
    }

    public CharSequence buttonsInvitation(Long moment) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(requestContext.getRedirectorUrl());
        if (moment != null) {
            builder.replaceQuery("before=" + moment);
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"buttons-invitation\">To react or comment&nbsp; ");
        buf.append("<a class=\"btn btn-outline-success btn-sm\" href=\"");
        buf.append(Util.he(builder.toUriString()));
        buf.append("\">View in Web Client</a></div>");
        return new SafeString(buf);
    }

}
