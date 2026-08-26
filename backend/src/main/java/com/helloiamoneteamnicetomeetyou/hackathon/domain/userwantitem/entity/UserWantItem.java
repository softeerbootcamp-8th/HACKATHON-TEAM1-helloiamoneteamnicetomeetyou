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
     * 교환으로 이만큼 받아서 그만큼 덜 찾아도 된다.
     *
     * <p>0 이하가 됐을 때 행을 지울지는 부르는 쪽(서비스)이 정한다. 여기서는 수량만 옮긴다.
     */
    public void reduceQuantity(int amount) {
        this.quantity -= amount;
    }
}
