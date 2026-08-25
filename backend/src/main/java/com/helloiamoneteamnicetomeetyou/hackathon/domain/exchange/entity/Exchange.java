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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchanges")
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

    /**
     * 시간 선택 격자의 0번 칸이 가리키는 시각이다. 교환을 만들 때 서버가 한 번 정한다.
     *
     * <p>참가자들이 각자 자기 시계로 격자를 만들면 같은 칸 번호가 서로 다른 시각을 뜻하게 된다.
     * 이 값을 함께 내려보내서 모두가 같은 격자를 보게 한다. 자세한 것은 {@link TimeSlotGrid} 에
     * 적어 뒀다.
     */
    @Column(nullable = false)
    private LocalDateTime slotBaseTime;

    /** 확정된 만나는 시각. 아직 안 정해졌으면 null 이다. */
    private LocalDateTime exchangeTime;

    /**
     * 식별 화면에서 서로를 찾는 표시다. 시안의 "레몬 28" 에서 레몬 자리다.
     *
     * <p><b>참가자 전원이 같은 값을 든다.</b> 같은 화면을 든 사람이 내 교환 상대라는 것이 이 화면의
     * 규칙이라, 사람마다 다르면 서로를 못 찾는다.
     *
     * <p>화면은 이 번호로 그림과 색을 고른다. 어떤 그림인지는 서버가 정하지 않는다. 지도 위 핀
     * 좌표를 화면이 정하는 것과 같은 이유로, 표시 방법이 바뀌어도 서버를 안 고치게 하려는 것이다.
     */
    @Column(nullable = false)
    private int identityMark;

    /**
     * 표시와 짝을 이루는 두 자리 번호다. 시안의 "레몬 28" 에서 28 자리다.
     *
     * <p>표시만으로는 가짓수가 모자라서 번호를 붙인다. 둘을 합친 값이 <b>진행 중인 교환 사이에서
     * 겹치지 않아야</b> 한다. 겹치면 행사장에서 엉뚱한 사람과 서로를 상대로 착각한다.
     */
    @Column(nullable = false)
    private int identityNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Exchange(Zone zone, ExchangeType type, LocalDateTime slotBaseTime) {
        this.zone = zone;
        this.type = type;
        this.status = ExchangeStatus.PENDING;
        this.slotBaseTime = slotBaseTime;
        this.createdAt = LocalDateTime.now();
    }

    public static Exchange of(Zone zone, ExchangeType type, LocalDateTime slotBaseTime) {
        return new Exchange(zone, type, slotBaseTime);
    }

    /** 식별자는 이미 쓰이고 있는 것을 봐야 정해지므로 서비스가 골라서 넣어 준다. */
    public void assignIdentity(int mark, int number) {
        this.identityMark = mark;
        this.identityNumber = number;
    }

    /** 아직 만나지 않은 교환. 식별자가 이 교환들 사이에서 겹치면 안 된다. */
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
     * 약속을 취소한다.
     *
     * <p>지우지 않고 상태만 바꾸는 것은, 상대 화면이 "취소됐다"를 읽을 수 있어야 하기 때문이다.
     * 행이 사라지면 상대는 404 만 받고 왜 없어졌는지 알 수 없다.
     */
    public void cancel() {
        requireActive();

        this.status = ExchangeStatus.CANCELLED;
        this.exchangeTime = null;
    }

    /**
     * 만나서 교환을 끝냈다.
     *
     * <p><b>카드 주인은 아직 여기서 바꾸지 않는다.</b> 누가 무엇을 주고받는지는 매칭(이슈 #20)이
     * 정하는 값이라 서버가 모른다. 지금은 "이 약속은 끝났다"만 남긴다.
     */
    public void complete() {
        requireActive();

        this.status = ExchangeStatus.COMPLETED;
    }

    /**
     * 이미 끝난 약속은 다시 끝내거나 취소할 수 없다.
     *
     * <p><b>두 사람이 서로 다른 버튼을 누를 수 있어서 필요하다.</b> 한 명이 "만났어요" 를 누르고
     * 다른 한 명이 "상대가 오지 않아요" 를 누르면 둘 중 먼저 도착한 것만 반영돼야 하고, 늦은
     * 쪽은 이미 정해진 결과를 받아 화면을 맞춰야 한다.
     */
    private void requireActive() {
        if (!isActive()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_ALREADY_FINISHED);
        }
    }

    /**
     * 시간을 처음부터 다시 고른다. 겹치는 칸이 없어서 조율을 요청할 때 부른다.
     *
     * <p>격자 시작점도 지금 기준으로 다시 잡는다. 그러지 않으면 한참 지난 뒤에 조율을 요청했을 때
     * 이미 지나간 시각이 선택지로 남는다.
     */
    public void resetTime() {
        this.exchangeTime = null;
        this.status = ExchangeStatus.PENDING;
        this.slotBaseTime = TimeSlotGrid.baseTimeFrom(LocalDateTime.now());
    }
}
