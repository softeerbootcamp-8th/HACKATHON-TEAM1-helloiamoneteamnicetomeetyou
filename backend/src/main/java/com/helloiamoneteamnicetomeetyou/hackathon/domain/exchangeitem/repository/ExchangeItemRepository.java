package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeItemRepository extends JpaRepository<ExchangeItem, Long> {

    @Query("""
        SELECT ei FROM ExchangeItem ei
        JOIN FETCH ei.fromUser
        JOIN FETCH ei.item
        JOIN FETCH ei.toUser
        WHERE ei.exchange.id = :exchangeId
    """)
    List<ExchangeItem> findByExchangeId(@Param("exchangeId") Long exchangeId);

    /**
     * 교환에서 오가는 카드 줄이다. 카드와 양쪽 사람을 함께 읽는다.
     *
     * <p>fetch join 이 없으면 줄마다 카드 조회와 사람 조회가 따로 나간다.
     */
    @Query("""
            select ei from ExchangeItem ei
            join fetch ei.item
            join fetch ei.fromUser
            join fetch ei.toUser
            where ei.exchange.id = :exchangeId
            order by ei.id asc
            """)
    List<ExchangeItem> findAllByExchangeId(Long exchangeId);
}
