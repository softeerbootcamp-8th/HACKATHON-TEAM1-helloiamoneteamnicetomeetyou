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

    /**
     * 어드민이 만든 사용자인지. 부스 시연에 세워 두는 더미다.
     *
     * <p><b>어드민이 대신 조작해도 되는 사람인지를 가르는 경계다.</b> 실제 참가자의 수락이나
     * 시간 선택을 운영자가 대신 눌러 버리면 그 사람이 하지 않은 일이 그 사람 이름으로 남는다.
     * 대리 조작은 이 값이 참인 사용자에게만 열어 둔다.
     *
     * <p>{@code false} 를 기본값으로 두어서, 화면에서 등록하는 기존 사용자는 컬럼이 늘어도
     * 그대로 진짜 참가자로 남는다.
     */
    @Column(nullable = false)
    private boolean adminManaged = false;

    private User(UUID id, String username, boolean adminManaged) {
        this.id = id;
        this.username = username;
        this.adminManaged = adminManaged;
        this.createdAt = LocalDateTime.now();
    }

    public static User of(UUID id) {
        return new User(id, null, false);
    }

    /**
     * 어드민이 만드는 사용자다. 화면에서 등록하는 사람과 달리 이름을 함께 받는다.
     *
     * <p>부스 운영자가 목록에서 누가 누군지 알아봐야 매칭에 손을 댈 수 있는데, 이름이 없으면
     * UUID 앞자리로 구분해야 해서 실수하기 쉽다.
     */
    public static User of(UUID id, String username) {
        return new User(id, username, false);
    }

    /** 어드민이 부스에 세워 두는 더미 사용자를 만든다. */
    public static User dummy(UUID id, String username) {
        return new User(id, username, true);
    }

    /** 어드민 화면에서 표시 이름을 고친다. */
    public void rename(String username) {
        this.username = username;
    }
}
