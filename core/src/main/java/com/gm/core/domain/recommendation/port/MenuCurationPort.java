package com.gm.core.domain.recommendation.port;

import java.util.List;

import com.gm.core.domain.recommendation.model.CuratedMenu;
import com.gm.core.domain.recommendation.model.MenuCurationCommand;

/**
 * 결정론 후보에 대한 AI 메뉴 큐레이션을 위임하는 포트.
 *
 * 보안 제약: AI는 DB에 접근하지 않는다. 앱이 후보 메뉴 이름과 멤버 소프트 신호(자유텍스트)를
 * 넘기고, AI는 후보 목록 안의 이름만 반환한다. 이름→UUID 매핑·저장은 코드가 한다.
 *
 * 역할: 표준 알레르기는 결정론 필터가 이미 제외했으므로, AI는 비표준 알레르기(자유텍스트)
 * 하드 제외 + 그룹 취향·다양성을 고려한 최종 선정 + 추천 이유 생성만 담당한다.
 */
public interface MenuCurationPort {

    /** 후보 메뉴를 큐레이션한다. 반환은 검증 전이라 후보 목록 밖 이름이 섞일 수 있다. */
    List<CuratedMenu> curate(MenuCurationCommand command);
}
