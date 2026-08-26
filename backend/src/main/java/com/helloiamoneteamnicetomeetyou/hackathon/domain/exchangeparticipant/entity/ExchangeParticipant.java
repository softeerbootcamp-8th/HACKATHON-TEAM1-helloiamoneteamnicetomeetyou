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
import org.hibernate.annotations.ColumnDefault;

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

    /**
     * 이 사람이 "이 시간으로 약속!" 을 눌렀는지.
     *
     * <p><b>{@code status} 에 넣지 않고 따로 둔다.</b> 시간을 확정한 사람은 그 뒤에 약속 장소에
     * 도착해서 {@code ARRIVED} 가 되는데, 한 칸짜리 상태로 두면 도착하는 순간 확정했다는 사실이
     * 지워진다. 둘은 같은 줄에서 함께 참인 값이라 서로 덮으면 안 된다.
     *
     * <p>전원이 눌러야 {@code Exchange.exchangeTime} 이 정해진다. 겹치는 가장 빠른 칸이 바뀌면
     * 전원의 이 값이 도로 내려간다({@link #cancelTimeConfirm}).
     */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean timeConfirmed;

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

    public static ExchangeParticipant create(Exchange exchange, User user) {
        ExchangeParticipant participant = new ExchangeParticipant();
        participant.exchange = exchange;
        participant.user = user;
        participant.status = ParticipantStatus.PENDING;
        participant.joinedAt = LocalDateTime.now();
        return participant;
    }

    /** "이 시간으로 약속!" 을 눌렀다. 참가자 전원이 누르면 그때 교환의 시각이 정해진다. */
    public void confirmTime() {
        this.timeConfirmed = true;
    }

    /**
     * 눌러 둔 확정을 되돌린다. 겹치는 가장 빠른 칸이 옮겨갔을 때 부른다.
     *
     * <p>동의는 그때 화면에 보이던 시각에 대한 것이다. 칸이 옮겨간 뒤에도 남겨 두면, 마지막
     * 사람이 누르는 순간 아무도 본 적 없는 시각으로 약속이 잡힌다.
     */
    public void cancelTimeConfirm() {
        this.timeConfirmed = false;
    }

    public boolean hasArrived() {
        return status == ParticipantStatus.ARRIVED;
    }

    /** 약속 장소에 도착했다. 상대 화면의 "도착" 배지가 이걸 본다. */
    public void arrive() {
        this.status = ParticipantStatus.ARRIVED;
    }
}
