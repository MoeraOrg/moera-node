package org.moera.node.auth.principal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class PrincipalType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class<Principal> returnedClass() {
        return Principal.class;
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

        return new Principal(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement statement,
                            Object value,
                            int index,
                            SharedSessionContractImplementor sharedSession) throws HibernateException, SQLException {

        if (Objects.isNull(value)) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, ((Principal) value).getValue());
        }
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        return o != null ? ((Principal) o).clone() : o;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        return o != null ? ((Principal) o).getValue() : null;
    }

    @Override
    public Object assemble(Serializable serializable, Object o) throws HibernateException {
        return o != null ? new Principal((String) o) : null;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

}
