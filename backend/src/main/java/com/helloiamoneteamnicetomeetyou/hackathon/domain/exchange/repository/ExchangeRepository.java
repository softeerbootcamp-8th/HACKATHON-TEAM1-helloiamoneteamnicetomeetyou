package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
}
