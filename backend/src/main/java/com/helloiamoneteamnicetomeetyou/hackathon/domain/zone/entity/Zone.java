package com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity;

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
import org.hibernate.annotations.ColumnDefault;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String location;

    /**
     * 약도 위 자리다. 약도 너비와 높이에 대한 백분율(0~100)로 둔다.
     *
     * <p><b>거리 계산에 쓰는 값이 아니다.</b> 행사장 약도 안에서 핀을 어디에 찍을지만 정한다.
     * 실제 좌표계가 아니라 그림 위 비율이라, 약도 이미지가 바뀌어도 같은 자리를 가리킨다.
     *
     * <p>화면이 아니라 DB 에 두는 것은 약속마다 만나는 자리가 다를 수 있고, 그 자리를 참가자
     * 전원이 같은 값으로 봐야 하기 때문이다. 어드민에서 구역을 늘려도 화면을 고칠 일이 없다.
     *
     * <p>기본값을 박아 두는 것은 이 컬럼을 모르는 코드가 구역을 넣을 때(어드민 화면, 손으로 쓰는
     * INSERT) 거절당하지 않게 하려는 것이다. 그 경우 약도 가운데에 겹쳐 뜨므로 자리를 정해 줘야
     * 한다는 것이 눈에 보인다.
     */
    @Column(nullable = false)
    @ColumnDefault("50")
    private int mapX = 50;

    @Column(nullable = false)
    @ColumnDefault("50")
    private int mapY = 50;

    private Zone(Booth booth, String name, String location, int mapX, int mapY) {
        this.booth = booth;
        this.name = name;
        this.location = location;
        this.mapX = mapX;
        this.mapY = mapY;
    }

    public static Zone of(Booth booth, String name, String location) {
        return new Zone(booth, name, location, 50, 50);
    }

    /** 약도 위 자리까지 정해서 만든다. 초기 데이터가 이걸 쓴다. */
    public static Zone of(Booth booth, String name, String location, int mapX, int mapY) {
        return new Zone(booth, name, location, mapX, mapY);
    }

    /** 어드민 화면에서 이름과 위치를 고친다. 소속 부스와 약도 자리는 바꾸지 않는다. */
    public void update(String name, String location) {
        this.name = name;
        this.location = location;
    }

    /** 약도 위 자리를 옮긴다. 화면 밖으로 나가지 않게 0~100 으로 가둔다. */
    public void moveOnMap(int mapX, int mapY) {
        this.mapX = Math.clamp(mapX, 0, 100);
        this.mapY = Math.clamp(mapY, 0, 100);
    }
}
