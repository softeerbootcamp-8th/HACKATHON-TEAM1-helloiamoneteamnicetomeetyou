package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchange_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    private ExchangeItem(Exchange exchange, User fromUser, Item item, User toUser) {
        this.exchange = exchange;
        this.fromUser = fromUser;
        this.item = item;
        this.toUser = toUser;
    }

    /**
     * "누가 누구에게 어떤 카드를" 한 줄이다. 1:1 교환이면 방향이 다른 두 줄이 생긴다.
     *
     * <p>인자 순서가 그대로 문장이 되게 뒀다. 주는 사람과 받는 사람을 뒤집어 넣으면 화면에는
     * 그럴듯하게 보이지만 실제 교환이 반대로 기록된다.
     */
    public static ExchangeItem of(Exchange exchange, User fromUser, Item item, User toUser) {
        return new ExchangeItem(exchange, fromUser, item, toUser);
    }
}
