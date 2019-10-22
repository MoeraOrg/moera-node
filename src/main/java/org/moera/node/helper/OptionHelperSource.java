package org.moera.node.helper;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.global.RequestContext;

@HelperSource
public class OptionHelperSource {

    @Inject
    private RequestContext requestContext;

    public CharSequence optionHtml(String name) {
        return new SafeString(requestContext.getOptions().getString(name));
    }

    public CharSequence assignOption(String variableName, String name, Options options) {
        options.data(variableName, requestContext.getOptions().getString(name));
        return "";
    }

}
