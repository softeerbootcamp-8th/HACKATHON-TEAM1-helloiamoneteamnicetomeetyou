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
     * 화면에 보여 줄 이름이다. 지금은 채우지 않는다.
     *
     * <p>목업이 나오면 사용자를 무엇으로 표시할지 정해질 텐데, 그때까지 자리만 비워 둔다.
     * 컬럼을 지웠다 다시 만드는 것보다 제약만 푸는 쪽이 변경이 작다.
     */
    @Column(length = 50)
    private String username;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private User(UUID id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public static User of(UUID id) {
        return new User(id);
    }
}
