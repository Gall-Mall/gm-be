package com.gm.core.event;

public interface EventPublisher {
    void publish(String routingKey, Object payload);
}
