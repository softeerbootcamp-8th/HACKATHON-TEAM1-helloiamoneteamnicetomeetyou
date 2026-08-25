package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeParticipantRepository extends JpaRepository<ExchangeParticipant, Long> {

    @Query("SELECT ep FROM ExchangeParticipant ep JOIN FETCH ep.user WHERE ep.exchange.id = :exchangeId")
    List<ExchangeParticipant> findByExchangeId(@Param("exchangeId") Long exchangeId);

    @Query("SELECT ep FROM ExchangeParticipant ep JOIN FETCH ep.exchange WHERE ep.user.id = :userId ORDER BY ep.joinedAt DESC")
    List<ExchangeParticipant> findByUserId(@Param("userId") Long userId);
}
