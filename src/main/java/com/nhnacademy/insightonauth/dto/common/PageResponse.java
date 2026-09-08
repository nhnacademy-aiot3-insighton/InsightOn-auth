package com.nhnacademy.insightonauth.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 목록 조회 응답. Spring Data {@link Page}(PageImpl) 를 그대로 직렬화하면
 * 포맷이 버전 의존적이고 클라이언트가 역직렬화하기 까다로워서, 안정적인 평범한 record 로 감싼다.
 * 필드명은 프론트(InsightOn-front)의 {@code common.dto.PageResponse} 와 맞춘다.
 */
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty,
        int numberOfElements
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                page.getNumberOfElements()
        );
    }
}
