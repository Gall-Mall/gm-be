package com.gm.mq.support;

import java.util.function.Consumer;

import org.springframework.amqp.core.Message;

/**
 * 역직렬화와 멱등 판정을 한 호출로 묶은 파사드.
 * 나눠두면 리스너가 멱등 래핑을 빠뜨려도 컴파일되므로 합쳐서 실수를 막는다.
 */
public interface EventConsumer {

    /** 메시지를 payloadType으로 읽고, 처음 보는 이벤트일 때만 task를 실행한다. */
    <T> void consumeOnce(Message message, Class<T> payloadType, Consumer<T> task);
}
