package org.moera.node.model;

public class AvatarOrdinal {

    private String id;
    private int ordinal;

    public AvatarOrdinal() {
    }

    public AvatarOrdinal(String id, int ordinal) {
        this.id = id;
        this.ordinal = ordinal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

}
