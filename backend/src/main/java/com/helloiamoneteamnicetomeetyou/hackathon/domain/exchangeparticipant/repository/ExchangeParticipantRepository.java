package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeParticipantRepository extends JpaRepository<ExchangeParticipant, Long> {

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
