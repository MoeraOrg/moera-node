package org.moera.node.helper;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.node.model.RegisteredNameInfo;
import org.springframework.util.StringUtils;

@HelperSource
public class MoeraHelperSource {

    public CharSequence regname(RegisteredNameInfo registeredName) {
        StringBuilder buf = new StringBuilder();
        if (registeredName != null && !StringUtils.isEmpty(registeredName.getName())) {
            buf.append("<span class=\"registered-name\">");
            HelperUtils.safeAppend(buf, registeredName.getName());
            buf.append("<sub class=\"generation\">");
            if (registeredName.getGeneration() != null) {
                buf.append(registeredName.getGeneration());
            } else {
                buf.append('?');
            }
            buf.append("</sub></span>");
        }
        return new SafeString(buf);
    }

}
