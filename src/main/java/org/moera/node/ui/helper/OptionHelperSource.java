package org.moera.node.ui.helper;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.moera.node.global.RequestContext;
import org.moera.node.option.OptionsMetadata;

@HelperSource
public class OptionHelperSource {

    @Inject
    private RequestContext requestContext;

    @Inject
    private OptionsMetadata optionsMetadata;

    private String getOptionString(String name) {
        return requestContext.getOptions() != null
                ? requestContext.getOptions().getString(name)
                : optionsMetadata.getDescriptor(name).getDefaultValue();
    }

    public CharSequence optionHtml(String name) {
        return new SafeString(getOptionString(name));
    }

    public CharSequence assignOption(String variableName, String name, Options options) {
        options.data(variableName, getOptionString(name));
        return "";
    }

}
