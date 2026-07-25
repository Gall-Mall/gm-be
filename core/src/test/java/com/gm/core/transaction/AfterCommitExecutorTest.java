package com.gm.core.transaction;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class AfterCommitExecutorTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("활성 트랜잭션에서는 커밋 전까지 작업을 실행하지 않는다")
    void execute_defersWorkUntilAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean executed = new AtomicBoolean();

        new AfterCommitExecutor().execute(() -> executed.set(true));

        assertThat(executed).isFalse();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(executed).isTrue();
    }

    @Test
    @DisplayName("롤백 완료 시에는 커밋 후 작업을 실행하지 않는다")
    void execute_doesNotRunWorkAfterRollback() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean executed = new AtomicBoolean();

        new AfterCommitExecutor().execute(() -> executed.set(true));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                ));

        assertThat(executed).isFalse();
    }
}
