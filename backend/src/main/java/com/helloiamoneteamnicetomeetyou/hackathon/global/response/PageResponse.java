package com.helloiamoneteamnicetomeetyou.hackathon.global.response;

import java.util.List;

/**
 * 목록 응답의 공통 껍데기다. {@code CommonResponse.data} 자리에 그대로 들어간다.
 *
 * <p>형식은 {@code contracts.md} 가 기준이라 필드를 마음대로 늘리거나 이름을 바꾸지 않는다.
 *
 * @param content    이번 페이지에 담긴 것
 * @param nextCursor 커서 방식으로 바꿀 때를 위해 자리만 잡아 둔 값이다. offset 을 쓰는 동안
 *                   항상 {@code null} 이다. 무한 스크롤이 필요해지면 팀에서 정한다
 * @param hasNext    다음 페이지가 있는지
 * @param size       <b>요청한 크기가 아니라 실제로 담긴 {@code content} 의 개수다.</b>
 *                   프론트가 마지막 페이지를 알아보는 데 쓴다
 */
public record PageResponse<T>(List<T> content, String nextCursor, boolean hasNext, int size) {

    public static <T> PageResponse<T> of(List<T> content, boolean hasNext) {
        return new PageResponse<>(content, null, hasNext, content.size());
    }
}
