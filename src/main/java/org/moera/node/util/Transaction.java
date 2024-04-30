package org.moera.node.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Component
public class Transaction {

    @Inject
    private PlatformTransactionManager txManager;

    public <T> T executeRead(Callable<T> inside) throws Exception {
        return execute(inside, true);
    }

    public void executeRead(CallableVoid inside) throws Exception {
        execute(inside, true);
    }

    public <T> T executeReadQuietly(Callable<T> inside, T defaultValue) {
        try {
            return execute(inside, true);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public <T> T executeReadQuietly(Callable<T> inside, Function<Throwable, T> onError) {
        try {
            return execute(inside, true);
        } catch (Throwable e) {
            if (onError != null) {
                return onError.apply(e);
            }
            return null;
        }
    }

    public <T> T executeReadQuietly(Callable<T> inside, CallableNoExceptions<T> onError) {
        try {
            return execute(inside, true);
        } catch (Throwable e) {
            if (onError != null) {
                return onError.call();
            }
            return null;
        }
    }

    public void executeReadQuietly(CallableVoid inside, Consumer<Throwable> onError) {
        try {
            execute(inside, true);
        } catch (Throwable e) {
            if (onError != null) {
                onError.accept(e);
            }
        }
    }

    public void executeReadQuietly(CallableVoid inside, Runnable onError) {
        try {
            execute(inside, true);
        } catch (Throwable e) {
            if (onError != null) {
                onError.run();
            }
        }
    }

    public <T> T executeReadQuietly(Callable<T> inside) {
        return executeReadQuietly(inside, (T) null);
    }

    public void executeReadQuietly(CallableVoid inside) {
        executeReadQuietly(inside, (Runnable) null);
    }

    public <T> T executeWrite(Callable<T> inside) throws Exception {
        return execute(inside, false);
    }

    public void executeWrite(CallableVoid inside) throws Exception {
        execute(inside, false);
    }

    public <T> T executeWriteQuietly(Callable<T> inside, T defaultValue) {
        try {
            return execute(inside, false);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public <T> T executeWriteQuietly(Callable<T> inside, Function<Throwable, T> onError) {
        try {
            return execute(inside, false);
        } catch (Throwable e) {
            if (onError != null) {
                return onError.apply(e);
            }
            return null;
        }
    }

    public <T> T executeWriteQuietly(Callable<T> inside, CallableNoExceptions<T> onError) {
        try {
            return execute(inside, false);
        } catch (Throwable e) {
            if (onError != null) {
                return onError.call();
            }
            return null;
        }
    }

    public void executeWriteQuietly(CallableVoid inside, Consumer<Throwable> onError) {
        try {
            execute(inside, false);
        } catch (Throwable e) {
            if (onError != null) {
                onError.accept(e);
            }
            // ignore
        }
    }

    public void executeWriteQuietly(CallableVoid inside, Runnable onError) {
        try {
            execute(inside, false);
        } catch (Throwable e) {
            if (onError != null) {
                onError.run();
            }
            // ignore
        }
    }

    public <T> T executeWriteQuietly(Callable<T> inside) {
        return executeWriteQuietly(inside, (T) null);
    }

    public void executeWriteQuietly(CallableVoid inside) {
        executeWriteQuietly(inside, (Runnable) null);
    }

    private <T> T execute(Callable<T> inside, boolean readOnly) throws Exception {
        TransactionStatus status = beginTransaction(readOnly);
        T result;
        try {
            result = inside.call();
            commitTransaction(status);
        } catch (Throwable e) {
            if (!isCompleted(status)) {
                rollbackTransaction(status);
            }
            throw e;
        }
        return result;
    }

    private void execute(CallableVoid inside, boolean readOnly) throws Exception {
        TransactionStatus status = beginTransaction(readOnly);
        try {
            inside.call();
            commitTransaction(status);
        } catch (Throwable e) {
            if (!isCompleted(status)) {
                rollbackTransaction(status);
            }
            throw e;
        }
    }

    private TransactionStatus beginTransaction(boolean readOnly) {
        if (txManager == null) {
            return null;
        }
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.setReadOnly(readOnly);
        return txManager.getTransaction(definition);
    }

    private boolean isCompleted(TransactionStatus status) {
        return status == null || status.isCompleted();
    }

    private void commitTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.commit(status);
        }
    }

    private void rollbackTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.rollback(status);
        }
    }

}
