package com.gm.mq.support;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.gm.core.event.ProcessedEventStore;

import tools.jackson.databind.ObjectMapper;

/**
 * DB를 건드리지 않는 리스너용. 화면 push나 외부 알림에 쓴다.
 *
 * <p><b>보장 수준이 낮다.</b> 기록과 처리가 한 트랜잭션이 아니라, 기록 직후 프로세스가
 * 죽으면 되돌릴 사람이 없다. 그러면 재전달분이 TTL 동안 "이미 처리됨"으로 스킵되어
 * <b>처리되지 않은 채 사라진다</b>. 중복이 아니라 유실이 위험이다.</p>
 *
 * <p>DB 쓰기가 생기는 리스너는 반드시 {@link InboxEventConsumer}로 옮길 것.</p>
 */
@Component("dedupOnlyEventConsumer")
public class DedupOnlyEventConsumer extends AbstractEventConsumer {

    public DedupOnlyEventConsumer(ObjectMapper objectMapper,
                                  @Qualifier("redisProcessedEventStore") ProcessedEventStore store) {
        super(objectMapper, store);
    }
}
