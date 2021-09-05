package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.naming.NodeName;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class TitleBuilder {

    @Inject
    private RequestContext requestContext;

    public CharSequence build(String title) {
        StringBuilder buf = new StringBuilder();
        if (!ObjectUtils.isEmpty(title)) {
            buf.append(title);
            buf.append(' ');
            String name = "";
            try {
                RequestContext rcp = requestContext.getPublic();
                name = rcp.fullName() != null ? rcp.fullName() : rcp.nodeName();
            } catch (Exception e) {
            }
            if (!ObjectUtils.isEmpty(name)) {
                buf.append("@ ");
                buf.append(NodeName.shorten(name));
                buf.append(' ');
            }
        }
        if (buf.length() > 0) {
            buf.append("| ");
        }
        buf.append("Moera");
        return buf;
    }

}
