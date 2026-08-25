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

    private Zone(Booth booth, String name, String location) {
        this.booth = booth;
        this.name = name;
        this.location = location;
    }

    public static Zone of(Booth booth, String name, String location) {
        return new Zone(booth, name, location);
    }

    /** 어드민 화면에서 이름과 위치를 고친다. 소속 부스는 바꾸지 않는다. */
    public void update(String name, String location) {
        this.name = name;
        this.location = location;
    }
}
