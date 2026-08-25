package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /**
     * 클라이언트가 만들어 보내는 UUID 다. 로그인이 없어서 이 값이 곧 신원이다.
     *
     * <p>서버가 만들지 않으므로 {@code @GeneratedValue} 가 없다. 저장 타입을 Hibernate 기본값인
     * binary(16) 대신 varchar(36) 으로 둔 것은, 해커톤 기간에는 조회 결과를 눈으로 읽는 편이
     * 공간을 아끼는 것보다 낫기 때문이다.
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    /**
     * 화면에 보여 줄 이름이다. 약속 화면에서 상대 줄의 라벨이 된다.
     *
     * <p>등록할 때 클라이언트가 함께 보낸다. 아직 안 보낸 사용자가 있을 수 있어서 null 을 허용하고,
     * 화면은 비어 있으면 "상대" 로 대신 보여 준다.
     */
    @Column(length = 50)
    private String username;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private User(UUID id, String username) {
        this.id = id;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    public static User of(UUID id, String username) {
        return new User(id, username);
    }

    /**
     * 이름을 고친다. 빈 값이면 무시한다.
     *
     * <p>등록 API 가 멱등이라 앱을 열 때마다 불리는데, 그때 이름을 안 보낸 요청이 이미 있던 이름을
     * 지워 버리면 상대 화면에서 이름이 사라진다.
     */
    public void changeUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        this.username = username;
    }
}
