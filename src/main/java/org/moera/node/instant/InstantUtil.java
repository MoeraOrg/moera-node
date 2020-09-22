package org.moera.node.instant;

import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;

public class InstantUtil {

    public static String formatNodeName(String name) {
        NodeName nodeName = NodeName.parse(name);
        if (nodeName instanceof RegisteredName) {
            RegisteredName registeredName = (RegisteredName) nodeName;
            if (registeredName.getGeneration() != null) {
                return String.format("<span class=\"node-name\">%s<span class=\"generation\">%d</span></span>",
                        registeredName.getName(), registeredName.getGeneration());
            }
        }
        return String.format("<span class=\"node-name\">%s</span>", name);
    }

}
