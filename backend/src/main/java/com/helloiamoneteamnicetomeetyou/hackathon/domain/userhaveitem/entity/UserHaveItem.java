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

    /**
     * 진행 중인 교환에 묶여 있는 개수.
     *
     * <p><b>이 값이 없으면 "지금 몇 장 내놓겠다" 를 받아 적을 수 없다.</b> 세 숫자가 뜻이 전부
     * 다르다. {@code quantity} 는 들고 있는 개수, {@code quantityLeft} 는 그중 지금 새로 내줄 수
     * 있는 개수, 이 값은 이미 약속에 걸려 손댈 수 없는 개수다. 예전에는 묶인 몫을
     * {@code quantity - quantityLeft} 로 짐작했는데, 교환으로 받은 카드가 {@code quantity} 만
     * 올리는 바람에 그 뺄셈이 맞지 않았다.
     *
     * <p>기본값을 박아 둔 이유는 {@code createdAt} 과 같다. {@code ddl-auto: update} 가 행이 있는
     * 테이블에 {@code NOT NULL} 컬럼을 그냥 붙이려다 실패하면 컬럼 없이 서버가 떠 버린다.
     */
    @ColumnDefault("0")
    @Column(nullable = false)
    private Integer reservedQuantity;

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
        userHaveItem.reservedQuantity = 0;
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
        userHaveItem.reservedQuantity = 0;
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
     * <p><b>{@code quantityLeft} 를 그만큼 깎아 {@code reservedQuantity} 로 옮긴다.</b> 행 전체를
     * 잠그지 않는 이유는, 3개 중 1개만 교환에 들어가도 나머지 2개까지 후보 쿼리에서 사라지기
     * 때문이다. 후보 쿼리는 {@code status} 가 아니라 {@code quantityLeft > 0} 을 본다.
     *
     * <p>남은 것보다 많이 잠글 수는 없다. 넘치게 들어오면 남은 만큼만 잠근다.
     */
    public void reserve(int amount) {
        int locked = Math.clamp(amount, 0, this.quantityLeft);

        this.quantityLeft -= locked;
        this.reservedQuantity += locked;
        refreshStatus();
    }

    /** 지금 이 행에 진행 중인 예약이 하나라도 있는가. 있으면 등록을 통째로 해제할 수 없다. */
    public boolean isReserved() {
        return this.reservedQuantity > 0;
    }

    /** 내가 내놓기로 등록해 둔 개수. 묶여 있는 몫까지 합친 값이라 등록 화면이 되살릴 값이 이것이다. */
    public int getRegisteredQuantity() {
        return this.quantityLeft + this.reservedQuantity;
    }

    /**
     * 지금 내놓기로 등록해 둔 몫이 있는가. <b>사용자 화면의 내 카드에 뜨는 줄과 같은 기준이다.</b>
     *
     * <p>{@code UserHaveItemRepository.findRegisteredByUserId} 가 JPQL 로 적어 둔 조건과 같다.
     * 그쪽을 고치면 여기도 같이 고쳐야 한다.
     *
     * <p>묶인 몫을 같이 세는 것은 교환에 걸린 카드도 내가 내놓은 카드이기 때문이다. 남은 개수만
     * 보면 통째로 예약된 카드가 화면에서 사라진다. 반대로 교환으로 받기만 한 카드와 다 넘긴
     * 카드는 둘 다 0 이라 빠진다.
     */
    public boolean isRegistered() {
        return getRegisteredQuantity() > 0;
    }

    /**
     * 예약해 둔 거래가 실제로 끝났다. 카드가 손을 떠났으므로 묶여 있던 몫과 보유 개수에서 뺀다.
     *
     * <p>{@code quantityLeft} 는 {@link #reserve} 에서 이미 옮겨 뒀으니 여기서 건드리지 않는다.
     *
     * <p><b>{@code quantity} 를 같이 깎지 않으면 그 카드를 다시 등록해도 아무 일이 안 일어난다.</b>
     * 예전 {@link #changeQuantity} 가 총 수량과의 차이로 움직였기 때문인데, 지금은 등록이 내줄
     * 개수를 직접 적으므로 그 문제는 없어졌다. 그래도 넘긴 카드가 보유 개수에 남아 있으면 어드민
     * 화면이 없는 카드를 있다고 말하게 된다.
     */
    public void completeExchange(int amount) {
        int done = Math.clamp(amount, 0, this.reservedQuantity);

        this.reservedQuantity -= done;
        this.quantity = Math.max(0, this.quantity - done);
        refreshStatus();
    }

    /**
     * 예약을 풀고 그만큼 다시 후보로 돌려놓는다.
     *
     * <p>{@link #reserve} 가 옮겨 둔 만큼을 되돌려 받는다. 묶어 둔 것보다 많이 풀 수는 없다.
     * 넘치게 들어오면 묶인 만큼만 푼다. 그러지 않으면 실제보다 부풀려진 개수가 후보에 오른다.
     */
    public void cancelReservation(int amount) {
        int back = Math.clamp(amount, 0, this.reservedQuantity);

        this.reservedQuantity -= back;
        this.quantityLeft += back;
        refreshStatus();
    }

    /**
     * 내놓을 개수를 이 값으로 맞춘다. 등록 화면의 "지금 몇 장 내놓겠다" 가 그대로 들어온다.
     *
     * <p><b>차이가 아니라 값을 그대로 적는다.</b> 예전에는 예전 총 수량과의 차이로
     * {@code quantityLeft} 를 움직였는데, 그러면 개수가 그대로일 때 차이가 0 이라 아무 일도 일어나지
     * 않았다. 다 넘긴 카드를 같은 개수로 다시 등록하거나, 교환으로 받은 카드를 내놓으려 할 때
     * 눌러도 반응이 없던 것이 이것 때문이다.
     *
     * <p>묶여 있는 몫은 건드리지 않는다. 이미 상대와 약속한 카드라 등록 화면에서 뺄 수 있는
     * 것이 아니다. 보유 개수는 내놓기로 한 몫과 묶인 몫을 합친 값이 된다.
     */
    public void changeQuantity(Integer quantity) {
        this.quantityLeft = Math.max(0, quantity);
        this.quantity = this.quantityLeft + this.reservedQuantity;
        refreshStatus();
    }

    /**
     * 세 숫자가 바뀔 때마다 상태를 다시 맞춘다.
     *
     * <p>묶인 것이 하나라도 있으면 {@code RESERVED} 다. 일부만 묶여 남은 몫이 있어도 마찬가지인데,
     * 이 상태가 후보에서 빼는 기준이 아니라 "등록을 통째로 해제할 수 없다" 는 표시이기 때문이다.
     * 후보 쿼리는 {@code quantityLeft > 0} 을 따로 본다.
     */
    private void refreshStatus() {
        if (this.reservedQuantity > 0) {
            this.status = ItemStatus.RESERVED;
            return;
        }

        this.status = this.quantityLeft > 0 ? ItemStatus.LEFT : ItemStatus.OUT;
    }
}
