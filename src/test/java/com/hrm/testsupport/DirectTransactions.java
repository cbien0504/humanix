package com.hrm.testsupport;

import com.hrm.dao.TransactionManager;

import java.util.function.Supplier;

/**
 * Pass-through {@link TransactionManager} for service tests. It records what a real one would
 * do: commit when the work returns, roll back when it throws.
 */
public final class DirectTransactions implements TransactionManager {

    private int started;
    private int committed;
    private int rolledBack;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        started++;
        try {
            T result = work.get();
            committed++;
            return result;
        } catch (RuntimeException e) {
            rolledBack++;
            throw e;
        }
    }

    public int started() {
        return started;
    }

    public int committed() {
        return committed;
    }

    public int rolledBack() {
        return rolledBack;
    }
}
