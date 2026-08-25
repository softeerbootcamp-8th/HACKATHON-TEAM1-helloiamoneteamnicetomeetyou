package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums.ParticipantStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
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

@Entity
@Table(name = "exchange_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ParticipantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private ExchangeParticipant(Exchange exchange, User user, ParticipantStatus status) {
        this.exchange = exchange;
        this.user = user;
        this.status = status;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * 교환에 참가자를 넣는다.
     *
     * <p>{@code ACCEPTED} 로 시작한다. 지금은 매칭 결과 화면에서 수락한 사람들만 여기까지 오기
     * 때문이다. 매칭 알고리즘이 붙어서 서버가 먼저 제안하게 되면 {@code PENDING} 으로 만들고
     * 수락을 기다리는 흐름이 필요해진다.
     */
    public static ExchangeParticipant of(Exchange exchange, User user) {
        return new ExchangeParticipant(exchange, user, ParticipantStatus.ACCEPTED);
    }

    /**
     * 어드민이 더미 사용자 대신 수락한다.
     *
     * <p>더미 사용자는 화면을 들고 있는 사람이 없어서 스스로 수락할 수가 없다. 부스에서 참가자가
     * 더미에게 찔러보기를 하면 어드민이 여기를 눌러 흐름을 이어 준다.
     */
    public void accept() {
        this.status = ParticipantStatus.ACCEPTED;
    }

    /** 어드민이 더미 사용자 대신 거절한다. 수락과 같은 이유로 열어 둔다. */
    public void reject() {
        this.status = ParticipantStatus.REJECTED;
    }

    public boolean hasArrived() {
        return status == ParticipantStatus.ARRIVED;
    }

    /** 약속 장소에 도착했다. 이미 도착했으면 아무 일도 하지 않는다. */
    public void arrive() {
        this.status = ParticipantStatus.ARRIVED;
    }
}
