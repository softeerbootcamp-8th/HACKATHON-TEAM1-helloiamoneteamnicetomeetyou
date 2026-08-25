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
}
