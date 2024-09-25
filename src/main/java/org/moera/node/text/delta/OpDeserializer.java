package org.moera.node.text.delta;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpDeserializer extends JsonDeserializer<Op> {

    @Override
    public Op deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Handle attribute only insert
        var codec = jsonParser.getCodec();
        var node = (ObjectNode) codec.readTree(jsonParser);
        AttributeMap map = null;
        if (node.findValue("attributes") != null) {
            map = codec.treeToValue(node.findValue("attributes"), AttributeMap.class);
        }
        var insertNode = node.findValue("insert");
        if (insertNode == null || insertNode.isTextual()) {
            if (insertNode != null) {
                return Op.insert(insertNode.asText(), map);
            }
            var deleteNode = node.findValue("delete");
            if (deleteNode != null) {
                return Op.delete(deleteNode.asInt());
            }
            return Op.retain(node.findValue("retain").asInt(), map);
        }
        var object = codec.treeToValue(node.get("insert"), Map.class);
        return Op.insert(object);
    }

}
