package org.moera.node.util;

import java.util.concurrent.Callable;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class Transaction {

    public static <T> T execute(PlatformTransactionManager txManager, Callable<T> inside) throws Throwable {
        TransactionStatus status = beginTransaction(txManager);
        T result;
        try {
            result = inside.call();
            commitTransaction(txManager, status);
        } catch (Throwable e) {
            rollbackTransaction(txManager, status);
            throw e;
        }
        return result;
    }

    public static <T> T executeQuietly(PlatformTransactionManager txManager, Callable<T> inside) {
        try {
            return execute(txManager, inside);
        } catch (Throwable e) {
            return null;
        }
    }

    private static TransactionStatus beginTransaction(PlatformTransactionManager txManager) {
        if (txManager == null) {
            return null;
        }
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return txManager.getTransaction(definition);
    }

    private static void commitTransaction(PlatformTransactionManager txManager, TransactionStatus status) {
        if (status != null) {
            txManager.commit(status);
        }
    }

    private static void rollbackTransaction(PlatformTransactionManager txManager, TransactionStatus status) {
        if (status != null) {
            txManager.rollback(status);
        }
    }

}
