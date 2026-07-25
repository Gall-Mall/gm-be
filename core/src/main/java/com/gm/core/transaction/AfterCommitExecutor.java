package com.gm.core.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션이 실제 커밋된 뒤에만 외부 저장소 후속 작업을 실행한다.
 */
@Component
public class AfterCommitExecutor {

    /**
     * 활성 트랜잭션에서는 커밋 후 실행하고, 트랜잭션 밖에서는 즉시 실행한다.
     *
     * @param action 커밋 후 실행할 작업
     */
    public void execute(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
