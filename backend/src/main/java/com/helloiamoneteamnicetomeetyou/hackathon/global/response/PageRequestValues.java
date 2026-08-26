package com.helloiamoneteamnicetomeetyou.hackathon.global.response;

import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;

/**
 * offset 페이징 파라미터를 검증하고 잘라내는 자리다.
 *
 * <p>Bean Validation 이 아직 없어서 {@code page}, {@code size} 검증을 손으로 한다. 목록 API 가
 * 늘어날 때마다 같은 검증을 다시 쓰지 않도록 여기 모아 둔다.
 *
 * <p>정렬까지 끝난 목록을 메모리에서 자른다. 부스 규모(카드 수십 장, 참가자 수십 명)에서는
 * 전부 읽어 와서 자르는 편이, 행마다 달라지는 정렬 기준을 SQL 로 옮기는 것보다 싸고 읽기 쉽다.
 */
public final class PageRequestValues {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestValues() {}

    /** 정렬이 끝난 전체 목록에서 해당 페이지만 떼어낸다. */
    public static <T> PageResponse<T> slice(List<T> sorted, int page, int size) {
        validate(page, size);

        // page * size 를 int 로 곱하면 큰 page 에서 넘쳐 음수가 되고 subList 가 터진다.
        int from = (int) Math.min((long) page * size, sorted.size());
        int to = (int) Math.min((long) from + size, sorted.size());

        return PageResponse.of(sorted.subList(from, to), to < sorted.size());
    }

    private static void validate(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
    }
}
