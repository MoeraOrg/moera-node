package org.moera.node.liberin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.plugin.PluginContext;

public class Liberin {

    private static final Map<String, String> TYPE_NAMES = new HashMap<>();

    private UUID nodeId;
    private String clientId;
    private PluginContext pluginContext;

    public Liberin() {
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public Liberin withNodeId(UUID nodeId) {
        setNodeId(nodeId);
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public PluginContext getPluginContext() {
        return pluginContext;
    }

    public void setPluginContext(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }

    public void setPluginContext(RequestContext requestContext) {
        setPluginContext(new PluginContext(requestContext));
    }

    public void setPluginContext(UniversalContext universalContext) {
        setPluginContext(new PluginContext(universalContext));
    }

    public Liberin withPluginContext(RequestContext requestContext) {
        setPluginContext(requestContext);
        return this;
    }

    public Liberin withPluginContext(UniversalContext universalContext) {
        setPluginContext(universalContext);
        return this;
    }

    public String getTypeName() {
        String className = getClass().getSimpleName();
        String typeName = TYPE_NAMES.get(className);
        if (typeName != null) {
            return typeName;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < className.length() - 7; i++) { // "Liberin" suffix excluded
            char c = className.charAt(i);
            if (i != 0 && Character.isUpperCase(c)) {
                buf.append('-');
            }
            buf.append(Character.toLowerCase(c));
        }
        typeName = buf.toString();

        TYPE_NAMES.put(className, typeName);
        return typeName;
    }

    public final Map<String, Object> getModel(EntityManager entityManager) {
        Map<String, Object> model = new HashMap<>();
        model.put("type", getTypeName());
        model.put("nodeId", nodeId);
        model.put("context", pluginContext);
        toModel(model, entityManager);
        return model;
    }

    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        toModel(model);
    }

    protected void toModel(Map<String, Object> model) {
    }

}
