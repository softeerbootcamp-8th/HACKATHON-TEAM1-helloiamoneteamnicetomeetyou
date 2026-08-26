package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import jakarta.persistence.Column;
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
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    private Item(Booth booth, String name, String description) {
        this.booth = booth;
        this.name = name;
        this.description = description;
    }

    public static Item of(Booth booth, String name, String description) {
        return new Item(booth, name, description);
    }

    /**
     * 어드민 화면에서 카드를 추가할 때 쓴다.
     *
     * <p>이미지가 아직 없는 카드가 대부분이라 {@code imageUrl} 은 비어 있을 수 있고, 화면은
     * 그때 프론트와 같은 그라데이션 썸네일을 그린다.
     */
    public static Item of(Booth booth, String name, String description, String imageUrl) {
        Item item = new Item(booth, name, description);
        item.imageUrl = imageUrl;
        return item;
    }

    /**
     * 그림이 비어 있던 카드에 주소를 붙인다. 시더가 이미 들어 있는 DB 를 채울 때 쓴다.
     *
     * <p>{@link #update} 를 쓰지 않는 것은 그쪽이 이름과 설명까지 같이 덮어쓰기 때문이다.
     * 운영자가 어드민에서 고쳐 둔 이름을 시더가 되돌리면 안 된다.
     */
    public void attachImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** 어드민 화면에서 카드 정보를 고친다. 소속 부스는 바꾸지 않는다. */
    public void update(String name, String description, String imageUrl) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
