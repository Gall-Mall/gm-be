package com.gm.core.domain.vote.session.model;

/**
 * 투표 세션의 진행 상태를 나타낸다.
 */
public enum VoteSessionStatus {

    /**
     * 그룹원들이 선호 음식과 비선호 음식을 입력하는 단계.
     */
    PREFERENCE_INPUT,

    /**
     * 입력된 선호 정보를 바탕으로 투표할 메뉴 후보를 생성하는 단계.
     */
    MENU_RECOMMENDING,

    /**
     * 그룹원들이 추천된 메뉴에 갈래, 애매, 말래로 투표하는 단계.
     */
    MENU_VOTING,

    /**
     * 후보로 남은 메뉴 중 최종 메뉴 하나를 선택하는 단계.
     */
    MENU_SELECTION,

    /**
     * 확정된 메뉴를 판매하는 주변 식당을 검색하는 단계.
     */
    RESTAURANT_SEARCHING,

    /**
     * 검색된 식당 중 최종 식당 하나를 선택하는 단계.
     */
    RESTAURANT_SELECTION,

    /**
     * 메뉴와 식당 선택까지 정상적으로 완료된 상태.
     */
    COMPLETED,

    /**
     * 사용자 또는 시스템에 의해 투표 세션이 취소된 상태.
     */
    CANCELLED,

    /**
     * 추천, 식당 검색 또는 비동기 처리 오류로 세션이 실패한 상태.
     */
    FAILED;

    /**
     * 다음 상태로 변경할 수 있는지 확인한다.
     *
     * @param nextStatus 변경할 상태
     * @return 변경할 수 있으면 {@code true}
     */
    public boolean canTransitionTo(VoteSessionStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }

        return switch (this) {
            case PREFERENCE_INPUT -> nextStatus == MENU_RECOMMENDING
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case MENU_RECOMMENDING -> nextStatus == MENU_VOTING
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case MENU_VOTING -> nextStatus == MENU_RECOMMENDING
                    || nextStatus == MENU_SELECTION
                    || nextStatus == RESTAURANT_SEARCHING
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case MENU_SELECTION -> nextStatus == MENU_RECOMMENDING
                    || nextStatus == RESTAURANT_SEARCHING
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case RESTAURANT_SEARCHING -> nextStatus == RESTAURANT_SELECTION
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case RESTAURANT_SELECTION -> nextStatus == COMPLETED
                    || nextStatus == CANCELLED
                    || nextStatus == FAILED;
            case COMPLETED, CANCELLED, FAILED -> false;
        };
    }
}