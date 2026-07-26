package com.gm.core.domain.vote.event;

/** Core의 투표 이벤트를 외부 실시간 전송 어댑터에 전달하는 출력 포트다. */
@FunctionalInterface
public interface VoteSocketEventPublisher {
    /** 투표 상태 변경 이벤트를 연결된 클라이언트에 전달한다. */
    void publish(VoteSocketEvent event);
}
