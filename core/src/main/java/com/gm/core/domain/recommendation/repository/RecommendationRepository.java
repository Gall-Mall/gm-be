package com.gm.core.domain.recommendation.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gm.core.domain.recommendation.model.GroupSoftSignals;
import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;

public interface RecommendationRepository {
    List<MemberPreference> findMemberPreferencesByGroupId(UUID groupId);

    /** ACTIVE 멤버들의 자유텍스트 신호를 모은다. AI 큐레이션 입력으로 쓴다. */
    GroupSoftSignals findSoftSignalsByGroupId(UUID groupId);
    List<MenuInfo> findAllMenus();
    Map<UUID, Recency> findRecencyByGroupId(UUID groupId);
    Map<UUID, Double> findMenuPopularity();
}
