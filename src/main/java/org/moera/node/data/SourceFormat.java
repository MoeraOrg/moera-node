package org.moera.node.data;

public enum SourceFormat implements Choosable {

    PLAIN_TEXT("No formatting"),
    HTML("HTML"),
    MARKDOWN("Markdown");

    private String title;

    SourceFormat(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static SourceFormat forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
