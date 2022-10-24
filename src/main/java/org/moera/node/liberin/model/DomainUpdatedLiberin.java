package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class DomainUpdatedLiberin extends Liberin {

    private String oldName;
    private String name;

    public DomainUpdatedLiberin(String oldName, String name) {
        this.oldName = oldName;
        this.name = name;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("oldName", oldName);
        model.put("name", name);
    }

}
