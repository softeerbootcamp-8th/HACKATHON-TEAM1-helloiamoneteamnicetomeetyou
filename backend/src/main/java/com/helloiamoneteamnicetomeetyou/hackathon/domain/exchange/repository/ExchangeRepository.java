package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    /**
     * 어드민 교환 목록이다. 구역과 부스를 같이 읽는다.
     *
     * <p>{@code zone} 이 {@code LAZY} 라 fetch join 없이 목록을 그리면 행마다 구역 조회가 따로
     * 나가고, 그 구역에서 부스 이름을 꺼낼 때 한 번 더 나간다. 최신 것을 위에 둔다.
     */
    @Query("""
            select e from Exchange e
            left join fetch e.zone z
            left join fetch z.booth
            order by e.id desc
            """)
    List<Exchange> findAllForAdmin();

    List<Exchange> findByStatusOrderByIdDesc(ExchangeStatus status);
}
