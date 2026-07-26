package com.gm.mq.support;

import java.util.function.Consumer;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.event.ProcessedEventStore;

import tools.jackson.databind.ObjectMapper;

/**
 * DB를 건드리는 리스너용. 처리 기록과 비즈니스 로직을 한 트랜잭션으로 묶는다(inbox).
 *
 * <p>여기서 트랜잭션을 열어야 안쪽 서비스의 @Transactional이 참여한다.
 * 열지 않으면 기록과 처리가 서로 다른 트랜잭션이 되어 inbox가 성립하지 않는다.</p>
 *
 * <p>프로세스가 죽어도 기록이 함께 롤백되므로 재전달분이 정상 처리된다.
 * DB를 쓰지 않는 리스너는 이 보장이 없는 {@link DedupOnlyEventConsumer}를 쓴다.</p>
 */
@Component("inboxEventConsumer")
public class InboxEventConsumer extends AbstractEventConsumer {

    public InboxEventConsumer(ObjectMapper objectMapper,
                              @Qualifier("dbProcessedEventStore") ProcessedEventStore store) {
        super(objectMapper, store);
    }

    @Override
    @Transactional
    public <T> void consumeOnce(Message message, Class<T> payloadType, Consumer<T> task) {
        super.consumeOnce(message, payloadType, task);
    }
}
