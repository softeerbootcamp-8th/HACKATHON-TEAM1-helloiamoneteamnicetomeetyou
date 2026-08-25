package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_have_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHaveItem {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @Column(nullable = false)
    private Integer quantityLeft;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserHaveItem create(User user, Item item, int quantity) {
        UserHaveItem userHaveItem = new UserHaveItem();
        userHaveItem.user = user;
        userHaveItem.item = item;
        userHaveItem.quantity = quantity;
        userHaveItem.status = ItemStatus.LEFT;
        userHaveItem.quantityLeft = quantity;
        userHaveItem.createdAt = LocalDateTime.now();
        return userHaveItem;
    }

    public void decreaseQuantityLeft(int amount) {
        this.quantityLeft -= amount;
        if (this.quantityLeft <= 0) {
            this.quantityLeft = 0;
            this.status = ItemStatus.OUT;
        }
    }
}
