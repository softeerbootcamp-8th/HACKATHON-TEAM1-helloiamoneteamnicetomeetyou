package com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /** 부스 안의 구역을 id 순으로 준다. 화면의 핀 배치가 이 순서에 기댄다. */
    List<Zone> findByBoothIdOrderByIdAsc(Long boothId);
}
