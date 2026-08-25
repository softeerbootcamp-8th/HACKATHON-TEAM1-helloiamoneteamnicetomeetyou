package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeItemRepository extends JpaRepository<ExchangeItem, Long> {

    void deleteByExchangeIdIn(List<Long> exchangeIds);
}
