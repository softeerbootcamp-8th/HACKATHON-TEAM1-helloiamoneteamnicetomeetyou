package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeParticipantRepository extends JpaRepository<ExchangeParticipant, Long> {

    @Query("SELECT ep FROM ExchangeParticipant ep JOIN FETCH ep.user WHERE ep.exchange.id = :exchangeId")
    List<ExchangeParticipant> findByExchangeId(@Param("exchangeId") Long exchangeId);

    @Query("SELECT ep FROM ExchangeParticipant ep JOIN FETCH ep.exchange WHERE ep.user.id = :userId ORDER BY ep.joinedAt DESC")
    List<ExchangeParticipant> findByUserId(@Param("userId") Long userId);

    /**
     * 이 사용자가 지금 PENDING 이나 IN_PROGRESS 인 교환에 끼어 있는지.
     *
     * <p>한 사용자는 동시에 하나의 매칭만 가져야 한다. {@code runMatching} 이 겹쳐 돌면
     * (카드 등록을 연달아 두 번 하는 경우 등) 서로 다른 카드로 서로 다른 상대와 동시에 두 건이
     * 생길 수 있는데 — 같은 물리 카드를 다투는 게 아니라서 낙관적 락으로는 안 막힌다. 매칭을
     * 시작하기 전에 여기서 먼저 걸러야 한다.
     */
    @Query("""
            SELECT COUNT(ep) > 0 FROM ExchangeParticipant ep
            WHERE ep.user.id = :userId
              AND ep.exchange.status IN ('PENDING', 'IN_PROGRESS')
            """)
    boolean existsActiveExchange(@Param("userId") UUID userId);
    /**
     * 참가자와 그 사용자를 한 번에 읽는다.
     *
     * <p>{@code fetch join} 이 없으면 참가자 수만큼 사용자 조회가 따로 나간다. 약속 화면은 이걸
     * 상대가 칸을 누를 때마다 다시 부르기 때문에 눈에 띄는 낭비가 된다.
     */
    @Query("""
            select p from ExchangeParticipant p
            join fetch p.user
            where p.exchange.id = :exchangeId
            order by p.id asc
            """)
    List<ExchangeParticipant> findAllByExchangeId(Long exchangeId);

    /** 어드민 목록에서 교환 여러 건의 참가자를 한 번에 읽는다. */
    @Query("""
            select p from ExchangeParticipant p
            join fetch p.user
            where p.exchange.id in :exchangeIds
            order by p.id asc
            """)
    List<ExchangeParticipant> findAllByExchangeIdIn(List<Long> exchangeIds);

    List<ExchangeParticipant> findByUserId(UUID userId);

    /** 부스 초기화에서 쓴다. 교환을 지우기 전에 참가자 줄부터 없애야 FK 에 걸리지 않는다. */
    void deleteByExchangeIdIn(List<Long> exchangeIds);
}
