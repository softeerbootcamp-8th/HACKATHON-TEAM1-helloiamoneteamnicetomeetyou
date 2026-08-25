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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchange_participants", indexes = {
        @Index(name = "idx_ep_user", columnList = "user_id")
})
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

    public static ExchangeParticipant create(Exchange exchange, User user) {
        ExchangeParticipant participant = new ExchangeParticipant();
        participant.exchange = exchange;
        participant.user = user;
        participant.status = ParticipantStatus.PENDING;
        participant.joinedAt = LocalDateTime.now();
        return participant;
    }
}
