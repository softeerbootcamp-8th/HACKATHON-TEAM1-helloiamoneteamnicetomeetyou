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

    /**
     * 교환으로 이 카드를 받았는데, 이 카드로는 처음 등록하는 행이다.
     *
     * <p>{@code quantityLeft} 를 0 으로 두고 바로 {@link ItemStatus#OUT} 으로 만든다. 받자마자
     * 재교환 후보에 올리지 않기 위해서다 — 이걸 다시 내놓고 싶으면 카드 등록 화면에서 직접
     * 등록해야 한다.
     */
    public static UserHaveItem acquired(User user, Item item, int quantity) {
        UserHaveItem userHaveItem = new UserHaveItem();
        userHaveItem.user = user;
        userHaveItem.item = item;
        userHaveItem.quantity = quantity;
        userHaveItem.quantityLeft = 0;
        userHaveItem.status = ItemStatus.OUT;
        userHaveItem.createdAt = LocalDateTime.now();
        return userHaveItem;
    }

    /**
     * 이미 이 카드 행이 있는데 교환으로 더 받았다.
     *
     * <p>{@code quantity} 만 올리고 {@code quantityLeft} 와 상태는 그대로 둔다. 이미 내놓고
     * 있던 수량이 있으면 그 거래를 건드리면 안 되고, 이미 {@link ItemStatus#OUT} 이었으면 방금
     * 받은 것도 곧바로 재교환 후보가 될 이유가 없다.
     */
    public void receiveMore(int amount) {
        this.quantity += amount;
    }

    /**
     * 이 중 {@code amount} 개를 이 순간 새로 제안할 수 없게 잠근다.
     *
     * <p><b>{@code quantityLeft} 를 그만큼 바로 깎는다.</b> 예전에는 여기서 상태만 바꾸고
     * 개수는 완료 시점에 깎았는데, 그러면 3개 중 1개만 교환에 들어가도 행 전체가
     * {@code RESERVED} 로 잠겨서 나머지 2개까지 후보 쿼리에서 사라졌다. 후보 쿼리가 이제
     * {@code status} 대신 {@code quantityLeft > 0} 을 본다.
     */
    public void reserve(int amount) {
        this.quantityLeft = Math.max(0, this.quantityLeft - amount);
        this.status = ItemStatus.RESERVED;
    }

    /** 지금 이 행에 진행 중인 예약이 하나라도 있는가. 있으면 등록을 통째로 해제할 수 없다. */
    public boolean isReserved() {
        return this.status == ItemStatus.RESERVED;
    }

    /**
     * 예약해 둔 거래가 실제로 끝났다.
     *
     * <p>{@code quantityLeft} 는 이미 {@link #reserve} 에서 깎아 뒀으니 여기서 또 깎지 않는다.
     * 상태만 지금 {@code quantityLeft} 에 맞게 정리한다.
     */
    public void completeExchange() {
        this.status = this.quantityLeft > 0 ? ItemStatus.LEFT : ItemStatus.OUT;
    }

    /**
     * 예약을 풀고 그만큼 다시 후보로 돌려놓는다.
     *
     * <p>{@link #reserve} 가 깎아 둔 만큼 {@code amount} 로 되돌려 받는다. 총 등록 수량
     * ({@code quantity}) 을 넘지 않게 잡아 둔다 — 넘으면 실제보다 부풀려진 개수가 후보에 오른다.
     */
    public void cancelReservation(int amount) {
        this.quantityLeft = Math.min(this.quantity, this.quantityLeft + amount);
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
