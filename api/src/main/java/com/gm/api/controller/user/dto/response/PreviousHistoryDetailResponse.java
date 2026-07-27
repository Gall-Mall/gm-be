package com.gm.api.controller.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousHistoryDetail;
import com.gm.core.domain.history.model.PreviousMenuCandidateHistory;
import com.gm.core.domain.history.model.PreviousVoteSessionHistory;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;

public record PreviousHistoryDetailResponse(
        UUID groupId,
        String groupName,
        UUID voteSessionId,
        String name,
        String url,
        String address,
        Double latitude,
        Double longitude,
        Integer distanceM,
        String externalPlaceId,
        int goCount,
        int maybeCount,
        int noCount,
        List<MenuCandidateResponse> menuCandidates,
        LocalDateTime completedAt
) {

    public static PreviousHistoryDetailResponse from(PreviousHistoryDetail detail) {
        PreviousVoteSessionHistory voteSession = detail.voteSession();
        return new PreviousHistoryDetailResponse(
                detail.groupId(),
                detail.groupName(),
                voteSession.voteSessionId(),
                voteSession.name(),
                voteSession.url(),
                voteSession.address(),
                voteSession.latitude(),
                voteSession.longitude(),
                voteSession.distanceM(),
                voteSession.externalPlaceId(),
                voteSession.goCount(),
                voteSession.maybeCount(),
                voteSession.noCount(),
                detail.menuCandidates().stream()
                        .map(MenuCandidateResponse::from)
                        .toList(),
                voteSession.completedAt()
        );
    }

    /** 지난 기록에 포함할 메뉴 후보별 최종 투표 결과 응답이다. */
    public record MenuCandidateResponse(
            UUID menuId,
            String name,
            String imageUrl,
            int displayOrder,
            boolean selected,
            int goCount,
            int maybeCount,
            int noCount,
            int respondentCount,
            VoteCandidateResult resultStatus
    ) {
        /**
         * 완료된 메뉴 후보 스냅샷을 API 응답으로 변환한다.
         *
         * @param candidate 완료 세션의 메뉴 후보 스냅샷
         * @return 메뉴 후보별 최종 투표 결과 응답
         */
        private static MenuCandidateResponse from(PreviousMenuCandidateHistory candidate) {
            return new MenuCandidateResponse(
                    candidate.menuId(),
                    candidate.name(),
                    candidate.imageUrl(),
                    candidate.displayOrder(),
                    candidate.selected(),
                    candidate.goCount(),
                    candidate.maybeCount(),
                    candidate.noCount(),
                    candidate.respondentCount(),
                    candidate.resultStatus()
            );
        }
    }
}
