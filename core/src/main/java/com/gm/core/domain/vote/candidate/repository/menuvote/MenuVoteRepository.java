package com.gm.core.domain.vote.candidate.repository.menuvote;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteState;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSubmitResult;

/**
 * 진행 중인 메뉴 투표 선택과 집계를 임시 저장한다.
 * 사용자별 선택은 투표가 진행되는 동안만 보관하고, 투표 종료 후 후보별 최종 집계만 DB에 저장한다.
 */
public interface MenuVoteRepository {

    /**
     * 투표 세션을 열고 임시 데이터의 만료 시간을 설정한다.
     * 이미 닫힌 세션은 다시 열지 않는다.
     *
     * 저장소는 후보 소속 정보와 투표 가능 시간을 함께 보관하고, 신뢰 가능한 서버 시간으로 마감 시각을 계산한다.
     *
     * @param session 투표 세션의 불변 메타데이터
     */
    void initialize(MenuVoteSession session);

    /**
     * 사용자 선택을 새로 반영하거나 기존 선택을 변경한다.
     * 같은 선택을 다시 제출하면 집계를 변경하지 않는다.
     *
     * @param voteSessionId 투표 세션 ID
     * @param candidateId 메뉴 후보 ID
     * @param userId 사용자 ID
     * @param choice 사용자 선택
     * @return 선택 반영 결과와 최신 후보 집계
     */
    MenuVoteSubmitResult submit(
            UUID voteSessionId,
            UUID candidateId,
            UUID userId,
            MenuVoteChoice choice
    );

    /** 화면 재진입 시 사용할 투표 진행 상태, deadline, 후보별 최신 집계를 반환한다. */
    Optional<MenuVoteState> findState(UUID voteSessionId);

    /**
     * 세션을 닫고 후보별 최종 집계 스냅샷을 반환한다.
     * 저장 재시도를 위해 이미 닫힌 세션도 같은 스냅샷을 반환한다.
     *
     * @param voteSessionId 투표 세션 ID
     * @return 초기화할 때 전달한 후보 순서대로 정렬된 최종 집계
     */
    MenuVoteCloseResult closeAndGetSnapshot(UUID voteSessionId);

    /** 응답자가 한 명도 없으면 세션을 열어 둔 채 수동 마감을 거절한다. */
    MenuVoteCloseResult closeAndGetSnapshotIfAnyResponse(UUID voteSessionId);

    /**
     * DB 저장이 완료된 세션의 임시 투표 데이터를 제거한다.
     *
     * @param voteSessionId 투표 세션 ID
     */
    void delete(UUID voteSessionId);
}
