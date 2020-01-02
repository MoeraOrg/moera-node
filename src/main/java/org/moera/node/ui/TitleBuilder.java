package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NodeName;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TitleBuilder {

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

    public CharSequence build(String title) {
        StringBuilder buf = new StringBuilder();
        if (!StringUtils.isEmpty(title)) {
            buf.append(title);
            buf.append(' ');
            String name = "";
            try {
                name = requestContext.getPublic().nodeName();
            } catch (Exception e) {
            }
            if (!StringUtils.isEmpty(name)) {
                boolean latest = namingCache.getFast(name).isLatest();
                buf.append("@ ");
                buf.append(!latest ? name : NodeName.shorten(name));
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
