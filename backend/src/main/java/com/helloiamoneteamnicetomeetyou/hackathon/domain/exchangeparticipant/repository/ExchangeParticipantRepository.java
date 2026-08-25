package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import java.util.List;
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
}
