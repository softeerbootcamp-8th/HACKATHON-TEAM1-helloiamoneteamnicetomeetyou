package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_want_items", indexes = {
        @Index(name = "idx_uwi_item_user", columnList = "item_id, user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWantItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 몇 장을 찾는지. 보유 카드와 마찬가지로 1 이상이다. */
    @Column(nullable = false)
    private Integer quantity;

    private UserWantItem(User user, Item item, Integer quantity) {
        this.user = user;
        this.item = item;
        this.quantity = quantity;
    }

    public static UserWantItem of(User user, Item item, Integer quantity) {
        return new UserWantItem(user, item, quantity);
    }

    /** 희망 카드는 수량 개념이 없어서 항상 1 로 둔다. */
    public static UserWantItem of(User user, Item item) {
        UserWantItem userWantItem = new UserWantItem();
        userWantItem.user = user;
        userWantItem.item = item;
        userWantItem.quantity = 1;
        return userWantItem;
    }

    /** 같은 (user, item) 으로 다시 등록하면 찾는 개수를 이 값으로 덮어쓴다. */
    public void changeQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 교환으로 이 카드를 받았다. 그만큼 찾는 개수를 줄인다.
     *
     * <p>줄이지 않으면 이미 받은 카드를 계속 원하는 사람으로 남아, 교환이 끝나자마자 같은
     * 카드로 다시 매칭된다.
     *
     * @return 더 찾을 것이 없어져서 이 줄을 지워야 하면 true
     */
    public boolean decrease(int amount) {
        this.quantity -= amount;
        if (this.quantity <= 0) {
            this.quantity = 0;
            return true;
        }
        return false;
    }
}
