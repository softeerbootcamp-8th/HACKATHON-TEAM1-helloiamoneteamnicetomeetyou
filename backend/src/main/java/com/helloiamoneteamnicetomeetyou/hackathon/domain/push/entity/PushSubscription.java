package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브라우저 하나가 푸시를 받기 위해 등록한 정보다.
 *
 * <p>사람이 아니라 <b>브라우저 설치본</b>이 단위다. 같은 사람이 폰과 노트북에서 각각 켜면 행이
 * 두 개 생기고 둘 다 알림을 받는다. 정상이다.
 */
@Entity
@Table(
        name = "push_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_push_subscriptions_endpoint", columnNames = "endpoint"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 푸시 서비스(APNs, FCM)가 준 이 구독의 주소다. 이게 구독의 정체성이라 unique 다.
     *
     * <p>길이 700 은 MySQL InnoDB 인덱스 키 상한 때문이다. DYNAMIC row format 의 상한이
     * 3072바이트인데 utf8mb4 는 문자당 최대 4바이트라, unique 를 걸 수 있는 최대가 768자다.
     * 실측 endpoint 는 500자를 넘지 않으므로 여유를 두면서 상한 아래로 잡았다.
     */
    @Column(nullable = false, length = 700)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = LocalDateTime.now();
    }

    public static PushSubscription of(User user, String endpoint, String p256dh, String auth) {
        return new PushSubscription(user, endpoint, p256dh, auth);
    }

    /** 같은 브라우저가 다시 구독하면 endpoint 는 같고 키만 바뀔 수 있다. 쓰는 사람도 바뀔 수 있다. */
    public void rebind(User user, String p256dh, String auth) {
        this.user = user;
        this.p256dh = p256dh;
        this.auth = auth;
    }
}
