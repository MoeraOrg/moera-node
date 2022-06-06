package org.moera.node.data;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class ChildOperationsType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class<ChildOperations> returnedClass() {
        return ChildOperations.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet,
                              String[] names,
                              SharedSessionContractImplementor sharedSession,
                              Object owner) throws HibernateException, SQLException {

        String value = resultSet.getString(names[0]);

        if (resultSet.wasNull() || value == null) {
            return null;
        }

        try {
            return ChildOperations.decode(value);
        } catch (IOException e) {
            throw new HibernateException("Cannot decode value", e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement statement,
                            Object value,
                            int index,
                            SharedSessionContractImplementor sharedSession) throws HibernateException, SQLException {

        if (Objects.isNull(value)) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            try {
                statement.setString(index, ChildOperations.encode((ChildOperations) value));
            } catch (JsonProcessingException e) {
                throw new HibernateException("Cannot encode value", e);
            }
        }
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        try {
            return o != null ? ((ChildOperations) o).clone() : o;
        } catch (CloneNotSupportedException e) {
            throw new HibernateException("Cannot clone value", e);
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        try {
            return ChildOperations.encode((ChildOperations) o);
        } catch (JsonProcessingException e) {
            throw new HibernateException("Cannot encode value", e);
        }
    }

    @Override
    public Object assemble(Serializable serializable, Object o) throws HibernateException {
        try {
            return ChildOperations.decode((String) o);
        } catch (IOException e) {
            throw new HibernateException("Cannot decode value", e);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

}
