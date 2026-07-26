package com.gm.core.event.payload;

import java.util.UUID;

import com.gm.core.event.DomainEvent;

/** 메뉴 후보 생성 완료 — 멤버 화면 push 트리거. */
public record SurveyListReady(UUID groupId, UUID voteSessionId) implements DomainEvent {

    public static final String TYPE = "group.survey.list.ready";

    @Override
    public String eventType() {
        return TYPE;
    }
}
