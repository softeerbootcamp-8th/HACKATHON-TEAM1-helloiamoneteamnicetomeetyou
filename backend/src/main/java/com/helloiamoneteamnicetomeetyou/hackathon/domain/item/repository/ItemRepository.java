package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
