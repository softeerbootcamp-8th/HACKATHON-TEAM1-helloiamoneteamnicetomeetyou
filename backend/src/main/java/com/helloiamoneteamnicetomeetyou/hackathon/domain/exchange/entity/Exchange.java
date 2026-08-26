package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
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
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchanges", indexes = {
        @Index(name = "idx_ex_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExchangeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExchangeStatus status;

    private LocalDateTime exchangeTime;

    /**
     * 시간 선택 격자의 0번 칸이 가리키는 시각이다.
     *
     * <p>참가자들이 각자 자기 시계로 격자를 만들면 같은 칸 번호가 서로 다른 시각을 뜻하게 된다.
     * 이 값을 함께 내려보내서 모두가 같은 격자를 보게 한다. 자세한 것은 {@link TimeSlotGrid} 에
     * 적어 뒀다.
     *
     * <p><b>매칭이 교환을 만드는 시점에는 비어 있다.</b> 아직 제안일 뿐이라 만날 자리도 시간도
     * 없다. 참가자가 결과를 보고 장소를 잡으러 들어올 때({@link #prepareAppointment}) 채워진다.
     */
    private LocalDateTime slotBaseTime;

    /**
     * 식별 화면에서 서로를 찾는 표시다. 시안의 "레몬 28" 에서 레몬 자리다.
     *
     * <p><b>참가자 전원이 같은 값을 든다.</b> 같은 화면을 든 사람이 내 교환 상대라는 것이 그
     * 화면의 규칙이라, 사람마다 다르면 서로를 못 찾는다.
     */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int identityMark;

    /** 표시와 짝을 이루는 두 자리 번호. 둘을 합친 값이 진행 중인 교환 사이에서 겹치지 않는다. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int identityNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Exchange create(ExchangeType type) {
        Exchange exchange = new Exchange();
        exchange.type = type;
        exchange.status = ExchangeStatus.PENDING;
        exchange.createdAt = LocalDateTime.now();
        return exchange;
    }

    /**
     * 두 사람 사이의 교환을 만든다. 찔러보기가 수락된 순간이 여기다.
     *
     * <p>{@code zone} 과 {@code exchangeTime} 은 비워 둔다. 어디서 언제 만날지는 성사된 뒤에
     * 두 사람이 약속 화면에서 정하는 것이라, 만드는 시점에는 알 수 없다.
     */
    public static Exchange oneToOne() {
        return create(ExchangeType.ONE_TO_ONE);
    }

    /** 참가자가 매칭 결과를 확인하고 장소를 잡으러 들어갔다. */
    public void startProgress() {
        this.status = ExchangeStatus.IN_PROGRESS;
    }

    /** 만날 자리와 시간 격자가 정해졌는지. 매칭이 막 만든 교환은 아직 아니다. */
    public boolean hasAppointment() {
        return zone != null && slotBaseTime != null;
    }

    /**
     * 약속을 잡을 준비를 한다. 만날 자리와 격자 시작점, 약속별 식별자가 여기서 붙는다.
     *
     * <p>매칭이 교환을 만드는 시점이 아니라 참가자가 결과를 보고 들어올 때 부른다. 제안 단계의
     * 교환에까지 자리를 잡아 두면, 거절될 교환이 식별자를 물고 있게 된다.
     *
     * <p>이미 준비된 교환은 건드리지 않는다. 참가자가 둘이면 두 번 들어오는데, 뒤에 온 사람이
     * 격자를 다시 잡으면 앞 사람이 고른 칸이 다른 시각을 가리키게 된다.
     */
    public void prepareAppointment(Zone zone, LocalDateTime slotBaseTime, int mark, int number) {
        if (hasAppointment()) {
            return;
        }

        this.zone = zone;
        this.slotBaseTime = slotBaseTime;
        this.identityMark = mark;
        this.identityNumber = number;
    }

    /** 아직 끝나지 않은 교환. 식별자가 이 교환들 사이에서 겹치면 안 된다. */
    public boolean isActive() {
        return status == ExchangeStatus.PENDING || status == ExchangeStatus.IN_PROGRESS;
    }

    public boolean isTimeConfirmed() {
        return exchangeTime != null;
    }

    /**
     * 만나는 시각을 정한다.
     *
     * <p>두 사람이 "약속 확정하기" 를 거의 동시에 누를 수 있어서, 이미 정해졌으면 막는다.
     * 늦게 누른 쪽은 409 를 받고 화면을 다시 읽어 이미 정해진 시각을 보게 된다.
     */
    public void confirmTime(int slotIndex) {
        if (isTimeConfirmed()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_TIME_ALREADY_CONFIRMED);
        }

        this.exchangeTime = TimeSlotGrid.timeOf(slotBaseTime, slotIndex);
        this.status = ExchangeStatus.IN_PROGRESS;
    }

    /**
     * 만나서 교환을 끝냈다.
     *
     * <p><b>카드 주인은 아직 여기서 바꾸지 않는다.</b> 수량 조정은 이슈 #45 에서 한다.
     */
    public void complete() {
        requireActive();

        this.status = ExchangeStatus.COMPLETED;
    }

    /**
     * 이미 끝난 약속은 다시 끝내거나 취소할 수 없다.
     *
     * <p><b>두 사람이 서로 다른 버튼을 누를 수 있어서 필요하다.</b> 한 명이 "만났어요" 를 누르고
     * 다른 한 명이 "상대가 오지 않아요" 를 누르면 둘 중 먼저 도착한 것만 반영돼야 한다.
     */
    /**
     * 만나는 자리를 옮긴다.
     *
     * <p>시간이 정해진 뒤에도 바꿀 수 있다. 장소와 시간은 서로 매인 값이 아니고, 현장에서 자리가
     * 붐비거나 운영이 구역을 옮기면 시간을 그대로 둔 채 자리만 바꾸는 쪽이 자연스럽다.
     *
     * <p>끝났거나 취소된 약속은 건드리지 않는다. 이미 헤어진 사람들에게 자리를 알려 봐야 의미가
     * 없고, 화면에는 지나간 약속이 되살아난 것처럼 보인다.
     */
    public void changeZone(Zone zone) {
        requireActive();

        this.zone = zone;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_ALREADY_FINISHED);
        }
    }

    /**
     * 참가자가 거절하거나 약속을 취소했다.
     *
     * <p>이미 끝난 약속은 막는다. 한쪽이 "만났어요" 를 누른 뒤 다른 쪽이 취소를 누르는 경우가
     * 있는데, 먼저 도착한 것만 반영돼야 한다. 어드민이 끊는 길은 {@link #cancelByAdmin} 이고
     * 그쪽은 막힌 것을 치우는 게 목적이라 상태를 따지지 않는다.
     */
    public void cancel() {
        requireActive();

        this.status = ExchangeStatus.CANCELLED;
        this.exchangeTime = null;
    }

    /**
     * 부스 운영자가 막힌 교환을 끊는다.
     *
     * <p>시연 중에 한쪽 사람이 자리를 뜨거나 화면이 죽어서 약속이 어중간하게 남는 일이 생긴다.
     * 그 교환이 남아 있으면 같은 사람으로 다음 시연을 시작할 수 없어서, 어드민이 상태만 바꿔
     * 정리할 수 있게 열어 둔다.
     */
    public void cancelByAdmin() {
        this.status = ExchangeStatus.CANCELLED;
    }

    /** 실물 교환은 끝났는데 화면에서 완료 처리가 안 된 건을 어드민이 닫는다. */
    public void completeByAdmin() {
        this.status = ExchangeStatus.COMPLETED;
    }
}
