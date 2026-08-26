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
     * 이미 수락한 참가자로 넣는다.
     *
     * <p>찔러보기가 수락된 교환은 두 사람의 뜻이 이미 확인된 상태다. 보낸 쪽은 제안할 때,
     * 받은 쪽은 카드를 고를 때 수락한 것이라 {@code PENDING} 을 거칠 일이 없다.
     */
    public static ExchangeParticipant accepted(Exchange exchange, User user) {
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

    /** 어드민이 더미 사용자 대신 거절한다. 거절 흐름도 시연에서 보여 줘야 한다. */
    public void reject() {
        this.status = ParticipantStatus.REJECTED;
    }

    public boolean hasArrived() {
        return status == ParticipantStatus.ARRIVED;
    }

    /** 약속 장소에 도착했다. 상대 화면의 "도착" 배지가 이걸 본다. */
    public void arrive() {
        this.status = ParticipantStatus.ARRIVED;
    }
}
