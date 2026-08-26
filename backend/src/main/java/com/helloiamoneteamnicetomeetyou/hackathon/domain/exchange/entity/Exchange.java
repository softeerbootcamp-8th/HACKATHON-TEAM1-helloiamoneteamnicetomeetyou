package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
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

    /** 참가자가 거절했다. 취소 자체는 어드민이 끊는 것과 같은 상태 전이라 여기로 모은다. */
    public void cancel() {
        this.status = ExchangeStatus.CANCELLED;
    }

    /**
     * 부스 운영자가 막힌 교환을 끊는다.
     *
     * <p>시연 중에 한쪽 사람이 자리를 뜨거나 화면이 죽어서 약속이 어중간하게 남는 일이 생긴다.
     * 그 교환이 남아 있으면 같은 사람으로 다음 시연을 시작할 수 없어서, 어드민이 상태만 바꿔
     * 정리할 수 있게 열어 둔다.
     */
    public void cancelByAdmin() {
        cancel();
    }

    /** 참가자가 실물 교환을 마쳤다. */
    public void complete() {
        this.status = ExchangeStatus.COMPLETED;
    }

    /** 실물 교환은 끝났는데 화면에서 완료 처리가 안 된 건을 어드민이 닫는다. */
    public void completeByAdmin() {
        complete();
    }
}
