package org.moera.node.helper;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NodeName;
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

}
