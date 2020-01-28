package org.moera.node.helper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionTotalInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
import org.springframework.util.StringUtils;

@HelperSource
public class MoeraHelperSource {

    @Inject
    private NamingCache namingCache;

    public CharSequence nodename(String nodeName, Options options) {
        boolean linked = HelperUtil.boolArg(options.hash("linked", "true"));

        StringBuilder buf = new StringBuilder();
        RegisteredName registeredName = (RegisteredName) NodeName.parse(nodeName);
        if (!StringUtils.isEmpty(registeredName.getName())) {
            RegisteredNameDetails details = namingCache.getFast(nodeName);
            if (!linked) {
                details.setNodeUri("");
            }
            String tag = !StringUtils.isEmpty(details.getNodeUri()) ? "a" : "span";

            buf.append('<');
            buf.append(tag);
            if (tag.equals("a")) {
                HelperUtil.appendAttr(buf, "href", details.getNodeUri());
            }
            HelperUtil.appendAttr(buf, "class", "node-name");
            buf.append('>');

            HelperUtil.safeAppend(buf, registeredName.getName());

            if (!details.isLatest()) {
                buf.append("<sub class=\"generation\">");
                if (registeredName.getGeneration() != null) {
                    buf.append(registeredName.getGeneration());
                } else {
                    buf.append('?');
                }
                buf.append("</sub>");
            }

            buf.append(String.format("</%s>", tag));
        }
        return new SafeString(buf);
    }

    public CharSequence reactions(PostingInfo postingInfo) {
        ReactionTotalsInfo totalsInfo = postingInfo.getReactions();
        boolean totalsVisible = Arrays.asList(postingInfo.getOperations().get("reactions")).contains("public");

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

}
