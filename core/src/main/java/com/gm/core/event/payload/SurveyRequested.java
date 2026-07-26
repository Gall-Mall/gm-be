package com.gm.core.event.payload;

import java.util.UUID;

import com.gm.core.event.DomainEvent;

/** 설문 요청 — 메뉴 후보 생성 트리거. */
public record SurveyRequested(UUID groupId, UUID voteSessionId) implements DomainEvent {

    public static final String TYPE = "group.survey.requested";

    @Override
    public String eventType() {
        return TYPE;
    }
}
