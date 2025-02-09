package org.moera.node.data;

import java.io.IOException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.HibernateException;

@Converter(autoApply = true)
public class ChildOperationsAttributeConverter implements AttributeConverter<ChildOperations, String> {

    @Override
    public String convertToDatabaseColumn(ChildOperations childOperations) {
        try {
            return childOperations != null ? ChildOperations.encode(childOperations) : null;
        } catch (JsonProcessingException e) {
            throw new HibernateException("Cannot encode value", e);
        }
    }

    @Override
    public ChildOperations convertToEntityAttribute(String s) {
        try {
            return s != null ? ChildOperations.decode(s) : null;
        } catch (IOException e) {
            throw new HibernateException("Cannot decode value", e);
        }
    }

}
