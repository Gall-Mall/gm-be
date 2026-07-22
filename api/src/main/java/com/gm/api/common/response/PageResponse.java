package com.gm.api.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 페이지 단위로 조회한 목록을 감싸는 응답이다.
 *
 * @param content 현재 페이지의 항목 목록
 * @param page 현재 페이지 번호 (0부터 시작)
 * @param size 페이지당 최대 항목 수
 * @param totalElements 전체 항목 수
 * @param totalPages 전체 페이지 수
 * @param hasNext 다음 페이지 존재 여부
 * @param <T> 항목 타입
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
