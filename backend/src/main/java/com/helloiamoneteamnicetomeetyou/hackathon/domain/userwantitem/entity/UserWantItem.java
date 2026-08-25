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

    @Column(nullable = false)
    private Integer quantity;

    public static UserWantItem create(User user, Item item, int quantity) {
        UserWantItem userWantItem = new UserWantItem();
        userWantItem.user = user;
        userWantItem.item = item;
        userWantItem.quantity = quantity;
        return userWantItem;
    }

    private UserWantItem(User user, Item item) {
        this.user = user;
        this.item = item;
    }

    public static UserWantItem of(User user, Item item) {
        return new UserWantItem(user, item);
    }
}
