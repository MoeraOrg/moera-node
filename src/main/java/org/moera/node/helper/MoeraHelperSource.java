package org.moera.node.helper;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.node.naming.DelegatedName;
import org.moera.node.naming.RegisteredName;
import org.springframework.util.StringUtils;

@HelperSource
public class MoeraHelperSource {

    public CharSequence regname(String registeredName) {
        StringBuilder buf = new StringBuilder();
        DelegatedName name = (DelegatedName) RegisteredName.parse(registeredName);
        if (!StringUtils.isEmpty(name.getName())) {
            buf.append("<span class=\"registered-name\">");
            HelperUtils.safeAppend(buf, name.getName());
            buf.append("<sub class=\"generation\">");
            if (name.getGeneration() != null) {
                buf.append(name.getGeneration());
            } else {
                buf.append('?');
            }
            buf.append("</sub></span>");
        }
        return new SafeString(buf);
    }

}
