package org.moera.node.helper;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.global.RequestContext;

@HelperSource
public class OptionHelperSource {

    @Inject
    private RequestContext requestContext;

    private String getOptionString(String name) {
        return requestContext.getOptions() != null ? requestContext.getOptions().getString(name) : "";
    }

    public CharSequence optionHtml(String name) {
        return new SafeString(getOptionString(name));
    }

    public CharSequence assignOption(String variableName, String name, Options options) {
        options.data(variableName, getOptionString(name));
        return "";
    }

}
