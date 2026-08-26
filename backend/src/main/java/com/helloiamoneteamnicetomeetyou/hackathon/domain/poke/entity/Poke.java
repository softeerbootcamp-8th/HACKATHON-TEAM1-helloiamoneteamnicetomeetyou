package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
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

/**
 * 찔러보기 한 건. 서로 원하는 것이 맞지 않는 상대에게 보내는 단방향 제안이다.
 *
 * <p>보내는 사람은 <b>받고 싶은 카드 한 장</b>만 지정하고, 내줄 카드는 정하지 않는다. 받는 쪽이
 * 보낸 사람의 보유 카드 묶음에서 한 장을 골라야 교환이 성립한다. 그래서 요청이 저장되는 시점에는
 * {@code chosenItem} 이 비어 있다.
 *
 * <p><b>{@code Exchange} 로 대신하지 않고 따로 둔 이유가 이것이다.</b> {@code ExchangeItem} 은
 * 주는 사람·받는 사람·카드가 모두 not null 이라, 내줄 카드가 정해지기 전의 제안을 담을 수 없다.
 *
 * <p>내놓는 묶음을 여기 저장하지 않는다. 조회할 때 보낸 사람의 현재 보유 카드를 읽으면 되고,
 * 그래야 그 사이 재고가 바뀐 것이 그대로 반영된다.
 */
@Entity
@Table(name = "pokes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poke {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 찔러본 사람. 내줄 카드는 이 사람의 보유 목록에서 상대가 고른다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    /** 찔린 사람. 수락과 거절을 이 사람만 할 수 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    /** 보낸 사람이 받고 싶어 지정한 카드. 받는 사람이 가진 것이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_item_id", nullable = false)
    private Item requestedItem;

    /** 받는 사람이 보낸 사람의 묶음에서 고른 카드. 수락하기 전에는 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_item_id")
    private Item chosenItem;

    /** 수락으로 만들어진 교환. 거절이나 대기 중에는 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PokeStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    private Poke(User fromUser, User toUser, Item requestedItem) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.requestedItem = requestedItem;
        this.status = PokeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static Poke of(User fromUser, User toUser, Item requestedItem) {
        return new Poke(fromUser, toUser, requestedItem);
    }

    /** 받는 사람이 고른 카드로 수락한다. 이때 만들어진 교환을 함께 물린다. */
    public void accept(Item chosenItem, Exchange exchange) {
        this.chosenItem = chosenItem;
        this.exchange = exchange;
        this.status = PokeStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = PokeStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == PokeStatus.PENDING;
    }
}
