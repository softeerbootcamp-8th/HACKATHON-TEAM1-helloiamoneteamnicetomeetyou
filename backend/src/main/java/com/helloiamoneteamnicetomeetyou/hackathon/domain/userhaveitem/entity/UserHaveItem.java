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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "user_have_items", indexes = {
        @Index(name = "idx_uhi_item_status", columnList = "item_id, status, quantity_left"),
        @Index(name = "idx_uhi_user_status", columnList = "user_id, status, quantity_left")
})
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

    /**
     * 매칭이 먼저 등록한 사람을 먼저 짝지어 주는 데 쓴다.
     *
     * <p>기본값을 박아 둔 것은 이미 보유 카드가 들어 있는 DB 때문이다. {@code ddl-auto: update}
     * 가 행이 있는 테이블에 {@code NOT NULL} 컬럼을 그냥 붙이려다 실패하면, 컬럼만 없는 채로
     * 서버가 떠서 매칭 쿼리가 조용히 죽는다.
     */
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserHaveItem of(User user, Item item, Integer quantity) {
        UserHaveItem userHaveItem = new UserHaveItem();
        userHaveItem.user = user;
        userHaveItem.item = item;
        userHaveItem.quantity = quantity;
        userHaveItem.status = ItemStatus.LEFT;
        userHaveItem.quantityLeft = quantity;
        userHaveItem.createdAt = LocalDateTime.now();
        return userHaveItem;
    }

    public void reserve() {
        this.status = ItemStatus.RESERVED;
    }

    /** 교환에 잡혀 있는가. 잡혀 있으면 등록을 해제할 수 없다. */
    public boolean isReserved() {
        return this.status == ItemStatus.RESERVED;
    }

    public void completeExchange(int amount) {
        this.quantityLeft -= amount;
        if (this.quantityLeft <= 0) {
            this.quantityLeft = 0;
            this.status = ItemStatus.OUT;
        } else {
            this.status = ItemStatus.LEFT;
        }
    }

    /**
     * 예약을 풀고 다시 매칭 후보로 돌려놓는다.
     *
     * <p>{@code quantityLeft} 는 건드리지 않는다. {@link #reserve()} 가 애초에 그 값을 깎지
     * 않기 때문이다(깎는 시점은 {@link #completeExchange}, 즉 실제 거래 완료 때다). 예약
     * 시점에 안 깎은 걸 여기서 더해 버리면 수량이 실제보다 부풀려진다.
     *
     * <p>{@code RESERVED} 일 때만 되돌린다. 상태를 보지 않고 {@code LEFT} 로 밀면 이미 다 나간
     * ({@code OUT}) 카드까지 되살아나서, 없는 카드가 매칭 후보로 다시 올라온다.
     */
    public void cancelReservation() {
        if (this.status != ItemStatus.RESERVED) {
            return;
        }
        this.status = this.quantityLeft > 0 ? ItemStatus.LEFT : ItemStatus.OUT;
    }

    /**
     * 수량을 바꾸고 남은 수량도 같은 만큼 움직인다.
     *
     * <p>남은 수량을 그대로 두면 어드민에서 카드를 더 붙여도 매칭에 잡히지 않는다.
     * 이미 예약 중이면 상태는 건드리지 않는다.
     */
    public void changeQuantity(Integer quantity) {
        int delta = quantity - this.quantity;
        this.quantity = quantity;
        this.quantityLeft = Math.clamp(this.quantityLeft + delta, 0, quantity);
        if (this.status != ItemStatus.RESERVED) {
            this.status = this.quantityLeft > 0 ? ItemStatus.LEFT : ItemStatus.OUT;
        }
    }
}
