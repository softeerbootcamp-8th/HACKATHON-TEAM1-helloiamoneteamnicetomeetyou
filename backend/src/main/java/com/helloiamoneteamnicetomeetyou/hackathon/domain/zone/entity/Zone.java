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
}
