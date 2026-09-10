package org.moera.node.util;

import java.sql.Connection;
import javax.sql.DataSource;

import org.moera.node.global.UniversalContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaTransactionTestSupport {

    public static Transaction transaction() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> mock(Connection.class));
        Transaction tx = new Transaction();
        ReflectionTestUtils.setField(tx, "txManager", new DataSourceTransactionManager(dataSource));
        ReflectionTestUtils.setField(tx, "universalContext", mock(UniversalContext.class));
        return tx;
    }

    public static void assertOutsideTransaction() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Network I/O holds a transaction");
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty(), "Network I/O holds a connection");
    }

    public static void assertInsideTransaction() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    }

}
