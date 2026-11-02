package com.hrm.dao;

import java.util.function.Supplier;

/**
 * Runs a unit of work in one database transaction. Nested calls join the outer transaction,
 * so a service method can call other transactional service methods safely.
 * Services depend on this abstraction only (DIP); tests use a pass-through fake.
 */
public interface TransactionManager {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
