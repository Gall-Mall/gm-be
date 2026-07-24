package com.gm.core.domain.vote.session.service;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

/**
 * 투표 세션의 생성·조회·취소·삭제 유스케이스를 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final VoteSessionRepository voteSessionRepository;
    private final GroupService groupService;

    /**
     * 수동 투표 세션을 생성하고 저장한다.
     *
     * @param diningGroupId 세션이 속한 식사 그룹 식별자
     * @param requestUserId 생성을 요청한 회원 식별자
     * @param title 세션 제목
     * @param likeKeyword 종합 선호 키워드
     * @param dislikeKeyword 종합 비선호 키워드
     * @return 저장된 투표 세션
     * @throws IllegalArgumentException 그룹 식별자가 없거나 제목이 비어 있는 경우
     */
    @Transactional
    public VoteSession createManualVoteSession(
            UUID diningGroupId,
            UUID requestUserId,
            String title,
            String likeKeyword,
            String dislikeKeyword
    ) {
        groupService.findGroupDetail(diningGroupId, requestUserId);
        VoteSession voteSession = VoteSession.createVoteSession(
                diningGroupId,
                title,
                likeKeyword,
                dislikeKeyword
        );

        return voteSessionRepository.save(voteSession);
    }

    /**
     * 식별자로 투표 세션을 조회한다.
     *
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 조회된 투표 세션
     * @throws VoteSessionException 세션이 존재하지 않아 {@code SESSION-001} 오류가 발생하는 경우
     */
    @Transactional(readOnly = true)
    public VoteSession findVoteSession(UUID voteSessionId) {
        return voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() ->
                        new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 투표 상태를 변경한다.
     *
     * @param voteSessionId 변경할 투표 세션 식별자
     * @param nextStatus 변경할 상태
     * @return 상태가 변경된 투표 세션
     * @throws VoteSessionException 세션이 없거나 변경할 수 없는 상태인 경우
     */
    @Transactional
    public VoteSession changeVoteSessionStatus(
            UUID voteSessionId,
            VoteSessionStatus nextStatus
    ) {
        VoteSession voteSession = findVoteSession(voteSessionId);
        VoteSession changed = voteSession.changeStatus(nextStatus);

        return voteSessionRepository.updateStatus(
                        voteSessionId,
                        changed.voteSessionStatus()
                )
                .orElseThrow(() ->
                        new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 진행 중인 투표를 취소한다.
     *
     * @param voteSessionId 취소할 투표 세션 식별자
     * @param requestId 취소를 요청한 사용자 식별자
     * @return 취소된 투표 세션
     * @throws VoteSessionException 세션이 없거나 취소할 수 없는 상태인 경우
     */
    @Transactional
    public VoteSession cancelVoteSession(UUID voteSessionId, UUID requestId) {
        VoteSession voteSession = findVoteSession(voteSessionId);
        VoteSession cancelled = voteSession.cancel(LocalDateTime.now());

        // TODO: requestId 통해서 그룹 리더인지 확인

        return voteSessionRepository.cancel(voteSessionId, cancelled.closedAt())
                .orElseThrow(() ->
                        new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 지정한 투표 세션을 영구 삭제한다.
     *
     * <p>그룹장 권한 검증은 그룹 도메인 연동 시 추가한다.</p>
     *
     * @param voteSessionId 삭제할 투표 세션 식별자
     * @param requestId 삭제를 요청한 사용자 식별자
     * @throws VoteSessionException 세션이 존재하지 않아 {@code SESSION-001} 오류가 발생하는 경우
     */
    @Transactional
    public void deleteVoteSession(UUID voteSessionId, UUID requestId) {

        VoteSession voteSession = voteSessionRepository.findById(voteSessionId)
                        .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));

        // TODO: requestId 통해서 그룹 리더인지 확인

        voteSessionRepository.deleteById(voteSessionId);
    }

}
