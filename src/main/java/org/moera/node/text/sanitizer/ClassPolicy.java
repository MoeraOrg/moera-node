package org.moera.node.text.sanitizer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.owasp.html.AttributePolicy;
import org.springframework.util.ObjectUtils;

public class ClassPolicy implements AttributePolicy {

    private static class ClassMatcher {
        String[] templates;
        Set<String> elements;
        boolean notInPreview;

        ClassMatcher onElements(String ...elements) {
            this.elements = Set.of(elements);
            return this;
        }

        ClassMatcher notInPreview() {
            this.notInPreview = true;
            return this;
        }

        boolean matches(String elementName, String klass, boolean preview) {
            if (preview && notInPreview) {
                return false;
            }
            if (elements != null && !elements.contains(elementName)) {
                return false;
            }
            return Arrays.stream(templates).anyMatch(template -> {
                if (template.endsWith("*")) {
                    return klass.startsWith(template.substring(0, template.length() - 1));
                } else {
                    return klass.equals(template);
                }
            });
        }
    }

    private static final ClassMatcher[] MATCHERS = {
            allowed("text-start", "text-end", "text-center"),
            allowed("text-*").notInPreview(),
            allowed("bg-*").notInPreview(),
            allowed("border-*").notInPreview(),
            allowed("fs-*").notInPreview(),
            allowed("entry-image").onElements("a"),
            allowed("emoji").onElements("img"),
            allowed("katex").onElements("div", "span")
    };

    private boolean preview;

    public ClassPolicy(boolean preview) {
        this.preview = preview;
    }

    private static ClassMatcher allowed(String... templates) {
        ClassMatcher matcher = new ClassMatcher();
        matcher.templates = templates;
        return matcher;
    }

    @Override
    public String apply(String elementName, String attributeName, String value) {
        String[] classes = value.split(" ");
        String klass = Arrays.stream(classes)
                .filter(k -> isClassAllowed(elementName, k))
                .collect(Collectors.joining(" "));
        return !ObjectUtils.isEmpty(klass) ? klass : null;
    }

    private boolean isClassAllowed(String elementName, String klass) {
        return Arrays.stream(MATCHERS).anyMatch(m -> m.matches(elementName, klass, preview));
    }

}
