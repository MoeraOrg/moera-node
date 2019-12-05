package org.moera.node.helper;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.naming.DelegatedName;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
import org.springframework.util.StringUtils;

@HelperSource
public class MoeraHelperSource {

    @Inject
    private NamingCache namingCache;

    public CharSequence regname(String registeredName, Options options) {
        boolean linked = HelperUtil.boolArg(options.hash("linked", "true"));

        StringBuilder buf = new StringBuilder();
        DelegatedName name = (DelegatedName) RegisteredName.parse(registeredName);
        if (!StringUtils.isEmpty(name.getName())) {
            RegisteredNameDetails details = namingCache.getFast(registeredName);
            if (!linked) {
                details.setNodeUri("");
            }
            String tag = !StringUtils.isEmpty(details.getNodeUri()) ? "a" : "span";

            buf.append('<');
            buf.append(tag);
            if (tag.equals("a")) {
                HelperUtil.appendAttr(buf, "href", details.getNodeUri());
            }
            HelperUtil.appendAttr(buf, "class", "registered-name");
            buf.append('>');

            HelperUtil.safeAppend(buf, name.getName());

            if (!details.isLatest()) {
                buf.append("<sub class=\"generation\">");
                if (name.getGeneration() != null) {
                    buf.append(name.getGeneration());
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
