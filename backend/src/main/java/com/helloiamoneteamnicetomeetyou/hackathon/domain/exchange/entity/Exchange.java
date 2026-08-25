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
}
