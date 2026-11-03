package com.hrm.dao;

import java.util.function.Supplier;

public interface TransactionManager {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
