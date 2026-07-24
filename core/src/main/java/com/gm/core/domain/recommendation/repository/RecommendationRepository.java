package com.gm.core.domain.recommendation.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;

public interface RecommendationRepository {
    List<MemberPreference> findMemberPreferencesByGroupId(UUID groupId);
    List<MenuInfo> findAllMenus();
    Map<UUID, Recency> findRecencyByGroupId(UUID groupId);
    Map<UUID, Double> findMenuPopularity();
}
