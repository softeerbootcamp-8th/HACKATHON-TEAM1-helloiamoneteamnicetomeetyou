package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;

import java.util.List;
import java.util.UUID;

/**
 * 대기장소에 세워 두는 더미 사용자다.
 *
 * <p><b>UUID 가 프론트의 {@code mocks/data.ts} 와 같은 값이어야 한다.</b> 화면이 목업 매칭으로
 * 고른 상대를 서버가 알아보고, 그 사람이 고른 시간이 실제로 DB 에서 읽혀야 하기 때문이다.
 * 값을 바꾸면 양쪽을 같이 고친다.
 *
 * <p>UUID 뒷자리를 {@code ...0001} 처럼 순번으로 둔 것은 DB 를 눈으로 볼 때 누가 누군지 바로
 * 알아보기 위해서다.
 */
public record DemoUser(String key, UUID id, String username) {

    public static final List<DemoUser> ALL = List.of(
            new DemoUser("u1", uuid(1), "캐스퍼"),
            new DemoUser("u2", uuid(2), "블루N"),
            new DemoUser("u3", uuid(3), "아이오닉러버"),
            new DemoUser("u4", uuid(4), "N드라이버"),
            new DemoUser("u5", uuid(5), "그랜저러버"),
            new DemoUser("u6", uuid(6), "포니덕후"),
            new DemoUser("u7", uuid(7), "레몬 16"),
            new DemoUser("u8", uuid(8), "레몬 07"),
            new DemoUser("u9", uuid(9), "싼타페러버"),
            new DemoUser("u10", uuid(10), "비전러버"));

    private static UUID uuid(int index) {
        return UUID.fromString("00000000-0000-4000-8000-%012d".formatted(index));
    }

    public static boolean isDemo(UUID userId) {
        return ALL.stream().anyMatch(user -> user.id().equals(userId));
    }
}
