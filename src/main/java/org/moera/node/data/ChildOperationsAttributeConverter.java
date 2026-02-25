package org.moera.node.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChildOperationsAttributeConverter implements AttributeConverter<ChildOperations, String> {

    @Override
    public String convertToDatabaseColumn(ChildOperations childOperations) {
        return childOperations != null ? ChildOperations.encode(childOperations) : null;
    }

    @Override
    public ChildOperations convertToEntityAttribute(String s) {
        return s != null ? ChildOperations.decode(s) : null;
    }

}
