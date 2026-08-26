package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간으로 나간 알림을 저장해 둔 한 건.
 *
 * <p>SSE 는 기기가 꺼져 있는 동안의 이벤트를 다시 보내지 않는다. 그래서 실시간 전송과 별개로
 * 여기 저장해 두고, 다시 켰을 때 목록 조회 API 로 놓친 것까지 확인할 수 있게 한다.
 *
 * <p>문구({@code title}, {@code body})는 보낼 때의 값을 그대로 굳혀 둔다. 나중에 알림 문구가
 * 바뀌어도 이미 쌓인 알림의 내용은 보낸 시점 그대로 남아야 하기 때문이다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** 실시간 알림과 같은 이름을 쓴다. {@link SseEventType} 참고. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SseEventType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    /**
     * 이 알림을 만든 교환. 교환과 무관한 알림(찔러보기 수신처럼 아직 교환이 없는 것)은 비어 있다.
     *
     * <p><b>이걸 남기는 이유는 지우기 위해서다.</b> 알림은 사람이 탭하거나 스와이프할 때만
     * 사라진다. 그래서 약속이 끝나거나 취소돼도 "시간 매칭에 실패했어요" 같은 중간 단계 알림이
     * 대기 화면에 계속 남고, 눌러 봐야 이미 없는 약속 화면으로 간다. 교환 번호가 있어야 그
     * 교환이 끝나는 순간 딸린 알림을 한 번에 읽음 처리할 수 있다.
     *
     * <p>연관관계가 아니라 값으로 둔다. 알림은 교환이 살아 있든 아니든 남는 기록이라 붙잡고
     * 있을 이유가 없고, 목록 조회가 교환까지 끌고 오게 만들 이유도 없다.
     */
    private Long exchangeId;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(User recipient, SseEventType type, String title, String body, Long exchangeId) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.exchangeId = exchangeId;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public static Notification of(
            User recipient, SseEventType type, String title, String body, Long exchangeId) {
        return new Notification(recipient, type, title, body, exchangeId);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
