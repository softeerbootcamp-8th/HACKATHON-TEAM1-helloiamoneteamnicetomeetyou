package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeItemRepository extends JpaRepository<ExchangeItem, Long> {

    /**
     * 이 카드가 오간 교환이 있는지. 카드를 지우기 전에 본다.
     *
     * <p>끝난 교환도 무엇을 주고받았는지 기록으로 들고 있어서 여기에 걸린다. 그 기록을 지우면
     * 완료된 교환이 빈 껍데기가 되므로, 카드 쪽을 못 지우게 막는 편을 골랐다.
     */
    boolean existsByItemId(Long itemId);

    /** 이 카드가 오간 교환들. 카드를 지울 때 그 교환까지 걷어내려고 먼저 찾는다. */
    @Query("SELECT DISTINCT ei.exchange.id FROM ExchangeItem ei WHERE ei.item.id = :itemId")
    List<Long> findExchangeIdsByItemId(@Param("itemId") Long itemId);

    void deleteByExchangeIdIn(List<Long> exchangeIds);

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
