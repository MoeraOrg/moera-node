package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TitleBuilder {

    @Inject
    private RequestContext requestContext;

    public CharSequence build(String title) {
        StringBuilder buf = new StringBuilder();
        if (!StringUtils.isEmpty(title)) {
            buf.append(title);
            buf.append(' ');
            String name = "";
            try {
                name = requestContext.getPublic().getOptions().getString("profile.registered-name");
            } catch (Exception e) {
            }
            if (!StringUtils.isEmpty(name)) {
                buf.append("@ ");
                buf.append(name);
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
