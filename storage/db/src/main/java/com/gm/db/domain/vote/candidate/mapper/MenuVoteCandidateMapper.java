package com.gm.db.domain.vote.candidate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gm.core.domain.vote.candidate.model.menu.MenuVoteCandidate;
import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import com.gm.db.domain.menu.menu.entity.MenuEntity;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;

/**
 * 별도로 조회한 후보·메뉴·카테고리를 core 조회 모델로 변환한다.
 */
@Mapper(componentModel = "spring")
public interface MenuVoteCandidateMapper {

    /** 후보와 메뉴·카테고리를 투표 화면 조회 모델로 변환한다. */
    @Mapping(target = "voteCandidateId", source = "candidate.id")
    @Mapping(target = "voteSessionId", source = "candidate.voteSessionId")
    @Mapping(target = "menuId", source = "candidate.menuId")
    @Mapping(target = "categoryId", source = "menu.categoryId")
    @Mapping(target = "menuName", source = "menu.name")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "imageUrl", source = "menu.imageUrl")
    @Mapping(target = "displayOrder", source = "candidate.displayOrder")
    @Mapping(target = "goCount", source = "candidate.goCount", defaultValue = "0")
    @Mapping(target = "maybeCount", source = "candidate.maybeCount", defaultValue = "0")
    @Mapping(target = "noCount", source = "candidate.noCount", defaultValue = "0")
    @Mapping(target = "respondentCount", source = "candidate.respondentCount", defaultValue = "0")
    @Mapping(target = "resultStatus", source = "candidate.resultStatus")
    @Mapping(target = "description", source = "candidate.description")
    MenuVoteCandidate toDomain(
            VoteCandidateEntity candidate,
            MenuEntity menu,
            FoodCategoryEntity category
    );
}
