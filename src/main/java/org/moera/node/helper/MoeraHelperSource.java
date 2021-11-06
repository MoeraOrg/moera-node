package org.moera.node.helper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgent;
import org.moera.node.global.UserAgentOs;
import org.moera.node.global.WebClient;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.AvatarInfo;
import org.moera.node.model.ReactionTotalInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.model.ReactionsInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
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
        RegisteredName registeredName = (RegisteredName) NodeName.parse(nodeName);
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            RegisteredNameDetails details = namingCache.getFast(nodeName);
            if (!linked) {
                details.setNodeUri("");
            }
            String tag = !ObjectUtils.isEmpty(details.getNodeUri()) ? "a" : "span";

            buf.append('<');
            buf.append(tag);
            if (tag.equals("a")) {
                HelperUtil.appendAttr(buf, "href", details.getNodeUri());
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
        switch (gender.toLowerCase()) {
            case "male":
                return "m.";
            case "female":
                return "f.";
            default:
                return gender;
        }
    }

    public CharSequence avatar(Object avatar, Object size, Options options) {
        long sz = HelperUtil.intArg(1, size);
        String nodeName = options.hash("nodeName");

        StringBuilder buf = new StringBuilder();
        RegisteredName registeredName = (RegisteredName) NodeName.parse(nodeName);
        if (!ObjectUtils.isEmpty(registeredName.getName())) {
            RegisteredNameDetails details = namingCache.getFast(nodeName);
            buf.append("<a");
            HelperUtil.appendAttr(buf, "href", details.getNodeProfileUri());
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
                    ? new AvatarImage((AvatarInfo) avatar) : (AvatarImage) avatar;

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
        boolean totalsVisible = Arrays.asList(reactionsInfo.getOperations().get("reactions")).contains("public");

        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"reactions\">");
        if (totalsInfo.getPositive().size() > 0) {
            buf.append("<span class=\"positive\">");
            appendEmojis(buf, totalsInfo.getPositive());
            if (totalsVisible) {
                buf.append(sum(totalsInfo.getPositive()));
            }
            buf.append("</span>");
        }
        if (totalsInfo.getNegative().size() > 0) {
            buf.append("<span class=\"negative\">");
            appendEmojis(buf, totalsInfo.getNegative());
            if (totalsVisible) {
                buf.append(sum(totalsInfo.getNegative()));
            }
            buf.append("</span>");
        }
        buf.append("</div>");
        return new SafeString(buf);
    }

    private long sum(List<ReactionTotalInfo> totals) {
        return totals.stream()
                .collect(Collectors.summarizingInt(ReactionTotalInfo::getTotal))
                .getSum();
    }

    private void appendEmojis(StringBuilder buf, List<ReactionTotalInfo> totals) {
        totals.sort(Comparator.comparingInt(ReactionTotalInfo::getTotal).reversed());
        buf.append("<span class=\"emojis\">");
        for (int i = 0; i < 3 && i < totals.size(); i++) {
            buf.append(String.format("&#%d;", totals.get(i).getEmoji()));
        }
        buf.append("</span>");
    }

    private boolean isAddonSupported() {
        switch (requestContext.getUserAgent()) {
            default:
            case UNKNOWN:
            case OPERA:
            case DOLPHIN:
                return false;
            case FIREFOX:
            case CHROME:
                return requestContext.getUserAgentOs() == UserAgentOs.UNKNOWN;
            case YANDEX:
            case BRAVE:
            case VIVALDI:
                return true;
        }
    }

    public CharSequence invitation() {
        StringBuilder buf = new StringBuilder();
        buf.append("This site participates in <a href=\"http://moera.org/\">Moera</a> Network. ");
        buf.append("To unlock all features, ");
        if (isAddonSupported()) {
            buf.append("install ");
            if (requestContext.getUserAgent() == UserAgent.FIREFOX) {
                buf.append("<a href=\"https://addons.mozilla.org/en-US/firefox/addon/moera/\">"
                        + "the Moera add-on for Firefox</a>");
            } else {
                buf.append("<a href=\"https://chrome.google.com/webstore/detail/moera/"
                        + "endpkknmpgamhhlojbgifimfcleeeghb\">the Moera add-on for Chrome</a>");
            }
            buf.append(" or ");
        }
        if (requestContext.getUserAgentOs() == UserAgentOs.ANDROID) {
            buf.append("<a href=\"https://play.google.com/store/apps/details"
                    + "?id=org.moera.web.twa&pcampaignid=invitation-node\">"
                    + "get Moera app on Google Play</a> or ");
        }
        buf.append("<a class=\"btn btn-success btn-sm\" href=\"");
        buf.append(WebClient.URL);
        buf.append("?href=");
        buf.append(Util.ue(requestContext.getUrl()));
        buf.append("\">View in Web Client</a>");
        return new SafeString(buf);
    }

    public CharSequence commentInvitation() {
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"alert alert-info mt-3\">To react or comment&nbsp; ");
        buf.append("<a class=\"btn btn-success btn-sm\" href=\"");
        buf.append(WebClient.URL);
        buf.append("?href=");
        buf.append(Util.ue(requestContext.getUrl()));
        buf.append("\">View in Web Client</a></div>");
        return new SafeString(buf);
    }

    public CharSequence buttonsInvitation(Long moment) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(requestContext.getUrl());
        if (moment != null) {
            builder.replaceQuery("before=" + moment);
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"buttons-invitation\">To react or comment&nbsp; ");
        buf.append("<a class=\"btn btn-outline-success btn-sm\" href=\"");
        buf.append(WebClient.URL);
        buf.append("?href=");
        buf.append(Util.ue(builder.toUriString()));
        buf.append("\">View in Web Client</a></div>");
        return new SafeString(buf);
    }

}
