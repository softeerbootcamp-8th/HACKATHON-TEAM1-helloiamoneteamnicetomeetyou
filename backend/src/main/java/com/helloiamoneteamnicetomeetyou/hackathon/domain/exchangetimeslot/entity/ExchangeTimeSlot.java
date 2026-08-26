package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가자 한 명이 고른 칸 하나다. 세 칸을 골랐으면 행이 세 개다.
 *
 * <p><b>서버 메모리가 아니라 DB 에 둔다.</b> 배포가 {@code docker stop} 으로 컨테이너를 바꾸는
 * 방식이라, 메모리에 두면 배포할 때마다 진행 중인 약속의 선택이 통째로 사라진다. 해커톤 기간에
 * 하루에도 여러 번 배포하기 때문에 실제로 겪게 되는 일이다.
 *
 * <p>칸을 조건으로 조회할 일이 없어서 목록을 한 컬럼에 문자열로 넣는 방법도 있었지만, 그러면
 * 한 사람의 선택을 지우고 다시 넣는 것이 문자열 파싱이 된다. 행으로 두면 지우고 넣는 것으로 끝난다.
 */
@Entity
@Table(
        name = "exchange_time_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exchange_user_slot",
                columnNames = {"exchange_id", "user_id", "slot_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 격자에서의 자리. 실제 시각은 {@code exchange.slotBaseTime} 과 함께 봐야 나온다. */
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    private ExchangeTimeSlot(Exchange exchange, User user, int slotIndex) {
        this.exchange = exchange;
        this.user = user;
        this.slotIndex = slotIndex;
    }

    public static ExchangeTimeSlot of(Exchange exchange, User user, int slotIndex) {
        return new ExchangeTimeSlot(exchange, user, slotIndex);
    }
}
